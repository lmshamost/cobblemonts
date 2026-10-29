package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.SecurityHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record UseItemOnPokemonPacket(UUID pokemonUuid, String itemId, int count, boolean allMoves) implements CustomPacketPayload {

    public static final Type<UseItemOnPokemonPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "use_item_on_pokemon"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UseItemOnPokemonPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public UseItemOnPokemonPacket decode(RegistryFriendlyByteBuf buf) {
            return new UseItemOnPokemonPacket(
                    buf.readUUID(),
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readBoolean()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, UseItemOnPokemonPacket packet) {
            buf.writeUUID(packet.pokemonUuid());
            buf.writeUtf(packet.itemId());
            buf.writeVarInt(packet.count());
            buf.writeBoolean(packet.allMoves());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServerSide(UseItemOnPokemonPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
            try {
                var cobblemonInstance = Class.forName("com.cobblemon.mod.common.Cobblemon")
                        .getDeclaredField("INSTANCE").get(null);
                var getStorageMethod = cobblemonInstance.getClass().getMethod("getStorage");
                var storage = getStorageMethod.invoke(cobblemonInstance);

                var getPartyMethod = storage.getClass().getMethod("getParty",
                        Class.forName("net.minecraft.server.level.ServerPlayer"));
                var party = getPartyMethod.invoke(storage, serverPlayer);

                if (party == null) {
                    CobblemonTS.LOGGER.warn("UseItemOnPokemonPacket: party is null for player {}",
                            serverPlayer.getName().getString());
                    return;
                }

                var pokemonClass = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
                var getUuidMethod = pokemonClass.getMethod("getUuid");
                var getCurrentHealthMethod = pokemonClass.getMethod("getCurrentHealth");
                var getMaxHealthMethod = pokemonClass.getMethod("getMaxHealth");
                var setCurrentHealthMethod = pokemonClass.getMethod("setCurrentHealth", int.class);
                var healMethod = pokemonClass.getMethod("heal");
                var isFaintedMethod = pokemonClass.getMethod("isFainted");
                var getStatusMethod = pokemonClass.getMethod("getStatus");
                var setStatusMethod = pokemonClass.getMethod("setStatus",
                        Class.forName("com.cobblemon.mod.common.pokemon.status.PersistentStatusContainer"));

                var sizeMethod = party.getClass().getMethod("size");
                var getMethod = party.getClass().getMethod("get", int.class);
                int size = (Integer) sizeMethod.invoke(party);

                for (int i = 0; i < size; i++) {
                    var pokemon = getMethod.invoke(party, i);
                    if (pokemon == null) continue;
                    var uuid = (UUID) getUuidMethod.invoke(pokemon);
                    if (uuid.equals(packet.pokemonUuid())) {
                        // ★ PP极限药特殊处理：直接对技能使用，无需选择技能UI
                        if ("cobblemon:pp_max".equals(packet.itemId())) {
                            if (handlePPMax(serverPlayer, pokemon, packet)) {
                                // 物品已在handlePPMax内部消耗
                            }
                            break;
                        }
                        // ★ 金色王冠特殊处理：切换金色王冠状态（NBT持久化），无需选择技能UI
                        if ("coblemon_ts:gold_crown".equals(packet.itemId())) {
                            if (handleGoldenBottleCap(serverPlayer, pokemon, packet)) {
                                // 物品已在handleGoldenBottleCap内部消耗
                            }
                            break;
                        }
                        // ★ 特性胶囊/特性膏药特殊处理：直接切换特性，无需选择UI
                        if ("cobblemon:ability_capsule".equals(packet.itemId())
                                || "cobblemon:ability_patch".equals(packet.itemId())) {
                            if (handleAbilityChange(serverPlayer, pokemon, packet)) {
                                // 物品已在handleAbilityChange内部消耗
                            }
                            break;
                        }
                        // 优先用 PokemonSelectingItem 原生逻辑（糖果/进化石等）
                        if (tryPokemonSelectingItem(serverPlayer, pokemon, packet)) {
                            // applyToPokemon 内部已处理物品消耗，不需要额外 consumeItem
                        } else if (applyItemEffect(pokemon, packet, getCurrentHealthMethod, getMaxHealthMethod,
                                setCurrentHealthMethod, healMethod, isFaintedMethod,
                                getStatusMethod, setStatusMethod)) {
                            // 只有治疗/复活等逻辑真正生效了才消耗物品
                            consumeItem(serverPlayer, packet);
                        }
                        // 否则什么都不做——物品既不是 PokemonSelectingItem 也不是治疗类，不消耗
                        break;
                    }
                }
            } catch (Exception e) {
                CobblemonTS.LOGGER.error("UseItemOnPokemonPacket: 处理失败", e);
            }
        });
    }

    private static boolean applyItemEffect(Object pokemon, UseItemOnPokemonPacket packet,
                                        java.lang.reflect.Method getCurrentHealthMethod,
                                        java.lang.reflect.Method getMaxHealthMethod,
                                        java.lang.reflect.Method setCurrentHealthMethod,
                                        java.lang.reflect.Method healMethod,
                                        java.lang.reflect.Method isFaintedMethod,
                                        java.lang.reflect.Method getStatusMethod,
                                        java.lang.reflect.Method setStatusMethod) throws Exception {
        String itemId = packet.itemId();
        int count = packet.count();

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

            int currentHp = (Integer) getCurrentHealthMethod.invoke(pokemon);
            int maxHp = (Integer) getMaxHealthMethod.invoke(pokemon);
            if (currentHp < maxHp) {
                anyEffect = true;
                if (healAmount == Integer.MAX_VALUE) {
                    healMethod.invoke(pokemon);
                } else {
                    for (int c = 0; c < count; c++) {
                        currentHp = Math.min(maxHp, currentHp + healAmount);
                    }
                    setCurrentHealthMethod.invoke(pokemon, currentHp);
                }
            }
        }

        if (isStatusCure) {
            Object status = getStatusMethod.invoke(pokemon);
            if (status != null) {
                java.lang.reflect.Method sizeMethod = status.getClass().getMethod("size");
                int statusCount = (Integer) sizeMethod.invoke(status);
                if (statusCount > 0) {
                    anyEffect = true;
                    Class<?> containerClass = Class.forName(
                            "com.cobblemon.mod.common.pokemon.status.PersistentStatusContainer");
                    Object emptyContainer = containerClass.getDeclaredConstructor().newInstance();
                    setStatusMethod.invoke(pokemon, emptyContainer);
                }
            }
        }

        if (isRevive) {
            boolean fainted = (Boolean) isFaintedMethod.invoke(pokemon);
            if (fainted) {
                anyEffect = true;
                int maxHp = (Integer) getMaxHealthMethod.invoke(pokemon);
                int restoreHp = itemId.equals("cobblemon:max_revive") ? maxHp : maxHp / 2;
                setCurrentHealthMethod.invoke(pokemon, restoreHp);
                Class<?> containerClass = Class.forName(
                        "com.cobblemon.mod.common.pokemon.status.PersistentStatusContainer");
                Object emptyContainer = containerClass.getDeclaredConstructor().newInstance();
                setStatusMethod.invoke(pokemon, emptyContainer);
            }
        }

        return anyEffect;
    }

    private static boolean tryPokemonSelectingItem(ServerPlayer player, Object pokemon,
                                                   UseItemOnPokemonPacket packet) {
        try {
            ItemStack cursorStack = player.containerMenu.getCarried();
            if (cursorStack.isEmpty()) return false;

            if (!(cursorStack.getItem() instanceof com.cobblemon.mod.common.api.item.PokemonSelectingItem selectingItem)) {
                return false;
            }

            // 检查是否能对这只精灵使用
            if (!selectingItem.canUseOnPokemon(cursorStack, 
                    (com.cobblemon.mod.common.pokemon.Pokemon) pokemon)) {
                return false;
            }

            // 应用物品
            for (int i = 0; i < packet.count(); i++) {
                var result = selectingItem.applyToPokemon(player, cursorStack,
                        (com.cobblemon.mod.common.pokemon.Pokemon) pokemon);
                // applyToPokemon 返回 InteractionResultHolder，如果失败就停止
                if (result.getResult() != net.minecraft.world.InteractionResult.SUCCESS) {
                    return i > 0; // 如果已经用了至少1个，也算成功
                }
            }
            return true;
        } catch (Exception e) {
            CobblemonTS.LOGGER.warn("PokemonSelectingItem use failed: {}", e.getMessage());
            return false;
        }
    }

    private static void consumeItem(ServerPlayer player, UseItemOnPokemonPacket packet) {
        ItemStack cursorStack = player.containerMenu.getCarried();
        if (!cursorStack.isEmpty()) {
            int shrinkCount = Math.min(packet.count(), cursorStack.getCount());
            cursorStack.shrink(shrinkCount);
            if (cursorStack.isEmpty()) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }
        }
    }

    // [优化-F03] handlePPMax 高频反射缓存 —— 避免每次使用PP Max时重复Class.forName/getMethod
    private static Class<?> cachedMoveClass;
    private static Class<?> cachedMoveTemplateClass;
    private static Method cachedMoveGetRaised;
    private static Method cachedMoveGetTemplate;
    private static Method cachedTemplateGetPp;
    private static Constructor<?> cachedMoveCtor;
    private static volatile boolean ppmReflectionInitFailed;

    private static boolean initPPMReflection() {
        if (ppmReflectionInitFailed) return false;
        if (cachedMoveClass != null && cachedMoveTemplateClass != null) return true;
        try {
            cachedMoveClass = Class.forName("com.cobblemon.mod.common.api.moves.Move");
            cachedMoveTemplateClass = Class.forName("com.cobblemon.mod.common.api.moves.MoveTemplate");
            cachedMoveGetRaised = cachedMoveClass.getMethod("getRaisedPpStages");
            cachedMoveGetTemplate = cachedMoveClass.getMethod("getTemplate");
            cachedTemplateGetPp = cachedMoveTemplateClass.getMethod("getPp");
            cachedMoveCtor = cachedMoveClass.getDeclaredConstructor(cachedMoveTemplateClass, int.class, int.class);
            cachedMoveCtor.setAccessible(true);
            return true;
        } catch (Throwable t) {
            ppmReflectionInitFailed = true;
            CobblemonTS.LOGGER.error("initPPMReflection failed: {}", t.getMessage());
            return false;
        }
    }

    // ★ PP极限药特殊处理 —— 线性逻辑树:
    //   ├─ 检查精灵是否为空
    //   ├─ 获取MoveSet → getMove(i)遍历4个技能
    //   │   ├─ move==null → skip(空技能无效)
    //   │   ├─ raisedPpStages>=3 → skip(已最大)
    //   │   └─ 可用 → 构造新Move(raised=3, pp=maxPP) → setMove
    //   ├─ !allMoves → 只处理第一个可用技能(break)
    //   └─ allMoves → 遍历全部4个技能
    //   [优化-F03] 反射全部缓存为静态字段，避免每次Class.forName/getMethod
    private static boolean handlePPMax(ServerPlayer player, Object pokemon, UseItemOnPokemonPacket packet) {
        try {
            if (!initPPMReflection()) return false;

            // [优化-F03] 复用缓存的getMoveSet反射
            Object moveSet = pokemon.getClass().getMethod("getMoveSet").invoke(pokemon);
            if (moveSet == null) return false;

            Method getMove = moveSet.getClass().getMethod("get", int.class);
            Method setMove = moveSet.getClass().getMethod("setMove", int.class, cachedMoveClass);

            boolean allMoves = packet.allMoves();
            int itemsConsumed = 0;

            // 遍历4个技能槽位
            for (int i = 0; i < 4; i++) {
                Object move = getMove.invoke(moveSet, i);
                if (move == null) continue; // 空技能无效

                // [优化-F03] 复用缓存的getRaisedPpStages
                int currentRaised = (int) cachedMoveGetRaised.invoke(move);
                if (currentRaised >= 3) continue; // 已最大，跳过

                // [优化-F03] 复用缓存的getTemplate / getPp
                Object template = cachedMoveGetTemplate.invoke(move);
                if (template == null) continue;

                int basePP = (int) cachedTemplateGetPp.invoke(template);

                // PP Max = raised 3 stages，计算最大PP
                int maxPP = (int) Math.floor(basePP * (1.0 + 0.2 * 3));

                // [优化-F03] 复用缓存的Move构造函数
                Object newMove = cachedMoveCtor.newInstance(template, maxPP, 3);
                setMove.invoke(moveSet, i, newMove);

                itemsConsumed++;

                // 非Shift/Alt模式：只处理第一个可用技能
                if (!allMoves) break;
            }

            if (itemsConsumed > 0) {
                consumeItems(player, itemsConsumed);
                return true;
            }
            return false;
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("handlePPMax: PP极限药处理失败 - {}", e.getMessage());
            return false;
        }
    }

    // ★ 特性胶囊/特性膏药特殊处理 —— 线性逻辑树:
    //   ├─ 获取species.getAbilities() → 收集所有特性模板ID列表
    //   ├─ 获取pokemon.getAbility() → 当前特性模板ID
    //   ├─ 找到当前特性在列表中的索引
    //   ├─ ability_capsule: 切换普通特性(索引0↔1)，跳过隐藏特性(最后一个)
    //   │   └─ 只有1个普通特性 → 无法切换，不消耗
    //   └─ ability_patch: 切换到隐藏特性(最后一个)
    //       └─ 已是隐藏特性 → 无法切换，不消耗
    private static boolean handleAbilityChange(ServerPlayer player, Object pokemon, UseItemOnPokemonPacket packet) {
        try {
            // 获取精灵的物种
            Object species = pokemon.getClass().getMethod("getSpecies").invoke(pokemon);
            if (species == null) return false;

            // 获取物种的所有特性列表
            Object abilitiesIterable = null;
            for (Method m : species.getClass().getMethods()) {
                if ((m.getName().equals("getAbilities") || m.getName().equals("abilities"))
                        && m.getParameterCount() == 0
                        && Iterable.class.isAssignableFrom(m.getReturnType())) {
                    abilitiesIterable = m.invoke(species);
                    break;
                }
            }
            if (abilitiesIterable == null) {
                CobblemonTS.LOGGER.warn("handleAbilityChange: species has no abilities");
                return false;
            }

            // 收集所有特性模板ID
            List<String> abilityIds = new ArrayList<>();
            for (Object potentialAbility : (Iterable<?>) abilitiesIterable) {
                if (potentialAbility == null) continue;
                Object template = null;
                for (Method m : potentialAbility.getClass().getMethods()) {
                    if ((m.getName().equals("getTemplate") || m.getName().equals("template"))
                            && m.getParameterCount() == 0) {
                        template = m.invoke(potentialAbility);
                        break;
                    }
                }
                if (template != null) {
                    String aid = extractAbilityId(template);
                    if (!aid.isEmpty()) abilityIds.add(aid);
                }
            }

            if (abilityIds.isEmpty()) {
                CobblemonTS.LOGGER.warn("handleAbilityChange: no ability IDs found");
                return false;
            }

            // 获取当前特性ID
            Object currentAbility = pokemon.getClass().getMethod("getAbility").invoke(pokemon);
            String currentId = "";
            if (currentAbility != null) {
                Object currentTemplate = null;
                for (Method m : currentAbility.getClass().getMethods()) {
                    if ((m.getName().equals("getTemplate") || m.getName().equals("template"))
                            && m.getParameterCount() == 0) {
                        currentTemplate = m.invoke(currentAbility);
                        break;
                    }
                }
                if (currentTemplate != null) {
                    currentId = extractAbilityId(currentTemplate);
                }
            }

            CobblemonTS.LOGGER.info("handleAbilityChange: abilities={} current={} item={}",
                    abilityIds, currentId, packet.itemId());

            // 找到当前特性的索引
            int currentIndex = currentId.isEmpty() ? -1 : abilityIds.indexOf(currentId);
            int targetIndex = -1;

            if ("cobblemon:ability_capsule".equals(packet.itemId())) {
                // 特性胶囊：在普通特性之间切换(前N-1个是普通特性，最后一个是隐藏特性)
                int normalCount = abilityIds.size() - 1; // 至少1个普通特性
                if (normalCount <= 1) {
                    CobblemonTS.LOGGER.info("handleAbilityChange: only 1 normal ability, capsule can't switch");
                    return false; // 只有1个普通特性，无法切换
                }
                if (currentIndex >= 0 && currentIndex < normalCount) {
                    // 在普通特性间切换: 0→1, 1→0
                    targetIndex = (currentIndex == 0) ? 1 : 0;
                } else {
                    // 当前是隐藏特性或未找到 → 切到第一个普通特性
                    targetIndex = 0;
                }
            } else {
                // 特性膏药：切换到隐藏特性(最后一个)
                int hiddenIndex = abilityIds.size() - 1;
                if (currentIndex == hiddenIndex) {
                    CobblemonTS.LOGGER.info("handleAbilityChange: already hidden ability, patch can't apply");
                    return false; // 已是隐藏特性
                }
                targetIndex = hiddenIndex;
            }

            if (targetIndex < 0 || targetIndex >= abilityIds.size()) {
                CobblemonTS.LOGGER.warn("handleAbilityChange: invalid target index {}", targetIndex);
                return false;
            }

            String targetAbilityId = abilityIds.get(targetIndex);
            CobblemonTS.LOGGER.info("handleAbilityChange: switching to {} (index {})", targetAbilityId, targetIndex);

            // 通过Abilities注册表获取AbilityTemplate，创建Ability实例
            Object abilityTemplate = null;
            Class<?> abilitiesClass = Class.forName("com.cobblemon.mod.common.api.abilities.Abilities");
            Object abilitiesInstance = abilitiesClass.getField("INSTANCE").get(null);
            for (Method m : abilitiesInstance.getClass().getMethods()) {
                if (m.getName().equals("get") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == String.class) {
                    abilityTemplate = m.invoke(abilitiesInstance, targetAbilityId);
                    if (abilityTemplate != null) break;
                    abilityTemplate = m.invoke(abilitiesInstance, "cobblemon:" + targetAbilityId);
                    if (abilityTemplate != null) break;
                }
            }
            if (abilityTemplate == null) {
                CobblemonTS.LOGGER.error("handleAbilityChange: ability template not found for '{}'", targetAbilityId);
                return false;
            }

            // 创建Ability实例
            Object priorityNormal = com.cobblemon.mod.common.api.Priority.valueOf("NORMAL");
            Method createM = abilityTemplate.getClass().getMethod("create",
                    boolean.class, com.cobblemon.mod.common.api.Priority.class);
            Object newAbility = createM.invoke(abilityTemplate, false, priorityNormal);

            // 设置到精灵
            pokemon.getClass().getMethod("setAbility$common",
                    Class.forName("com.cobblemon.mod.common.api.abilities.Ability"))
                    .invoke(pokemon, newAbility);

            consumeItems(player, 1);
            CobblemonTS.LOGGER.info("handleAbilityChange: SUCCESS! {} -> {}", currentId, targetAbilityId);
            return true;
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("handleAbilityChange: 特性切换失败 - {}", e.getMessage());
            return false;
        }
    }

    // ★ 从AbilityTemplate提取ID（去掉命名空间前缀）
    private static String extractAbilityId(Object template) {
        try {
            for (Method m : template.getClass().getMethods()) {
                if ((m.getName().equals("getShowdownId") || m.getName().equals("showdownId")
                        || m.getName().equals("getName") || m.getName().equals("name"))
                        && m.getParameterCount() == 0) {
                    Object v = m.invoke(template);
                    if (v != null) {
                        String s = v.toString();
                        if (s.contains(":")) s = s.substring(s.indexOf(":") + 1);
                        return s.toLowerCase().trim();
                    }
                }
            }
            String s = template.toString();
            if (s.contains(":")) s = s.substring(s.indexOf(":") + 1);
            if (s.contains(".")) s = s.substring(s.lastIndexOf(".") + 1);
            return s.toLowerCase().trim();
        } catch (Throwable ignored) {}
        return "";
    }

    // ★ 金色王冠特殊处理 —— 直接操作hyperTrainedIVs Map
    //   cobblemon的setHyperTrainedIV会在原始IV==31时自动移除条目(优化)
    //   导致满个体精灵永远无法开启王冠，改用put直接写入Map
    private static boolean handleGoldenBottleCap(ServerPlayer player, Object pokemon, UseItemOnPokemonPacket packet) {
        try {
            var getIvsMethod = pokemon.getClass().getMethod("getIvs");
            Object ivs = getIvsMethod.invoke(pokemon);
            if (ivs == null) {
                CobblemonTS.LOGGER.warn("handleGoldenBottleCap: ivs is null");
                return false;
            }

            var isHyperTrainedMethod = ivs.getClass().getMethod("isHyperTrained",
                    Class.forName("com.cobblemon.mod.common.api.pokemon.stats.Stat"));

            var statsClass = Class.forName("com.cobblemon.mod.common.api.pokemon.stats.Stats");
            var hpStat = statsClass.getField("HP").get(null);
            var attackStat = statsClass.getField("ATTACK").get(null);
            var defenceStat = statsClass.getField("DEFENCE").get(null);
            var spAtkStat = statsClass.getField("SPECIAL_ATTACK").get(null);
            var spDefStat = statsClass.getField("SPECIAL_DEFENCE").get(null);
            var speedStat = statsClass.getField("SPEED").get(null);
            Object[] allStats = {hpStat, attackStat, defenceStat, spAtkStat, spDefStat, speedStat};

            // ★ 反射获取hyperTrainedIVs Map（私有字段）
            var htField = ivs.getClass().getDeclaredField("hyperTrainedIVs");
            htField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<Object, Object> htMap = (java.util.Map<Object, Object>) htField.get(ivs);

            // ★ 判断当前是否全部超特训已激活
            boolean allHyperTrained = true;
            for (Object stat : allStats) {
                if (!(boolean) isHyperTrainedMethod.invoke(ivs, stat)) {
                    allHyperTrained = false;
                    break;
                }
            }

            if (allHyperTrained) {
                // ★ 关闭金色王冠：清空hyperTrainedIVs Map
                if (htMap != null) htMap.clear();
                player.sendSystemMessage(Component.literal("§e已关闭金色王冠"));
            } else {
                // ★ 开启金色王冠：直接put到hyperTrainedIVs Map(绕过setHyperTrainedIV的优化逻辑)
                for (Object stat : allStats) {
                    htMap.put(stat, 31);
                }
                player.sendSystemMessage(Component.literal("§a已开启金色王冠"));
            }

            // ★ 触发pokemon.update()刷新战斗属性
            try {
                var updateMethod = pokemon.getClass().getMethod("update");
                updateMethod.invoke(pokemon);
            } catch (Throwable ignored) {}

            consumeItems(player, 1);
            CobblemonTS.LOGGER.info("handleGoldenBottleCap: golden_crown active={}", !allHyperTrained);
            return true;
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("handleGoldenBottleCap: 金色王冠处理失败 - {}", e.getMessage());
            return false;
        }
    }

    // [修复-F02] 消耗物品：只检查光标（玩家用鼠标拿着物品对精灵栏使用，物品始终在光标上）
    private static void consumeItems(ServerPlayer player, int count) {
        ItemStack cursorStack = player.containerMenu.getCarried();
        if (!cursorStack.isEmpty()) {
            int shrinkCount = Math.min(count, cursorStack.getCount());
            cursorStack.shrink(shrinkCount);
            if (cursorStack.isEmpty()) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }
        }
    }
}