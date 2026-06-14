package com.cobblemon_ts.mod.pokemoneditor;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CobblemonBridge {

    private static final Map<String, Object> registryCache = new HashMap<>();
    private static final Gson GSON = new Gson();

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
            data.speciesName = extractStringId(pokemon.getSpecies());
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
                Method getSlotsM = null;
                Class<?> cls = party.getClass();
                while (cls != null && getSlotsM == null) {
                    try {
                        getSlotsM = cls.getDeclaredMethod("getSlots");
                    } catch (NoSuchMethodException ignored) {}
                    cls = cls.getSuperclass();
                }
                if (getSlotsM != null) {
                    getSlotsM.setAccessible(true);
                    java.util.List<?> slotsList = (java.util.List<?>) getSlotsM.invoke(party);
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
                    Method getM = party.getClass().getMethod("get", int.class);
                    Object result = getM.invoke(party, targetSlot);
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
                        try {
                            Class<?> ppClass = Class.forName("com.cobblemon.mod.common.api.storage.party.PartyPosition");
                            java.lang.reflect.Constructor<?> ppCtor = ppClass.getConstructor(int.class);
                            Object position = ppCtor.newInstance(targetSlot);

                            Class<?> partyPosClass = Class.forName("com.cobblemon.mod.common.api.storage.party.PartyPosition");
                            Method setAtPosM = null;
                            Class<?> cls = party.getClass();
                            while (cls != null && setAtPosM == null) {
                                try {
                                    setAtPosM = cls.getDeclaredMethod("setAtPosition", partyPosClass, Pokemon.class);
                                } catch (NoSuchMethodException ignored) {}
                                cls = cls.getSuperclass();
                            }
                            if (setAtPosM != null) {
                                setAtPosM.setAccessible(true);
                                setAtPosM.invoke(party, position, pokemon);
                                PokemonEditorDebug.logEditor("party.setAtPosition(PartyPosition({}), Pokemon) OK - created new pokemon", targetSlot);
                            }
                        } catch (Throwable t) {
                            PokemonEditorDebug.warnEditor("setAtPosition for new pokemon failed: {}", t.getMessage());
                            try {
                                Method setM = party.getClass().getMethod("set", int.class, Pokemon.class);
                                setM.invoke(party, targetSlot, pokemon);
                                PokemonEditorDebug.logEditor("party.set(int, Pokemon) fallback OK - created new pokemon", targetSlot);
                            } catch (Throwable t2) {
                                PokemonEditorDebug.warnEditor("party.set(int, Pokemon) fallback also failed: {}", t2.getMessage());
                            }
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
                Method onChangedM = party.getClass().getMethod("onPokemonChanged", Pokemon.class);
                onChangedM.invoke(party, pokemon);
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
                Method sendToM = party.getClass().getMethod("sendTo", ServerPlayer.class);
                sendToM.invoke(party, targetPlayer);
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
                Method updateM = pokemon.getClass().getMethod("update");
                updateM.invoke(pokemon);
                PokemonEditorDebug.logEditor("pokemon.update() OK");
            } catch (Throwable ignored) {}

            try {
                Object moveSetObj = pokemon.getMoveSet();
                if (moveSetObj != null) {
                    Method moveSetUpdateM = moveSetObj.getClass().getMethod("update");
                    moveSetUpdateM.invoke(moveSetObj);
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
                Method getSlotsM = null;
                Class<?> cls = party.getClass();
                while (cls != null && getSlotsM == null) {
                    try {
                        getSlotsM = cls.getDeclaredMethod("getSlots");
                    } catch (NoSuchMethodException ignored) {}
                    cls = cls.getSuperclass();
                }
                if (getSlotsM != null) {
                    getSlotsM.setAccessible(true);
                    java.util.List<?> slotsList = (java.util.List<?>) getSlotsM.invoke(party);
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
                    Method getM = party.getClass().getMethod("get", int.class);
                    pokemon = getM.invoke(party, slot);
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
                Class<?> ppClass = Class.forName("com.cobblemon.mod.common.api.storage.party.PartyPosition");
                java.lang.reflect.Constructor<?> ppCtor = ppClass.getConstructor(int.class);
                Object position = ppCtor.newInstance(slot);

                Class<?> partyPosClass = Class.forName("com.cobblemon.mod.common.api.storage.party.PartyPosition");
                Method setAtPosM = null;
                Class<?> cls = party.getClass();
                while (cls != null && setAtPosM == null) {
                    try {
                        setAtPosM = cls.getDeclaredMethod("setAtPosition", partyPosClass, Pokemon.class);
                    } catch (NoSuchMethodException ignored) {}
                    cls = cls.getSuperclass();
                }
                if (setAtPosM != null) {
                    setAtPosM.setAccessible(true);
                    setAtPosM.invoke(party, position, null);
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
                Method sendToM = party.getClass().getMethod("sendTo", ServerPlayer.class);
                sendToM.invoke(party, targetPlayer);
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
            Class<?> movesClass = Class.forName("com.cobblemon.mod.common.api.moves.Moves");
            for (java.lang.reflect.Method m : movesClass.getMethods()) {
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

        registryCache.put(key, result);
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
            java.lang.reflect.Constructor<?> ctor = null;
            PokemonEditorDebug.infoPP("[constructMove] Move.class.getDeclaredConstructors() count={}",
                    Move.class.getDeclaredConstructors().length);
            for (java.lang.reflect.Constructor<?> c : Move.class.getDeclaredConstructors()) {
                Class<?>[] pts = c.getParameterTypes();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < pts.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(pts[i].getSimpleName());
                }
                PokemonEditorDebug.infoPP("[constructMove]   ctor({}) params=[{}]",
                        pts.length, sb.toString());
                if (pts.length == 3 && pts[1] == int.class && pts[2] == int.class) {
                    ctor = c;
                    PokemonEditorDebug.infoPP("[constructMove]   MATCHED! using this ctor");
                    break;
                }
            }
            if (ctor != null) {
                ctor.setAccessible(true);
                Move result = (Move) ctor.newInstance(tpl, finalPP, raised);
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

    private static List<Object> allMoveTemplatesCache = null;
    public static List<String> getAllMoveIds() {
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
            Class<?> movesClass = Class.forName("com.cobblemon.mod.common.api.moves.Moves");
            for (java.lang.reflect.Field f : movesClass.getFields()) {
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
            for (java.lang.reflect.Method m : movesClass.getMethods()) {
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
        return new ArrayList<>(unique);
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

        // 等级修改
        boolean levelChanged = false;
        if (modifiedFields.contains("level") || modifiedFields.isEmpty()) {
            try {
                int oldLevel = pokemon.getLevel();
                pokemon.setLevel(pkt.level());
                PokemonEditorDebug.logEditor("level: {} -> {}", oldLevel, pkt.level());
                levelChanged = true;
            } catch (Throwable e) {
                PokemonEditorDebug.warnEditor("setLevel failed: {}", e.getMessage());
            }
        }

        // 经验值修改
        if (modifiedFields.contains("experience") || modifiedFields.isEmpty()) {
            try {
                int expToSet;
                if (pkt.experience() <= 0) {
                    expToSet = 0;
                } else {
                    expToSet = Math.max(0, Math.min(Integer.MAX_VALUE, pkt.experience()));
                }
                try {
                    pokemon.getClass().getMethod("setExperience$common", int.class).invoke(pokemon, expToSet);
                    PokemonEditorDebug.logEditor("experience: {}", expToSet);
                } catch (Throwable e) {
                    try {
                        pokemon.setExperienceAndUpdateLevel(expToSet);
                        PokemonEditorDebug.logEditor("experience (fallback): {}", expToSet);
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
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

        PokemonEditorDebug.logEditor("writePokemonFields DONE");
    }

    private static void writePokemonFieldsLegacy(Pokemon pokemon, PokemonEditorNetwork.UpdatePokemonPacket pkt) {

        if (pkt.speciesName() != null && !pkt.speciesName().isEmpty()) {
            try {
                pokemon.setSpecies((com.cobblemon.mod.common.pokemon.Species) lookupSpeciesObj(pkt.speciesName()));
            } catch (Throwable e) {
                PokemonEditorDebug.log("setSpecies 婵犮垺鍎肩划鍓ф喆? " + e.getMessage());
            }
        }

        try {
            pokemon.setLevel(pkt.level());
        } catch (Throwable e) {
            PokemonEditorDebug.log("setLevel 婵犮垺鍎肩划鍓ф喆? " + e.getMessage());
        }

        try {
            pokemon.getClass().getMethod("setExperience$common", int.class).invoke(pokemon, pkt.experience());
        } catch (Throwable e) {
            try {
                pokemon.setExperienceAndUpdateLevel(pkt.experience());
            } catch (Throwable ignored) {}
        }

        try {
            pokemon.setFriendship(Math.max(0, Math.min(255, pkt.friendship())), true);
        } catch (Throwable e) {
            PokemonEditorDebug.log("setFriendship 婵犮垺鍎肩划鍓ф喆? " + e.getMessage());
        }

        try {
            pokemon.setShiny(pkt.shiny());
        } catch (Throwable ignored) {}

        try {
            if (pkt.displayName() == null || pkt.displayName().isEmpty()) {
                pokemon.setNickname(null);
            } else {
                pokemon.setNickname(net.minecraft.network.chat.Component.literal(pkt.displayName()));
            }
        } catch (Throwable ignored) {}

        String uuidStr = pkt.pokemonUUID();
        if (uuidStr != null && !uuidStr.isEmpty()) {
            try {
                UUID newUuid = UUID.fromString(uuidStr);
                pokemon.setUuid(newUuid);
            } catch (Throwable ignored) {}
        }

        try {
            Object genderEnum = resolveGender(pkt.gender());
            if (genderEnum != null)
                pokemon.setGender((com.cobblemon.mod.common.pokemon.Gender) genderEnum);
        } catch (Throwable ignored) {}

        if (pkt.nature() != null && !pkt.nature().isEmpty()) {
            Object natureObj = lookupRegistryObj(pkt.nature());
            if (natureObj != null) {
                try {
                    pokemon.setNature((com.cobblemon.mod.common.pokemon.Nature) natureObj);
                } catch (Throwable ignored) {}
            }
        }

        if (pkt.ability() != null && !pkt.ability().isEmpty()) {
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

        if (pkt.heldItem() != null && !pkt.heldItem().isEmpty()) {
            Item item = findItem(pkt.heldItem());
            if (item != null) {
                ItemStack stack = new ItemStack(item);
                try {
                    pokemon.setHeldItem$common(stack);
                } catch (Throwable ignored) {}
            }
        }

        try {
            PokemonEditorDebug.infoStatus("[writeLegacy] pkt.status()='{}'", pkt.status());
            if (pkt.status() != null && !pkt.status().isEmpty()) {
                Object statusObj = lookupStatusObj(pkt.status());
                PokemonEditorDebug.infoStatus("[writeLegacy] lookupStatusObj -> {}", statusObj);
                if (statusObj != null) {
                    pokemon.applyStatus((com.cobblemon.mod.common.pokemon.status.PersistentStatus) statusObj);
                    PokemonEditorDebug.infoStatus("[writeLegacy] applyStatus OK, getStatus={}", pokemon.getStatus());
                }
            } else {
                pokemon.setStatus(null);
                PokemonEditorDebug.infoStatus("[writeLegacy] setStatus(null) OK");
            }
        } catch (Throwable t) {
            PokemonEditorDebug.errorStatus("[writeLegacy] EXCEPTION: {}", t.toString());
        }

        try {
            Object ivsObj = pokemon.getIvs();
            if (ivsObj != null) {
                writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivHp())), "HP", "hp", "Hp");
                writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivAtk())), "ATK", "attack", "atk", "Atk");
                writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivDef())), "DEF", "defense", "def", "Def");
                writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivSpA())), "SPA", "specialAttack", "spAttack", "spa", "SpA");
                writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivSpD())), "SPD", "specialDefense", "spDefense", "spd", "SpD");
                writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivSpe())), "SPE", "speed", "spe", "Spe");
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
                writeStat(pokemon, evsObj, Math.max(0, Math.min(252, evHp)), "HP", "hp", "Hp");
                writeStat(pokemon, evsObj, Math.max(0, Math.min(252, evAtk)), "ATK", "attack", "atk", "Atk");
                writeStat(pokemon, evsObj, Math.max(0, Math.min(252, evDef)), "DEF", "defense", "def", "Def");
                writeStat(pokemon, evsObj, Math.max(0, Math.min(252, evSpA)), "SPA", "specialAttack", "spAttack", "spa", "SpA");
                writeStat(pokemon, evsObj, Math.max(0, Math.min(252, evSpD)), "SPD", "specialDefense", "spDefense", "spd", "SpD");
                writeStat(pokemon, evsObj, Math.max(0, Math.min(252, evSpe)), "SPE", "speed", "spe", "Spe");
            }
        } catch (Throwable ignored) {}

        try {
            Object moveSetObj = callNoArgs(pokemon, "getMoveSet", "moveSet");
            if (moveSetObj != null) {
                String[] moveNames = {pkt.move1(), pkt.move2(), pkt.move3(), pkt.move4()};
                int[] movePPs = {pkt.move1PP(), pkt.move2PP(), pkt.move3PP(), pkt.move4PP()};
                int[] moveMaxPPs = {pkt.move1MaxPP(), pkt.move2MaxPP(), pkt.move3MaxPP(), pkt.move4MaxPP()};

                Method clearM = moveSetObj.getClass().getMethod("clear");
                clearM.invoke(moveSetObj);

                Method addM = null;
                for (Method m : moveSetObj.getClass().getMethods()) {
                    if (m.getName().equals("add") && m.getParameterCount() == 1) {
                        addM = m;
                        break;
                    }
                }

                for (int i = 0; i < 4; i++) {
                    String moveName = moveNames[i];
                    int ppVal = movePPs[i];
                    int maxPP = moveMaxPPs[i];

                    if (moveName == null || moveName.isEmpty()) continue;

                    MoveTemplate template = lookupMoveTemplate(moveName);
                    if (template == null) {
                        PokemonEditorDebug.log("Move not found: " + moveName + ", skipping");
                        continue;
                    }

                    int basePP = template.getPp();
                    int raisedLimit = (int) Math.floor(basePP * 1.6);
                    int raisedStages = (maxPP >= raisedLimit) ? 1 : 0;
                    int cap = (raisedStages == 1) ? raisedLimit : basePP;
                    int finalPP;
                    if (ppVal > 0) {
                        finalPP = Math.min(ppVal, cap);
                    } else {
                        finalPP = cap;
                    }

                    Move newMove = template.create(finalPP, raisedStages);
                    if (newMove != null && addM != null) {
                        addM.invoke(moveSetObj, newMove);
                        PokemonEditorDebug.log("Move set: " + moveName + " (PP=" + finalPP + "/" + cap + ", raised=" + raisedStages + ")");
                    }
                }
            }
        } catch (Throwable t) {
            PokemonEditorDebug.log("闂佺懓鐏堥崑鎾绘煠鐎圭姵顥夐柛鐔告崌瀹曟濡烽敂鐟颁壕閻犲洤鐪版禍濂告煥? " + t.getMessage());
        }

        try {
            int currentMaxHp = pokemon.getMaxHealth();
            int hp = pkt.currentHp();
            if (hp <= 0) {
                hp = currentMaxHp;
            }
            hp = Math.max(0, Math.min(currentMaxHp, hp));
            pokemon.setCurrentHealth(hp);
        } catch (Throwable ignored) {}
    }

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
            Class<?> genderClass = Class.forName("com.cobblemon.mod.common.pokemon.Gender");
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
            for (Method m : obj.getClass().getMethods()) {
                if ((m.getName().equals("getShowdownId") || m.getName().equals("showdownId")
                        || m.getName().equals("getName") || m.getName().equals("name"))
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

        registryCache.put(cacheKey, result);
        return result;
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

        registryCache.put(cacheKey, result);
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
            registryCache.put(cacheKey, result); // 只缓存非null结果，避免null缓存阻止重试
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
        registryCache.put(cacheKey, result);
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
            Class<?> statsEnum = Class.forName("com.cobblemon.mod.common.api.pokemon.stats.Stats");
            Object statEnumVal = null;
            String upperName = statName.toUpperCase();
            for (Object e : statsEnum.getEnumConstants()) {
                String n = e.toString().toUpperCase();
                if (n.equals(upperName)) { statEnumVal = e; break; }
            }
            if (statEnumVal == null) {
                for (Object e : statsEnum.getEnumConstants()) {
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
                for (Object e : statsEnum.getEnumConstants()) {
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
            Class<?> statsEnum = Class.forName("com.cobblemon.mod.common.api.pokemon.stats.Stats");
            Object statEnumVal = null;
            String upperName = statName.toUpperCase();
            for (Object e : statsEnum.getEnumConstants()) {
                String n = e.toString().toUpperCase();
                if (n.equals(upperName)) { statEnumVal = e; break; }
            }
            if (statEnumVal == null) {
                for (Object e : statsEnum.getEnumConstants()) {
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