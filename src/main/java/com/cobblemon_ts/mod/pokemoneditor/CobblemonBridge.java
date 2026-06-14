package com.cobblemon_ts.mod.pokemoneditor;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CobblemonBridge {

    private static final Map<String, Object> registryCache = new HashMap<>();
    private static final Gson GSON = new Gson();
    private static final int REGISTRY_CACHE_MAX = 500; // [优化-C01] registryCache上限，防止无限增长

    // [线性-L07] 高频反射缓存：避免pokemonToData/writePokemonToSlot中重复Class.forName
    private static Class<?> cachedStatClass;
    private static Class<?> cachedStatsClass;
    private static Method cachedGetIvsMethod;
    private static Class<?> cachedPartyPositionClass;
    private static java.lang.reflect.Constructor<?> cachedPartyPositionCtor;
    private static Class<?> cachedMovesClass;
    private static Class<?> cachedAbilitiesClass;
    private static Class<?> cachedGenderClass;
    private static volatile boolean bridgeCacheInit;

    private static void initBridgeCache() {
        if (bridgeCacheInit) return;
        try {
            cachedStatClass = Class.forName("com.cobblemon.mod.common.api.pokemon.stats.Stat");
            cachedStatsClass = Class.forName("com.cobblemon.mod.common.api.pokemon.stats.Stats");
            cachedPartyPositionClass = Class.forName("com.cobblemon.mod.common.api.storage.party.PartyPosition");
            cachedPartyPositionCtor = cachedPartyPositionClass.getConstructor(int.class);
            cachedMovesClass = Class.forName("com.cobblemon.mod.common.api.moves.Moves");
            cachedAbilitiesClass = Class.forName("com.cobblemon.mod.common.api.abilities.Abilities");
            cachedGenderClass = Class.forName("com.cobblemon.mod.common.pokemon.Gender");
            bridgeCacheInit = true;
        } catch (Throwable ignored) {}
    }

    // [优化-B01] 高频反射 Method 缓存：writePokemonToSlot / deletePokemonFromSlot / pokemonToData 复用
    private static Method cachedPartyGetSlotsMethod;       // party.getSlots()
    private static Method cachedPartyGetIntMethod;          // party.get(int)
    private static Method cachedPartySendToMethod;          // party.sendTo(ServerPlayer)
    private static Method cachedPartyNotifyChangedMethod;   // party.onPokemonChanged(Pokemon)
    private static Method cachedPartySetAtPositionMethod;   // party.setAtPosition(PartyPosition, Pokemon)
    private static Method cachedPokemonUpdateMethod;        // pokemon.update()
    private static Method cachedMoveSetUpdateMethod;        // moveSet.update()
    private static Method cachedIsHyperTrainedMethod;       // ivs.isHyperTrained(Stat)
    private static java.lang.reflect.Field cachedStatsHPField, cachedStatsATKField, cachedStatsDEFField;
    private static java.lang.reflect.Field cachedStatsSPAField, cachedStatsSPDField, cachedStatsSPEField;
    private static java.lang.reflect.Constructor<?> cachedMoveCtor; // Move(MoveTemplate, int, int)
    private static List<String> cachedAllMoveIds;           // [优化-C03] getAllMoveIds 结果缓存
    private static List<String> cachedAllAbilityIds;        // [优化-C03] getAllAbilityIds 结果缓存
    // [优化-C01] registryCache 安全写入：超限时清空避免无限增长
    private static void putToRegistryCache(String key, Object value) {
        if (registryCache.size() >= REGISTRY_CACHE_MAX) {
            registryCache.clear();
        }
        registryCache.put(key, value);
    }

    public static int getPartySize(ServerPlayer serverPlayer, UUID targetUUID) {
        List<Object> list = getPartyList(serverPlayer, targetUUID);
        return list.size();
    }

    public static PokemonData readPokemonFromSlot(ServerPlayer serverPlayer, UUID targetUUID, int slot) {
        List<Object> party = getPartyList(serverPlayer, targetUUID);
        if (slot < 0 || slot >= party.size()) {
            PokemonData empty = new PokemonData();
            empty.exists = false;
            return empty;
        }
        Object pokemon = party.get(slot);
        return pokemonToData(pokemon);
    }

    @SuppressWarnings("unchecked")
    private static PokemonData pokemonToData(Object pokemonObj) {
        PokemonData data = new PokemonData();
        if (pokemonObj == null) {
            data.exists = false;
            return data;
        }
        data.exists = true;
        Pokemon pokemon = (Pokemon) pokemonObj;

        try {
            data.pokemonUUID = pokemon.getUuid();
            data.speciesName = pokemon.getSpecies().showdownId().toLowerCase();
            Object formObj = pokemon.getForm();
            if (formObj != null) data.formName = extractStringId(formObj);

            data.level = pokemon.getLevel();
            data.experience = pokemon.getExperience();
            data.friendship = pokemon.getFriendship();
            data.shiny = pokemon.getShiny();
            data.currentHp = pokemon.getCurrentHealth();
            data.maxHp = pokemon.getMaxHealth();
            if (data.maxHp < 1) data.maxHp = 1;

            try {
                Object held = callNoArgs(pokemon, "heldItem");
                if (held instanceof ItemStack) {
                    ItemStack is = (ItemStack) held;
                    if (!is.isEmpty()) {
                        String id = BuiltInRegistries.ITEM.getKey(is.getItem()).toString();
                        if (id.contains(":")) id = id.substring(id.indexOf(":") + 1);
                        data.heldItem = id;
                    }
                }
            } catch (Throwable ignored) {}

            try {
                Object statusContainer = pokemon.getStatus();
                if (statusContainer != null) {
                    Object statusObj = statusContainer.getClass().getMethod("getStatus").invoke(statusContainer);
                    if (statusObj != null) {
                        Object nameRL = statusObj.getClass().getMethod("getName").invoke(statusObj);
                        if (nameRL instanceof ResourceLocation rl) {
                            data.status = rl.getPath();
                        }
                    }
                }
            } catch (Throwable ignored) {}

            try {
                Object ivsObj = callNoArgs(pokemon, "getIvs", "ivs");
                if (ivsObj == null) ivsObj = pokemon.getIvs();
                if (ivsObj != null) {
                    data.ivHp = readStat(ivsObj, "HP", "hp", "Hp");
                    data.ivAtk = readStat(ivsObj, "ATK", "attack", "atk", "Atk");
                    data.ivDef = readStat(ivsObj, "DEF", "defense", "def", "Def");
                    data.ivSpA = readStat(ivsObj, "SPA", "specialAttack", "spAttack", "spa", "SpA");
                    data.ivSpD = readStat(ivsObj, "SPD", "specialDefence", "specialDefense", "spDefense", "spd", "SpD");
                    data.ivSpe = readStat(ivsObj, "SPE", "speed", "spe", "Spe");
                }
                Object evsObj = callNoArgs(pokemon, "getEvs", "evs");
                if (evsObj == null) evsObj = pokemon.getEvs();
                if (evsObj != null) {
                    data.evHp = readStat(evsObj, "HP", "hp", "Hp");
                    data.evAtk = readStat(evsObj, "ATK", "attack", "atk", "Atk");
                    data.evDef = readStat(evsObj, "DEF", "defense", "def", "Def");
                    data.evSpA = readStat(evsObj, "SPA", "specialAttack", "spAttack", "spa", "SpA");
                    data.evSpD = readStat(evsObj, "SPD", "specialDefence", "specialDefense", "spDefense", "spd", "SpD");
                    data.evSpe = readStat(evsObj, "SPE", "speed", "spe", "Spe");
                }
            } catch (Throwable ignored) {}

            Object natureObj = pokemon.getNature();
            if (natureObj != null) data.nature = extractStringId(natureObj);

            Object abilityObj = pokemon.getAbility();
            if (abilityObj != null) data.ability = extractStringId(abilityObj);

            try {
                Object speciesObj = pokemon.getSpecies();
                if (speciesObj != null) {
                    data.possibleAbilities.addAll(extractAbilityIdsFromSpecies(speciesObj));
                }
                if (data.possibleAbilities.isEmpty() && formObj != null) {
                    data.possibleAbilities.addAll(extractAbilityIdsFromSpecies(formObj));
                }
            } catch (Throwable ignored) {}

            try {
                Object moveSetObj = callNoArgs(pokemon, "getMoveSet", "moveSet");
                if (moveSetObj == null) moveSetObj = pokemon.getMoveSet();
                if (moveSetObj != null) {
                    PokemonEditorDebug.infoMoves("[MoveRead] START reading moveSet (class={})", moveSetObj.getClass().getName());
                    List<String> moveNames = new ArrayList<>();
                    int[] movePPs = new int[4];
                    int[] moveMaxPPs = new int[4];
                    int[] moveRaised = new int[4];
                    for (int i = 0; i < 4; i++) moveNames.add("");

                    boolean usedInternal = readMovesFromInternalArray(moveSetObj, moveNames, movePPs, moveMaxPPs, moveRaised);
                    PokemonEditorDebug.infoMoves("[MoveRead] usedInternal={}", usedInternal);

                    if (!usedInternal) {
                        boolean usedIndexed = readMovesByIndex(moveSetObj, moveNames, movePPs, moveMaxPPs, moveRaised);
                        PokemonEditorDebug.infoMoves("[MoveRead] usedIndexed={}", usedIndexed);
                        if (!usedIndexed) {
                            PokemonEditorDebug.warnMoves("[MoveRead] FALLBACK to iterator (may skip empty slots)");
                            int idx = 0;
                            try {
                                Object itObj = callNoArgs(moveSetObj, "iterator");
                                if (itObj instanceof java.util.Iterator<?> it) {
                                    while (it.hasNext() && idx < 4) {
                                        Object mv = it.next();
                                        if (mv instanceof Move move) {
                                            MoveTemplate tpl = move.getTemplate();
                                            String nm = (tpl != null) ? tpl.getName() : move.getName();
                                            moveNames.set(idx, nm == null ? "" : nm);
                                            movePPs[idx] = move.getCurrentPp();
                                            moveMaxPPs[idx] = move.getMaxPp();
                                            moveRaised[idx] = move.getRaisedPpStages();
                                        }
                                        idx++;
                                    }
                                }
                            } catch (Throwable t1) {
                                try {
                                    Object listObj = callNoArgs(moveSetObj, "getMoves", "moves");
                                    if (listObj instanceof List<?> list) {
                                        for (Object o : list) {
                                            if (idx >= 4) break;
                                            if (o instanceof Move move) {
                                                MoveTemplate tpl = move.getTemplate();
                                                String nm = (tpl != null) ? tpl.getName() : move.getName();
                                                moveNames.set(idx, nm == null ? "" : nm);
                                                movePPs[idx] = move.getCurrentPp();
                                                moveMaxPPs[idx] = move.getMaxPp();
                                                moveRaised[idx] = move.getRaisedPpStages();
                                            }
                                            idx++;
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                    }

                    PokemonEditorDebug.infoMoves("[MoveRead] FINAL result: slot0='{}' slot1='{}' slot2='{}' slot3='{}' pp=[{},{},{},{}]",
                            moveNames.get(0), moveNames.get(1), moveNames.get(2), moveNames.get(3),
                            movePPs[0], movePPs[1], movePPs[2], movePPs[3]);

                    while (moveNames.size() < 4) moveNames.add("");
                    data.move1 = moveNames.get(0);
                    data.move2 = moveNames.get(1);
                    data.move3 = moveNames.get(2);
                    data.move4 = moveNames.get(3);
                    data.move1PP = movePPs[0];
                    data.move2PP = movePPs[1];
                    data.move3PP = movePPs[2];
                    data.move4PP = movePPs[3];
                    data.move1MaxPP = moveMaxPPs[0];
                    data.move2MaxPP = moveMaxPPs[1];
                    data.move3MaxPP = moveMaxPPs[2];
                    data.move4MaxPP = moveMaxPPs[3];
                    data.move1Raised = moveRaised[0];
                    data.move2Raised = moveRaised[1];
                    data.move3Raised = moveRaised[2];
                    data.move4Raised = moveRaised[3];
                }
            } catch (Throwable ignored) {}

            Object genderObj = pokemon.getGender();
            if (genderObj != null) {
                String gn = genderObj.toString().toUpperCase();
                if (gn.contains("MALE") && !gn.contains("FE")) data.gender = 0;
                else if (gn.contains("FEMALE")) data.gender = 1;
                else data.gender = 2;
            }

            // ★ 读取金色王冠状态（通过cobblemon内置的isHyperTrained判断，全部反射避免KMappedMarker问题）
            try {
                // [优化-B01] 缓存getIvs方法 + isHyperTrained方法 + 6个Stats字段，避免每次pokemonToData都反射
                if (cachedGetIvsMethod == null) {
                    cachedGetIvsMethod = pokemon.getClass().getMethod("getIvs");
                }
                var ivs = cachedGetIvsMethod.invoke(pokemon);
                if (ivs != null) {
                    initBridgeCache();
                    if (cachedIsHyperTrainedMethod == null) {
                        cachedIsHyperTrainedMethod = ivs.getClass().getMethod("isHyperTrained", cachedStatClass);
                    }
                    // 缓存6个Stats常量Field
                    if (cachedStatsHPField == null) {
                        cachedStatsHPField = cachedStatsClass.getField("HP");
                        cachedStatsATKField = cachedStatsClass.getField("ATTACK");
                        cachedStatsDEFField = cachedStatsClass.getField("DEFENCE");
                        cachedStatsSPAField = cachedStatsClass.getField("SPECIAL_ATTACK");
                        cachedStatsSPDField = cachedStatsClass.getField("SPECIAL_DEFENCE");
                        cachedStatsSPEField = cachedStatsClass.getField("SPEED");
                    }
                    boolean allHyperTrained = (boolean) cachedIsHyperTrainedMethod.invoke(ivs, cachedStatsHPField.get(null))
                        && (boolean) cachedIsHyperTrainedMethod.invoke(ivs, cachedStatsATKField.get(null))
                        && (boolean) cachedIsHyperTrainedMethod.invoke(ivs, cachedStatsDEFField.get(null))
                        && (boolean) cachedIsHyperTrainedMethod.invoke(ivs, cachedStatsSPAField.get(null))
                        && (boolean) cachedIsHyperTrainedMethod.invoke(ivs, cachedStatsSPDField.get(null))
                        && (boolean) cachedIsHyperTrainedMethod.invoke(ivs, cachedStatsSPEField.get(null));
                    data.goldenBottleCap = allHyperTrained;
                }
            } catch (Throwable ignored) {}

            try {
                Object nick = pokemon.getNickname();
                if (nick != null) data.displayName = ((net.minecraft.network.chat.Component) nick).getString();
            } catch (Throwable ignored) {}

        } catch (Throwable t) {
            PokemonEditorDebug.warnEditor("pokemonToData error: {}", t.getMessage());
        }
        return data;
    }

    public static void writePokemonToSlot(ServerPlayer serverPlayer, UUID targetUUID, PokemonEditorNetwork.UpdatePokemonPacket pkt) {
        ServerPlayer targetPlayer = resolveTargetPlayer(serverPlayer, targetUUID);
        if (targetPlayer == null) {
            PokemonEditorDebug.errorEditor("targetPlayer is null, abort write");
            return;
        }

        try {
            Object storage = Cobblemon.INSTANCE.getStorage();
            Object party = getPartyFromStorage(storage, targetPlayer);
            if (party == null) {
                PokemonEditorDebug.errorEditor("party is null, abort write");
                return;
            }

            PokemonEditorDebug.logEditor("party class={}, slot={}", party.getClass().getName(), pkt.slot());
            PokemonEditorDebug.logEditor("pkt: species={}, level={}, shiny={}, friendship={}, ivHp={}, evHp={}, move1={}, uuid={}",
                    pkt.speciesName(), pkt.level(), pkt.shiny(), pkt.friendship(),
                    pkt.ivHp(), pkt.evHp(), pkt.move1(), pkt.pokemonUUID());

            Pokemon pokemon = null;
            int targetSlot = pkt.slot();

            try {
                // [优化-B02] 使用缓存的getSlots方法，避免每次while遍历父类查找
                if (cachedPartyGetSlotsMethod == null) {
                    Class<?> cls = party.getClass();
                    while (cls != null && cachedPartyGetSlotsMethod == null) {
                        try { cachedPartyGetSlotsMethod = cls.getDeclaredMethod("getSlots"); }
                        catch (NoSuchMethodException ignored) {}
                        cls = cls.getSuperclass();
                    }
                    if (cachedPartyGetSlotsMethod != null) cachedPartyGetSlotsMethod.setAccessible(true);
                }
                if (cachedPartyGetSlotsMethod != null) {
                    java.util.List<?> slotsList = (java.util.List<?>) cachedPartyGetSlotsMethod.invoke(party);
                    if (targetSlot >= 0 && targetSlot < slotsList.size()) {
                        Object result = slotsList.get(targetSlot);
                        if (result instanceof Pokemon) pokemon = (Pokemon) result;
                    }
                    PokemonEditorDebug.logEditor("write getSlots() OK, list[{}]={}", targetSlot, pokemon != null ? "pokemon" : "null");
                }
            } catch (Throwable t) {
                PokemonEditorDebug.warnEditor("write getSlots() failed: {}", t.getMessage());
            }

            if (pokemon == null) {
                try {
                    java.lang.reflect.Field slotsField = null;
                    Class<?> cls = party.getClass();
                    while (cls != null && slotsField == null) {
                        try {
                            slotsField = cls.getDeclaredField("slots");
                        } catch (NoSuchFieldException ignored) {}
                        cls = cls.getSuperclass();
                    }
                    if (slotsField != null) {
                        slotsField.setAccessible(true);
                        Object slotsObj = slotsField.get(party);
                        if (slotsObj instanceof java.util.List) {
                            java.util.List<?> slotsList = (java.util.List<?>) slotsObj;
                            if (targetSlot >= 0 && targetSlot < slotsList.size()) {
                                Object result = slotsList.get(targetSlot);
                                if (result instanceof Pokemon) pokemon = (Pokemon) result;
                            }
                        }
                        PokemonEditorDebug.logEditor("write slots field OK, list[{}]={}", targetSlot, pokemon != null ? "pokemon" : "null");
                    }
                } catch (Throwable t) {
                    PokemonEditorDebug.warnEditor("write slots field failed: {}", t.getMessage());
                }
            }

            if (pokemon == null) {
                try {
                    // [优化-B02] 缓存party.get(int)
                    if (cachedPartyGetIntMethod == null) {
                        cachedPartyGetIntMethod = party.getClass().getMethod("get", int.class);
                    }
                    Object result = cachedPartyGetIntMethod.invoke(party, targetSlot);
                    if (result instanceof Pokemon) pokemon = (Pokemon) result;
                    PokemonEditorDebug.logEditor("write get(int) fallback, result={}", pokemon != null ? "pokemon" : "null");
                } catch (Throwable t) {
                    PokemonEditorDebug.warnEditor("write get(int) failed: {}", t.getMessage());
                }
            }

            if (pokemon == null) {
                if (pkt.speciesName() == null || pkt.speciesName().isEmpty()) {
                    PokemonEditorDebug.logEditor("empty species, abort");
                    return;
                }
                com.cobblemon.mod.common.pokemon.Species speciesObj =
                        (com.cobblemon.mod.common.pokemon.Species) lookupSpeciesObj(pkt.speciesName());
                if (speciesObj != null) {
                    pokemon = speciesObj.create(Math.max(1, pkt.level()));
                    if (pokemon != null) {
                        try {
                            if (pokemon.getUuid() == null) {
                                String userUuid = pkt.pokemonUUID();
                                if (userUuid != null && !userUuid.isEmpty()) {
                                    pokemon.setUuid(UUID.fromString(userUuid));
                                } else {
                                    pokemon.setUuid(UUID.randomUUID());
                                }
                            }
                        } catch (Throwable ignored) {}
                        // [线性-L09] 设置原始训练师：空槽位创建时Species.create()不设OT，导致精灵被视为野生
                        // [线性-L09a] 必须同时设置UUID和名称，仅UUID不显示训练师名
                        try {
                            pokemon.setOriginalTrainer(targetPlayer.getUUID());
                            PokemonEditorDebug.logEditor("originalTrainer UUID set to {}", targetPlayer.getUUID());
                        } catch (Throwable t) {
                            PokemonEditorDebug.warnEditor("setOriginalTrainer(UUID) failed: {}", t.getMessage());
                        }
                        try {
                            // 尝试多种可能的签名设置训练师名称
                            boolean nameSet = false;
                            for (java.lang.reflect.Method m : pokemon.getClass().getMethods()) {
                                if (m.getName().equals("setOriginalTrainerName") && m.getParameterCount() == 1) {
                                    Class<?> pt = m.getParameterTypes()[0];
                                    try {
                                        if (pt == String.class) {
                                            m.invoke(pokemon, targetPlayer.getName().getString());
                                            nameSet = true;
                                            break;
                                        } else if (pt.isAssignableFrom(net.minecraft.network.chat.Component.class)) {
                                            m.invoke(pokemon, targetPlayer.getName());
                                            nameSet = true;
                                            break;
                                        }
                                    } catch (Throwable ignored) {}
                                }
                            }
                            if (nameSet) {
                                PokemonEditorDebug.logEditor("originalTrainerName set to {}", targetPlayer.getName().getString());
                            } else {
                                PokemonEditorDebug.warnEditor("setOriginalTrainerName method not found");
                            }
                        } catch (Throwable t) {
                            PokemonEditorDebug.warnEditor("setOriginalTrainerName failed: {}", t.getMessage());
                        }
                        try {
                            initBridgeCache(); // [线性-L07] 确保PartyPosition已缓存
                            Object position = cachedPartyPositionCtor.newInstance(targetSlot);
                            // [线性-L13] 使用 party.set(PartyPosition, pokemon) 而非 setAtPosition
                            //    PokemonStore.set() 内部调用 pokemon.getStoreCoordinates().set(new StoreCoordinates(store, position))
                            //    这是 getOwnerPlayer() 能返回正确玩家的关键 — 仅 setAtPosition 不设置 storeCoordinates
                            Method setM = party.getClass().getMethod("set", cachedPartyPositionClass, Pokemon.class);
                            setM.invoke(party, position, pokemon);
                            PokemonEditorDebug.logEditor("party.set(PartyPosition({}), Pokemon) OK — storeCoordinates已自动设置", targetSlot);
                        } catch (Throwable t) {
                            PokemonEditorDebug.warnEditor("party.set(PartyPosition, Pokemon) failed: {}", t.getMessage());
                        }
                    }
                }
            }

            if (pokemon == null) {
                PokemonEditorDebug.warnEditor("pokemon is still null, abort");
                return;
            }

            try {
                PokemonEditorDebug.logEditor("BEFORE modify: species={}, level={}, shiny={}, hp={}",
                        pokemon.getSpecies().toString(), pokemon.getLevel(), pokemon.getShiny(), pokemon.getCurrentHealth());
            } catch (Throwable t) {
                PokemonEditorDebug.warnEditor("BEFORE log failed: {}", t.getMessage());
            }

            net.minecraft.core.RegistryAccess regAccess = targetPlayer.server.registryAccess();
            String rawData = pkt.rawJsonData();
            if (rawData != null && !rawData.trim().isEmpty() && rawData.trim().startsWith("{")) {
                try {
                    JsonObject jsonObj = GSON.fromJson(rawData, JsonObject.class);
                    if (jsonObj != null) {
                        pokemon.loadFromJSON(regAccess, jsonObj);
                        PokemonEditorDebug.logEditor("applied JSON data");
                    }
                } catch (Throwable t) {
                    PokemonEditorDebug.warnEditor("JSON modify failed, fall back to fields: {}", t.getMessage());
                    writePokemonFields(pokemon, pkt, regAccess);
                }
            } else {
                PokemonEditorDebug.logEditor("applying field-level modifications (species={}, level={})",
                        pkt.speciesName(), pkt.level());
                writePokemonFields(pokemon, pkt, regAccess);
            }

            try {
                PokemonEditorDebug.logEditor("AFTER modify: species={}, level={}, shiny={}, hp={}",
                        pokemon.getSpecies().toString(), pokemon.getLevel(), pokemon.getShiny(), pokemon.getCurrentHealth());
            } catch (Throwable t) {
                PokemonEditorDebug.warnEditor("AFTER log failed: {}", t.getMessage());
            }

            try {
                // [优化-B04] 缓存 party.onPokemonChanged(Pokemon)
                if (cachedPartyNotifyChangedMethod == null) {
                    cachedPartyNotifyChangedMethod = party.getClass().getMethod("onPokemonChanged", Pokemon.class);
                }
                cachedPartyNotifyChangedMethod.invoke(party, pokemon);
                PokemonEditorDebug.logEditor("party.onPokemonChanged(pokemon) OK");
            } catch (Throwable t) {
                try {
                    Method notifyM = party.getClass().getMethod("notifyChanged", Pokemon.class);
                    notifyM.invoke(party, pokemon);
                    PokemonEditorDebug.logEditor("FALLBACK: party.notifyChanged(pokemon) OK");
                } catch (Throwable t2) {
                    PokemonEditorDebug.warnEditor("party.onPokemonChanged failed: {}", t.getMessage());
                }
            }

            boolean syncOK = false;
            try {
                // [优化-B03] 缓存party.sendTo(ServerPlayer)
                if (cachedPartySendToMethod == null) {
                    cachedPartySendToMethod = party.getClass().getMethod("sendTo", ServerPlayer.class);
                }
                cachedPartySendToMethod.invoke(party, targetPlayer);
                syncOK = true;
                PokemonEditorDebug.logEditor("party.sendTo(ServerPlayer) OK - update pushed to client");
            } catch (Throwable t) {
                try {
                    Method sendToM2 = party.getClass().getMethod("sendTo", net.minecraft.world.entity.player.Player.class);
                    sendToM2.invoke(party, targetPlayer);
                    syncOK = true;
                    PokemonEditorDebug.logEditor("party.sendTo(Player) OK");
                } catch (Throwable t2) {
                    for (Method m : party.getClass().getMethods()) {
                        String name = m.getName().toLowerCase();
                        if ((name.contains("send") || name.contains("sync") || name.contains("notify") || name.contains("flush") || name.contains("update"))
                                && m.getParameterCount() <= 1) {
                            try {
                                if (m.getParameterCount() == 0) {
                                    m.invoke(party);
                                } else if (m.getParameterTypes()[0].isInstance(targetPlayer)) {
                                    m.invoke(party, targetPlayer);
                                } else {
                                    continue;
                                }
                                syncOK = true;
                                PokemonEditorDebug.logEditor("FALLBACK: party.{}() OK - update pushed to client", m.getName());
                                break;
                            } catch (Throwable ignored) {}
                        }
                    }
                    if (!syncOK) {
                        PokemonEditorDebug.errorEditor("All sync methods failed - moves may not update on client until Summary UI is opened");
                    }
                }
            }

            try {
                // [优化-B05] 缓存 pokemon.update()
                if (cachedPokemonUpdateMethod == null) {
                    cachedPokemonUpdateMethod = pokemon.getClass().getMethod("update");
                }
                cachedPokemonUpdateMethod.invoke(pokemon);
                PokemonEditorDebug.logEditor("pokemon.update() OK");
            } catch (Throwable ignored) {}

            try {
                Object moveSetObj = pokemon.getMoveSet();
                if (moveSetObj != null) {
                    // [优化-B05] 缓存 moveSet.update()
                    if (cachedMoveSetUpdateMethod == null) {
                        cachedMoveSetUpdateMethod = moveSetObj.getClass().getMethod("update");
                    }
                    cachedMoveSetUpdateMethod.invoke(moveSetObj);
                    PokemonEditorDebug.logEditor("moveSet.update() OK");
                }
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            PokemonEditorDebug.errorEditor("writePokemonToSlot fatal error: {}", t.getMessage());
            t.printStackTrace();
        }
    }

    public static void deletePokemonFromSlot(ServerPlayer serverPlayer, UUID targetUUID, int slot) {
        ServerPlayer targetPlayer = resolveTargetPlayer(serverPlayer, targetUUID);
        if (targetPlayer == null) {
            PokemonEditorDebug.errorEditor("targetPlayer is null, abort delete");
            return;
        }

        try {
            Object storage = Cobblemon.INSTANCE.getStorage();
            Object party = getPartyFromStorage(storage, targetPlayer);
            if (party == null) {
                PokemonEditorDebug.errorEditor("party is null, abort delete");
                return;
            }

            PokemonEditorDebug.logEditor("DELETE - party class={}, slot={}", party.getClass().getName(), slot);

            Object pokemon = null;
            try {
                // [优化-B06] 复用缓存的getSlots方法
                if (cachedPartyGetSlotsMethod == null) {
                    Class<?> cls = party.getClass();
                    while (cls != null && cachedPartyGetSlotsMethod == null) {
                        try { cachedPartyGetSlotsMethod = cls.getDeclaredMethod("getSlots"); }
                        catch (NoSuchMethodException ignored) {}
                        cls = cls.getSuperclass();
                    }
                    if (cachedPartyGetSlotsMethod != null) cachedPartyGetSlotsMethod.setAccessible(true);
                }
                if (cachedPartyGetSlotsMethod != null) {
                    java.util.List<?> slotsList = (java.util.List<?>) cachedPartyGetSlotsMethod.invoke(party);
                    if (slot >= 0 && slot < slotsList.size()) {
                        pokemon = slotsList.get(slot);
                    }
                    PokemonEditorDebug.logEditor("getSlots() OK, list[{}]={}", slot, pokemon != null ? "pokemon" : "null");
                }
            } catch (Throwable t) {
                PokemonEditorDebug.warnEditor("getSlots() failed: {}", t.getMessage());
            }

            if (pokemon == null) {
                try {
                    java.lang.reflect.Field slotsField = null;
                    Class<?> cls = party.getClass();
                    while (cls != null && slotsField == null) {
                        try {
                            slotsField = cls.getDeclaredField("slots");
                        } catch (NoSuchFieldException ignored) {}
                        cls = cls.getSuperclass();
                    }
                    if (slotsField != null) {
                        slotsField.setAccessible(true);
                        Object slotsObj = slotsField.get(party);
                        if (slotsObj instanceof java.util.List) {
                            java.util.List<?> slotsList = (java.util.List<?>) slotsObj;
                            if (slot >= 0 && slot < slotsList.size()) {
                                pokemon = slotsList.get(slot);
                            }
                        }
                        PokemonEditorDebug.logEditor("slots field OK, list[{}]={}", slot, pokemon != null ? "pokemon" : "null");
                    }
                } catch (Throwable t) {
                    PokemonEditorDebug.warnEditor("slots field failed: {}", t.getMessage());
                }
            }

            if (pokemon == null) {
                try {
                    // [优化-B06] 复用缓存的get(int)
                    if (cachedPartyGetIntMethod == null) {
                        cachedPartyGetIntMethod = party.getClass().getMethod("get", int.class);
                    }
                    pokemon = cachedPartyGetIntMethod.invoke(party, slot);
                    PokemonEditorDebug.logEditor("get(int) fallback, result={}", pokemon != null ? "pokemon" : "null");
                } catch (Throwable t) {
                    PokemonEditorDebug.warnEditor("get(int) failed: {}", t.getMessage());
                }
            }
            if (pokemon == null) {
                PokemonEditorDebug.logEditor("slot {} already empty, nothing to do", slot);
                return;
            }
            PokemonEditorDebug.logEditor("Found pokemon to delete: {}", ((Pokemon) pokemon).getSpecies().toString());

            try {
                initBridgeCache(); // [优化-B07] 使用缓存的PartyPosition
                Object position = cachedPartyPositionCtor.newInstance(slot);

                // [优化-B07] 缓存 setAtPosition
                if (cachedPartySetAtPositionMethod == null) {
                    Class<?> cls = party.getClass();
                    while (cls != null && cachedPartySetAtPositionMethod == null) {
                        try { cachedPartySetAtPositionMethod = cls.getDeclaredMethod("setAtPosition", cachedPartyPositionClass, Pokemon.class); }
                        catch (NoSuchMethodException ignored) {}
                        cls = cls.getSuperclass();
                    }
                    if (cachedPartySetAtPositionMethod != null) cachedPartySetAtPositionMethod.setAccessible(true);
                }
                if (cachedPartySetAtPositionMethod != null) {
                    cachedPartySetAtPositionMethod.invoke(party, position, null);
                    PokemonEditorDebug.logEditor("setAtPosition(slot={}, null) OK", slot);
                } else {
                    throw new RuntimeException("setAtPosition not found");
                }
            } catch (Throwable t) {
                PokemonEditorDebug.errorEditor("setAtPosition(slot={}, null) FAILED: {}", slot, t.toString());
                try {
                    Method removeM = party.getClass().getMethod("remove", Pokemon.class);
                    removeM.invoke(party, pokemon);
                    PokemonEditorDebug.logEditor("FALLBACK: party.remove(pokemon) OK");
                } catch (Throwable t2) {
                    PokemonEditorDebug.errorEditor("FALLBACK also failed: {}", t2.toString());
                }
            }

            try {
                // [优化-B07] 复用缓存的sendTo
                if (cachedPartySendToMethod == null) {
                    cachedPartySendToMethod = party.getClass().getMethod("sendTo", ServerPlayer.class);
                }
                cachedPartySendToMethod.invoke(party, targetPlayer);
                PokemonEditorDebug.logEditor("party.sendTo(player) OK - synced to client");
            } catch (Throwable t) {
                PokemonEditorDebug.errorEditor("party.sendTo(player) FAILED: {}", t.toString());
            }
        } catch (Throwable t) {
            PokemonEditorDebug.errorEditor("deletePokemonFromSlot fatal error: {}", t.getMessage());
            t.printStackTrace();
        }
    }

    public static int getBasePP(String moveName) {
        MoveTemplate tpl = lookupMoveTemplate(moveName);
        if (tpl != null) return tpl.getPp();
        return 0;
    }

    private static MoveTemplate lookupMoveTemplate(String name) {
        if (name == null || name.isEmpty()) {
            PokemonEditorDebug.warnMoves("[MoveLookup] input is null/empty, return null");
            return null;
        }
        PokemonEditorDebug.infoMoves("[MoveLookup] input='{}'", name);
        String cleanName = name.trim();
        if (cleanName.contains("cobblemon.move.") || cleanName.contains(".move.")) {
            int idx = cleanName.lastIndexOf('.');
            if (idx >= 0 && idx < cleanName.length() - 1) {
                cleanName = cleanName.substring(idx + 1);
            }
        }
        String key = "mvtpl#" + cleanName.toLowerCase();
        PokemonEditorDebug.infoMoves("[MoveLookup] cleanName='{}' cacheKey='{}'", cleanName, key);
        Object cached = registryCache.get(key);
        if (cached instanceof MoveTemplate) {
            PokemonEditorDebug.infoMoves("[MoveLookup] cache HIT for '{}', return cached", key);
            return (MoveTemplate) cached;
        }
        if (cached != null) {
            PokemonEditorDebug.warnMoves("[MoveLookup] cache MISS but has cached=null for '{}', return null", key);
            return null;
        }

        String lowerName = cleanName.toLowerCase();
        MoveTemplate result = null;

        try {
            initBridgeCache(); // [线性-L07] 使用缓存的Moves类
            for (java.lang.reflect.Method m : cachedMovesClass.getMethods()) {
                if (m.getName().equals("getByName") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == String.class && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                    Object r = m.invoke(null, cleanName);
                    if (r instanceof MoveTemplate) { result = (MoveTemplate) r; PokemonEditorDebug.infoMoves("[MoveLookup] Moves.getByName('{}') OK", cleanName); break; }
                    if (result == null) {
                        Object r2 = m.invoke(null, lowerName);
                        if (r2 instanceof MoveTemplate) { result = (MoveTemplate) r2; PokemonEditorDebug.infoMoves("[MoveLookup] Moves.getByName('{}') OK", lowerName); break; }
                    }
                }
            }
        } catch (Throwable ignored) {}

        if (result == null) {
            try {
                Object companion = MoveTemplate.class.getField("Companion").get(null);
            for (Method m : companion.getClass().getMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class
                        && !m.getName().equals("toString")) {
                    try {
                        Object r = m.invoke(companion, lowerName);
                        if (r instanceof MoveTemplate) { result = (MoveTemplate) r; break; }
                    } catch (Throwable ignored) {}
                }
            }
            if (result == null && !lowerName.isEmpty()) {
                String capName = Character.toUpperCase(lowerName.charAt(0)) + lowerName.substring(1);
                for (Method m : companion.getClass().getMethods()) {
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) {
                        try {
                            Object r = m.invoke(companion, capName);
                            if (r instanceof MoveTemplate) { result = (MoveTemplate) r; break; }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}
        }

        if (result == null) {
            try {
                Object companion = Move.class.getField("Companion").get(null);
                for (Method m : companion.getClass().getMethods()) {
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class
                            && !m.getName().equals("toString")) {
                        try {
                            Object r = m.invoke(companion, lowerName);
                            if (r instanceof Move move) { result = move.getTemplate(); break; }
                            if (r instanceof MoveTemplate) { result = (MoveTemplate) r; break; }
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (result == null) {
            try {
                List<Object> allTpls = getAllMoveTemplateObjects();
                for (Object obj : allTpls) {
                    if (obj instanceof MoveTemplate tpl) {
                        String tplName = tpl.getName();
                        if (tplName != null && tplName.equalsIgnoreCase(lowerName)) {
                            result = tpl;
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (result == null) {
            try {
                for (java.lang.reflect.Field f : Cobblemon.class.getFields()) {
                    String fn = f.getName().toLowerCase();
                    if (fn.contains("move") && !fn.contains("pokemon") && !fn.contains("party") && !fn.contains("species")) {
                        try {
                            Object val = f.get(null);
                            if (val instanceof Iterable<?> iter) {
                                for (Object obj : iter) {
                                    if (obj instanceof MoveTemplate tpl) {
                                        String tplName = tpl.getName();
                                        if (tplName != null && tplName.equalsIgnoreCase(lowerName)) {
                                            result = tpl;
                                            break;
                                        }
                                    }
                                }
                                if (result != null) break;
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }

        putToRegistryCache(key, result);  // [优化-C01] 使用上限保护的写入
        if (result != null) {
            PokemonEditorDebug.infoMoves("[MoveLookup] lookup('{}') -> FOUND template '{}' (pp={})",
                    name, result.getName(), result.getPp());
        } else {
            PokemonEditorDebug.warnMoves("[MoveLookup] lookup('{}') -> NOT FOUND (cleanName='{}', lowerName='{}')",
                    name, cleanName, lowerName);
        }
        return result;
    }

    private static List<Object> getAllMoveTemplateObjects() {
        List<Object> result = new ArrayList<>();
        try {
            Object companion = MoveTemplate.class.getField("Companion").get(null);
            for (Method m : companion.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && !m.getName().equals("toString")
                        && !m.getName().equals("hashCode") && !m.getName().equals("equals")) {
                    try {
                        Object r = m.invoke(companion);
                        if (r instanceof Iterable<?> iter) {
                            for (Object obj : iter) {
                                if (obj instanceof MoveTemplate) {
                                    result.add(obj);
                                }
                            }
                            if (!result.isEmpty()) break;
                        }
                    } catch (Throwable ignored) {}
                }
            }
            if (result.isEmpty()) {
                for (java.lang.reflect.Field f : Cobblemon.class.getFields()) {
                    String fn = f.getName().toLowerCase();
                    if (fn.contains("move") && !fn.contains("pokemon") && !fn.contains("party") && !fn.contains("species")) {
                        try {
                            Object val = f.get(null);
                            if (val instanceof Iterable<?> iter) {
                                for (Object obj : iter) {
                                    if (obj instanceof MoveTemplate) {
                                        result.add(obj);
                                    }
                                }
                                if (!result.isEmpty()) break;
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}
        return result;
    }

    private static Field moveSetMovesField = null;
    private static boolean moveSetFieldIsList = false;
    private static boolean moveSetFieldResolved = false;

    private static boolean moveClassProbed = false;
    private static void probeMoveClassIfNeeded(Object sampleMove) {
        if (moveClassProbed || sampleMove == null) return;
        if (!PokemonEditorDebug.isMovesDebugEnabled() && !PokemonEditorDebug.isDebugEnabled()) return;
        moveClassProbed = true;
        try {
            Class<?> cls = sampleMove.getClass();
            PokemonEditorDebug.infoMoves("[MoveProbe] === probing Move class: {} ===", cls.getName());
            for (Field f : cls.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object v = f.get(sampleMove);
                    String valStr = (v == null) ? "null" : (v.getClass().isArray() ? java.util.Arrays.toString((Object[]) v) : v.toString());
                    if (valStr.length() > 80) valStr = valStr.substring(0, 80) + "...";
                    PokemonEditorDebug.infoMoves("[MoveProbe]   field {}: {} = {}",
                            f.getName(), f.getType().getSimpleName(), valStr);
                } catch (Throwable ignored) {}
            }
            StringBuilder ctors = new StringBuilder();
            for (java.lang.reflect.Constructor<?> c : cls.getDeclaredConstructors()) {
                if (ctors.length() > 0) ctors.append(", ");
                ctors.append("(");
                Class<?>[] pts = c.getParameterTypes();
                for (int i = 0; i < pts.length; i++) {
                    if (i > 0) ctors.append(",");
                    ctors.append(pts[i].getSimpleName());
                }
                ctors.append(")");
            }
            PokemonEditorDebug.infoMoves("[MoveProbe]   constructors: {}", ctors.toString());
            StringBuilder methods = new StringBuilder();
            for (Method m : cls.getMethods()) {
                String name = m.getName();
                if (name.startsWith("get") || name.startsWith("set") || name.equals("create")
                        || name.equals("iterator") || name.equals("hasSpace") || name.equals("copy")) {
                    if (methods.length() > 0) methods.append(", ");
                    methods.append(name).append("(").append(m.getParameterCount()).append(")");
                }
            }
            if (methods.length() > 0) PokemonEditorDebug.infoMoves("[MoveProbe]   key methods: {}", methods.toString());
        } catch (Throwable t) {
            PokemonEditorDebug.warnMoves("[MoveProbe] probe failed: {}", t.getMessage());
        }
    }

    private static void verifyMoveInstance(Object moveObj, String expectedName, int slot) {
        if (!PokemonEditorDebug.isMovesDebugEnabled() && !PokemonEditorDebug.isDebugEnabled()) return;
        if (moveObj == null) {
            PokemonEditorDebug.warnMoves("[MoveVerify] slot {}: instance is null!", slot);
            return;
        }
        try {
            Class<?> cls = moveObj.getClass();
            StringBuilder sb = new StringBuilder();
            sb.append("slot").append(slot).append(": ");
            String[] getters = {"getTemplate", "getCurrentPp", "getMaxPp", "getPp", "getRaisedPpStages", "getRaised", "getName", "toString"};
            for (String g : getters) {
                try {
                    Method m = cls.getMethod(g);
                    Object v = m.invoke(moveObj);
                    String s = (v == null) ? "null" : v.toString();
                    if (s.length() > 60) s = s.substring(0, 60) + "...";
                    sb.append(g).append("=").append(s).append(" ");
                } catch (Throwable ignored) {}
            }
            for (Field f : cls.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object v = f.get(moveObj);
                    String s = (v == null) ? "null" : v.toString();
                    if (s.length() > 40) s = s.substring(0, 40) + "...";
                    sb.append("[").append(f.getName()).append("=").append(s).append("] ");
                } catch (Throwable ignored) {}
            }
            PokemonEditorDebug.infoMoves("[MoveVerify] {}", sb.toString());
        } catch (Throwable t) {
            PokemonEditorDebug.warnMoves("[MoveVerify] slot {} error: {}", slot, t.getMessage());
        }
    }

    private static Field getMoveSetMovesField(Object moveSetObj) {
        if (!moveSetFieldResolved) {
            try {
                PokemonEditorDebug.infoMoves("[MoveSetProbe] === probing MoveSet class: {} ===",
                        moveSetObj.getClass().getName());
                for (Field f : moveSetObj.getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    Object val = null;
                    try { val = f.get(moveSetObj); } catch (Throwable ignored) {}
                    PokemonEditorDebug.infoMoves("[MoveSetProbe]   field {}: {} = {}",
                            f.getName(), f.getType().getSimpleName(),
                            (val != null ? val.toString() : "null"));
                }
                StringBuilder methodsSB = new StringBuilder();
                for (Method m : moveSetObj.getClass().getMethods()) {
                    String name = m.getName();
                    if (name.length() > 20) continue;
                    if (methodsSB.length() > 0) methodsSB.append(", ");
                    methodsSB.append(name).append("(").append(m.getParameterCount()).append(")");
                }
                PokemonEditorDebug.infoMoves("[MoveSetProbe]   methods: {}", methodsSB.toString());
            } catch (Throwable t) {
                PokemonEditorDebug.warnMoves("[MoveSetProbe] probe failed: {}", t.getMessage());
            }

            String[] candidates = {"moves", "_moves", "moveList", "movesList", "learnedMoves", "skillMoves"};
            for (String candidate : candidates) {
                try {
                    Field f = moveSetObj.getClass().getDeclaredField(candidate);
                    f.setAccessible(true);
                    Class<?> type = f.getType();
                    boolean isArray = type.isArray();
                    boolean isList = java.util.List.class.isAssignableFrom(type)
                            || java.util.Collection.class.isAssignableFrom(type);
                    if (isArray || isList) {
                        moveSetMovesField = f;
                        moveSetFieldIsList = isList;
                        PokemonEditorDebug.infoMoves("[MoveSetProbe] FOUND field '{}' (isList={}, type={})",
                                candidate, isList, type.getSimpleName());
                        break;
                    } else {
                        PokemonEditorDebug.infoMoves("[MoveSetProbe] field '{}' type={} (not array/list, skip)",
                                candidate, type.getSimpleName());
                    }
                } catch (NoSuchFieldException ignored) {
                } catch (Throwable t) {
                    PokemonEditorDebug.warnMoves("[MoveSetProbe] candidate '{}' error: {}", candidate, t.getMessage());
                }
            }
            moveSetFieldResolved = true;
            PokemonEditorDebug.infoMoves("[MoveSetProbe] final resolved: field={} isList={}",
                    (moveSetMovesField != null ? moveSetMovesField.getName() : "null"), moveSetFieldIsList);
        }
        return moveSetMovesField;
    }

    private static boolean readMovesFromInternalArray(Object moveSetObj, List<String> outNames,
                                                      int[] outPPs, int[] outMaxPPs, int[] outRaised) {
        Field f = getMoveSetMovesField(moveSetObj);
        if (f == null) {
            PokemonEditorDebug.warnMoves("[MoveRead] internal field not found, skip");
            return false;
        }
        PokemonEditorDebug.infoMoves("[MoveRead] internal field found, fieldType={}", f.getType().getName());
        try {
            Object val = f.get(moveSetObj);
            boolean ok = false;
            if (val instanceof Object[] arr) {
                PokemonEditorDebug.infoMoves("[MoveRead] array mode, len={}", arr.length);
                for (int i = 0; i < 4 && i < arr.length; i++) {
                    Object mvObj = arr[i];
                    PokemonEditorDebug.infoMoves("[MoveRead] slot{}: rawType={} isMove={}",
                            i, (mvObj != null ? mvObj.getClass().getName() : "null"),
                            (mvObj instanceof Move));
                    if (mvObj instanceof Move move) {
                        probeMoveClassIfNeeded(move);
                        MoveTemplate tpl = move.getTemplate();
                        String nm = (tpl != null) ? tpl.getName() : move.getName();
                        outNames.set(i, nm == null ? "" : nm);
                        outPPs[i] = move.getCurrentPp();
                        outMaxPPs[i] = move.getMaxPp();
                        outRaised[i] = move.getRaisedPpStages();
                        PokemonEditorDebug.infoMoves("[MoveRead] slot{}: name='{}' currentPP={} maxPP={} raised={}",
                                i, nm, outPPs[i], outMaxPPs[i], outRaised[i]);
                    } else {
                        outNames.set(i, "");
                        outPPs[i] = 0;
                        outMaxPPs[i] = 0;
                        outRaised[i] = 0;
                        PokemonEditorDebug.infoMoves("[MoveRead] slot{}: EMPTY/NULL", i);
                    }
                }
                ok = true;
            } else if (val instanceof java.util.List<?> list) {
                PokemonEditorDebug.infoMoves("[MoveRead] list mode, size={}", list.size());
                for (int i = 0; i < 4 && i < list.size(); i++) {
                    Object mvObj = list.get(i);
                    PokemonEditorDebug.infoMoves("[MoveRead] slot{}: rawType={} isMove={}",
                            i, (mvObj != null ? mvObj.getClass().getName() : "null"),
                            (mvObj instanceof Move));
                    if (mvObj instanceof Move move) {
                        probeMoveClassIfNeeded(move);
                        MoveTemplate tpl = move.getTemplate();
                        String nm = (tpl != null) ? tpl.getName() : move.getName();
                        outNames.set(i, nm == null ? "" : nm);
                        outPPs[i] = move.getCurrentPp();
                        outMaxPPs[i] = move.getMaxPp();
                        outRaised[i] = move.getRaisedPpStages();
                        PokemonEditorDebug.infoMoves("[MoveRead] slot{}: name='{}' currentPP={} maxPP={} raised={}",
                                i, nm, outPPs[i], outMaxPPs[i], outRaised[i]);
                    } else {
                        outNames.set(i, "");
                        outPPs[i] = 0;
                        outMaxPPs[i] = 0;
                        outRaised[i] = 0;
                        PokemonEditorDebug.infoMoves("[MoveRead] slot{}: EMPTY/NULL", i);
                    }
                }
                ok = true;
            } else if (val != null) {
                PokemonEditorDebug.warnMoves("[MoveRead] unsupported container type: {} (class={})",
                        val, val.getClass().getName());
            }
            if (ok) {
                PokemonEditorDebug.infoMoves("[MoveRead] internal read OK: [{},{},{},{}]",
                        outNames.get(0), outNames.get(1), outNames.get(2), outNames.get(3));
            }
            return ok;
        } catch (Throwable t) {
            PokemonEditorDebug.errorMoves("[MoveRead] internal read failed: {}", t.getMessage());
            t.printStackTrace();
        }
        return false;
    }

    private static Move constructMove(MoveTemplate tpl, int finalPP, int raised) {
        if (tpl == null) return null;
        PokemonEditorDebug.infoPP("[constructMove] template='{}' basePP={} finalPP={} raised={}",
                tpl.getName(), tpl.getPp(), finalPP, raised);
        try {
            // [优化-C02] 缓存Move(MoveTemplate, int, int)构造函数
            if (cachedMoveCtor == null) {
                for (java.lang.reflect.Constructor<?> c : Move.class.getDeclaredConstructors()) {
                    Class<?>[] pts = c.getParameterTypes();
                    if (pts.length == 3 && pts[1] == int.class && pts[2] == int.class) {
                        cachedMoveCtor = c;
                        cachedMoveCtor.setAccessible(true);
                        break;
                    }
                }
            }
            if (cachedMoveCtor != null) {
                Move result = (Move) cachedMoveCtor.newInstance(tpl, finalPP, raised);
                PokemonEditorDebug.infoPP("[constructMove] SUCCESS: created Move, getMaxPp={} getCurrentPp={}",
                        result.getMaxPp(), result.getCurrentPp());
                return result;
            }
            PokemonEditorDebug.warnPP("[constructMove] no matching ctor, trying Companion.create...");
            Object companion = Move.class.getField("Companion").get(null);
            for (Method m : companion.getClass().getMethods()) {
                if (m.getName().equals("create") && m.getParameterCount() >= 2) {
                    try {
                        PokemonEditorDebug.infoPP("[constructMove] trying Companion.{} with {} params",
                                m.getName(), m.getParameterCount());
                        return (Move) m.invoke(companion, tpl, finalPP, raised);
                    }
                    catch (Throwable ignored) {}
                }
            }
            PokemonEditorDebug.warnPP("[constructMove] Companion.create also failed");
        } catch (Throwable t) {
            PokemonEditorDebug.errorPP("[constructMove] FAILED: {}", t.toString());
        }
        return null;
    }

    private static class MovePPInfo {
        final int raised;
        final int finalPP;
        final int maxPP;
        MovePPInfo(int raised, int finalPP, int maxPP) {
            this.raised = raised;
            this.finalPP = finalPP;
            this.maxPP = maxPP;
        }
    }
    private static MovePPInfo calculatePP(MoveTemplate tpl, int userPP, int userMaxPP) {
        int basePP = tpl.getPp();
        int raised = 0;
        PokemonEditorDebug.infoPP("[calculatePP] template='{}' basePP={} userPP={} userMaxPP={}",
                tpl.getName(), basePP, userPP, userMaxPP);
        if (basePP > 0 && userMaxPP > basePP) {
            int bestRaised = 0;
            for (int r = 1; r <= 3; r++) {
                int expectedMax = (int) Math.floor(basePP * (1 + 0.2 * r));
                PokemonEditorDebug.infoPP("[calculatePP]   r={} expectedMax=floor({}*{})={}",
                        r, basePP, (1 + 0.2 * r), expectedMax);
                if (expectedMax == userMaxPP) {
                    bestRaised = r;
                }
            }
            raised = bestRaised;
            PokemonEditorDebug.infoPP("[calculatePP] userMaxPP={} -> bestRaised={}", userMaxPP, raised);
        }
        int maxPP = (raised > 0) ? (int) Math.floor(basePP * (1 + 0.2 * raised)) : basePP;
        int finalPP = (userPP > 0) ? Math.min(userPP, maxPP) : maxPP;
        PokemonEditorDebug.infoPP("[calculatePP] RESULT: raised={} finalPP={} maxPP={}", raised, finalPP, maxPP);
        return new MovePPInfo(raised, finalPP, maxPP);
    }

    @SuppressWarnings("unchecked")
    private static boolean writeMovesToInternalArray(Object moveSetObj,
                                                     String[] moveNames, int[] movePPs, int[] moveMaxPPs) {
        try {
            Method setMoveM = null;
            for (Method m : moveSetObj.getClass().getMethods()) {
                if (m.getName().equals("setMove") && m.getParameterCount() == 2
                        && m.getParameterTypes()[0] == int.class) {
                    setMoveM = m; break;
                }
            }
            if (setMoveM != null) {
                PokemonEditorDebug.infoMoves("[MoveWrite] using setMove(int, Move): names=['{}','{}','{}','{}']",
                        moveNames[0], moveNames[1], moveNames[2], moveNames[3]);
                for (int i = 0; i < 4; i++) {
                    String name = moveNames[i];
                    if (name == null || name.isEmpty()) {
                        try { setMoveM.invoke(moveSetObj, i, null); } catch (Throwable ignored) {}
                        continue;
                    }
                    MoveTemplate tpl = lookupMoveTemplate(name);
                    if (tpl == null) {
                        PokemonEditorDebug.warnMoves("[MoveWrite] slot {}: template NOT FOUND for '{}', setting to null", i, name);
                        try { setMoveM.invoke(moveSetObj, i, null); } catch (Throwable ignored) {}
                        continue;
                    }
                    MovePPInfo ppInfo = calculatePP(tpl, movePPs[i], moveMaxPPs[i]);
                    Move newMove = constructMove(tpl, ppInfo.finalPP, ppInfo.raised);
                    if (newMove != null) {
                        PokemonEditorDebug.infoPP("[MoveWrite] slot {}: setMove('{}') pp={}/{} raised={} | Move.getMaxPp={} getCurrentPp={}",
                                i, name, ppInfo.finalPP, ppInfo.maxPP, ppInfo.raised,
                                newMove.getMaxPp(), newMove.getCurrentPp());
                    } else {
                        PokemonEditorDebug.errorPP("[MoveWrite] slot {}: constructMove returned NULL for '{}', setting null", i, name);
                    }
                    setMoveM.invoke(moveSetObj, i, newMove);
                }
                return true;
            }
        } catch (Throwable t) {
            PokemonEditorDebug.errorMoves("[MoveWrite] setMove path failed: {}", t.getMessage());
        }

        Field f = getMoveSetMovesField(moveSetObj);
        if (f == null) {
            return false;
        }
        try {
            Object fieldVal = f.get(moveSetObj);
            if (fieldVal instanceof Object[] arr) {
                for (int i = 0; i < 4 && i < arr.length; i++) {
                    String name = moveNames[i];
                    if (name == null || name.isEmpty()) { arr[i] = null; continue; }
                    MoveTemplate tpl = lookupMoveTemplate(name);
                    if (tpl == null) { arr[i] = null; continue; }
                    MovePPInfo ppInfo = calculatePP(tpl, movePPs[i], moveMaxPPs[i]);
                    arr[i] = constructMove(tpl, ppInfo.finalPP, ppInfo.raised);
                }
                return true;
            } else if (fieldVal instanceof java.util.List<?> list) {
                java.util.List<Object> objList = (java.util.List<Object>) list;
                while (objList.size() < 4) objList.add(null);
                for (int i = 0; i < 4; i++) {
                    String name = moveNames[i];
                    if (name == null || name.isEmpty()) { objList.set(i, null); continue; }
                    MoveTemplate tpl = lookupMoveTemplate(name);
                    if (tpl == null) { objList.set(i, null); continue; }
                    MovePPInfo ppInfo = calculatePP(tpl, movePPs[i], moveMaxPPs[i]);
                    objList.set(i, constructMove(tpl, ppInfo.finalPP, ppInfo.raised));
                }
                return true;
            }
        } catch (Throwable t) {
            PokemonEditorDebug.errorMoves("[MoveWrite] REFLECT path failed: {}", t.getMessage());
        }
        return false;
    }

    // [优化-C03] 缓存allMoveIds，游戏运行期间不变
    public static List<String> getAllMoveIds() {
        if (cachedAllMoveIds != null) return cachedAllMoveIds;
        List<String> result = new ArrayList<>();
        try {
            Object companion = MoveTemplate.class.getField("Companion").get(null);
            for (Method m : companion.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && !m.getName().equals("toString") && !m.getName().equals("hashCode") && !m.getName().equals("equals")) {
                    try {
                        Object r = m.invoke(companion);
                        if (r instanceof Iterable<?> iter) {
                            for (Object obj : iter) {
                                if (obj instanceof MoveTemplate tpl) {
                                    String id = tpl.getName();
                                    if (id != null && !id.isEmpty()) result.add(id.toLowerCase());
                                } else {
                                    String id = extractStringId(obj);
                                    if (id != null && !id.isEmpty()) result.add(id);
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        try {
            for (java.lang.reflect.Field f : Cobblemon.class.getFields()) {
                String n = f.getName().toLowerCase();
                if (n.contains("move") && !n.contains("pokemon") && !n.contains("party") && !n.contains("species")) {
                    try {
                        Object val = f.get(null);
                        if (val instanceof Iterable<?> iter) {
                            for (Object obj : iter) {
                                if (obj instanceof MoveTemplate tpl) {
                                    String id = tpl.getName();
                                    if (id != null && !id.isEmpty()) result.add(id.toLowerCase());
                                } else {
                                    String id = extractStringId(obj);
                                    if (id != null && !id.isEmpty()) result.add(id);
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        try {
            initBridgeCache(); // [线性-L07] 使用缓存的Moves类
            for (java.lang.reflect.Field f : cachedMovesClass.getFields()) {
                try {
                    Object val = f.get(null);
                    if (val instanceof Iterable<?> iter) {
                        for (Object obj : iter) {
                            if (obj instanceof MoveTemplate tpl) {
                                String id = tpl.getName();
                                if (id != null && !id.isEmpty()) result.add(id.toLowerCase());
                            } else {
                                String id = extractStringId(obj);
                                if (id != null && !id.isEmpty()) result.add(id);
                            }
                        }
                    } else if (val instanceof java.util.Map<?, ?> map) {
                        for (Object obj : map.values()) {
                            if (obj instanceof MoveTemplate tpl) {
                                String id = tpl.getName();
                                if (id != null && !id.isEmpty()) result.add(id.toLowerCase());
                            } else {
                                String id = extractStringId(obj);
                                if (id != null && !id.isEmpty()) result.add(id);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
            for (java.lang.reflect.Method m : cachedMovesClass.getMethods()) {
                if (m.getParameterCount() == 0 && java.lang.reflect.Modifier.isStatic(m.getModifiers())
                        && !m.getName().equals("toString") && !m.getName().equals("hashCode")
                        && !m.getName().equals("equals") && !m.getName().contains("byName")) {
                    try {
                        Object r = m.invoke(null);
                        if (r instanceof Iterable<?> iter) {
                            for (Object obj : iter) {
                                if (obj instanceof MoveTemplate tpl) {
                                    String id = tpl.getName();
                                    if (id != null && !id.isEmpty()) result.add(id.toLowerCase());
                                } else {
                                    String id = extractStringId(obj);
                                    if (id != null && !id.isEmpty()) result.add(id);
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
        for (String s : result) {
            if (s != null && !s.isEmpty()) unique.add(s);
        }
        cachedAllMoveIds = new ArrayList<>(unique);  // [优化-C03] 缓存结果
        return cachedAllMoveIds;
    }

    // [线性-L12] 获取所有Cobblemon特性ID（模仿getAllMoveIds模式，直接用反射枚举Abilities注册表）
    //    通过lang键值(cobblemon.ability.<id>)反向查找，不再依赖硬编码翻译
    public static List<String> getAllAbilityIds() {
        if (cachedAllAbilityIds != null) return cachedAllAbilityIds;  // [优化-C03] 缓存
        List<String> result = new ArrayList<>();
        initBridgeCache();
        // 1. AbilityTemplate.Companion (Kotlin companion object)
        try {
            Class<?> abilityTemplateClass = Class.forName("com.cobblemon.mod.common.api.abilities.AbilityTemplate");
            Object companion = abilityTemplateClass.getField("Companion").get(null);
            for (Method m : companion.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && !m.getName().equals("toString")
                        && !m.getName().equals("hashCode") && !m.getName().equals("equals")) {
                    try {
                        Object r = m.invoke(companion);
                        if (r instanceof Iterable<?> iter) {
                            for (Object obj : iter) {
                                String id = extractStringId(obj);
                                if (id != null && !id.isEmpty()) result.add(id.toLowerCase());
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
        // 2. Abilities 类静态字段
        if (cachedAbilitiesClass != null) {
            try {
                for (java.lang.reflect.Field f : cachedAbilitiesClass.getFields()) {
                    try {
                        Object val = f.get(null);
                        if (val instanceof Iterable<?> iter) {
                            for (Object obj : iter) {
                                String id = extractStringId(obj);
                                if (id != null && !id.isEmpty()) result.add(id.toLowerCase());
                            }
                        } else if (val instanceof java.util.Map<?, ?> map) {
                            for (Object k : map.keySet()) {
                                if (k != null) result.add(k.toString().toLowerCase());
                            }
                            for (Object v : map.values()) {
                                String id = extractStringId(v);
                                if (id != null && !id.isEmpty()) result.add(id.toLowerCase());
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
            // 3. Abilities 类静态方法（返回 Iterable/Map）
            try {
                for (java.lang.reflect.Method m : cachedAbilitiesClass.getMethods()) {
                    if (m.getParameterCount() == 0 && java.lang.reflect.Modifier.isStatic(m.getModifiers())
                            && !m.getName().equals("toString") && !m.getName().equals("hashCode")
                            && !m.getName().equals("equals") && !m.getName().contains("byName")) {
                        try {
                            Object r = m.invoke(null);
                            if (r instanceof Iterable<?> iter) {
                                for (Object obj : iter) {
                                    String id = extractStringId(obj);
                                    if (id != null && !id.isEmpty()) result.add(id.toLowerCase());
                                }
                            } else if (r instanceof java.util.Map<?, ?> map) {
                                for (Object k : map.keySet()) {
                                    if (k != null) result.add(k.toString().toLowerCase());
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }
        // 4. Cobblemon 类中带 "ability" 的静态字段
        try {
            Class<?> cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            for (java.lang.reflect.Field f : cobblemonClass.getFields()) {
                String n = f.getName().toLowerCase();
                if (n.contains("ability")) {
                    try {
                        Object val = f.get(null);
                        if (val instanceof Iterable<?> iter) {
                            for (Object obj : iter) {
                                String id = extractStringId(obj);
                                if (id != null && !id.isEmpty()) result.add(id.toLowerCase());
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
        java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
        for (String s : result) {
            if (s != null && !s.isEmpty()) unique.add(s);
        }
        cachedAllAbilityIds = new ArrayList<>(unique);  // [优化-C03] 缓存结果
        return cachedAllAbilityIds;
    }

    // [线性-L14fix] 获取所有Cobblemon物种ID — 用showdownId()而非getName()
    //    getName()="Ho-Oh" → "ho-oh" ❌ (getByName("ho-oh")失败)
    //    showdownId()="hooh" ✅ (getByName("hooh")成功)
    public static List<String> getAllSpeciesIds() {
        List<String> result = new ArrayList<>();
        try {
            Collection<Species> allSpecies = PokemonSpecies.getSpecies();
            for (Species s : allSpecies) {
                if (s != null) {
                    String id = s.showdownId();
                    if (id != null && !id.isEmpty()) {
                        result.add(id.toLowerCase());
                    }
                }
            }
        } catch (Throwable t) {
            PokemonEditorDebug.warnEditor("getAllSpeciesIds failed: {}", t.getMessage());
        }
        return result;
    }

    // [线性-L15] 获取图鉴编号→物种ID映射（用于序号搜索）
    // [优化-C04] 缓存结果，游戏运行期间图鉴映射不变
    private static java.util.Map<Integer, String> cachedNationalDexNumberMap;
    // [优化-C05] 缓存Jecharacters Match.contains反射方法，避免每次搜索都Class.forName
    private static java.lang.reflect.Method cachedMatchContainsMethod;
    private static boolean matchContainsLookupFailed;

    public static boolean pinyinContains(String zh, String q) {
        if (matchContainsLookupFailed) return false;
        try {
            if (cachedMatchContainsMethod == null) {
                Class<?> matchClass = Class.forName("me.towdium.jecharacters.utils.Match");
                cachedMatchContainsMethod = matchClass.getMethod("contains", CharSequence.class, CharSequence.class);
            }
            return Boolean.TRUE.equals(cachedMatchContainsMethod.invoke(null, zh, q));
        } catch (Throwable t) {
            matchContainsLookupFailed = true;
            return false;
        }
    }
    public static java.util.Map<Integer, String> getNationalDexNumberMap() {
        if (cachedNationalDexNumberMap != null) return cachedNationalDexNumberMap;
        java.util.Map<Integer, String> result = new java.util.LinkedHashMap<>();
        try {
            Collection<Species> allSpecies = PokemonSpecies.getSpecies();
            for (Species s : allSpecies) {
                if (s != null) {
                    int num = s.getNationalPokedexNumber();
                    if (num > 0) {
                        String id = s.showdownId();
                        if (id != null && !id.isEmpty()) {
                            result.put(num, id.toLowerCase());
                        }
                    }
                }
            }
        } catch (Throwable t) {
            PokemonEditorDebug.warnEditor("getNationalDexNumberMap failed: {}", t.getMessage());
        }
        cachedNationalDexNumberMap = result;  // [优化-C04]
        return result;
    }

    private static void writePokemonFields(Pokemon pokemon, PokemonEditorNetwork.UpdatePokemonPacket pkt, net.minecraft.core.RegistryAccess regAccess) {
        if (pokemon == null) {
            PokemonEditorDebug.warnEditor("writePokemonFields: pokemon is null, abort");
            return;
        }
        PokemonEditorDebug.logEditor("writePokemonFields START - applying modifications to pokemon");
        
        // 解析modifiedFields：客户端发送的修改字段列表
        java.util.Set<String> modifiedFields = new java.util.HashSet<>();
        String rawData = pkt.rawJsonData();
        if (rawData != null && !rawData.isEmpty()) {
            // 格式：__MODIFIED__=speciesName,level,ivHp
            if (rawData.startsWith("__MODIFIED__=")) {
                String[] parts = rawData.split("\\n", 2);
                if (parts.length > 0) {
                    String modPart = parts[0].substring("__MODIFIED__=".length());
                    for (String field : modPart.split(",")) {
                        modifiedFields.add(field.trim());
                    }
                    PokemonEditorDebug.logEditor("Parsed modifiedFields: {}", modifiedFields);
                }
            }
        }

        // 物种修改
        boolean speciesChanged = false;
        if ((modifiedFields.contains("speciesName") || modifiedFields.isEmpty()) && pkt.speciesName() != null && !pkt.speciesName().isEmpty()) {
            try {
                Object oldSpecies = pokemon.getSpecies();
                pokemon.setSpecies((com.cobblemon.mod.common.pokemon.Species) lookupSpeciesObj(pkt.speciesName()));
                PokemonEditorDebug.logEditor("species: {} -> {}", oldSpecies, pkt.speciesName());
                speciesChanged = true;
            } catch (Throwable e) {
                PokemonEditorDebug.warnEditor("setSpecies failed: {}", e.getMessage());
            }
        }
        // 形态修改
        if ((modifiedFields.contains("formName") || modifiedFields.isEmpty()) && pkt.formName() != null && !pkt.formName().isEmpty()) {
            try {
                for (Method m : pokemon.getClass().getMethods()) {
                    if ((m.getName().equals("setForm") || m.getName().equals("form")) && m.getParameterCount() == 1) {
                        m.invoke(pokemon, pkt.formName());
                        PokemonEditorDebug.logEditor("form: {}", pkt.formName());
                        break;
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 等级修改 —— 保存旧经验 → setLevel → 恢复旧经验
        //   setLevel() 内部会调用 setExperience$common 重算经验值
        //   setExperience$common 在满级时又强制覆盖，所以用直接字段写入绕过
        if (modifiedFields.contains("level") || modifiedFields.isEmpty()) {
            try {
                int oldLevel = pokemon.getLevel();
                int oldExp = pokemon.getExperience();
                pokemon.setLevel(pkt.level());
                if (!modifiedFields.contains("experience")) {
                    // 直接写 experience 字段，绕过 setExperience$common 的满级覆盖
                    java.lang.reflect.Field f = pokemon.getClass().getDeclaredField("experience");
                    f.setAccessible(true);
                    f.setInt(pokemon, oldExp);
                    PokemonEditorDebug.logEditor("level: {} -> {} (exp preserved: {})", oldLevel, pkt.level(), oldExp);
                }
            } catch (Throwable e) {
                PokemonEditorDebug.warnEditor("setLevel failed: {}", e.getMessage());
            }
        }

        // 经验值修改 —— 直接字段写入绕过 setExperience$common 满级覆盖
        if (modifiedFields.contains("experience")) {
            try {
                int expToSet = Math.max(0, pkt.experience());
                java.lang.reflect.Field expField = pokemon.getClass().getDeclaredField("experience");
                expField.setAccessible(true);
                expField.setInt(pokemon, expToSet);
                PokemonEditorDebug.logEditor("experience: {} (direct field write)", expToSet);
            } catch (Throwable e) {
                PokemonEditorDebug.warnEditor("setExperience failed: {}", e.getMessage());
            }
        }

        // 亲密度修改
        if (modifiedFields.contains("friendship") || modifiedFields.isEmpty()) {
            try {
                int oldFriendship = pokemon.getFriendship();
                pokemon.setFriendship(Math.max(0, Math.min(255, pkt.friendship())), true);
                PokemonEditorDebug.logEditor("friendship: {} -> {}", oldFriendship, pkt.friendship());
            } catch (Throwable e) {
                PokemonEditorDebug.warnEditor("setFriendship failed: {}", e.getMessage());
            }
        }

        // 闪光修改
        if (modifiedFields.contains("shiny") || modifiedFields.isEmpty()) {
            try {
                boolean oldShiny = pokemon.getShiny();
                pokemon.setShiny(pkt.shiny());
                PokemonEditorDebug.logEditor("shiny: {} -> {}", oldShiny, pkt.shiny());
            } catch (Throwable ignored) {}
        }

        // 昵称修改
        if (modifiedFields.contains("displayName") || modifiedFields.isEmpty()) {
            try {
                if (pkt.displayName() == null || pkt.displayName().isEmpty()) {
                    pokemon.setNickname(null);
                    PokemonEditorDebug.logEditor("nickname: cleared");
                } else {
                    pokemon.setNickname(net.minecraft.network.chat.Component.literal(pkt.displayName()));
                    PokemonEditorDebug.logEditor("nickname: {}", pkt.displayName());
                }
            } catch (Throwable ignored) {}
        }

        // UUID修改
        String uuidStr = pkt.pokemonUUID();
        if ((modifiedFields.contains("pokemonUUID") || modifiedFields.isEmpty()) && uuidStr != null && !uuidStr.isEmpty()) {
            try {
                UUID newUuid = UUID.fromString(uuidStr);
                pokemon.setUuid(newUuid);
                PokemonEditorDebug.logEditor("uuid: {}", uuidStr);
            } catch (Throwable ignored) {}
        }

        // 性别修改
        if (modifiedFields.contains("gender") || modifiedFields.isEmpty()) {
            try {
                Object genderEnum = resolveGender(pkt.gender());
                if (genderEnum != null) {
                    pokemon.setGender((com.cobblemon.mod.common.pokemon.Gender) genderEnum);
                    PokemonEditorDebug.logEditor("gender: {}", pkt.gender());
                }
            } catch (Throwable ignored) {}
        }

        // 性格修改
        if ((modifiedFields.contains("nature") || modifiedFields.isEmpty()) && pkt.nature() != null && !pkt.nature().isEmpty()) {
            Object natureObj = lookupRegistryObj(pkt.nature());
            if (natureObj != null) {
                try {
                    pokemon.setNature((com.cobblemon.mod.common.pokemon.Nature) natureObj);
                    PokemonEditorDebug.logEditor("nature: {}", pkt.nature());
                } catch (Throwable ignored) {}
            }
        }

        // 特性修改
        if ((modifiedFields.contains("ability") || modifiedFields.isEmpty()) && pkt.ability() != null && !pkt.ability().isEmpty()) {
            Object abilityObj = lookupAbilityObj(pkt.ability());
            if (abilityObj != null) {
                try {
                    pokemon.getClass().getMethod("setAbility$common", com.cobblemon.mod.common.api.abilities.Ability.class).invoke(pokemon, abilityObj);
                } catch (Throwable e) {
                    try {
                        for (Method m : pokemon.getClass().getMethods()) {
                            if (m.getName().startsWith("setAbility") && m.getParameterCount() == 1) {
                                m.invoke(pokemon, abilityObj);
                                break;
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }

        // 携带物修改
        if ((modifiedFields.contains("heldItem") || modifiedFields.isEmpty()) && pkt.heldItem() != null && !pkt.heldItem().isEmpty()) {
            Item item = findItem(pkt.heldItem());
            if (item != null) {
                ItemStack stack = new ItemStack(item);
                try {
                    pokemon.setHeldItem$common(stack);
                    PokemonEditorDebug.logEditor("heldItem: {}", pkt.heldItem());
                } catch (Throwable ignored) {}
            }
        }

        // 状态修改 + 自动清除逻辑
        boolean shouldClearStatus = speciesChanged && !modifiedFields.contains("status");
        if (shouldClearStatus) {
            // 物种改变且未手动修改状态：自动清除状态
            try {
                pokemon.setStatus(null);
                PokemonEditorDebug.logEditor("Auto-cleared status (species changed, status not modified)");
            } catch (Throwable t) {
                PokemonEditorDebug.warnEditor("Auto-clear status failed: {}", t.getMessage());
            }
        } else if (modifiedFields.contains("status") || modifiedFields.isEmpty()) {
            // 用户手动修改状态或全量更新
            PokemonEditorDebug.infoStatus("[writeStatus] pkt.status()='{}' modifiedFields.contains('status')={}",
                    pkt.status(), modifiedFields.contains("status"));
            try {
                if (pkt.status() != null && !pkt.status().isEmpty()) {
                    PokemonEditorDebug.infoStatus("[writeStatus] looking up status object for '{}'", pkt.status());
                    Object statusObj = lookupStatusObj(pkt.status());
                    PokemonEditorDebug.infoStatus("[writeStatus] lookupStatusObj returned: {} (class={})",
                            statusObj, statusObj != null ? statusObj.getClass().getName() : "null");
                    if (statusObj != null) {
                        com.cobblemon.mod.common.pokemon.status.PersistentStatus realStatus =
                                (com.cobblemon.mod.common.pokemon.status.PersistentStatus) statusObj;
                        PokemonEditorDebug.infoStatus("[writeStatus] calling pokemon.applyStatus({})... pokemon class={}",
                                realStatus.getClass().getSimpleName(), pokemon.getClass().getName());
                        pokemon.applyStatus(realStatus);
                        PokemonEditorDebug.infoStatus("[writeStatus] applyStatus succeeded! Verifying: pokemon.getStatus()={}",
                                pokemon.getStatus());
                    } else {
                        PokemonEditorDebug.warnStatus("[writeStatus] lookupStatusObj returned null for '{}'", pkt.status());
                    }
                } else {
                    PokemonEditorDebug.infoStatus("[writeStatus] clearing status via setStatus(null), current={}",
                            pokemon.getStatus());
                    pokemon.setStatus(null);
                    PokemonEditorDebug.infoStatus("[writeStatus] status cleared OK, now={}", pokemon.getStatus());
                }
            } catch (Throwable t) {
                PokemonEditorDebug.errorStatus("[writeStatus] EXCEPTION: {} | {}", t.getClass().getName(), t.getMessage());
                t.printStackTrace();
            }
        }

        // IV/EV修改
        try {
            Object ivsObj = pokemon.getIvs();
            if (ivsObj != null) {
                if (modifiedFields.contains("ivHp") || modifiedFields.isEmpty()) {
                    writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivHp())), "HP", "hp", "Hp");
                }
                if (modifiedFields.contains("ivAtk") || modifiedFields.isEmpty()) {
                    writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivAtk())), "ATK", "attack", "atk", "Atk");
                }
                if (modifiedFields.contains("ivDef") || modifiedFields.isEmpty()) {
                    writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivDef())), "DEF", "defense", "def", "Def");
                }
                if (modifiedFields.contains("ivSpA") || modifiedFields.isEmpty()) {
                    writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivSpA())), "SPA", "specialAttack", "spAttack", "spa", "SpA");
                }
                if (modifiedFields.contains("ivSpD") || modifiedFields.isEmpty()) {
                    writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivSpD())), "SPD", "specialDefense", "spDefense", "spd", "SpD");
                }
                if (modifiedFields.contains("ivSpe") || modifiedFields.isEmpty()) {
                    writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivSpe())), "SPE", "speed", "spe", "Spe");
                }
                if (!modifiedFields.isEmpty()) {
                    PokemonEditorDebug.logEditor("IVs modified: {}", modifiedFields.stream().filter(f -> f.startsWith("iv")).toList());
                } else {
                    PokemonEditorDebug.logEditor("IVs: HP={} ATK={} DEF={} SPA={} SPD={} SPE={}",
                            pkt.ivHp(), pkt.ivAtk(), pkt.ivDef(), pkt.ivSpA(), pkt.ivSpD(), pkt.ivSpe());
                }
            }

            Object evsObj = pokemon.getEvs();
            if (evsObj != null) {
                int evCap = CobblemonTSConfig.getEVTotalCap();
                int total = pkt.evHp() + pkt.evAtk() + pkt.evDef() + pkt.evSpA() + pkt.evSpD() + pkt.evSpe();
                int evHp = pkt.evHp(), evAtk = pkt.evAtk(), evDef = pkt.evDef();
                int evSpA = pkt.evSpA(), evSpD = pkt.evSpD(), evSpe = pkt.evSpe();
                if (total > evCap) {
                    float ratio = (float) evCap / total;
                    evHp = (int) (evHp * ratio);
                    evAtk = (int) (evAtk * ratio);
                    evDef = (int) (evDef * ratio);
                    evSpA = (int) (evSpA * ratio);
                    evSpD = (int) (evSpD * ratio);
                    evSpe = (int) (evSpe * ratio);
                }
                if (modifiedFields.contains("evHp") || modifiedFields.isEmpty()) {
                    writeStat(pokemon, evsObj, Math.max(0, Math.min(252, evHp)), "HP", "hp", "Hp");
                }
                if (modifiedFields.contains("evAtk") || modifiedFields.isEmpty()) {
                    writeStat(pokemon, evsObj, Math.max(0, Math.min(252, evAtk)), "ATK", "attack", "atk", "Atk");
                }
                if (modifiedFields.contains("evDef") || modifiedFields.isEmpty()) {
                    writeStat(pokemon, evsObj, Math.max(0, Math.min(252, evDef)), "DEF", "defense", "def", "Def");
                }
                if (modifiedFields.contains("evSpA") || modifiedFields.isEmpty()) {
                    writeStat(pokemon, evsObj, Math.max(0, Math.min(252, evSpA)), "SPA", "specialAttack", "spAttack", "spa", "SpA");
                }
                if (modifiedFields.contains("evSpD") || modifiedFields.isEmpty()) {
                    writeStat(pokemon, evsObj, Math.max(0, Math.min(252, evSpD)), "SPD", "specialDefense", "spDefense", "spd", "SpD");
                }
                if (modifiedFields.contains("evSpe") || modifiedFields.isEmpty()) {
                    writeStat(pokemon, evsObj, Math.max(0, Math.min(252, evSpe)), "SPE", "speed", "spe", "Spe");
                }
                if (!modifiedFields.isEmpty()) {
                    PokemonEditorDebug.logEditor("EVs modified: {}", modifiedFields.stream().filter(f -> f.startsWith("ev")).toList());
                } else {
                    PokemonEditorDebug.logEditor("EVs: HP={} ATK={} DEF={} SPA={} SPD={} SPE={} (cap={})",
                            evHp, evAtk, evDef, evSpA, evSpD, evSpe, evCap);
                }
            }
        } catch (Throwable e) {
            PokemonEditorDebug.warnEditor("IV/EV modify failed: {}", e.getMessage());
        }

        // 技能修改
        try {
            Object moveSetObj = callNoArgs(pokemon, "getMoveSet", "moveSet");
            if (moveSetObj != null) {
                String[] moveNames = {pkt.move1(), pkt.move2(), pkt.move3(), pkt.move4()};
                int[] movePPs = {pkt.move1PP(), pkt.move2PP(), pkt.move3PP(), pkt.move4PP()};
                int[] moveMaxPPs = {pkt.move1MaxPP(), pkt.move2MaxPP(), pkt.move3MaxPP(), pkt.move4MaxPP()};
                PokemonEditorDebug.infoMoves("[MoveWrite] writing: slot0='{}' slot1='{}' slot2='{}' slot3='{}'",
                        moveNames[0], moveNames[1], moveNames[2], moveNames[3]);

                // 检查是否有任何技能相关字段被修改
                boolean hasMoveChanges = modifiedFields.isEmpty() || 
                    modifiedFields.stream().anyMatch(f -> f.startsWith("move") && !f.contains("PP"));
                boolean hasPPChanges = modifiedFields.isEmpty() || 
                    modifiedFields.stream().anyMatch(f -> f.contains("PP"));
                
                if (hasMoveChanges || hasPPChanges) {
                    boolean ok = writeMovesToInternalArray(moveSetObj, moveNames, movePPs, moveMaxPPs);
                    if (!ok) {
                        Method setMoveM = null;
                        for (Method m : moveSetObj.getClass().getMethods()) {
                            if (m.getName().equals("setMove") && m.getParameterCount() == 2
                                    && m.getParameterTypes()[0] == int.class) {
                                setMoveM = m; break;
                            }
                        }
                        if (setMoveM != null) {
                            for (int i = 0; i < 4; i++) {
                                boolean slotChanged = modifiedFields.isEmpty() || 
                                    modifiedFields.contains("move" + (i+1)) ||
                                    modifiedFields.contains("move" + (i+1) + "PP") ||
                                    modifiedFields.contains("move" + (i+1) + "MaxPP");
                                
                                if (!slotChanged) continue;
                                
                                try { setMoveM.invoke(moveSetObj, i, null); } catch (Throwable ignored) {}
                                String moveName = moveNames[i];
                                if (moveName == null || moveName.isEmpty()) continue;
                                MoveTemplate tpl = lookupMoveTemplate(moveName);
                                if (tpl == null) continue;
                                MovePPInfo ppInfo = calculatePP(tpl, movePPs[i], moveMaxPPs[i]);
                                try {
                                    Move fallbackMove = new Move(tpl, ppInfo.finalPP, ppInfo.raised);
                                    PokemonEditorDebug.infoPP("[MoveWrite] fallback new Move('{}') OK: getMaxPp={} getCurrentPp={}",
                                            moveName, fallbackMove.getMaxPp(), fallbackMove.getCurrentPp());
                                    setMoveM.invoke(moveSetObj, i, fallbackMove);
                                }
                                catch (Throwable t2) {
                                    PokemonEditorDebug.errorPP("[MoveWrite] fallback new Move('{}') FAILED: {}", moveName, t2.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            PokemonEditorDebug.errorMoves("[MoveWrite] error: {}", t.getMessage());
        }

        // HP修改 + 自动补满逻辑
        try {
            int currentMaxHp = pokemon.getMaxHealth();
            int hp = pkt.currentHp();
            
            // 自动补满HP：当物种或等级改变，且用户未手动修改HP时
            boolean levelChanged = modifiedFields.contains("level") || modifiedFields.isEmpty();
            boolean shouldAutoFillHp = (speciesChanged || levelChanged) && !modifiedFields.contains("currentHp");
            if (shouldAutoFillHp) {
                hp = currentMaxHp;
                PokemonEditorDebug.logEditor("Auto-filled HP to max (species/level changed, HP not modified)");
            } else if (hp <= 0) {
                hp = currentMaxHp;
            }
            
            hp = Math.max(0, Math.min(currentMaxHp, hp));
            pokemon.setCurrentHealth(hp);
            PokemonEditorDebug.logEditor("currentHp: {} (max={})", hp, currentMaxHp);
        } catch (Throwable ignored) {}

        // ★ 金色王冠状态写入（modifiedFields校验 + 直接操作hyperTrainedIVs Map绕过setHyperTrainedIV优化）
        if (modifiedFields.contains("goldenBottleCap") || modifiedFields.isEmpty()) {
            try {
                // [优化-B08] 复用缓存的getIvs + Stats字段
                if (cachedGetIvsMethod == null) {
                    cachedGetIvsMethod = pokemon.getClass().getMethod("getIvs");
                }
                var ivs = cachedGetIvsMethod.invoke(pokemon);
                if (ivs != null) {
                    initBridgeCache();
                    // 确保Stats字段已缓存
                    if (cachedStatsHPField == null) {
                        cachedStatsHPField = cachedStatsClass.getField("HP");
                        cachedStatsATKField = cachedStatsClass.getField("ATTACK");
                        cachedStatsDEFField = cachedStatsClass.getField("DEFENCE");
                        cachedStatsSPAField = cachedStatsClass.getField("SPECIAL_ATTACK");
                        cachedStatsSPDField = cachedStatsClass.getField("SPECIAL_DEFENCE");
                        cachedStatsSPEField = cachedStatsClass.getField("SPEED");
                    }
                    Object[] allStats = {cachedStatsHPField.get(null), cachedStatsATKField.get(null),
                        cachedStatsDEFField.get(null), cachedStatsSPAField.get(null),
                        cachedStatsSPDField.get(null), cachedStatsSPEField.get(null)};

                    var htField = ivs.getClass().getDeclaredField("hyperTrainedIVs");
                    htField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    java.util.Map<Object, Object> htMap = (java.util.Map<Object, Object>) htField.get(ivs);

                    if (pkt.goldenBottleCap()) {
                        for (Object stat : allStats) {
                            htMap.put(stat, 31);
                        }
                    } else {
                        if (htMap != null) htMap.clear();
                    }
                    PokemonEditorDebug.logEditor("goldenBottleCap set to {}", pkt.goldenBottleCap());
                }
            } catch (Throwable ignored) {}
        }

        PokemonEditorDebug.logEditor("writePokemonFields DONE");
    }

    // [线性-L08] 清理: writePokemonFieldsLegacy 从未被调用，已删除

    private static ServerPlayer resolveTargetPlayer(ServerPlayer serverPlayer, UUID targetUUID) {
        if (targetUUID == null || targetUUID.equals(serverPlayer.getUUID())) {
            return serverPlayer;
        }
        if (serverPlayer.server != null) {
            for (ServerPlayer p : serverPlayer.server.getPlayerList().getPlayers()) {
                if (p.getUUID().equals(targetUUID)) return p;
            }
        }
        return null;
    }

    private static Object getPartyFromStorage(Object storage, ServerPlayer targetPlayer) {
        try {
            for (Method m : storage.getClass().getMethods()) {
                if ((m.getName().equals("getParty") || m.getName().equals("party")) && m.getParameterCount() == 1) {
                    return m.invoke(storage, targetPlayer);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> getPartyList(ServerPlayer serverPlayer, UUID targetUUID) {
        List<Object> list = new ArrayList<>();
        try {
            Object targetPlayer = serverPlayer;
            if (targetUUID != null && !targetUUID.equals(serverPlayer.getUUID())) {
                if (serverPlayer.server != null) {
                    for (ServerPlayer p : serverPlayer.server.getPlayerList().getPlayers()) {
                        if (p.getUUID().equals(targetUUID)) { targetPlayer = p; break; }
                    }
                }
            }

            Object storage = Cobblemon.INSTANCE.getStorage();
            Object party = null;
            for (Method m : storage.getClass().getMethods()) {
                if ((m.getName().equals("getParty") || m.getName().equals("party")) && m.getParameterCount() == 1) {
                    party = m.invoke(storage, targetPlayer);
                    break;
                }
            }
            if (party == null) return list;

            try {
                Method gappyMethod = party.getClass().getMethod("toGappyList");
                List<?> gappyList = (List<?>) gappyMethod.invoke(party);
                if (gappyList != null) {
                    list.addAll(gappyList);
                }
            } catch (NoSuchMethodException e1) {
                try {
                    Method sizeM = party.getClass().getMethod("size");
                    Method getM = party.getClass().getMethod("get", int.class);
                    int sz = (Integer) sizeM.invoke(party);
                    for (int i = 0; i < sz; i++) {
                        list.add(getM.invoke(party, i));
                    }
                } catch (Throwable ignored) {
                    if (party instanceof List<?>) {
                        list.addAll((List<?>) party);
                    }
                }
            }
        } catch (Throwable t) {
            PokemonEditorDebug.log("getPartyList 闂備焦瀵ч悷銊╊敋? " + t.getMessage());
        }
        return list;
    }

    private static boolean readMovesByIndex(Object moveSetObj, List<String> outNames,
                                            int[] outPPs, int[] outMaxPPs, int[] outRaised) {
        if (moveSetObj == null) return false;
        PokemonEditorDebug.infoMoves("[MoveRead][Index] probing moveSet methods...");

        Method indexedGetM = null;
        try {
            indexedGetM = moveSetObj.getClass().getMethod("get", int.class);
        } catch (Throwable ignored) {}
        if (indexedGetM == null) {
            for (Method m : moveSetObj.getClass().getMethods()) {
                if ((m.getName().equals("getMove") || m.getName().equals("get"))
                        && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == int.class) {
                    indexedGetM = m;
                    break;
                }
            }
        }

        if (indexedGetM != null) {
            boolean foundAny = false;
            for (int i = 0; i < 4; i++) {
                try {
                    Object mv = indexedGetM.invoke(moveSetObj, i);
                    if (mv instanceof Move move) {
                        MoveTemplate tpl = move.getTemplate();
                        String nm = (tpl != null) ? tpl.getName() : move.getName();
                        outNames.set(i, nm == null ? "" : nm);
                        outPPs[i] = move.getCurrentPp();
                        outMaxPPs[i] = move.getMaxPp();
                        outRaised[i] = move.getRaisedPpStages();
                        foundAny = true;
                    } else {
                    }
                } catch (Throwable ignored) {}
            }
            if (foundAny || outNames.stream().anyMatch(s -> s != null && !s.isEmpty())) {
                return true;
            }
        }

        try {
            Method listM = moveSetObj.getClass().getMethod("toGappyList");
            Object listObj = listM.invoke(moveSetObj);
            if (listObj instanceof List<?> list) {
                for (int i = 0; i < Math.min(4, list.size()); i++) {
                    Object o = list.get(i);
                    if (o instanceof Move move) {
                        MoveTemplate tpl = move.getTemplate();
                        String nm = (tpl != null) ? tpl.getName() : move.getName();
                        outNames.set(i, nm == null ? "" : nm);
                        outPPs[i] = move.getCurrentPp();
                        outMaxPPs[i] = move.getMaxPp();
                        outRaised[i] = move.getRaisedPpStages();
                    }
                }
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            for (java.lang.reflect.Field f : moveSetObj.getClass().getFields()) {
                String fn = f.getName().toLowerCase();
                if (fn.contains("slot") || fn.equals("moves") || fn.equals("move_list")) {
                    Object val = f.get(moveSetObj);
                    if (val instanceof List<?> list) {
                        for (int i = 0; i < Math.min(4, list.size()); i++) {
                            Object o = list.get(i);
                            if (o instanceof Move move) {
                                MoveTemplate tpl = move.getTemplate();
                                String nm = (tpl != null) ? tpl.getName() : move.getName();
                                outNames.set(i, nm == null ? "" : nm);
                                outPPs[i] = move.getCurrentPp();
                                outMaxPPs[i] = move.getMaxPp();
                                outRaised[i] = move.getRaisedPpStages();
                            }
                        }
                        return true;
                    }
                }
            }
            for (java.lang.reflect.Field f : moveSetObj.getClass().getDeclaredFields()) {
                String fn = f.getName().toLowerCase();
                if (fn.contains("slot") || fn.equals("moves") || fn.equals("move_list")) {
                    f.setAccessible(true);
                    Object val = f.get(moveSetObj);
                    if (val instanceof List<?> list) {
                        for (int i = 0; i < Math.min(4, list.size()); i++) {
                            Object o = list.get(i);
                            if (o instanceof Move move) {
                                MoveTemplate tpl = move.getTemplate();
                                String nm = (tpl != null) ? tpl.getName() : move.getName();
                                outNames.set(i, nm == null ? "" : nm);
                                outPPs[i] = move.getCurrentPp();
                                outMaxPPs[i] = move.getMaxPp();
                                outRaised[i] = move.getRaisedPpStages();
                            }
                        }
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        return false;
    }

    private static Object callNoArgs(Object target, String... names) {
        for (String name : names) {
            try {
                Method m = target.getClass().getMethod(name);
                return m.invoke(target);
            } catch (Throwable ignored) {}
        }
        for (Method m : target.getClass().getMethods()) {
            for (String name : names) {
                if (m.getName().equalsIgnoreCase(name) && m.getParameterCount() == 0) {
                    try { return m.invoke(target); } catch (Throwable ignored) {}
                }
            }
        }
        return null;
    }

    private static void callInt(Object target, int value, String... names) {
        for (String name : names) {
            try {
                Method m = target.getClass().getMethod(name, int.class);
                m.invoke(target, value);
                return;
            } catch (Throwable ignored) {}
        }
        for (Method m : target.getClass().getMethods()) {
            for (String name : names) {
                if (m.getName().toLowerCase().contains(name.toLowerCase())
                        && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == int.class) {
                    try { m.invoke(target, value); return; } catch (Throwable ignored) {}
                }
            }
        }
    }

    private static Object resolveGender(int genderIdx) {
        try {
            initBridgeCache(); // [线性-L07] 使用缓存的Gender类
            Class<?> genderClass = cachedGenderClass;
            if (!genderClass.isEnum()) return null;
            Object[] values = (Object[]) genderClass.getMethod("values").invoke(null);
            String matchName = genderIdx == 0 ? "MALE" : (genderIdx == 1 ? "FEMALE" : "GENDERLESS");
            for (Object e : values) {
                String n = e.toString().toUpperCase();
                if (n.contains(matchName)) return e;
                if (genderIdx == 2 && (n.contains("NONE") || n.contains("UNKNOWN"))) return e;
            }
            if (genderIdx >= 0 && genderIdx < values.length) return values[genderIdx];
        } catch (Throwable ignored) {}
        return null;
    }

    private static String extractStringId(Object obj) {
        if (obj == null) return "";
        try {
            // [线性-L15fix] ★ showdownId优先于getName：避免getName()返回"Ho-Oh"而showdownId()返回"hooh"的不一致
            //    先精确匹配showdownId，再回落getName
            Method[] methods = obj.getClass().getMethods();
            // 第一遍：优先showdownId
            for (Method m : methods) {
                if (m.getName().equals("showdownId") && m.getParameterCount() == 0) {
                    Object v = m.invoke(obj);
                    if (v != null) {
                        String s = v.toString();
                        if (s.contains(":")) s = s.substring(s.indexOf(":") + 1);
                        return s.toLowerCase().trim();
                    }
                }
            }
            // 第二遍：回落getName
            for (Method m : methods) {
                if ((m.getName().equals("getShowdownId") || m.getName().equals("getName") || m.getName().equals("name"))
                        && m.getParameterCount() == 0) {
                    Object v = m.invoke(obj);
                    if (v != null) {
                        String s = v.toString();
                        if (s.contains(":")) s = s.substring(s.indexOf(":") + 1);
                        return s.toLowerCase().trim();
                    }
                }
            }
            String s = obj.toString();
            if (s.contains(":")) s = s.substring(s.indexOf(":") + 1);
            if (s.contains(".")) s = s.substring(s.lastIndexOf(".") + 1);
            return s.toLowerCase().trim();
        } catch (Throwable ignored) {}
        return "";
    }

    private static Item findItem(String itemName) {
        String lower = itemName.toLowerCase();
        for (Map.Entry<net.minecraft.resources.ResourceKey<Item>, Item> e : BuiltInRegistries.ITEM.entrySet()) {
            String id = e.getKey().location().getPath();
            if (id.equalsIgnoreCase(lower) || e.getKey().location().toString().equals(itemName)) {
                return e.getValue();
            }
        }
        return null;
    }

    private static Object lookupRegistryObj(String name) {
        if (name == null || name.isEmpty()) return null;
        String lowerName = name.toLowerCase().trim();
        String cacheKey = "reg#" + lowerName;
        if (registryCache.containsKey(cacheKey)) return registryCache.get(cacheKey);

        Object result = null;
        try {
            java.lang.reflect.Method getNatureM = com.cobblemon.mod.common.api.pokemon.Natures.class.getMethod("getNature", String.class);
            Object r = getNatureM.invoke(null, lowerName);
            if (r != null) result = r;
        } catch (Throwable ignored) {}
        if (result == null) {
            try {
                java.lang.reflect.Method getNatureM = com.cobblemon.mod.common.api.pokemon.Natures.class.getMethod("getNature", String.class);
                Object r = getNatureM.invoke(null, "cobblemon:" + lowerName);
                if (r != null) result = r;
            } catch (Throwable ignored) {}
        }
        if (result == null) {
            try {
                java.lang.reflect.Method allM = com.cobblemon.mod.common.api.pokemon.Natures.class.getMethod("all");
                Object all = allM.invoke(null);
                if (all instanceof java.util.Collection<?> c) {
                    for (Object natureObj : c) {
                        try {
                            java.lang.reflect.Method getNameM = natureObj.getClass().getMethod("getName");
                            Object nameVal = getNameM.invoke(natureObj);
                            if (nameVal != null) {
                                String s = nameVal.toString();
                                if (s.contains(":")) s = s.substring(s.indexOf(":") + 1);
                                if (s.equalsIgnoreCase(lowerName)) { result = natureObj; break; }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }

        putToRegistryCache(cacheKey, result);  // [优化-C01]
        return result;
    }

    // [线性-L11] 设置精灵的storeCoordinates，使getOwnerPlayer()能正确返回玩家
    //   仅设置originalTrainer不够，Cobblemon通过storeCoordinates链查找玩家归属
    private static void trySetStoreCoordinates(Pokemon pokemon, Object partyStore, int slot) {
        try {
            // 尝试通过 setStoreCoordinates 方法
            for (java.lang.reflect.Method m : pokemon.getClass().getMethods()) {
                if (m.getName().equals("setStoreCoordinates") && m.getParameterCount() == 1) {
                    // 需要创建 StoreCoordinates(Store, int)
                    Class<?> storeCoordClass = m.getParameterTypes()[0];
                    java.lang.reflect.Constructor<?> ctor = null;
                    for (java.lang.reflect.Constructor<?> c : storeCoordClass.getConstructors()) {
                        if (c.getParameterCount() == 2) {
                            ctor = c;
                            break;
                        }
                    }
                    if (ctor != null) {
                        Object coords = ctor.newInstance(partyStore, slot);
                        m.invoke(pokemon, coords);
                        PokemonEditorDebug.logEditor("storeCoordinates set via setStoreCoordinates → party[{}]", slot);
                        return;
                    }
                }
            }
            // 回退：直接设置 storeCoordinates 字段
            try {
                java.lang.reflect.Field scField = pokemon.getClass().getDeclaredField("storeCoordinates");
                scField.setAccessible(true);
                Class<?> scType = scField.getType();
                for (java.lang.reflect.Constructor<?> c : scType.getConstructors()) {
                    if (c.getParameterCount() == 2) {
                        Object coords = c.newInstance(partyStore, slot);
                        scField.set(pokemon, coords);
                        PokemonEditorDebug.logEditor("storeCoordinates set via field → party[{}]", slot);
                        return;
                    }
                }
            } catch (Throwable ignored) {}
            PokemonEditorDebug.warnEditor("storeCoordinates: all approaches failed");
        } catch (Throwable t) {
            PokemonEditorDebug.warnEditor("storeCoordinates setup failed: {}", t.getMessage());
        }
    }

    private static Object lookupAbilityObj(String name) {
        if (name == null || name.isEmpty()) return null;
        String lowerName = name.toLowerCase().trim();
        String cacheKey = "ability#" + lowerName;
        if (registryCache.containsKey(cacheKey)) return registryCache.get(cacheKey);

        Object result = null;
        Object template = null;
        try {
            java.lang.reflect.Method getM = com.cobblemon.mod.common.api.abilities.Abilities.class.getMethod("get", String.class);
            template = getM.invoke(null, lowerName);
        } catch (Throwable ignored) {}
        if (template == null) {
            try {
                java.lang.reflect.Method getM = com.cobblemon.mod.common.api.abilities.Abilities.class.getMethod("get", String.class);
                template = getM.invoke(null, "cobblemon:" + lowerName);
            } catch (Throwable ignored) {}
        }
        if (template == null) {
            try {
                Object companion = com.cobblemon.mod.common.api.abilities.AbilityTemplate.class.getField("Companion").get(null);
                for (java.lang.reflect.Method m : companion.getClass().getMethods()) {
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) {
                        try {
                            Object r = m.invoke(companion, lowerName);
                            if (r != null) { template = r; break; }
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (template != null) {
            try {
                Object priorityNormal = com.cobblemon.mod.common.api.Priority.valueOf("NORMAL");
                java.lang.reflect.Method createM = template.getClass().getMethod("create", boolean.class, com.cobblemon.mod.common.api.Priority.class);
                result = createM.invoke(template, false, priorityNormal);
            } catch (Throwable ignored) {}
        }

        putToRegistryCache(cacheKey, result);  // [优化-C01]
        return result;
    }

    private static Object lookupStatusObj(String name) {
        if (name == null || name.isEmpty()) return null;
        String lowerName = name.toLowerCase().trim();
        String cacheKey = "status#" + lowerName;
        if (registryCache.containsKey(cacheKey)) {
            Object cached = registryCache.get(cacheKey);
            PokemonEditorDebug.infoStatus("[lookupStatus] cache {} for '{}': {}",
                    cached != null ? "HIT" : "HIT(null)", lowerName, cached);
            return cached;
        }

        Object result = null;
        PokemonEditorDebug.infoStatus("[lookupStatus] searching for '{}' (lowerName='{}')", name, lowerName);

        // 尝试1: Statuses.getStatus(lowerName)
        try {
            java.lang.reflect.Method getStatusM = com.cobblemon.mod.common.api.pokemon.status.Statuses.class.getMethod("getStatus", String.class);
            Object status = getStatusM.invoke(null, lowerName);
            PokemonEditorDebug.infoStatus("[lookupStatus] Statuses.getStatus('{}') -> {} (class={})",
                    lowerName, status, status != null ? status.getClass().getName() : "null");
            if (status != null && status instanceof com.cobblemon.mod.common.pokemon.status.PersistentStatus) {
                result = status;
            }
        } catch (Throwable t) {
            PokemonEditorDebug.warnStatus("[lookupStatus] Statuses.getStatus('{}') FAILED: {}", lowerName, t.getMessage());
        }

        // 尝试2: Statuses.getStatus("cobblemon:" + lowerName)
        if (result == null) {
            try {
                java.lang.reflect.Method getStatusM = com.cobblemon.mod.common.api.pokemon.status.Statuses.class.getMethod("getStatus", String.class);
                String ns = "cobblemon:" + lowerName;
                Object status = getStatusM.invoke(null, ns);
                PokemonEditorDebug.infoStatus("[lookupStatus] Statuses.getStatus('{}') -> {}", ns, status);
                if (status != null && status instanceof com.cobblemon.mod.common.pokemon.status.PersistentStatus) {
                    result = status;
                }
            } catch (Throwable t) {
                PokemonEditorDebug.warnStatus("[lookupStatus] Statuses.getStatus('cobblemon:{}') FAILED: {}", lowerName, t.getMessage());
            }
        }

        // 尝试3: 变体
        if (result == null) {
            String[] variants = {
                lowerName,
                lowerName.replace("_", ""),
                lowerName.contains("freez") ? "frozen" : null,
                lowerName.equals("frozen") ? "freeze" : null,
                lowerName.equals("bad_poison") ? "poison_badly" : null,
                lowerName.equals("poison_badly") ? "bad_poison" : null
            };
            for (String variant : variants) {
                if (variant == null) continue;
                try {
                    java.lang.reflect.Method getStatusM = com.cobblemon.mod.common.api.pokemon.status.Statuses.class.getMethod("getStatus", String.class);
                    Object status = getStatusM.invoke(null, variant);
                    PokemonEditorDebug.infoStatus("[lookupStatus] variant '{}' -> {}", variant, status);
                    if (status != null && status instanceof com.cobblemon.mod.common.pokemon.status.PersistentStatus) {
                        result = status;
                        break;
                    }
                } catch (Throwable ignored) {}
            }
        }

        // 尝试4: Statuses 静态字段遍历
        if (result == null) {
            try {
                for (java.lang.reflect.Field f : com.cobblemon.mod.common.api.pokemon.status.Statuses.class.getFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                        Object val = f.get(null);
                        if (val != null && val instanceof com.cobblemon.mod.common.pokemon.status.PersistentStatus) {
                            try {
                                Object nameRL = val.getClass().getMethod("getName").invoke(val);
                                PokemonEditorDebug.infoStatus("[lookupStatus] field '{}' getName()={}", f.getName(), nameRL);
                                if (nameRL instanceof ResourceLocation rl) {
                                    if (rl.getPath().equalsIgnoreCase(lowerName) ||
                                            rl.getPath().equalsIgnoreCase(lowerName.replace("_", ""))) {
                                        result = val;
                                        PokemonEditorDebug.infoStatus("[lookupStatus] MATCH via field '{}' path='{}'", f.getName(), rl.getPath());
                                        break;
                                    }
                                    // 别名匹配：Cobblemon内部状态名与用户输入不一致
                                    // freeze -> frozen, bad_poison -> poisonbadly
                                    String aliasPath = null;
                                    if (lowerName.equals("freeze")) aliasPath = "frozen";
                                    else if (lowerName.equals("bad_poison") || lowerName.equals("badly_poison") || lowerName.equals("toxic")) aliasPath = "poisonbadly";
                                    if (aliasPath != null && rl.getPath().equalsIgnoreCase(aliasPath)) {
                                        result = val;
                                        PokemonEditorDebug.infoStatus("[lookupStatus] alias match '{}' -> '{}' via field '{}'", lowerName, aliasPath, f.getName());
                                        break;
                                    }
                                }
                            } catch (Throwable ignored) {}
                            if (f.getName().toLowerCase().contains(lowerName.replace("_", "")) && result == null) {
                                result = val;
                                PokemonEditorDebug.infoStatus("[lookupStatus] MATCH via field name: '{}'", f.getName());
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                PokemonEditorDebug.warnStatus("[lookupStatus] field traversal FAILED: {}", t.getMessage());
            }
        }

        if (result == null) {
            PokemonEditorDebug.errorStatus("[lookupStatus] ALL methods FAILED for '{}'", lowerName);
        } else {
            PokemonEditorDebug.infoStatus("[lookupStatus] RESULT: {} (class={})", result, result.getClass().getName());
            putToRegistryCache(cacheKey, result);  // [优化-C01] 只缓存非null结果
        }
        return result;
    }

    private static Object lookupSpeciesObj(String name) {
        if (name == null || name.isEmpty()) return null;
        String lowerName = name.toLowerCase().trim();
        String cacheKey = "species#" + lowerName;
        if (registryCache.containsKey(cacheKey)) return registryCache.get(cacheKey);

        Object result = null;
        try {
            Method getByName = PokemonSpecies.class.getMethod("getByName", String.class);
            Object r = getByName.invoke(null, lowerName);
            if (r != null) result = r;
        } catch (Throwable ignored) {}
        if (result == null) {
            try {
                Object companion = com.cobblemon.mod.common.pokemon.Species.class.getField("Companion").get(null);
                for (Method m : companion.getClass().getMethods()) {
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) {
                        try {
                            Object r = m.invoke(companion, lowerName);
                            if (r != null && r.getClass().getName().contains("Species")) {
                                result = r;
                                break;
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }
        putToRegistryCache(cacheKey, result);  // [优化-C01]
        return result;
    }

    private static List<String> extractAbilityIdsFromSpecies(Object speciesOrForm) {
        List<String> result = new ArrayList<>();
        if (speciesOrForm == null) return result;
        try {
            Object abilityPool = null;
            for (Method m : speciesOrForm.getClass().getMethods()) {
                if ((m.getName().equals("getAbilities") || m.getName().equals("abilities"))
                        && m.getParameterCount() == 0) {
                    abilityPool = m.invoke(speciesOrForm);
                    break;
                }
            }
            if (abilityPool instanceof Iterable<?>) {
                for (Object potentialAbility : (Iterable<?>) abilityPool) {
                    if (potentialAbility == null) continue;
                    try {
                        Object template = null;
                        for (Method m : potentialAbility.getClass().getMethods()) {
                            if ((m.getName().equals("getTemplate") || m.getName().equals("template"))
                                    && m.getParameterCount() == 0) {
                                template = m.invoke(potentialAbility);
                                break;
                            }
                        }
                        if (template != null) {
                            String nm = extractStringId(template);
                            if (!nm.isEmpty() && !result.contains(nm)) result.add(nm);
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
        return result;
    }

    private static int readStat(Object statsObj, String statName, String... accessorNames) {
        if (statsObj == null) return 0;
        Class<?> c = statsObj.getClass();

        try {
            initBridgeCache(); // [线性-L07] 使用缓存的Stats类
            Object statEnumVal = null;
            String upperName = statName.toUpperCase();
            for (Object e : cachedStatsClass.getEnumConstants()) {
                String n = e.toString().toUpperCase();
                if (n.equals(upperName)) { statEnumVal = e; break; }
            }
            if (statEnumVal == null) {
                for (Object e : cachedStatsClass.getEnumConstants()) {
                    String n = e.toString().toUpperCase();
                    boolean match = false;
                    if (upperName.equals("ATK") && n.equals("ATTACK")) match = true;
                    else if (upperName.equals("SPA") && (n.equals("SPECIAL_ATTACK") || n.equals("SPATTACK") || n.contains("SPECIALATTACK"))) match = true;
                    else if (upperName.equals("SPD") && (n.equals("SPECIAL_DEFENCE") || n.equals("SPECIAL_DEFENSE") || n.equals("SPDEF") || n.contains("SPECIALDEFENCE") || n.contains("SPECIALDEFENSE"))) match = true;
                    else if (upperName.equals("HP") && n.equals("HP")) match = true;
                    else if (upperName.equals("DEF") && (n.equals("DEFENCE") || n.equals("DEFENSE"))) match = true;
                    else if (upperName.equals("SPE") && n.equals("SPEED")) match = true;
                    if (match) { statEnumVal = e; break; }
                }
            }
            if (statEnumVal == null) {
                for (Object e : cachedStatsClass.getEnumConstants()) {
                    String n = e.toString().toUpperCase();
                    if (upperName.equals("SPE") && (n.contains("SPECIAL_") || n.contains("SPATTACK") || n.contains("SPDEF"))) continue;
                    if (upperName.equals("SPA") && (n.contains("SPECIAL_DEFENSE") || n.contains("SPDEF"))) continue;
                    if (upperName.equals("SPD") && (n.contains("SPECIAL_ATTACK") || n.contains("SPATTACK"))) continue;
                    if (n.contains(upperName)) { statEnumVal = e; break; }
                }
            }
            if (statEnumVal != null) {
                for (Method m : c.getMethods()) {
                    if (m.getName().equals("get") && m.getParameterCount() == 1) {
                        try {
                            Object v = m.invoke(statsObj, statEnumVal);
                            if (v instanceof Number) return ((Number) v).intValue();
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}

        for (String name : accessorNames) {
            try {
                Method m = c.getMethod(name);
                Object v = m.invoke(statsObj);
                if (v instanceof Number) return ((Number) v).intValue();
            } catch (Throwable ignored) {}
            String getXxx = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            try {
                Method m = c.getMethod(getXxx);
                Object v = m.invoke(statsObj);
                if (v instanceof Number) return ((Number) v).intValue();
            } catch (Throwable ignored) {}
        }

        for (Method m : c.getMethods()) {
            String mn = m.getName().toLowerCase();
            if (m.getParameterCount() != 0) continue;
            for (String name : accessorNames) {
                if (mn.contains(name.toLowerCase())) {
                    try {
                        Object v = m.invoke(statsObj);
                        if (v instanceof Number) return ((Number) v).intValue();
                    } catch (Throwable ignored) {}
                }
            }
        }
        return 0;
    }

    private static void writeStat(Pokemon pokemon, Object statsObj, int value, String statName, String... accessorNames) {
        if (statsObj == null) return;
        Class<?> c = statsObj.getClass();

        try {
            initBridgeCache(); // [线性-L07] 使用缓存的Stats类
            Object statEnumVal = null;
            String upperName = statName.toUpperCase();
            for (Object e : cachedStatsClass.getEnumConstants()) {
                String n = e.toString().toUpperCase();
                if (n.equals(upperName)) { statEnumVal = e; break; }
            }
            if (statEnumVal == null) {
                for (Object e : cachedStatsClass.getEnumConstants()) {
                    String n = e.toString().toUpperCase();
                    boolean match = false;
                    if (upperName.equals("ATK") && n.equals("ATTACK")) match = true;
                    else if (upperName.equals("SPA") && (n.equals("SPECIAL_ATTACK") || n.equals("SPATTACK"))) match = true;
                    else if (upperName.equals("SPD") && (n.equals("SPECIAL_DEFENCE") || n.equals("SPECIAL_DEFENSE") || n.equals("SPDEF"))) match = true;
                    else if (upperName.equals("HP") && n.equals("HP")) match = true;
                    else if (upperName.equals("DEF") && (n.equals("DEFENCE") || n.equals("DEFENSE"))) match = true;
                    else if (upperName.equals("SPE") && n.equals("SPEED")) match = true;
                    if (match) { statEnumVal = e; break; }
                }
            }
            if (statEnumVal != null) {
                boolean isIV = c.getSimpleName().contains("IV");
                String setterName = isIV ? "setIV" : "setEV";
                for (Method m : pokemon.getClass().getMethods()) {
                    if (m.getName().equalsIgnoreCase(setterName) && m.getParameterCount() == 2) {
                        try {
                            m.invoke(pokemon, statEnumVal, value);
                            return;
                        } catch (Throwable ignored) {}
                    }
                }
                for (Method m : c.getMethods()) {
                    if (m.getName().equals("set") && m.getParameterCount() == 2) {
                        try { m.invoke(statsObj, statEnumVal, Integer.valueOf(value)); return; } catch (Throwable ignored) {}
                        try { m.invoke(statsObj, statEnumVal, value); return; } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}

        for (String name : accessorNames) {
            String capName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            for (String mName : new String[]{capName, name}) {
                for (Method m : c.getMethods()) {
                    if (m.getName().equals(mName) && m.getParameterCount() == 1) {
                        try { m.invoke(statsObj, value); return; } catch (Throwable ignored) {}
                        try { m.invoke(statsObj, Integer.valueOf(value)); return; } catch (Throwable ignored) {}
                    }
                }
            }
        }

        for (Method m : c.getMethods()) {
            String mn = m.getName().toLowerCase();
            if (m.getParameterCount() != 1) continue;
            for (String name : accessorNames) {
                if (mn.contains(name.toLowerCase())) {
                    try { m.invoke(statsObj, value); return; } catch (Throwable ignored) {}
                    try { m.invoke(statsObj, Integer.valueOf(value)); return; } catch (Throwable ignored) {}
                }
            }
        }
    }
}