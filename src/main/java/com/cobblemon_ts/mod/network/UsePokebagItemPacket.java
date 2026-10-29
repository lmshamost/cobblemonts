package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.SecurityHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// [线性-C02] 使用道具袋物品包：客户端→服务端
// 逻辑：先尝试使用物品（调用Cobblemon原生useItem逻辑），使用成功后才从道具袋扣除
// 如果物品无法使用（如满血精灵使用药水），则不扣除，无反应
public record UsePokebagItemPacket(int partySlot, String itemId, int count) implements CustomPacketPayload {

    // [线性-O01] 反射缓存：服务端处理包时高频使用，必须静态缓存避免GC压力
    private static Class<?> cachedCobblemonClass;
    private static Object cachedCobblemonInstance;
    private static Method cachedGetStorageMethod;
    private static Method cachedGetPartyMethod;
    private static Class<?> cachedPokemonClass;
    private static Method cachedPokemonGetMethod;
    private static Method cachedPokemonGetUuidMethod;
    // [线性-O01a] tryUseItemOnPokemon 治疗/状态反射缓存（避免每次调用getMethod）
    private static Method cachedGetCurrentHealth;
    private static Method cachedGetMaxHealth;
    private static Method cachedSetCurrentHealth;
    private static Method cachedHeal;
    private static Method cachedIsFainted;
    private static Method cachedGetStatus;
    private static Method cachedSetStatus;
    private static Class<?> cachedPersistentStatusContainerClass;
    // [线性-O01b] 金色王冠反射缓存
    private static Method cachedGetIvs;
    private static Method cachedIsHyperTrained;
    private static Object[] cachedAllStats;
    private static Field cachedHyperTrainedIVsField;
    private static Method cachedPokemonUpdateMethod;
    private static Class<?> cachedServerPlayerClass;
    private static boolean reflectionInitialized;

    public static final Type<UsePokebagItemPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "use_pokebag_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UsePokebagItemPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public UsePokebagItemPacket decode(RegistryFriendlyByteBuf buf) {
            return new UsePokebagItemPacket(
                    buf.readVarInt(),
                    buf.readUtf(),
                    buf.readVarInt()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, UsePokebagItemPacket packet) {
            buf.writeVarInt(packet.partySlot());
            buf.writeUtf(packet.itemId());
            buf.writeVarInt(packet.count());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // [线性-O01] 初始化反射缓存（线程安全，只执行一次）
    private static synchronized void initReflection() {
        if (reflectionInitialized) return;
        try {
            cachedCobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            cachedCobblemonInstance = cachedCobblemonClass.getDeclaredField("INSTANCE").get(null);
            cachedGetStorageMethod = cachedCobblemonInstance.getClass().getMethod("getStorage");

            cachedServerPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
            cachedGetPartyMethod = cachedGetStorageMethod.getReturnType().getMethod("getParty", cachedServerPlayerClass);

            cachedPokemonClass = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
            cachedPokemonGetUuidMethod = cachedPokemonClass.getMethod("getUuid");

            // [线性-O01a] 缓存治疗/状态反射方法
            cachedGetCurrentHealth = cachedPokemonClass.getMethod("getCurrentHealth");
            cachedGetMaxHealth = cachedPokemonClass.getMethod("getMaxHealth");
            cachedSetCurrentHealth = cachedPokemonClass.getMethod("setCurrentHealth", int.class);
            cachedHeal = cachedPokemonClass.getMethod("heal");
            cachedIsFainted = cachedPokemonClass.getMethod("isFainted");
            cachedGetStatus = cachedPokemonClass.getMethod("getStatus");
            cachedPersistentStatusContainerClass = Class.forName(
                    "com.cobblemon.mod.common.pokemon.status.PersistentStatusContainer");
            cachedSetStatus = cachedPokemonClass.getMethod("setStatus", cachedPersistentStatusContainerClass);

            // [线性-O01b] 缓存金色王冠反射：IVs/Stats/hyperTrainedIVs
            cachedGetIvs = cachedPokemonClass.getMethod("getIvs");
            var ivsClass = cachedGetIvs.getReturnType();
            cachedIsHyperTrained = ivsClass.getMethod("isHyperTrained",
                    Class.forName("com.cobblemon.mod.common.api.pokemon.stats.Stat"));
            var statsClass = Class.forName("com.cobblemon.mod.common.api.pokemon.stats.Stats");
            cachedAllStats = new Object[] {
                statsClass.getField("HP").get(null),
                statsClass.getField("ATTACK").get(null),
                statsClass.getField("DEFENCE").get(null),
                statsClass.getField("SPECIAL_ATTACK").get(null),
                statsClass.getField("SPECIAL_DEFENCE").get(null),
                statsClass.getField("SPEED").get(null)
            };
            cachedHyperTrainedIVsField = ivsClass.getDeclaredField("hyperTrainedIVs");
            cachedHyperTrainedIVsField.setAccessible(true);
            cachedPokemonUpdateMethod = cachedPokemonClass.getMethod("update");

            reflectionInitialized = true;
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("[UsePokebagItem] 反射缓存初始化失败", e);
        }
    }

    public static void handleServerSide(UsePokebagItemPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;

            // [线性-SEC] 权限校验：仅创造模式或OP
            if (!SecurityHelper.isTrusted(serverPlayer)) {
                CobblemonTS.LOGGER.warn("[Security] Unauthorized UsePokebagItem from {}",
                        serverPlayer.getName().getString());
                return;
            }

            // [线性-O01] 确保反射缓存已初始化
            if (!reflectionInitialized) initReflection();
            if (cachedCobblemonInstance == null || cachedGetPartyMethod == null) return;

            try {
                // [线性-O01] 1. 获取目标精灵（使用缓存的反射）
                var storage = cachedGetStorageMethod.invoke(cachedCobblemonInstance);
                var party = cachedGetPartyMethod.invoke(storage, serverPlayer);
                if (party == null) return;

                var getMethod = party.getClass().getMethod("get", int.class);
                var pokemon = getMethod.invoke(party, packet.partySlot());
                if (pokemon == null) return;

                var uuid = (UUID) cachedPokemonGetUuidMethod.invoke(pokemon);

                // [线性-O01] 2. 构造临时ItemStack（用于尝试使用）
                var itemOpt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(packet.itemId()));
                if (itemOpt.isEmpty()) return;
                ItemStack tempStack = new ItemStack(itemOpt.get(), packet.count());

                // [线性-O01] 3. 先尝试使用物品
                boolean useSuccess = tryUseItemOnPokemon(serverPlayer, pokemon, tempStack, packet);

                if (!useSuccess) {
                    CobblemonTS.LOGGER.info("[UsePokebagItem] 物品 {} 无法使用在精灵槽位 {}，不扣除", packet.itemId(), packet.partySlot());
                    return; // 使用失败，不扣除道具袋物品
                }

                // [线性-O01] 4. 使用成功，从道具袋扣除物品
                var playerData = com.cobblemon_ts.mod.CharmPlayerData.get(serverPlayer);
                var bagManager = playerData.getPokebagManager();
                if (bagManager == null) return;

                var inventory = bagManager.getBagInventory();
                int remainingToConsume = packet.count();
                for (int i = 0; i < inventory.getContainerSize() && remainingToConsume > 0; i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (stack.isEmpty()) continue;
                    String stackId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    if (stackId.equals(packet.itemId())) {
                        int consume = Math.min(remainingToConsume, stack.getCount());
                        stack.shrink(consume);
                        remainingToConsume -= consume;
                        if (stack.isEmpty()) {
                            inventory.setItem(i, ItemStack.EMPTY);
                        }
                    }
                }

                // [线性-O01] 5. 同步更新后的道具袋内容到客户端
                syncPokebagToClient(serverPlayer, bagManager);

                if (remainingToConsume > 0) {
                    CobblemonTS.LOGGER.warn("[UsePokebagItem] 物品不足: 需要{}个, 实际消耗{}个",
                            packet.count(), packet.count() - remainingToConsume);
                } else {
                    CobblemonTS.LOGGER.info("[UsePokebagItem] 成功使用并消耗 {}x{} 在精灵槽位 {}",
                            packet.itemId(), packet.count(), packet.partySlot());
                }
            } catch (Exception e) {
                CobblemonTS.LOGGER.error("[UsePokebagItem] 处理失败", e);
            }
        });
    }

    // [线性-O01] 尝试使用物品在精灵上（复用UseItemOnPokemonPacket的核心逻辑）
    // 注意：此方法的反射调用也做了缓存优化
    private static boolean tryUseItemOnPokemon(ServerPlayer player, Object pokemon, ItemStack stack, UsePokebagItemPacket packet) {
        try {
            // 0. 金色王冠特殊处理：切换超级特训状态（道具袋渠道）
            if ("coblemon_ts:gold_crown".equals(packet.itemId())) {
                return handleGoldenBottleCapFromBag(player, pokemon);
            }

            // 1. 尝试 PokemonSelectingItem（糖果/进化石等）
            if (stack.getItem() instanceof com.cobblemon.mod.common.api.item.PokemonSelectingItem selectingItem) {
                if (selectingItem.canUseOnPokemon(stack, (com.cobblemon.mod.common.pokemon.Pokemon) pokemon)) {
                    for (int i = 0; i < packet.count(); i++) {
                        var result = selectingItem.applyToPokemon(player, stack,
                                (com.cobblemon.mod.common.pokemon.Pokemon) pokemon);
                        if (result.getResult() != net.minecraft.world.InteractionResult.SUCCESS) {
                            return i > 0;
                        }
                    }
                    return true;
                }
                return false;
            }

            // 2. 尝试治疗/复活类物品
            String itemId = packet.itemId();

            // [线性-O01a] 使用静态缓存的反射Method，避免每次调用getMethod
            if (cachedGetCurrentHealth == null || cachedSetStatus == null) return false;

            boolean isHealing = switch (itemId) {
                case "cobblemon:potion", "cobblemon:super_potion",
                     "cobblemon:hyper_potion", "cobblemon:max_potion",
                     "cobblemon:full_restore" -> true;
                default -> false;
            };

            boolean isStatusCure = switch (itemId) {
                case "cobblemon:antidote", "cobblemon:awakening",
                     "cobblemon:burn_heal", "cobblemon:ice_heal",
                     "cobblemon:paralyze_heal", "cobblemon:full_heal",
                     "cobblemon:full_restore", "cobblemon:remedy",
                     "cobblemon:fine_remedy", "cobblemon:superb_remedy",
                     "cobblemon:heal_powder" -> true;
                default -> false;
            };

            boolean isRevive = switch (itemId) {
                case "cobblemon:revive", "cobblemon:max_revive" -> true;
                default -> false;
            };

            boolean anyEffect = false;

            if (isHealing) {
                int healAmount = switch (itemId) {
                    case "cobblemon:potion" -> 20;
                    case "cobblemon:super_potion" -> 60;
                    case "cobblemon:hyper_potion" -> 120;
                    case "cobblemon:max_potion", "cobblemon:full_restore" -> Integer.MAX_VALUE;
                    default -> 0;
                };

                int currentHp = (Integer) cachedGetCurrentHealth.invoke(pokemon);
                int maxHp = (Integer) cachedGetMaxHealth.invoke(pokemon);
                if (currentHp < maxHp) {
                    anyEffect = true;
                    if (healAmount == Integer.MAX_VALUE) {
                        cachedHeal.invoke(pokemon);
                    } else {
                        for (int c = 0; c < packet.count(); c++) {
                            currentHp = Math.min(maxHp, currentHp + healAmount);
                        }
                        cachedSetCurrentHealth.invoke(pokemon, currentHp);
                    }
                }
            }

            if (isStatusCure) {
                Object status = cachedGetStatus.invoke(pokemon);
                if (status != null) {
                    int statusCount = (Integer) status.getClass().getMethod("size").invoke(status);
                    if (statusCount > 0) {
                        anyEffect = true;
                        Object emptyContainer = cachedPersistentStatusContainerClass.getDeclaredConstructor().newInstance();
                        cachedSetStatus.invoke(pokemon, emptyContainer);
                    }
                }
            }

            if (isRevive) {
                boolean fainted = (Boolean) cachedIsFainted.invoke(pokemon);
                if (fainted) {
                    anyEffect = true;
                    int maxHp = (Integer) cachedGetMaxHealth.invoke(pokemon);
                    int healAmount = itemId.equals("cobblemon:max_revive") ? maxHp : maxHp / 2;
                    cachedSetCurrentHealth.invoke(pokemon, healAmount);
                }
            }

            return anyEffect;
        } catch (Exception e) {
            CobblemonTS.LOGGER.warn("[UsePokebagItem] tryUseItemOnPokemon failed: {}", e.getMessage());
            return false;
        }
    }

    // [线性-O01b] 金色王冠处理（道具袋渠道）——使用缓存的反射
    @SuppressWarnings("unchecked")
    private static boolean handleGoldenBottleCapFromBag(ServerPlayer player, Object pokemon) {
        try {
            Object ivs = cachedGetIvs.invoke(pokemon);
            if (ivs == null) return false;

            java.util.Map<Object, Object> htMap = (java.util.Map<Object, Object>) cachedHyperTrainedIVsField.get(ivs);

            boolean allHyperTrained = true;
            for (Object stat : cachedAllStats) {
                if (!(boolean) cachedIsHyperTrained.invoke(ivs, stat)) {
                    allHyperTrained = false;
                    break;
                }
            }

            if (allHyperTrained) {
                if (htMap != null) htMap.clear();
                player.sendSystemMessage(Component.literal("§e已关闭金色王冠"));
            } else {
                for (Object stat : cachedAllStats) {
                    htMap.put(stat, 31);
                }
                player.sendSystemMessage(Component.literal("§a已开启金色王冠"));
            }

            cachedPokemonUpdateMethod.invoke(pokemon);
            CobblemonTS.LOGGER.info("[UsePokebagItem] golden_crown active={}", !allHyperTrained);
            return true;
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("[UsePokebagItem] 金色王冠处理失败: {}", e.getMessage());
            return false;
        }
    }

    // [线性-O01] 同步道具袋内容到客户端
    private static void syncPokebagToClient(ServerPlayer player, com.cobblemon_ts.mod.PokebagManager bagManager) {
        List<SyncPokebagContentsPacket.ItemEntry> updatedItems = new ArrayList<>();
        var bagInventory = bagManager.getBagInventory();
        for (int i = 0; i < bagInventory.getContainerSize(); i++) {
            ItemStack s = bagInventory.getItem(i);
            if (!s.isEmpty()) {
                updatedItems.add(new SyncPokebagContentsPacket.ItemEntry(
                        BuiltInRegistries.ITEM.getKey(s.getItem()).toString(),
                        s.getCount()));
            }
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player, new SyncPokebagContentsPacket(updatedItems));
    }
}