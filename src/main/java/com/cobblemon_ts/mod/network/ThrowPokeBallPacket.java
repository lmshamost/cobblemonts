package com.cobblemon_ts.mod.network;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.item.PokeBallItem;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.PokebagManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

// [线性-B05ze] 直接投掷精灵球包：跳过gameMode.useItem()→ServerboundUseItemPacket→PokeBallItem.use()链条
// 客户端→服务端：携带itemId，服务端手动创建EmptyPokeBallEntity瞄准对手精灵投掷
public record ThrowPokeBallPacket(String itemId) implements CustomPacketPayload {

    public static final Type<ThrowPokeBallPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "throw_poke_ball"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ThrowPokeBallPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ThrowPokeBallPacket decode(RegistryFriendlyByteBuf buf) {
            return new ThrowPokeBallPacket(buf.readUtf());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ThrowPokeBallPacket packet) {
            buf.writeUtf(packet.itemId());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // [线性-B05ze] 反射缓存：throwPokeBall() 私有方法
    private static Method cachedThrowPokeBall;
    private static volatile boolean throwReflectionInitFailed;

    private static Method getThrowPokeBallMethod() {
        if (throwReflectionInitFailed) return null;
        if (cachedThrowPokeBall != null) return cachedThrowPokeBall;
        try {
            cachedThrowPokeBall = PokeBallItem.class.getDeclaredMethod(
                    "throwPokeBall", net.minecraft.world.level.Level.class, ServerPlayer.class);
            cachedThrowPokeBall.setAccessible(true);
            return cachedThrowPokeBall;
        } catch (Throwable t) {
            throwReflectionInitFailed = true;
            CobblemonTS.LOGGER.error("getThrowPokeBallMethod failed: {}", t.getMessage());
            return null;
        }
    }

    // [线性-B05zg] 反射缓存：EmptyPokeBallEntity构造函数(PokeBall, Level, LivingEntity, EntityType, int, DefaultConstructorMarker)
    // 通过参数数量匹配而非直接引用Kotlin类型，避免编译时依赖kotlin.jvm.internal
    private static Constructor<EmptyPokeBallEntity> cachedBallEntityCtor;
    private static volatile boolean ballEntityCtorInitFailed;

    @SuppressWarnings("unchecked")
    private static Constructor<EmptyPokeBallEntity> getBallEntityConstructor() {
        if (ballEntityCtorInitFailed) return null;
        if (cachedBallEntityCtor != null) return cachedBallEntityCtor;
        try {
            for (Constructor<?> ctor : EmptyPokeBallEntity.class.getDeclaredConstructors()) {
                if (ctor.getParameterCount() == 6) {
                    ctor.setAccessible(true);
                    cachedBallEntityCtor = (Constructor<EmptyPokeBallEntity>) ctor;
                    return cachedBallEntityCtor;
                }
            }
        } catch (Throwable t) {
            CobblemonTS.LOGGER.error("getBallEntityConstructor failed: {}", t.getMessage());
        }
        ballEntityCtorInitFailed = true;
        return null;
    }

    // [线性-B05zh] 反射缓存：onHitEntity(EntityHitResult) protected方法
    // 等价于 Pixelmon 的 useFromBag(user, target, itemStack) 中直接传入目标精灵
    private static Method cachedOnHitEntity;
    private static volatile boolean onHitEntityInitFailed;

    private static Method getOnHitEntityMethod() {
        if (onHitEntityInitFailed) return null;
        if (cachedOnHitEntity != null) return cachedOnHitEntity;
        try {
            cachedOnHitEntity = EmptyPokeBallEntity.class.getDeclaredMethod(
                    "onHitEntity", EntityHitResult.class);
            cachedOnHitEntity.setAccessible(true);
            return cachedOnHitEntity;
        } catch (Throwable t) {
            onHitEntityInitFailed = true;
            CobblemonTS.LOGGER.error("getOnHitEntityMethod failed: {}", t.getMessage());
            return null;
        }
    }

    // [线性-B05zh] 获取对手PokemonEntity（战斗中）
    // 等价于 Pixelmon 的 useFromBag 参数中的 target PixelmonWrapper
    private static PokemonEntity getOpponentEntity(ServerPlayer serverPlayer) {
        PokemonBattle battle = BattleRegistry.getBattleByParticipatingPlayer(serverPlayer);
        if (battle == null) return null;

        BattleActor playerActor = battle.getActor(serverPlayer);
        for (BattleActor actor : battle.getActors()) {
            if (actor == playerActor) continue;
            for (ActiveBattlePokemon active : actor.getActivePokemon()) {
                if (active.hasPokemon() && active.isAlive()) {
                    if (active.getBattlePokemon().getEntity() != null) {
                        return active.getBattlePokemon().getEntity();
                    }
                }
            }
        }
        return null;
    }

    // [线性-B05zh] 创建精灵球实体→定位到对手碰撞箱→addFreshEntity→反射调onHitEntity触发捕获
    // 等价于 Pixelmon: new EmptyPokeBallEntity(level, player, TARGET, pokeBall, battle) 后tick中追踪目标
    // Cobblemon没有传入目标的构造器，故通过onHitEntity直接触发捕获，跳过飞行阶段
    private static void throwBallAtOpponent(PokeBallItem pokeBallItem, ServerPlayer serverPlayer) {
        PokemonEntity opponentEntity = getOpponentEntity(serverPlayer);

        if (opponentEntity == null) {
            // [线性-B05zh] 非战斗：回退到反射调用throwPokeBall（原版行为：玩家朝向投掷）
            Method m = getThrowPokeBallMethod();
            if (m == null) return;
            try {
                m.invoke(pokeBallItem, serverPlayer.level(), serverPlayer);
            } catch (Exception e) {
                CobblemonTS.LOGGER.error("throwPokeBall fallback failed: {}", e.getMessage());
            }
            return;
        }

        Constructor<EmptyPokeBallEntity> ctor = getBallEntityConstructor();
        if (ctor == null) {
            // 回退到throwPokeBall
            Method m = getThrowPokeBallMethod();
            if (m == null) return;
            try {
                m.invoke(pokeBallItem, serverPlayer.level(), serverPlayer);
            } catch (Exception e) {
                CobblemonTS.LOGGER.error("throwPokeBall fallback failed: {}", e.getMessage());
            }
            return;
        }

        PokeBall pokeBall = pokeBallItem.getPokeBall();

        EmptyPokeBallEntity ballEntity;
        try {
            // [线性-B05zh] 构造器参数：(PokeBall, Level, LivingEntity, null, 8, null)
            ballEntity = ctor.newInstance(pokeBall, serverPlayer.level(), serverPlayer, null, 8, null);
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("EmptyPokeBallEntity construct failed: {}", e.getMessage());
            return;
        }

        // [线性-B05zh] 精灵球定位到对手碰撞箱中心（等价于Pixelmon的getBeamTarget()返回getPairedEntity().getBoundingBox().getCenter()）
        Vec3 targetPos = opponentEntity.getBoundingBox().getCenter();
        ballEntity.setPos(targetPos.x, targetPos.y, targetPos.z);

        serverPlayer.level().addFreshEntity(ballEntity);

        // [线性-B05zh] 构建EntityHitResult→反射调用onHitEntity触发捕获流程
        // onHitEntity内部检查: captureState==NOT ✓, hitEntity instanceof PokemonEntity ✓, isServerSide ✓ → 执行captureState=HIT→attemptCatch
        EntityHitResult hitResult = new EntityHitResult(opponentEntity, targetPos);
        Method onHitEntity = getOnHitEntityMethod();
        if (onHitEntity != null) {
            try {
                onHitEntity.invoke(ballEntity, hitResult);
            } catch (Exception e) {
                CobblemonTS.LOGGER.error("onHitEntity invoke failed: {}", e.getMessage());
            }
        }
    }

    // [线性-B05zg] 服务端处理：先搜玩家背包，再搜Pokebag库存，手动创建实体瞄准对手投掷
    public static void handleServerSide(ThrowPokeBallPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;

            // [线性-B05ze] 步骤1：遍历玩家主背包
            for (int i = 0; i < serverPlayer.getInventory().items.size(); i++) {
                ItemStack stack = serverPlayer.getInventory().items.get(i);
                if (stack.isEmpty()) continue;
                if (!(stack.getItem() instanceof PokeBallItem pokeBallItem)) continue;

                String stackId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(stack.getItem()).toString();
                if (!stackId.equals(packet.itemId())) continue;

                throwBallAtOpponent(pokeBallItem, serverPlayer);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    serverPlayer.getInventory().items.set(i, ItemStack.EMPTY);
                }
                return;
            }

            // [线性-B05zf] 步骤2：遍历Pokebag库存
            PokebagManager manager = CharmPlayerData.get(serverPlayer).getPokebagManager();
            SimpleContainer bagInv = manager.getBagInventory();
            for (int i = 0; i < bagInv.getContainerSize(); i++) {
                ItemStack stack = bagInv.getItem(i);
                if (stack.isEmpty()) continue;
                if (!(stack.getItem() instanceof PokeBallItem pokeBallItem)) continue;

                String stackId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(stack.getItem()).toString();
                if (!stackId.equals(packet.itemId())) continue;

                throwBallAtOpponent(pokeBallItem, serverPlayer);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    bagInv.setItem(i, ItemStack.EMPTY);
                }
                return;
            }

            CobblemonTS.LOGGER.warn("ThrowPokeBallPacket: 玩家{}背包/Pokebag中均未找到{}",
                    serverPlayer.getName().getString(), packet.itemId());
        });
    }
}