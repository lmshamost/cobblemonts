package com.cobblemon_ts.mod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class CharmEventHandler {

    private static final List<String> EVENT_NAMES = Arrays.asList(
        "POKEMON_CATCH_RATE",
        "SHINY_CHANCE_CALCULATION",
        "EXPERIENCE_GAINED_EVENT_PRE",
        "HATCH_EGG_PRE",
        "BATTLE_VICTORY"
    );

    private static boolean eventsRegistered = false;
    private static volatile boolean isDistributingExp = false;

    // === 核心反射缓存：游戏启动初始化一次，高频调用 ===
    // Cobblemon 核心
    private static Class<?> cobblemonClass;
    private static Field cobblemonInstanceField;
    private static Method getStorageMethod;
    private static Method getExperienceCalculatorMethod;
    // Party 相关
    private static Class<?> serverPlayerClass;
    private static Method getPartyMethod;
    private static Method partyGetMethod;
    private static Method partySizeMethod;
    // 经验源
    private static Constructor<?> sidemodExpSourceConstructor;
    // Pokemon 方法
    private static Method addExperienceWithPlayerMethod;
    private static Method getLevelMethod;
    private static Method getExperienceMethod;
    // 升级所需经验的方法缓存（不同Cobblemon版本方法名不同）
    private static Method getExpToNextLevelMethod;
    // BattleVictory 相关
    private static Class<?> playerBattleActorClass;
    private static Class<?> battlePokemonClass;
    // rctmod
    private static Class<?> levelUtilsClass;
    private static Method levelCapMethod;
    // [优化-H01] 高频反射Method缓存：避免invokeMethod/setEventValue/getPlayerFromPokemon每调用都getMethod
    private static final java.util.Map<String, Method> cachedEventHandlerMethods = new java.util.HashMap<>();
    private static final int EVENT_METHOD_CACHE_MAX = 100; // [优化-H01] 上限防止无限增长
    // [线性-L07] Priority 反射缓存
    private static Class<?> cachedPriorityClass;
    private static Field cachedNormalField;

    private static Method getCachedMethod(Object obj, String methodName) {
        String key = obj.getClass().getName() + "#" + methodName;
        Method m = cachedEventHandlerMethods.get(key);
        if (m == null) {
            try {
                m = obj.getClass().getMethod(methodName);
                // [优化-C01] 缓存上限保护
                if (cachedEventHandlerMethods.size() >= EVENT_METHOD_CACHE_MAX) {
                    cachedEventHandlerMethods.clear();
                }
                cachedEventHandlerMethods.put(key, m);
            } catch (Exception ignored) {}
        }
        return m;
    }

    private static Method getCachedMethodWithTypes(Object obj, String methodName, Class<?>... paramTypes) {
        StringBuilder keyBuilder = new StringBuilder(obj.getClass().getName() + "#" + methodName + "(");
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) keyBuilder.append(",");
            keyBuilder.append(paramTypes[i].getName());
        }
        keyBuilder.append(")");
        String key = keyBuilder.toString();
        Method m = cachedEventHandlerMethods.get(key);
        if (m == null) {
            try {
                m = obj.getClass().getMethod(methodName, paramTypes);
                if (cachedEventHandlerMethods.size() >= EVENT_METHOD_CACHE_MAX) {
                    cachedEventHandlerMethods.clear();
                }
                cachedEventHandlerMethods.put(key, m);
            } catch (Exception ignored) {}
        }
        return m;
    }
    // 缓存是否已初始化
    private static boolean reflectionCacheInitialized = false;

    public static void init() {
        if (eventsRegistered) {
            return;
        }

        initReflectionCache();

        try {
            Class<?> cobblemonEventsClass = Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents");
            Field[] fields = cobblemonEventsClass.getDeclaredFields();

            CobblemonTS.LOGGER.info("=== 开始注册Cobblemon事件 ===");

            boolean foundAnyEvent = false;

            for (Field field : fields) {
                String fieldName = field.getName();

                if (!EVENT_NAMES.contains(fieldName)) {
                    continue;
                }

                foundAnyEvent = registerEventByType(field, fieldName) || foundAnyEvent;
            }

            if (foundAnyEvent) {
                eventsRegistered = true;
                CobblemonTS.LOGGER.info("=== Cobblemon事件注册完成 ===");
            } else {
                CobblemonTS.LOGGER.warn("=== 未找到任何匹配的Cobblemon事件 ===");
            }

        } catch (ClassNotFoundException e) {
            CobblemonTS.LOGGER.error("找不到CobblemonEvents类", e);
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("注册Cobblemon事件失败", e);
        }
    }

    private static void initReflectionCache() {
        if (reflectionCacheInitialized) return;
        try {
            // Cobblemon 核心
            cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            cobblemonInstanceField = cobblemonClass.getDeclaredField("INSTANCE");
            getStorageMethod = cobblemonClass.getMethod("getStorage");
            try {
                getExperienceCalculatorMethod = cobblemonClass.getMethod("getExperienceCalculator");
            } catch (NoSuchMethodException ignored) {}

            // ServerPlayer
            serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");

            // 经验源
            Class<?> experienceSourceInterface = Class.forName("com.cobblemon.mod.common.api.pokemon.experience.ExperienceSource");
            Class<?> sidemodExperienceSourceClass = Class.forName("com.cobblemon.mod.common.api.pokemon.experience.SidemodExperienceSource");
            sidemodExpSourceConstructor = sidemodExperienceSourceClass.getConstructor(String.class);

            // Pokemon 方法（每个方法独立捕获异常，避免单个方法缺失导致整体失败）
            Class<?> pokemonClass = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
            getLevelMethod = pokemonClass.getMethod("getLevel");
            getExperienceMethod = pokemonClass.getMethod("getExperience");

            // 尝试多种方法名（不同 Cobblemon 版本使用不同命名）
            String[] expMethodNames = {
                "getExperienceToNextLevel",
                "getExperienceNeeded",
                "getExperienceToLevel"
            };
            for (String name : expMethodNames) {
                try {
                    getExpToNextLevelMethod = pokemonClass.getMethod(name);
                    break;
                } catch (NoSuchMethodException ignored) {}
            }

            // addExperienceWithPlayer：必须获取，失败则记录错误但不中断其他方法
            try {
                addExperienceWithPlayerMethod = pokemonClass.getMethod("addExperienceWithPlayer",
                    serverPlayerClass, experienceSourceInterface, int.class);
            } catch (NoSuchMethodException e) {
                CobblemonTS.LOGGER.warn("找不到 addExperienceWithPlayer 方法，高级学习装置可能无法正常工作", e);
            }

            // BattleVictory
            try {
                playerBattleActorClass = Class.forName("com.cobblemon.mod.common.battles.actor.PlayerBattleActor");
                battlePokemonClass = Class.forName("com.cobblemon.mod.common.battles.pokemon.BattlePokemon");
            } catch (ClassNotFoundException ignored) {}

            // rctmod
            try {
                levelUtilsClass = Class.forName("com.gitlab.srcmc.rctmod.api.utils.LevelUtils");
                levelCapMethod = levelUtilsClass.getMethod("levelCap", Player.class);
            } catch (ClassNotFoundException ignored) {}

            reflectionCacheInitialized = true;
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("反射缓存初始化失败", e);
        }
    }

    private static boolean registerEventByType(Field field, String fieldName) {
        return switch (fieldName) {
            case "POKEMON_CATCH_RATE" -> registerCatchRateEvent(field);
            case "SHINY_CHANCE_CALCULATION" -> registerShinyEvent(field);
            case "EXPERIENCE_GAINED_EVENT_PRE" -> registerExpEvent(field);
            case "HATCH_EGG_PRE" -> registerHatchEvent(field);
            case "BATTLE_VICTORY" -> registerBattleVictoryEvent(field);
            default -> false;
        };
    }

    private static Method findSubscribeMethod(Object eventBus) {
        Class<?> clazz = eventBus.getClass();

        try {
            Method method = clazz.getMethod("subscribe", Consumer.class);
            return method;
        } catch (NoSuchMethodException ignored) {}

        try {
            Method method = clazz.getMethod("subscribe",
                Class.forName("com.cobblemon.mod.common.api.Priority"),
                Consumer.class);
            return method;
        } catch (Exception ignored) {}

        for (Method method : clazz.getMethods()) {
            if (method.getName().equals("subscribe")) {
                Class<?>[] params = method.getParameterTypes();
                for (Class<?> param : params) {
                    if (Consumer.class.isAssignableFrom(param)
                        || param.getName().contains("Consumer")) {
                        return method;
                    }
                }
            }
        }

        return null;
    }

    private static void invokeSubscribe(Object eventBus, Method method, Consumer<Object> handler) {
        try {
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 1) {
                method.invoke(eventBus, handler);
            } else if (params.length == 2) {
                // [线性-L07] 缓存 Priority 类和 NORMAL 字段，避免每次事件调用都反射
                Class<?> priorityClass = cachedPriorityClass;
                if (priorityClass == null) {
                    priorityClass = Class.forName("com.cobblemon.mod.common.api.Priority");
                    cachedPriorityClass = priorityClass;
                }
                Field normalField = cachedNormalField;
                if (normalField == null) {
                    normalField = priorityClass.getDeclaredField("NORMAL");
                    cachedNormalField = normalField;
                }
                method.invoke(eventBus, normalField.get(null), handler);
            }
        } catch (Exception ignored) {}
    }

    private static boolean registerEvent(Field field, Consumer<Object> handler, String eventName) {
        try {
            Object eventBus = field.get(null);
            if (eventBus == null) {
                return false;
            }

            Method subscribeMethod = findSubscribeMethod(eventBus);
            if (subscribeMethod == null) {
                return false;
            }

            invokeSubscribe(eventBus, subscribeMethod, handler);
            CobblemonTS.LOGGER.info("成功注册{}事件: {}", eventName, field.getName());
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private static Player getPlayerFromEvent(Object event, String... methods) {
        for (String methodName : methods) {
            Object result = invokeMethod(event, methodName);
            if (result instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    // ============================================================
    // 事件处理器
    // ============================================================

    private static boolean registerCatchRateEvent(Field field) {
        return registerEvent(field, event -> {
            Player player = getPlayerFromEvent(event, "getThrower", "getPlayer");
            if (player == null) return;

            processCharmEffect(player, CharmType.CATCHING, () -> {
                float boost = CharmConfig.CATCHING_CHARM_BOOST.get().floatValue();
                if (boost <= 1.0f) return;

                Number catchRate = getEventValue(event, "getCatchRate");
                if (catchRate == null) return;

                float newRate = catchRate.floatValue() * boost;
                setEventValue(event, newRate, float.class, "setCatchRate");
                CobblemonTS.LOGGER.info("防晃护符生效！玩家: {}, 捕获率: {} -> {}",
                    player.getName().getString(), catchRate.floatValue(), newRate);
            });
        }, "捕获率");
    }

    private static boolean registerExpEvent(Field field) {
        return registerEvent(field, event -> {
            // 关键：当高级学习装置正在给未出战宝可梦分配经验时，跳过护符加成
            // 否则会导致：出战宝可梦 26 → PRE→104 → 高级学习装置给未出战104 → PRE又→416
            if (isDistributingExp) return;

            Object pokemon = invokeMethod(event, "getPokemon");
            if (pokemon == null) return;

            Player player = getPlayerFromPokemon(pokemon);
            if (player == null) return;

            processCharmEffect(player, CharmType.EXP, () -> {
                float boost = CharmConfig.EXP_CHARM_BOOST.get().floatValue();
                if (boost <= 1.0f) return;

                Number exp = getEventValue(event, "getExperience");
                if (exp == null) return;

                int newExp = (int)(exp.intValue() * boost);
                setEventValue(event, newExp, int.class, "setExperience");
                CobblemonTS.LOGGER.info("经验护符生效！玩家: {}, 经验: {} -> {}",
                    player.getName().getString(), exp.intValue(), newExp);
            });
        }, "经验");
    }

    private static boolean registerShinyEvent(Field field) {
        return registerEvent(field, event -> {
            Object pokemon = invokeMethod(event, "getPokemon");
            if (pokemon == null) return;

            Player player = getPlayerFromPokemon(pokemon);
            if (player == null) return;

            processCharmEffect(player, CharmType.SHINY, () -> {
                Boolean isShiny = invokeBooleanMethod(event, "isShiny");
                if (Boolean.TRUE.equals(isShiny)) return;

                double boost = CharmConfig.SHINY_CHARM_BOOST.get();
                if (Math.random() < boost / 100.0) {
                    setEventValue(event, true, boolean.class, "setShiny");
                    CobblemonTS.LOGGER.info("闪耀护符生效！玩家: {} 遇到闪光宝可梦！",
                        player.getName().getString());
                }
            });
        }, "闪光");
    }

    private static boolean registerHatchEvent(Field field) {
        return registerEvent(field, event -> {
            Player player = getPlayerFromEvent(event, "getPlayer");
            if (player == null) return;

            processCharmEffect(player, CharmType.OVAL, () -> {
                float boost = CharmConfig.OVAL_CHARM_HATCH_BOOST.get().floatValue();
                if (boost <= 1.0f) return;

                Number steps = getEventValue(event, "getHatchSteps");
                if (steps != null) {
                    int newSteps = (int)(steps.intValue() / boost);
                    setEventValue(event, newSteps, int.class, "setHatchSteps");
                    CobblemonTS.LOGGER.info("圆形护符生效！玩家: {}, 孵化步数: {} -> {}",
                        player.getName().getString(), steps.intValue(), newSteps);
                }
            });
        }, "孵化");
    }

    /**
     * 高级学习装置 - 监听战斗胜利事件
     * 使用 expCalculator 计算击败对手的原始经验值，按比例分享给未出战宝可梦
     */
    private static boolean registerBattleVictoryEvent(Field field) {
        return registerEvent(field, event -> {
            try {
                Object winners = invokeMethod(event, "getWinners");
                if (!(winners instanceof java.util.List<?> winnerList)) {
                    return;
                }

                for (Object winner : winnerList) {
                    if (playerBattleActorClass == null || !playerBattleActorClass.isInstance(winner)) {
                        continue;
                    }

                    Object entity = invokeMethod(winner, "getEntity");
                    if (!(entity instanceof Player player)) {
                        continue;
                    }

                    if (!isExpAllActive(player)) continue;

                    Object battle = invokeMethod(event, "getBattle");
                    Object activePokemon = invokeMethod(battle, "getActivePokemon");
                    if (!(activePokemon instanceof java.util.List<?> activeList)) {
                        continue;
                    }

                    Object sourceBattlePokemon = null;
                    Object sourcePokemon = null;
                    for (Object active : activeList) {
                        Object actor = invokeMethod(active, "getActor");
                        if (playerBattleActorClass == null || playerBattleActorClass.isInstance(actor)) {
                            sourceBattlePokemon = invokeMethod(active, "getBattlePokemon");
                            sourcePokemon = invokeMethod(sourceBattlePokemon, "getOriginalPokemon");
                            break;
                        }
                    }

                    if (sourceBattlePokemon == null || sourcePokemon == null) {
                        continue;
                    }

                    // 计算击败经验 - 用 expCalculator 计算对手经验（原始击败经验）
                    Object expCalculator = getExperienceCalculatorMethod != null
                        ? getExperienceCalculatorMethod.invoke(cobblemonInstanceField.get(null))
                        : null;

                    int totalExp = 0;
                    if (expCalculator != null) {
                        Object losers = invokeMethod(battle, "getLosers");
                        if (losers instanceof java.util.List<?> loserList) {
                            for (Object loser : loserList) {
                                Object pokemonList = invokeMethod(loser, "getPokemonList");
                                if (pokemonList instanceof java.util.List<?> list) {
                                    for (Object defeated : list) {
                                        Number exp = (Number) invokeMethodWithTypes(
                                            expCalculator, "calculate",
                                            new Class<?>[]{battlePokemonClass, battlePokemonClass, double.class},
                                            sourceBattlePokemon, defeated, 1.0);
                                        if (exp != null) {
                                            totalExp += exp.intValue();
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 如果 expCalculator 返回 0，用等级×10 作为基础经验
                    double percent = CharmConfig.EXP_ALL_PERCENT.get().doubleValue();
                    int expAllAmount = (int)(totalExp * percent / 100.0);

                    if (expAllAmount <= 0) {
                        try {
                            int level = (Integer) getLevelMethod.invoke(sourcePokemon);
                            totalExp = level * 10;
                            expAllAmount = (int)(totalExp * percent / 100.0);
                        } catch (Exception ignored) {
                            continue;
                        }
                    }

                    if (expAllAmount <= 0) continue;

                    distributeExpToPartyMembers(player, sourcePokemon, expAllAmount);
                }

            } catch (Exception e) {
                CobblemonTS.LOGGER.error("BattleVictory 事件处理异常", e);
            }
        }, "战斗胜利");
    }

    /**
     * 公开入口：由 PokemonMixin 调用，分配经验给队伍中其他宝可梦
     * 使用强类型参数，方便 PokemonMixin 直接调用
     */
    public static void distributePartyExp(
        net.minecraft.server.level.ServerPlayer player,
        com.cobblemon.mod.common.pokemon.Pokemon sourcePokemon,
        int expAmount
    ) {
        distributeExpToPartyMembers(player, sourcePokemon, expAmount);
    }

    /**
     * 核心方法：给队伍中其他宝可梦分配经验
     */
    private static void distributeExpToPartyMembers(Player player, Object sourcePokemon, int expAmount) {
        // 防止无限递归 - 必须在第一行检查
        if (isDistributingExp) {
            return;
        }
        isDistributingExp = true;

        try {
            if (!serverPlayerClass.isInstance(player)) {
                return;
            }

            Object cobblemonInstance = cobblemonInstanceField.get(null);
            Object pokemonStoreManager = getStorageMethod.invoke(cobblemonInstance);

            // 延迟初始化 Party 方法缓存
            if (getPartyMethod == null) {
                getPartyMethod = pokemonStoreManager.getClass().getMethod("getParty", serverPlayerClass);
            }
            Object party = getPartyMethod.invoke(pokemonStoreManager, player);
            if (party == null) {
                return;
            }

            if (partySizeMethod == null) {
                partySizeMethod = party.getClass().getMethod("size");
                partyGetMethod = party.getClass().getMethod("get", int.class);
            }
            int size = (Integer) partySizeMethod.invoke(party);

            // 如果 addExperienceWithPlayerMethod 未初始化，动态查找该方法
            if (addExperienceWithPlayerMethod == null) {
                try {
                    for (String name : new String[]{"addExperienceWithPlayer", "addExperience"}) {
                        try {
                            Method m = pokemonStoreManager.getClass().getClassLoader()
                                .loadClass("com.cobblemon.mod.common.pokemon.Pokemon")
                                .getMethod(name, serverPlayerClass,
                                    Class.forName("com.cobblemon.mod.common.api.pokemon.experience.ExperienceSource"),
                                    int.class);
                            addExperienceWithPlayerMethod = m;
                            break;
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
            if (addExperienceWithPlayerMethod == null) {
                CobblemonTS.LOGGER.warn("无法获取 addExperienceWithPlayer 方法，跳过经验分配");
                return;
            }

            int count = 0;
            for (int i = 0; i < size; i++) {
                Object slot = partyGetMethod.invoke(party, i);
                if (slot == null) continue;

                Object partyPokemon;
                try {
                    Method getPokemonMethod = slot.getClass().getMethod("getPokemon");
                    partyPokemon = getPokemonMethod.invoke(slot);
                } catch (Exception e) {
                    partyPokemon = slot;
                }

                if (partyPokemon != null && partyPokemon != sourcePokemon) {
                    int actualExp = calculateMaxExpForLevelCap(partyPokemon, player, expAmount);
                    if (actualExp <= 0) continue;

                    Object experienceSource = sidemodExpSourceConstructor.newInstance("cobblemon_ts");
                    addExperienceWithPlayerMethod.invoke(partyPokemon, player, experienceSource, actualExp);
                    count++;
                }
            }

            if (count > 0) {
                CobblemonTS.LOGGER.info("高级学习装置生效！玩家: {}, 每只未出战宝可梦获得: {} 经验, 共 {} 只",
                    player.getName().getString(), expAmount, count);
            }
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("高级学习装置分配经验失败", e);
        } finally {
            isDistributingExp = false;
        }
    }

    private static boolean isExpAllActive(Player player) {
        CharmPlayerData playerData = CharmPlayerData.get(player);
        if (playerData == null) return false;
        CharmSlotManager manager = playerData.getCharmSlotManager();
        if (manager == null) return false;
        return manager.isCharmActive(CharmType.EXP_ALL);
    }

    private static void processCharmEffect(Player player, CharmType type, Runnable action) {
        CharmPlayerData playerData = CharmPlayerData.get(player);
        if (playerData == null) return;
        CharmSlotManager manager = playerData.getCharmSlotManager();
        if (manager == null) return;
        if (manager.isCharmActive(type)) {
            action.run();
        }
    }

    private static Player getPlayerFromPokemon(Object pokemon) {
        // [优化-H01] 使用缓存获取3个owner方法
        for (String name : new String[]{"getOwnerEntity", "getOwnerPlayer", "getOwner"}) {
            try {
                Method m = getCachedMethod(pokemon, name);
                if (m != null) {
                    Object owner = m.invoke(pokemon);
                    if (owner instanceof Player p) return p;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static Number getEventValue(Object event, String... methodNames) {
        for (String methodName : methodNames) {
            Object result = invokeMethod(event, methodName);
            if (result instanceof Number) {
                return (Number) result;
            }
        }
        return null;
    }

    private static Boolean invokeBooleanMethod(Object event, String... methodNames) {
        for (String methodName : methodNames) {
            Object result = invokeMethod(event, methodName);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        }
        return null;
    }

    private static void setEventValue(Object event, Object value, Class<?> valueType, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                // [优化-H01] 使用缓存获取Method
                Method setterMethod = getCachedMethodWithTypes(event, methodName, valueType);
                if (setterMethod != null) {
                    setterMethod.invoke(event, value);
                    return;
                }
            } catch (Exception ignored) {}
        }
    }

    private static Object invokeMethod(Object obj, String methodName) {
        try {
            // [优化-H01] 使用缓存获取Method
            Method method = getCachedMethod(obj, methodName);
            if (method != null) return method.invoke(obj);
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Object invokeMethodWithTypes(Object obj, String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            // [优化-H01] 使用缓存获取Method
            Method method = getCachedMethodWithTypes(obj, methodName, paramTypes);
            if (method != null) return method.invoke(obj, args);
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查等级上限，计算可分配的最大经验
     */
    private static int calculateMaxExpForLevelCap(Object partyPokemon, Player player, int expAmount) {
        try {
            int currentLevel = (Integer) getLevelMethod.invoke(partyPokemon);

            if (levelUtilsClass == null) {
                return expAmount;
            }

            int levelCap = (Integer) levelCapMethod.invoke(null, player);

            if (currentLevel >= levelCap) {
                return 0;
            }

            int levelsToCap = levelCap - currentLevel;
            if (levelsToCap <= 1) {
                int currentExp = (Integer) getExperienceMethod.invoke(partyPokemon);
                int expNeeded = 0;
                if (getExpToNextLevelMethod != null) {
                    expNeeded = (Integer) getExpToNextLevelMethod.invoke(partyPokemon);
                } else {
                    // [优化-H01] 动态查找：使用缓存方法，避免每次getMethod
                    String[] names = {"getExperienceToNextLevel", "getExperienceNeeded", "getExperienceToLevel"};
                    for (String name : names) {
                        Method m = getCachedMethod(partyPokemon, name);
                        if (m != null) {
                            try {
                                expNeeded = (Integer) m.invoke(partyPokemon);
                                getExpToNextLevelMethod = m;
                                break;
                            } catch (Exception ignored) {}
                        }
                    }
                }
                int expToNextLevel = expNeeded - currentExp;
                return Math.min(expAmount, expToNextLevel);
            }

            return expAmount;
        } catch (Exception e) {
            return expAmount;
        }
    }
}