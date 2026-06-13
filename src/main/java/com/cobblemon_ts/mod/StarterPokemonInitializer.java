package com.cobblemon_ts.mod;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 专门处理"初始选择宝可梦"的闪光和满 IV 逻辑。
 *
 * 不使用 mixin 到 Pokemon.setSpecies（会影响所有宝可梦），而是：
 * 1. 通过反射监听 CobblemonEvents.STARTER_CHOSEN 事件
 * 2. 当玩家选择初始宝可梦时，修改 event.getPokemon() 的属性
 * 3. 这样就只会影响初始选择的那只宝可梦
 */
public class StarterPokemonInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("CobblemonTS-Starter");
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        try {
            // 1. 找到 CobblemonEvents.STARTER_CHOSEN 字段
            Class<?> cobblemonEventsClass = Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents");
            Field starterChosenField = cobblemonEventsClass.getField("STARTER_CHOSEN");
            Object eventBus = starterChosenField.get(null);

            if (eventBus == null) {
                LOGGER.warn("STARTER_CHOSEN event bus 为 null，初始精灵功能不可用");
                return;
            }

            // 2. 找到 subscribe 方法
            Method subscribeMethod = null;
            Class<?> eventBusClass = eventBus.getClass();
            for (Method m : eventBusClass.getMethods()) {
                if (m.getName().equals("subscribe") && m.getParameterCount() >= 1) {
                    Class<?>[] params = m.getParameterTypes();
                    // 查找接受 Consumer 或 Kotlin Function1 的 subscribe 方法
                    for (Class<?> p : params) {
                        String pname = p.getName();
                        if (pname.contains("Consumer") || pname.contains("Function1") || pname.contains("function")) {
                            subscribeMethod = m;
                            break;
                        }
                    }
                    if (subscribeMethod != null) break;
                }
            }

            if (subscribeMethod == null) {
                // 备选：查找 EventObservable 的 subscribe 方法
                for (Method m : eventBusClass.getMethods()) {
                    if (m.getName().equals("subscribe") && m.getParameterCount() == 1) {
                        subscribeMethod = m;
                        break;
                    }
                }
            }

            if (subscribeMethod == null) {
                LOGGER.warn("找不到 subscribe 方法，初始精灵功能不可用");
                return;
            }

            // 3. 创建处理器
            java.util.function.Consumer<Object> handler = event -> {
                try {
                    handleStarterChosen(event);
                } catch (Throwable t) {
                    LOGGER.error("处理初始精灵事件失败", t);
                }
            };

            // 4. 注册处理器
            Class<?>[] params = subscribeMethod.getParameterTypes();
            if (params.length == 1) {
                // 单参数版本：直接接受 Consumer
                subscribeMethod.invoke(eventBus, handler);
            } else {
                // 可能需要其他参数，尝试用反射查找合适的方式
                // 备选：尝试用 Kotlin 的方式注册
                try {
                    // 查找 EventObservable 的 subscribe(Consumer) 或类似方法
                    Method simpleSubscribe = eventBusClass.getMethod("subscribe", java.util.function.Consumer.class);
                    simpleSubscribe.invoke(eventBus, handler);
                } catch (NoSuchMethodException e1) {
                    // 再试试直接调用带有 Event 类型参数的 subscribe
                    try {
                        Method altMethod = null;
                        for (Method m : eventBusClass.getMethods()) {
                            if (m.getName().equals("subscribe") && m.getParameterCount() == 2) {
                                altMethod = m;
                                break;
                            }
                        }
                        if (altMethod != null) {
                            altMethod.invoke(eventBus, null, handler);
                        } else {
                            LOGGER.warn("无法注册 STARTER_CHOSEN 事件处理器");
                        }
                    } catch (Throwable ignored) {}
                }
            }

            LOGGER.info("初始精灵事件处理器注册成功");

        } catch (Throwable t) {
            LOGGER.error("注册初始精灵事件处理器失败", t);
        }
    }

    private static void handleStarterChosen(Object event) {
        // 检查配置是否开启
        if (!Boolean.TRUE.equals(CobblemonTSConfig.INITIAL_POKEMON_MAX_IV.get())
                && !Boolean.TRUE.equals(CobblemonTSConfig.INITIAL_POKEMON_SHINY.get())) {
            return;
        }

        try {
            // 从事件对象中获取 pokemon
            Method getPokemon = event.getClass().getMethod("getPokemon");
            Object pokemonObj = getPokemon.invoke(event);

            if (!(pokemonObj instanceof Pokemon pokemon)) {
                LOGGER.warn("StarterChosenEvent.getPokemon() 返回的不是 Pokemon 类型");
                return;
            }

            // 1. 满 IV - 只对 6 个基础属性（PERMANENT stat）设置，跳过 accuracy/evasion
            if (Boolean.TRUE.equals(CobblemonTSConfig.INITIAL_POKEMON_MAX_IV.get())) {
                try {
                    Object ivs = pokemon.getIvs();
                    if (ivs != null) {
                        // 只设置 6 个基础属性（名字如 HP, ATTACK, DEFENCE, SPECIAL_ATTACK, SPECIAL_DEFENCE, SPEED）
                        // 用白名单名称匹配，避免给非 PERMANENT stat（accuracy/evasion）设置
                        for (Stats stat : Stats.values()) {
                            try {
                                String statName = stat.toString().toUpperCase();
                                // 只匹配 6 个合法的基础属性
                                boolean isPerm =
                                        statName.equals("HP") ||
                                        statName.equals("ATTACK") || statName.equals("ATK") ||
                                        statName.equals("DEFENCE") || statName.equals("DEFENSE") || statName.equals("DEF") ||
                                        statName.equals("SPECIAL_ATTACK") || statName.equals("SPATTACK") || statName.equals("SPA") ||
                                        statName.equals("SPECIAL_DEFENCE") || statName.equals("SPECIAL_DEFENSE") ||
                                                statName.equals("SPDEF") || statName.equals("SPD") ||
                                        statName.equals("SPEED") || statName.equals("SPE");
                                if (!isPerm) continue;

                                try {
                                    Method setM = ivs.getClass().getMethod("set", com.cobblemon.mod.common.api.pokemon.stats.Stat.class, int.class);
                                    setM.invoke(ivs, stat, 31);
                                } catch (NoSuchMethodException ignored) {
                                    try {
                                        Method setIV = pokemon.getClass().getMethod("setIV", com.cobblemon.mod.common.api.pokemon.stats.Stat.class, int.class);
                                        setIV.invoke(pokemon, stat, 31);
                                    } catch (Throwable ignored2) {}
                                } catch (Throwable ignored) {}
                            } catch (Throwable ignored) {}
                        }
                    }
                } catch (Throwable ignored) {}
            }

            // 2. 闪光
            if (Boolean.TRUE.equals(CobblemonTSConfig.INITIAL_POKEMON_SHINY.get())) {
                try {
                    pokemon.setShiny(true);
                } catch (Throwable ignored) {}
            }

            LOGGER.info("初始精灵已处理: species={}, level={}, shiny={}",
                    pokemon.getSpecies().getName(), pokemon.getLevel(), pokemon.getShiny());

        } catch (Throwable t) {
            LOGGER.error("修改初始精灵属性失败", t);
        }
    }
}