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

    // ============================================================
    // 闁哄鐗嗛幊搴㈡叏椤忓牆妫橀悷娆忓閵嗗﹪鏌ㄥ☉娆忓摵闁逞屽墯娣囪櫣鎹㈤崘銊庯綁骞嬮幒鏃傂㈢紓浣风┒閸ㄥ湱妲愰埡鍐╁珰閻犲洦褰冪徊鍧楁倵鐟欏嫮绠撶憸鐗堝絻椤繘鏁傞懞銉︻啀闂佺顕栭崰鏍嚐閻斿憡濮滄い鎺嶇閻?PokemonData
    // 闂佹椿娼块崝瀣姳椤掑倻纾介柡宥庡墰鐢棝鏌涢敐鍐ㄥ闁哄拋鍋婂Λ鍐閻樿尙浠存繝娈垮枛椤戝懐鈧灚鐓″畷顏嗕沪閻愵兘鏋栫紓浣插亾闁告繂瀚粔濂告煕閹惧磭效婵¤闄勭€电厧顓奸崶顬箓鏌?
    // ============================================================
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

    // ============================================================
    // 闁哄鐗嗛幊搴㈡叏椤忓牆妫橀悷娆忓閵嗗﹪鏌ㄥ☉娆掑闁?Cobblemon Pokemon 闁诲海鏁搁、濠囨寘閸曨剚濮滄い鎺嶇閻忓洦淇婇銈囩？缂侇喖绉瑰畷姗€宕ㄩ弶鎴濆Г PokemonData
    // PokemonData 闂佸搫瀚烽崹宕囨椤忓懏缍囬柟瀛樼箖閻濄倝鏌ｉ敐鍛劯婵炶偐澧楃粙澶嬫償閵忥紕鐤€闂佸憡妫戠槐鏇㈩敂椤掍胶鈻曢悗锝庡枟閿涚喖鏌ｉ妸銉ヮ仾闁哄棛鍠栭獮鎴︻敊閹稿海銈伴梺?
    // ============================================================
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
            // 闂佸疇妫勯幊搴ㄦ偄閳ь剟鏌￠崼顐㈠闁活偅锕㈤獮鈧憸婵堟閻х嚡ID闂侀潧妫旈懗鑸垫櫠鐠恒劎鐭撶€广儱鍟犻崑鎾存媴濮濆苯鐓傞梺璇″厸闂勫秹鍩€椤戣儻鍏岄柣鈩冨灩閻ヮ亣顦伴柍褜鍏涢懗鍓佸垝閳╁喚娈介悘鐐靛劦閸嬫挻鎷呮搴＄墯闁诲酣娼уΛ妤冣偓瑙勫▕婵″瓨绗熼埀顒€螞椤栫偛绀傚璺虹墐閸嬫挻绋夐悤?
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

            // 闂佸疇妫勯幊蹇涘箚閿涘嫭鏆滈柨鏃€鍎抽埛鏍煥濞戞澧曠憸鐗堢叀閺屽懏寰勫☉姘鳖槷闂備緡鍠撻崝宀勫垂鎼淬劍鍋嬮柛顐ゅ枑閹烽亶鏌涘▎妯虹仴闁稿繑锚閳讳粙鍩勯崘鈺冪暢闂?
            // 闁诲繐绻戠换鍡涙儊椤栫偞鐒绘慨妯虹－缁?pokemon.heldItem() 闂佸吋鍎抽崲鑼躲亹?ItemStack闂佹寧绋戦惉濂稿礉瑜斿畷銉︽償閵堝棛銈梺鐟扮摠閸旀绮婇锔芥櫢?ID
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

            // ==================== Status ====================
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

            // IVs / EVs - 闂備緡鍋呮穱铏规崲閸愵喖鐭楃€广儱鎳愬▓鎼佹偣鐎ｎ亜鏆熼柡?getIvs / getEvs闂佹寧绋戦悧鎾诲磿閹绢喗鏅?Cobblemon 闂佺粯顨呴悧濠傦耿娴煎瓨鏅?getIvs() 闁哄鏅滈弻銊ッ洪弽銊р枖鐎广儱鎳忛崐閬嶆倵閻㈡鏀伴柦鏍у閺?
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

            // Nature / Ability
            Object natureObj = pokemon.getNature();
            if (natureObj != null) data.nature = extractStringId(natureObj);

            Object abilityObj = pokemon.getAbility();
            if (abilityObj != null) data.ability = extractStringId(abilityObj);

            // 闂?species/form 闂佸吋鍎抽崲鑼躲亹?possibleAbilities
            try {
                Object speciesObj = pokemon.getSpecies();
                if (speciesObj != null) {
                    data.possibleAbilities.addAll(extractAbilityIdsFromSpecies(speciesObj));
                }
                if (data.possibleAbilities.isEmpty() && formObj != null) {
                    data.possibleAbilities.addAll(extractAbilityIdsFromSpecies(formObj));
                }
            } catch (Throwable ignored) {}

            // ==================== MoveSet - 闂佸疇妫勯幊鎰洪崸妤€绠抽柕澶堝劚閸愨偓闁诲繐绻愮€氼垶顢氭导鏉戠煑闁哄诞鍐╂殽闂?moves 闂佽桨鐒︽竟鍡欏垝瀹ュ鏅?====================
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

                    // 闂佸疇妫勯幊宥囨椤撱垹绀傞柛顐墰缁愭鏌ｉ埡浣烘憼閻㈩垱鎸冲畷锝咁吋閸涱垱顔忛柣鐘叉穿椤曆囧船閹绢喗鐒鹃柕濠忛檮濞堝墎绱撴担绋款伒缂佽鲸鐟╁鐢稿焵椤掑倸鍨濋柛鎾椻偓閳ь剚锕㈤弫宥囦沪閻ｅ本娈梺绋跨箞閸斿秶鈧灚绮屽嵄闁瑰瓨甯炵粔瀵哥磼娓氬灝鐏╃紒鎵佹櫊閺?
                    boolean usedInternal = readMovesFromInternalArray(moveSetObj, moveNames, movePPs, moveMaxPPs, moveRaised);
                    PokemonEditorDebug.infoMoves("[MoveRead] usedInternal={}", usedInternal);

                    if (!usedInternal) {
                        // 闂佹悶鍎抽崑銈夊焵椤戣棄浜鹃梺鎸庣⊕椤掓瓱t(int) 闂佸搫鍊介～澶屾兜閸洘鏅柛顐ｇ箥濞层倝鏌＄€ｎ偆鐭婇柣掳鍔戝畷鐑解€﹂幒鏃傤槴
                        boolean usedIndexed = readMovesByIndex(moveSetObj, moveNames, movePPs, moveMaxPPs, moveRaised);
                        PokemonEditorDebug.infoMoves("[MoveRead] usedIndexed={}", usedIndexed);
                        if (!usedIndexed) {
                            // 闂佸搫鐗冮崑鎾绘煕濮橆剟顎楁繛鍙夌墵閺屽懘鍩€椤掑嫭鏅慨婵撶erator闂佹寧绋戦悧鍡氥亹閺屻儲鍤勯柛濠勫枔閸庢煡寮堕埡浣瑰窛闁宠鐗曞嵄闁瑰瓨甯炵粔鎾煥?
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
                                            moveMaxPPs[idx] = (tpl != null ? tpl.getPp() : move.getMaxPp());
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
                                                moveMaxPPs[idx] = (tpl != null ? tpl.getPp() : move.getMaxPp());
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

            // Gender
            Object genderObj = pokemon.getGender();
            if (genderObj != null) {
                String gn = genderObj.toString().toUpperCase();
                if (gn.contains("MALE") && !gn.contains("FE")) data.gender = 0;
                else if (gn.contains("FEMALE")) data.gender = 1;
                else data.gender = 2;
            }

            // Nickname
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
        // 闂侀潧妫欓崝鏍э耿閸ヮ剙绀夐柨娑樺娴煎倿鏌曢崱妯虹仴闁哥喐鎹囬弫?闂佸搫娲ら悺銊╁蓟婵犲洤绠伴柛銉戝懏姣庡┑鈥冲级閸ㄦ繄绱為崨瀛樺剭闁告洦鍋嗛弶娲煕濞嗘ê鐏￠柍顏嗘暬瀵偊鎮ч崼婵堛偊闂佹寧绋戦悧蹇曟椤忓懏缍囬柟瀛樼箖閻濄倕菐閸ャ劎绠撻柣掳鍔戦幃浠嬧€﹂幒鏃傤槴
        // 婵炴垶鎸堕崐妤咃綖閹烘绠氶柛娑卞幖閺嬪矂鏌ㄥ☉娆樺剸) 濠碘€冲级閸ㄦ繄绱為崨顓у晠闁告瑥顦扮粻鎺楁倵鐟欏嫮绠撶憸鐗堢叀閺?闂?婵烇絽娴傞崰妤呭极閼测晙娌柣鎰ˉ閸嬫捁顧傜紒杈╂超) 濠碘€冲级閸ㄦ繄绱為崨顔锯枖闁惧繐鍘滈弫?闂?闂佸憡甯楃粙鎴犵磽閹捐妫橀柡澶庢硶閺夋椽鏌涘▎妯虹仭闁愁亞鏁诲銊╁箚瑜嶅?
        // 婵炴垶鎹佸銊у垝閸喓鈻曢柛顐犲灪閺嗗繘鏌涘▎蹇曅㈤柣銊у█閺佸秹鎮㈤柨瀣伅 delete 闂佺儵鏅濋…鍫ュ箖閹捐鍌ㄩ柣鏃堟敱缁€鍫ユ煥濞戞瑨澹樼憸鎶婂洦鏅?setAtPosition 闂佺厧鐤囨慨銈団偓鍨矊鍗遍柟瀛樺笧缁夊绱掗鐓庘挃闁炽儲锕㈤獮娆忣吋閸曨厾鈻?
        ServerPlayer targetPlayer = resolveTargetPlayer(serverPlayer, targetUUID);
        // 闂佸搫绋勭换婵嬫偘濞嗘挻鍎庢い鏃傛櫕閸ㄥジ鏌ｅ┃鐐珔妞ゆ柨绉瑰浼搭敍濮橆厼鍓ㄩ梺闈╄礋閸斿酣宕?
        if (targetPlayer == null) {
            PokemonEditorDebug.errorEditor("targetPlayer is null, abort write");
            return;
        }

        try {
            // 闂佸吋鍎抽崲鑼躲亹?Cobblemon 闂佺绻堥崝宀勬儑?Storage 闂?闂佸吋鍎抽崲鑼躲亹閸ヮ剚鍋濋柍杞扮贰閸熲偓闂?PartyStore
            Object storage = Cobblemon.INSTANCE.getStorage();
            Object party = getPartyFromStorage(storage, targetPlayer);
            if (party == null) {
                PokemonEditorDebug.errorEditor("party is null, abort write");
                return;
            }

            // 闂佺懓鐏氶幐绋跨暤?party 缂備緡鍋夐褔骞冮弴銏℃櫢?pkt 闂佸憡鍔曢幊搴敊?
            PokemonEditorDebug.logEditor("party class={}, slot={}", party.getClass().getName(), pkt.slot());
            PokemonEditorDebug.logEditor("pkt: species={}, level={}, shiny={}, friendship={}, ivHp={}, evHp={}, move1={}, uuid={}",
                    pkt.speciesName(), pkt.level(), pkt.shiny(), pkt.friendship(),
                    pkt.ivHp(), pkt.evHp(), pkt.move1(), pkt.pokemonUUID());

            // 1. 闂佸湱顭堥ˇ鎷屾綍婵炶揪绲界粔鍫曞磼閵娿儺鍤曢柡鍥ｂ偓宕囶唵闂佸憡鐟﹂悧鏇犵礊鐎ｎ喗鏅?Pokemon
            // 闂佸疇妫勯幊搴ㄥ矗瑜旈弻銊╊敋閸℃瑧鐛ラ棅顐㈡搐閹虫﹢濡磋箛娑樼濠㈣泛鐟╅锝嗗鐎广儱娲ら崑宥団偓娈垮枟濞叉繈鐛箛娑樼煑闁哄秶鏁哥粈澶娾槈閹惧磭啸闁稿繑锕㈤弫?party.get(int)闂佹寧绋戦懟顖炴偩閻樿鐭楁い鏍ㄧ箓閸樻挳寮堕埡鍌涚叆婵炲弶鐗犻幆鍐礋椤掍絿锕傛煥?n 婵炴垶鎼╂禍顏勵焽椤忓棛鐭氶柛婵嗗閺夋椽鏌涘▎妯虹仭闁?
            // 婵炴挻鑹鹃鍛淬€呰閺佸秴顫㈢壕宄痶=0 缂備礁鐬肩亸銊ф濠曠ot=1 闂佸搫鐗嗛ˇ鍗炐掓繝鍥х闁炽儴灏欓悷?闂?party.get(0) 闂佸憡鐟崹鐢稿礂濡粯浜ら柡鍌涘缁€鈧梺娲荤厛閸犳牕鐣锋潏銊р枖婵☆垰绨遍崑鎾朵沪閼测晝鎲归梺?null
            Pokemon pokemon = null;
            int targetSlot = pkt.slot();

            // 闁诲繐绻戠换鍡涙儊?getSlots() 闂佸搫鍊介～澶屾兜?
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

            // getSlots() 婵犮垺鍎肩划鍓ф喆閿曞倸绀嗘繛鍡楃箲缁€鈧梻渚囧亐閸嬫捇鏌?slots 闁诲孩绋掗〃鍡涱敊?
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

            // 闂佸搫鐗冮崑鎾绘煕濮橆剟顎楁繛鍙夌墵閺屽懘鍩€椤掑嫭鏅?get(int) 闂佸搫鍊介～澶屾兜?
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

            // 2. 缂備礁鐬奸崕銈堟綍闂?-> 闂佸憡甯楃粙鎴犵磽閹剧粯鏅?Pokemon
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
                        // 闂佸疇妫勯幊搴ㄥ矗瑜旈弻銊╊敋閸℃瑧鐛ラ梺?setAtPosition(PartyPosition, Pokemon) 闂佽　鍋撻柟顖嗗啫姹插┑鈥冲级閸ㄦ繄绱為崨瀛樻櫖鐎光偓閸愶絽浜鹃悘鐐跺亹閻熸繈鏌?party.set(int, Pokemon)
                        // party.set(int, Pokemon) 闂佸憡鐟崹鐢稿礂濡吋鍟哄ù锝呮贡濠€?Pokemon 闂傚倸鐗忛崑鐔煎煘閺嶎厽鏅悘鐐村劤閻忔鎮跺☉妯侯殭閻犳劗鍠栭弫?setAtPosition 婵炴垶鎸哥粔鎾箖?
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
                            // 闂佹悶鍎抽崑銈夊焵椤戣棄浜鹃梺?set 闂佸搫鍊介～澶屾兜?
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

            // 缂傚倷绀佺换妤冩崲閸愵喗鍤旂€瑰嫭婢樼徊?闂佸憡甯楃粙鎴犵磽閹炬剚娼伴柕澶嗘櫓閳ь剙鍟撮弫宥団偓鍦亽kemon 婵炲濮寸粔宕囨嫻閻旇櫣鐭氶柣姘嚟缁€澶愭煛閸愵亜校缁绢厼澧庣槐鎺曨槾闁?
            if (pokemon == null) {
                PokemonEditorDebug.warnEditor("pokemon is still null, abort");
                return;
            }

            // 闂佺懓鐏氶幐绋跨暤閸愨晝鈹嶆い鏃囧Г閺嗩參鏌涢幘宕囆ゅ┑顔芥倐楠炩偓濞撴艾锕︾粈鍕偣鐎ｎ亜鏆㈤柣锔诲灦閹粙鈥﹂幒鏃傤槴
            try {
                PokemonEditorDebug.logEditor("BEFORE modify: species={}, level={}, shiny={}, hp={}",
                        pokemon.getSpecies().toString(), pokemon.getLevel(), pokemon.getShiny(), pokemon.getCurrentHealth());
            } catch (Throwable t) {
                PokemonEditorDebug.warnEditor("BEFORE log failed: {}", t.getMessage());
            }

            // 3. 婵烇絽娴傞崰妤呭极?Pokemon 闁诲繒鍋熼崕鎴﹀箯?
            // 婵炴垶鎸堕崐妤咃綖閹烘妫橀柣鐔稿绾偓闂佹寧绋掗? rawJsonData 闂?JSON 闂佽桨鑳剁换婵堢礊鐎ｎ喖绀夐柣妯煎劋缁佷即鏌ㄥ☉娆撴祵) 闂佸憡鐔粻鎴﹀垂?-> 闂備緡鍋呴崝鏇㈡偤瑜嶉埢鎾存媴閸愭儳鏅梺?
            net.minecraft.core.RegistryAccess regAccess = targetPlayer.server.registryAccess();
            String rawData = pkt.rawJsonData();
            if (rawData != null && !rawData.trim().isEmpty() && rawData.trim().startsWith("{")) {
                // 闂佸搫鍊婚幊鎾舵?A闂佹寧绋掑畝鎼佹偟椤斿墽妫憸搴ㄥ汲閻旂厧绠叉い鏃傚帶闂呮﹢鏌?JSON 闂佽桨鑳舵晶妤€鐣垫笟鈧弫宥呯暆閳ь剟寮?pokemon.loadFromJSON() 闂佽桨鑳剁换婵堢礊鐎ｎ喖绀夐柣妯煎劋缁?
                try {
                    JsonObject jsonObj = GSON.fromJson(rawData, JsonObject.class);
                    if (jsonObj != null) {
                        pokemon.loadFromJSON(regAccess, jsonObj);
                        PokemonEditorDebug.logEditor("applied JSON data");
                    }
                } catch (Throwable t) {
                    // JSON 闁荤喐鐟辩徊楣冩倵閼恒儱绶為弶鍫亯琚濋梺鎸庣☉閻倿鍩€椤戣棄浜鹃梺鎼炲劤閸嬫盯宕哄畝鍕劵闁归偊鍓涢幗鐔哥箾閸繄浠涢柡宀€鍠栭弫?
                    PokemonEditorDebug.warnEditor("JSON modify failed, fall back to fields: {}", t.getMessage());
                    writePokemonFields(pokemon, pkt, regAccess);
                }
            } else {
                // 闂佸搫鍊婚幊鎾舵?B闂佹寧绋掔喊宥呪枍濮椻偓閺屽懎顫濋悙瑙ｅ亾閸愵喖纭€闁哄洨鍋熼幗鐔哥箾閸埄鏆滅紒杈ㄧ懇閹囧煛閸愮偓缍掗梺闈涙閼冲爼鎮洪幋鐘垫／鐟滃鍩€椤戣法绐旀俊顐灦瀹曟寰勫畝鈧幖濂告煥濞戞﹩妾х紒杈ㄧ箞閺屽懘骞囬鍌涙喕濠电偛鐗呯徊濠氼敊閺囥垺鏅?
                PokemonEditorDebug.logEditor("applying field-level modifications (species={}, level={})",
                        pkt.speciesName(), pkt.level());
                writePokemonFields(pokemon, pkt, regAccess);
            }

            // 闂佺懓鐏氶幐绋跨暤閸愨晝鈹嶆い鏃囧Г閺嗩參鏌涘顒佹崳濠殿喗鎮傞獮鈧〒姘功缁€鍕偣鐎ｎ亜鏆㈤柣锔诲灦閹粙鈥﹂幒鏃傤槴
            try {
                PokemonEditorDebug.logEditor("AFTER modify: species={}, level={}, shiny={}, hp={}",
                        pokemon.getSpecies().toString(), pokemon.getLevel(), pokemon.getShiny(), pokemon.getCurrentHealth());
            } catch (Throwable t) {
                PokemonEditorDebug.warnEditor("AFTER log failed: {}", t.getMessage());
            }

            // 4. 闂佸疇妫勯幊搴ㄥ矗瑜旈弻銊╊敊閸忕厧鈧鲸鎱ㄥ┑鍕姷缂佸彉鍗抽弻鍛潩鏉堚晛寮?PartyStore 闂佸憡鐟﹂敋閻庡灚鐓￠悰顕€鎳滈棃娑滃悅闂備緡鍋傜粈渚€宕哄畝鈧埀顒€绠嶉崹娲春濞戞氨鍗氭い鏍殕椤?
            //    - party.onPokemonChanged(pokemon)闂佹寧绋掑畝鎼佸焵椤掍焦鐨戦柣?PartyStore 闂佸憡鍔曢幊姗€宕曢幘顔藉亹闁煎摜顣介崑鎾存媴缁嬭法绉梺?
            //    - party.sendTo(targetPlayer)闂佹寧绋掔喊宥嗕繆閹间礁鍗抽悗娑櫳戦悡鈧梺鍛婄墪閹虫劕锕㈤垾鎰佹桨闁挎繂鎳庣徊娲⒑椤惵ゅ厡缂侇喗澹嗛埀顒€绠嶉崹娲春濞戞氨鍗氭い鏍ュ€楃粈鍕煛閸艾浜鹃梺绋跨箲婵炲﹪寮鐔衡枖闁逞屽墮椤垽濡烽婊咁槴
            //    - 婵☆偆澧楃换鍌炈囨繝姘櫖婵ê纾粻宄懊归崗鑹板闁汇劌澧介幏鐘诲即閻戝棙鈻肩紓浣割槸缁夌銇愰弻銉﹀殑闁伙絽鏈悾閬嶆煛閸屾繍娼愮痪顓炵埣閺?sendTo/sync/notify
            try {
                Method onChangedM = party.getClass().getMethod("onPokemonChanged", Pokemon.class);
                onChangedM.invoke(party, pokemon);
                PokemonEditorDebug.logEditor("party.onPokemonChanged(pokemon) OK");
            } catch (Throwable t) {
                // 闂佹悶鍎抽崑銈夊焵椤戣棄浜鹃梺鎸庣⊕閼归箖鎯冮崜褎瀚氶柡鍥╁仜瀵版挸霉閻樺磭澧甸柍褜鍓氬銊╂偂閿熺姴妫橀悷娆忓閵嗗﹪鏌ㄥ☉妯煎妞?notifyChanged / update / sync闂?
                try {
                    Method notifyM = party.getClass().getMethod("notifyChanged", Pokemon.class);
                    notifyM.invoke(party, pokemon);
                    PokemonEditorDebug.logEditor("FALLBACK: party.notifyChanged(pokemon) OK");
                } catch (Throwable t2) {
                    PokemonEditorDebug.warnEditor("party.onPokemonChanged failed: {}", t.getMessage());
                }
            }

            // 闂佸憡鐟﹂崹鍧楀焵椤戣法鍔嶆繛鎻掓健瀵剟寮堕崹顔惧帓闁诲骸绠嶉崹娲春濞戙垺鏅?- 闁诲繐绻戠换鍡涙儊椤栨稑绶炴慨姗€纭稿姘舵煕濞嗘ê鐏ラ柛蹇旓耿閹啴宕熼浣哄幍濠电偛顦板ú婊堫敆閻戣姤鏅?
            boolean syncOK = false;
            try {
                // 闂佺绻愰悧鍡涙儍妤ｅ啯鏅?sendTo(ServerPlayer)
                Method sendToM = party.getClass().getMethod("sendTo", ServerPlayer.class);
                sendToM.invoke(party, targetPlayer);
                syncOK = true;
                PokemonEditorDebug.logEditor("party.sendTo(ServerPlayer) OK - update pushed to client");
            } catch (Throwable t) {
                // 闁诲繐绻戠换鍡涙儊?sendTo(Player)
                try {
                    Method sendToM2 = party.getClass().getMethod("sendTo", net.minecraft.world.entity.player.Player.class);
                    sendToM2.invoke(party, targetPlayer);
                    syncOK = true;
                    PokemonEditorDebug.logEditor("party.sendTo(Player) OK");
                } catch (Throwable t2) {
                    // 闁诲繐绻戠换鍡涙儊椤栫偛钃熼柕澶涚畱椤ユ繈鏌熺喊妯轰壕闂佸搫鐗嗛ˇ顖溾偓鍨耿閺?"send"/"sync"/"notify" 闂佹眹鍔岀€氼厼螞閵堝鐭楅柛灞剧⊕閻撴瑩鏌?
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

            // 婵☆偆澧楃换鍌炈囨繝姘Е閻忕偟铏庨崝鍕煥濞戞瑦鐨戞繛鎻掓健楠炴帡濡烽妸褎顔嶉梺?Pokemon.update() 缂佺虎鍙庨崰鏇犳崲濮樿泛绀冮柛娑欐綑閸斻儵鏌ｅΟ鍨厫闁逞屽厸閻掞箓顢楀鍫熸櫢?
            try {
                Method updateM = pokemon.getClass().getMethod("update");
                updateM.invoke(pokemon);
                PokemonEditorDebug.logEditor("pokemon.update() OK");
            } catch (Throwable ignored) {}

            // 婵☆偆澧楃换鍌炈囨繝姘Е閻忕偟铏庨崝鍕煥濞戞瑤浜㈤柣銊у枛閺?MoveSet.update() 缂佺虎鍙庨崰鏇犳崲濮樿泛绠柍褜鍓熼幊妤呮寠婢跺﹦绉梺鍛婄墬閻楃娀鍩€椤掍焦鐨戦柣?
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
        // ============================================================
        // 闂侀潧妫欓崝鏍э耿閸ヮ剙绀夐柨娑樺娴煎倿鏌曢崱妯虹仴闁搞劌缍婂浠嬪Χ閸℃妯勯柣搴ゎ潐绾板秷娼曟繛杈剧到缁夎泛鈻撻幋鐘亾鐟欏嫮绠撶憸鐗堝絻椤繘鏁傞懖鈺冾槱闂佽　鍋撻柛鎾楀嫭娅?缂傚倸鍊归悧鐐垫椤愶箑闂柕濞垮劚閻忊晠姊婚崟鈺佲偓鏇㈠矗閿熺姵鍋ㄦい顓熷笧缁€?
        //
        // 闂佸疇妫勯幊搴ㄥ矗瑜旈弻銊╊敊濞嗘儳鏅柣鐘辫閳ь剙鍟块弸鈧紓浣圭⊕閻楊厾妲愬▎鎰枖閺夌偞澹嗛惌鍡椻槈閺傝法澧㈤柡浣靛€濆畷锝咁吋閸涱垱顔忛梺鍏兼緲婵傛梻绮径鎰強妞ゆ牗绮嶇痪顖炴煙閹帒鍔ラ柣銊у枛閹粙鈥﹂幒鏂挎優闂佹寧绋戦¨鈧紒?
        //   1. party.set(int, Pokemon) 闂?Cobblemon 婵炴垶鎼╅崢鐓庮嚕椤掑嫬鏋侀柟宄扮焾濞煎爼鏌″鍛┛妞ゆ柨娲弫?@NotNull闂?
        //      闂?null 婵炴潙鍚嬬喊宥嗘叏韫囨搩鍤曢柛灞炬皑閸╂鏌ㄥ☉妯侯殭缂佹顦甸幊妤呮偩鐏炵偓娈㈤柣搴ｆ嚀閸燁垰顭囬棃娑栤偓鎺楀川椤栨鏅炲┑鈥冲级閸ㄦ繄绱?
        //   2. party.remove(Pokemon) 婵炴潙鍚嬬喊宥嗕繆閹间礁瑙﹂幖娣妽濡椼劑鏌ｉ妸銉ヮ仼闁汇倕鍊垮畷锝夘敍濞戞帒寰嶉悗鍨閸嬫捇鏌涢幘宕囆ゆ禍娑㈡煥?
        //      闂佸憡锚椤戝洨绱撴径鎰挀闁绘柨澧庣粈濠囨⒒閳ь剟宕归纰卞敽闂佹寧绋戦悧鍛垝?婵炴垶鎼╂禍婊嗐亹婢舵劖鏅?婵炴垶鎼╂禍椋庢濮樿埖鏅悘鐐跺亹閻熸繈鏌￠崟闈涚仭闁搞劍绮嶇粋鎺楊敆閸愮偓鐭楅梺?婵烇絽娲︾换鍕汲閳ь剛绱掑畝鈧崕銈堟綍闂?
        //   3. 闂佸憡鐟禍婵嗭耿?party.setAtPosition(PartyPosition, Pokemon) 闂佽浜介崕杈亹?null 闂佸憡鐟ラ崐褰掑汲閻斿吋鏅?
        //      闂佺儵鏅涢悺銊ф暜閹绢喖绠┑鐘插暙閻﹀綊鎮楃憴鍕暡閾﹀骸霉閿濆懐肖闁汇倕妫涚划姘舵偖鐎靛摜顦繛锝呮处缁诲嫰寮抽埀顒勬⒒閸愵亜鏋嶇紒杈╁仧缁辨帡骞樼€电硶鍋撻鐐存櫢?濠碘€冲级閸ㄦ繄绮径鎰煑婵鐗忕粈?
        //   4. setAtPosition 闂?protected 闂佸搫鍊介～澶屾兜閸洘鏅悘鐐垫櫕缁犳垵顪冮妶鍫濆惞闁轰降鍊濆畷锝咁吋閸涱垱顔忛柣鐘差儏閸熶即寮?
        //
        // 闁诲海鎳撻張顒勫汲閿濆悿瑙勬媴鐞涒剝鐓犻梺?
        //   Client (InventoryScreenMixin / PokemonEditorScreen)
        //      闂?闂佸憡鐟﹂崺濠囧箯?DeletePokemonPacket / ReleasePokemonPacket
        //   Server.handleServerSide()
        //      闂?闁荤姴顑呴崯浼村极閵堝瀚夋い鎺嶇劍閻撴瑩鏌?
        //   deletePokemonFromSlot:
        //      1. 闂佸吋鍎抽崲鑼躲亹閸ヮ剚鍋濋柍杞扮贰閸熲偓闂?PartyStore闂佹寧绋戦悧濠傦耿閸ヮ剙绀夐柨娑樺娴煎倿鏌ｉ妸銉ヮ伃婵¤闄勭€电厧顓奸崪浣告闁荤姷鍋愬Σ鍕?
        //      2. 闂佸湱顭堥ˇ鎷屾綍婵炶揪绲界粔鍫曞磼閵娿儺鍤曢柡鍥ｂ偓宕囶唵闂佸憡鐟﹂悧鏇㈡偪閸岀偛鐭楁い鏍ㄧ▓閸欐椽鏌ㄥ☉妯煎缂佹顦靛浼搭敍濞戞妯嗛柟鍏兼綑缁绘劘銇愰崸妤佹櫖濞撴艾锕︾粈?
        //      3. onPokemonChanged(pokemon) 闂?闂備緡鍋呭銊╂偂?PartyStore 闂佸搫鐗嗛ˇ顖濄亹婢舵劖鏅?
        //      4. setAtPosition(PartyPosition(slot), null) 闂?濠电偞鎸搁幊鎰板煘閺嶎灛锝夊箣閹烘梻孝
        //      5. sendTo(player) 闂?闂佺娉涢敃銉ャ€掗崼鏇炴闁规鍠楅悾閬嶆⒒閸愵亜鏋嶇紒杈╁仱閹晠鎳滅喊妯轰壕濞达絿鎳撻懙褰掓⒑椤愶紕顦﹂柛鈺佺灱閳ь剙绠嶉崹娲春濞戙垺鏅?
        // ============================================================
        ServerPlayer targetPlayer = resolveTargetPlayer(serverPlayer, targetUUID);
        if (targetPlayer == null) {
            PokemonEditorDebug.errorEditor("targetPlayer is null, abort delete");
            return;
        }

        try {
            // 闂佸吋鍎抽崲鑼躲亹?Cobblemon 闂佺绻堥崝宀勬儑?Storage 缂備胶濯寸槐鏇㈠箖婵犲洤闂い顓熷笧缁€澶愭煟閹烘洘纭鹃柟顔筋殜閹炽垹鐣￠弶璺ㄦ偧闂佺粯澹曢弲娑㈩敊瀹ュ鍎嶉柛鏇ㄥ灡琚濋梺?
            // Cobblemon.INSTANCE.getStorage().getParty(player) = PartyStore
            Object storage = Cobblemon.INSTANCE.getStorage();
            Object party = getPartyFromStorage(storage, targetPlayer);
            if (party == null) {
                PokemonEditorDebug.errorEditor("party is null, abort delete");
                return;
            }

            PokemonEditorDebug.logEditor("DELETE - party class={}, slot={}", party.getClass().getName(), slot);

            // 1. 闂佸憡鐟﹂悧鏇㈠吹椤撶倣锝夊箣閹烘梻孝婵炴垶鎼╅崢鎯р枔?Pokemon闂佹寧绋戦悧蹇涘灳濡吋濯奸柕鍫濐槹婵粎绱掑畝鈧亸銊ф?
            // 闂佸疇妫勯幊搴ㄥ矗瑜旈弻銊╊敋閸℃瑧鐛ラ梺?getSlots() 闂佺懓鐡ㄩ悧婊兠洪崸妤€绠抽柕澶樼厛閸熷繘鏌?slots 闁诲孩绋掗〃鍡涱敊瀹€鍕櫖閻忕偞鍎抽惁缁樹繆閸欏鐏婄紓宥呮噽濡叉劙濮€閻樼數鈹涢梺鍏煎劤閸㈣尪銇?
            // party.get(int) 闂佸憡鐟崹鐢稿礂濡粯浜ら柡鍌涘缁€鈧梺姹囧妼鐎氼厼危閹间焦鏅?n 婵炴垶鎼╂禍顏勵焽椤忓棛鐭氶柛婵嗗閺夋椽鏌涘▎妯虹仭闁愁亞鏁婚弫宥囦沪閼测晝鎲归梺鍝勫閸ㄨ京鈧灚绮屽嵄闁瑰瓨甯炵粔瀵哥磼娓氬灝鐏╃紒鎵佹櫊閺?
            // 婵炴挻鑹鹃鍛淬€呰閺佸秴顫濋澶嬫悙闂?slot=0 缂備礁鐬肩亸銊ф濠曠ot=1 闂佸搫鐗嗛ˇ鍗炐掓繝鍥х闁炽儴灏欓悷?
            //      party.get(0) 闂佸憡鐟崹鐢稿礂濡粯浜ら柡鍌涘缁€鈧梺娲荤厛閸犳牕鐣锋潏銊р枖婵鐗忕粈鍕磼濡ゅ﹤鍘哥紒鏂跨摠缁嬪顢旈崼鐔奉潯缂備礁鐬肩亸銊ф濮樿埖鏅€光偓閸愶絽浜鹃悘鐐跺亹閻熸繈鏌?null
            Object pokemon = null;
            try {
                // 婵炴潙鍚嬮敋闁告ɑ绋撴禍绋库攽婵犲嫭笑 getSlots() 闂佸搫鍊介～澶屾兜閸洘鏅柛锔芥▌ientParty 闂佸搫鐗嗛ˇ宕囨崲閺嶃劎鈻旀い蹇撴噺閻撴瑦绻涙径瀣┛缂佽鲸绱攍ayerPartyStore 婵炴垶姊婚崰搴ゃ亹閺屻儲鍤勯柤鎭掑劜缁犳帡鏌?
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

            // getSlots() 婵犮垺鍎肩划鍓ф喆閿曞倸绀嗘繛鍡楃箲缁€鈧梻渚囧亐閸嬫捇鏌?slots 闁诲孩绋掗〃鍡涱敊瀹€鍕櫖闁割偁鍨虹痪顖炴煙閹帒鍔ユい鏃€鐗犻弫?PartyStore 闂佹眹鍔岀€氼噣锝為崱娑樺珘濠㈣泛锕﹂幗鐔哥箾閸埄鏆滅紒?
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

            // 闂佸搫鐗冮崑鎾绘煕濮橆剟顎楁繛鍙夌墵閺屽懘鍩€椤掑嫭鏅?get(int) 闂佸搫鍊介～澶屾兜閸洘鏅柛顐ｇ箓鐠佹煡鏌ら崘娴嬪亾閻旇櫣顔夐梺鎼炲劤閸嬬偟鈧灚绮嶉幆鏃囩疀閹惧啿鈻忛梺姹囧妼鐎氼剟鎮块崒鐐茬煑妞ゆ牗绋堥崣娲煥濞戞ê顨欑紒妤€顦靛浼搭敍濞戞妯嗗┑鈥冲级閸ㄦ繄绱為崨瀛樻櫢?
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

            // 2. 闂?setAtPosition(PartyPosition(slot), null) 濠电偞鎸搁幊鎰板煘閺嶎灛锝夊箣閹烘梻孝
            // 闂佸疇妫勯幊宥囩箔婢舵劖鍤勯柣锝呮湰閺?party.remove(pokemon)闂佹寧绋愬鎺旀娴兼潙绠┑鐘插€归崐鐢告⒒閸稑鐏╅柣銈呭€垮畷锝夘敍濞戞帒寰嶉悗鍨閸嬫捇鏌涢幘宕囆ゆ禍娑㈡煥濞戞瀚伴悽顖氭川缁辨捇鍩￠崨顔ф洖霉閻撳海效闁哄棝浜堕弫?
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
                // 闂佹悶鍎抽崑銈夊焵椤戣棄浜鹃梺鎸庣⊕椤掔寴rty.remove (婵炴潙鍚嬮懝鍓ф暜閸モ晝纾介柍杞扮閻忔瑩鎮跺☉杈╁帨缂佽鲸绻冮幏鍛村幢濮橆剚顔曢柣蹇撶箲閸ㄥジ宕楀Ο鎭掆偓鎺楀川椤旇偐顏?
                try {
                    Method removeM = party.getClass().getMethod("remove", Pokemon.class);
                    removeM.invoke(party, pokemon);
                    PokemonEditorDebug.logEditor("FALLBACK: party.remove(pokemon) OK");
                } catch (Throwable t2) {
                    PokemonEditorDebug.errorEditor("FALLBACK also failed: {}", t2.toString());
                }
            }

            // 3. 闂佸憡鑹鹃張顒勵敆閻愮儤鈷撻柣鏂垮缁€濠囨煟濡灝鐓愰柍褜鍏涚粈渚€宕哄畝鈧埀顒€绠嶉崹娲春濞戙垺鏅?
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

    // ==================== Move 闂佸搫琚崕鍙夌珶?====================
    private static MoveTemplate lookupMoveTemplate(String name) {
        if (name == null || name.isEmpty()) {
            PokemonEditorDebug.warnMoves("[MoveLookup] input is null/empty, return null");
            return null;
        }
        PokemonEditorDebug.infoMoves("[MoveLookup] input='{}'", name);
        // 濠电偞鎸搁幊鎰板箖婵犲啯缍囬柟鎯у暱瀵娊鏌ㄥ☉娆掑妤犵偞鎹囬獮鎺撳緞瀹€鈧崐鎶芥偣閸ャ劌鐏撮柡渚囧弮瀹曟粌顓奸崶鈺冿紱闂佹寧绋戦悧鍡涖€?"cobblemon.move.tackle" 闂?"tackle"闂?
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

        // 闂?婵炴潙鍚嬮敋闁? Moves.getByName(String) - Cobblemon 1.21 闁诲氦顫夎摫闁哄瞼鍠栭獮搴ㄥ焵椤掑嫭鍤勯柤鎭掑劤閸欌偓闂?API闂佹寧绋戦悧濠傘€掗崼鏇炵煑妞ゆ牜鍋炲ù鍡涙煥?
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

        // 闂佸搫鍊介～澶屾兜?1: MoveTemplate.Companion 闂傚倸鐗婇悷锕傚焵椤戣法鍔嶉柣鎿勭節楠炲秹鎮欓浣哄幍濠电偛顦板ú鈺冩濡惧巸Identifier/valueOf/of 缂備焦绋戦¨鈧紒?
        if (result == null) {
            try {
                Object companion = MoveTemplate.class.getField("Companion").get(null);
            // 闂佺绻愰悧鍡涙儍閸撗勫珰闁哄洨濮风紙杈╃棯椤撗冩灈閻忓浚鍣ｉ弫?
            for (Method m : companion.getClass().getMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class
                        && !m.getName().equals("toString")) {
                    try {
                        Object r = m.invoke(companion, lowerName);
                        if (r instanceof MoveTemplate) { result = (MoveTemplate) r; break; }
                    } catch (Throwable ignored) {}
                }
            }
            // 闁诲繐绻戠换鍡涙儊椤栨冻绱旈柡宓懏鎲诲┑顕嗙到缁夋挳濡甸崶顒€绀冩繛鍡楃箰閻栭亶姊洪弶璺ㄢ枌缂佽鲸鐟╅弫?"Tackle"闂?
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
        }  // 闂傚倸鍋嗛崢濂稿箖?if (result == null) {

        // 闂佸搫鍊介～澶屾兜?2: 婵炴垶姊婚崰鏇㈡儊椤栨稓鈻旈柍褜鍓熼弫?Move.Companion
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

        // 闂佸搫鍊介～澶屾兜?3: 闂備緡鍓欑粔鏉戭啅婵犳艾绠ラ柍褜鍓熼弫?MoveTemplate闂佹寧绋戦惌鍌炲焵椤掍椒浜㈢紒?name 闁诲孩绋掗〃鍡涱敊瀹€鈧崚鎺楀礈瑜夐埀顒侊耿瀹曠姾銇愰幒鎴濊祴闂佹寧绋戦悧濠傘€掗崼鏇炵煑妞ゆ牜鍋炲ù鍡涙煟閵娿儱顏╂繛鍙夌墵閺屽懘鍩€椤掑嫬妫橀柣鐔稿绾偓闂?
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

        // 闂佸搫鍊介～澶屾兜?4: 闂?Cobblemon 缂備緡鍋夊畷闈涒枔閹达附顥堟繛鍡樺姀閸嬫挻鎷呴崫銉︽喕濠电偛鐗嗛崥鈧柤鍨灴楠炲秹骞橀崨顖滎槱闂?MovesRegistry闂侀潧妫旀惔娣ES 缂備焦绋戦¨鈧紒?
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

    // 闁哄鐗嗛幊搴㈡叏椤忓牆妫橀悷娆忓閵嗗﹪鏌ㄥ☉娆庝孩妤犵偛绻樺畷锝夊冀閵娿儺鏆￠梺?MoveTemplate 闁诲海鏁搁、濠囨寘閸曨垱鏅柛顐ｇ箓閺佸爼姊洪鍝勫闁轰降鍊濋弫宥囦沪閼测晝鎲归梺鍝勬閹稿啿锕㈤崜浣虹＜婵炲棗娴氬Ο鍡涙⒑椤曞懐鍘滅紒?
    private static List<Object> getAllMoveTemplateObjects() {
        // 婵犮垼娉涚粔鍫曞极?getAllMoveIds 闂佹眹鍔岀€氱増绂嶉崼鏇炲偍闁糕剝绋堥崑鎾诲及韫囨洖绔奸梺鎸庣☉婵傛梻寰婇悙顒佷氦闁哄倹瀵х粈鈧柣搴ｆ暩椤㈠﹪鎸呴崟顖涘殌鐎光偓閸曨剙顫￠梺鍛婅壘缁夋儼鈪?
        List<Object> result = new ArrayList<>();
        try {
            // 闂佸搫鍊介～澶屾兜?1: 闂?MoveTemplate.Companion 闂佺懓鐏氬畷妯兼崲閹达附鏅?Iterable 闂佹眹鍔岀€氼參寮婚悢鍏兼櫢?
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
            // 闂佸搫鍊介～澶屾兜?2: 闂?Cobblemon 缂備緡鍋夊畷闈涒枔閹达附顥堟繛鍡樺姀閸嬫挻鎷呴崫銉︽喕濠电偛鐗嗛悘姘珶?
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

    // ========== 闂佸疇妫勯幊蹇涙偋鐎圭姷鐤€闁告劘娉曠粣妤呮煟閳轰胶鎽犻悽顖涙尦瀹曪絽顓奸崨顖涱啅闂佺懓鐏濈粔宕囩礊?MoveSet 闂佸憡鍔曢幊姗€宕?moves 闂佽桨鐒︽竟鍡欏垝瀹ュ鏅?
    // 闂佹寧绋戦悧蹇曞垝椤愶附鏅?add() 闂佹眹鍔岀€氼剟鐛幘缁樼厒鐎广儱妫楣冩煛鐏炶鍔橀柍褜鍏涙径鐒憈Move 闂?observable 闁荤姳闄嶉崹钘壩ｉ崟顓犻┏濠㈣埖鍔﹂弶鍝勵熆閼哥數澧甸柍褜鍓氬Σ鎺旀椤愶附鏅?
    // Move 闂佸搫顑呯€氫即鍩€椤掑倸顎撶紒杈╂ove(MoveTemplate, int currentPp, int raisedPpStages)
    // =====================================================================
    private static Field moveSetMovesField = null;  // MoveSet 闂?moves 闂佽桨鐒︽竟鍡欏垝?闂佸憡甯楅〃澶愬Υ閸愵亖鍋撳☉娆樻畷妞?
    private static boolean moveSetFieldIsList = false;  // 闁诲孩绋掗〃鍡涱敊瀹€鍕櫢?List 闁哄鏅滆摫婵″弶鎮傚顐﹀箥椤斿墽鐓?
    private static boolean moveSetFieldResolved = false;

    // ========== Move 缂備緡鍋夊畷鐢靛垝閵娾晛鍑犻柛鏇ㄥ墮鐠у瓨绻涢弶鎴劀缂佽鲸鐟╅弫?DEBUG 闂佸搫鍟抽崺鏍ㄦ櫠閻ｅ本鍋樼€光偓鐎ｎ剛顦?=========
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

    // ========== 婵°倗濮撮惌渚€鎯?Move 闁诲骸婀遍崑妯兼閵夆晜鍋愰柤鍝ヮ暯閸嬫挻绗熸繝鍕槱闂?DEBUG 闂佸搫鍟抽崺鏍ㄦ櫠閻ｅ本鍋樼€光偓鐎ｎ剛顦?=========
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
            // 闂佽浜介崹铏圭矈?MoveSet 缂備緡鍋夊畷闈涒枔閹寸姭鍋撻悷鐗堟拱闁哄棴绲跨槐鎺楀箻鐎电硶鍋撻鐐存櫖闁割偅绻傜紞鈧?DEBUG_MOVES/DEBUG_EDITOR 闂佺鐭囬崘銊у幀闂?
            try {
                PokemonEditorDebug.infoMoves("[MoveSetProbe] === probing MoveSet class: {} ===",
                        moveSetObj.getClass().getName());
                // 闂佺懓鐏氶幐绋跨暤閸愵喖绠ラ柍褜鍓熷鍨緞婵犲嫭鎲婚梺?
                for (Field f : moveSetObj.getClass().getDeclaredFields()) {
                    f.setAccessible(true);
                    Object val = null;
                    try { val = f.get(moveSetObj); } catch (Throwable ignored) {}
                    PokemonEditorDebug.infoMoves("[MoveSetProbe]   field {}: {} = {}",
                            f.getName(), f.getType().getSimpleName(),
                            (val != null ? val.toString() : "null"));
                }
                // 闂佺懓鐏氶幐绋跨暤閸愵喖绠ラ柍褜鍓熷鍨緞婵犲倸绲鹃梺绋跨箺妞存悂寮婚悢灏佹灃闁哄浂浜炵粈鍕磼閻橆偄浜鹃柣鐔告磻缁躲倗妲?
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

            // 婵炴挻纰嶇换鍐敃閼测晙鐒婃繝闈涳功濡叉悂鎮介娑氭憙妞ゎ偄妫涢埀顒佺⊕椤ㄥ棝顢欏畝鍕櫢?
            String[] candidates = {"moves", "_moves", "moveList", "movesList", "learnedMoves", "skillMoves"};
            for (String candidate : candidates) {
                try {
                    Field f = moveSetObj.getClass().getDeclaredField(candidate);
                    f.setAccessible(true);
                    Class<?> type = f.getType();
                    // 闂佽　鍋撴い鏍ㄧ☉閻︻噣鏌ㄥ☉娆戞惒ove[]闂侀潧妫旀穱鐥爏t<Move>闂侀潧妫旂粩绱€llection<Move>
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

    // 闂佺儵鏅涢悺銊ф暜閹绢喗鏅?MoveSet.moves 闁诲孩绋掗〃鍡涱敊瀹€鈧幏鐘垫嫚閹绘帞鎮奸梺鎸庣☉閻楀﹪寮鈧獮鎰媴鐞涒剝鏅炲┑鈥冲级閸ㄦ繄绱為崨瀛樻櫖鐎光偓閳ь剛鑺辩捄銊﹀厹妞ゆ棁宕电粔瀵哥磽閸愭儳娅欑紒杈ㄧ箞瀵劑顢涘☉妯兼Х闂佽桨鐒︽竟鍡欏垝?List闂?
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
                        probeMoveClassIfNeeded(move); // 闂?闂佽浜介崹铏圭矈?Move 缂備緡鍋夊畷鐢靛垝閵娾晜鏅?
                        MoveTemplate tpl = move.getTemplate();
                        String nm = (tpl != null) ? tpl.getName() : move.getName();
                        outNames.set(i, nm == null ? "" : nm);
                        outPPs[i] = move.getCurrentPp();
                        outMaxPPs[i] = (tpl != null ? tpl.getPp() : move.getMaxPp());
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
                        probeMoveClassIfNeeded(move); // 闂?闂佽浜介崹铏圭矈?Move 缂備緡鍋夊畷鐢靛垝閵娾晜鏅?
                        MoveTemplate tpl = move.getTemplate();
                        String nm = (tpl != null) ? tpl.getName() : move.getName();
                        outNames.set(i, nm == null ? "" : nm);
                        outPPs[i] = move.getCurrentPp();
                        outMaxPPs[i] = (tpl != null ? tpl.getPp() : move.getMaxPp());
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

    // 闂佸搫顑呯€氫即鍩€椤掑倸小缂佹柨鐡ㄧ粙澶愵敂閸℃浠剧紓浣规煥椤戝洤鈻?Move 闁诲骸婀遍崑妯兼閵夆晜鏅柛顐ゅ枔濡层劍绻涢崰掳鍊楀▓鍫曟煟椤剙濮傞柛蹇旓耿瀵即顢涘☉娆戝帎闁诲海鏁搁、濠囨寘閸曨垱鏅€光偓閸曨亞绱氶梺绋跨箰缁夋挳宕ｉ埄鍐洸妞ゆ劑鍨荤粚鍧楁煟椤剙濡兼い鏇憾閹虫盯寮堕幋婵堫唽闂備焦褰冪粙鍕?
    private static Move constructMove(MoveTemplate tpl, int finalPP, int raised) {
        if (tpl == null) return null;
        try {
            java.lang.reflect.Constructor<?> ctor = null;
            for (java.lang.reflect.Constructor<?> c : Move.class.getDeclaredConstructors()) {
                Class<?>[] pts = c.getParameterTypes();
                if (pts.length == 3 && pts[1] == int.class && pts[2] == int.class) {
                    ctor = c; break;
                }
            }
            if (ctor != null) {
                return (Move) ctor.newInstance(tpl, finalPP, raised);
            }
            // 闂佹悶鍎抽崑銈夊焵椤戣棄浜鹃梺鎸庣⊕閻＄捇eate 闂佸搫鍊介～澶屾兜?
            Object companion = Move.class.getField("Companion").get(null);
            for (Method m : companion.getClass().getMethods()) {
                if (m.getName().equals("create") && m.getParameterCount() >= 2) {
                    try { return (Move) m.invoke(companion, tpl, finalPP, raised); }
                    catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            PokemonEditorDebug.warnMoves("[MoveWrite] constructMove failed: {}", t.getMessage());
        }
        return null;
    }

    // 闂佸疇妫勯幊鎰垝閻戞鈻旈柍褜鍓熼弫?PP / raised 闁荤姳绶ょ槐鏇㈡偩婵犳碍鏅柛锔芥▓bblemon 闂佸搫绉村ú銈夊闯椤栫偛绀傛い鎺嗗亾缂佹唻绻濋弫宥咁潰閻㈩暈Pp = floor(basePP * (1 + 0.2 * raised)))
    // 闂佸憡鐟ョ粔鐢垫暜?raised闂佹寧绋掗惌顔惧垝?userMaxPP / movePP 闁荤姳绶ょ槐鏇㈡偩婵犳艾瑙﹂柛顐犲灪閸婄偤鏌ｉ妸銉ヮ伃婵☆垯绀侀埢鎾诲垂椤愶絾顔?
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
        // 闂?userMaxPP 闂佸憡鐟ョ粔鐢垫暜?raised 闂傚倸鍟抽崺鏍敊瀹€鍕瀬閻㈩垼鍠氱粈鍕⒒閸愵厼鐓愭い鏂跨焸閺?0-3闂?
        // 闂佺绻戞繛濠囧极椤撱垺鏅慨姗嗗墮濞懷囨煛閸繍妲哥紓?userMaxPP 缂備緡鍠栨晶浠嬪灳濮椻偓瀹曠姾銇愰幒鎴濊祴闂佸搫鏈崝鎺楁煂?floor(basePP*(1+0.2*r)) 闂佸搫鍟抽鎰濠靛绠ョ€广儱鐗滈崥鍥р槈閹捐泛鍔嬫繝鈧?PP 闂佸湱绮崝鏇炵暦?
        // 闂佸憡鐔粻鎴﹀垂椤栨侗娓舵俊顖涱儥閸?raised=0闂佹寧绋戦悧濠偽涢妶澶婄闁归偊鍓欑壕鎶芥煥濞戞﹩妾х紒杈ㄧ哗axPP=basePP
        int raised = 0;
        if (basePP > 0 && userMaxPP > basePP) {
            // 闂備緡鍓欑粔鏉戭啅婵犳艾绠ラ柍褜鍓熷鍨緞婵犲倽顔夐梺鐓庣－閺咁偄鈻?raised 闂佺锕﹂妴瀣?-3闂佹寧绋戦¨鈧紒杈ㄧ箞楠炲秹骞嗚閻撳倻绱掗鐓庘挃闁炽儲锕㈠畷鐘恒亹閹烘垵璧嬮梺?
            int bestRaised = 0;
            for (int r = 1; r <= 3; r++) {
                int expectedMax = (int) Math.floor(basePP * (1 + 0.2 * r));
                // 缂備緡鍠栨晶浠嬪灳濮椻偓瀹曠姾銇愰幒鎴濊祴闂佺懓鐡ㄩ悧鏇炍熸繝鍌ゅ殨闁稿本顨呴埛?闂佹寧绋戦悧鎰板焵椤掆偓閸熷啿顬婄拠瑁佸綊顢欓幆褌鍖栭柣鐘叉祩閸ㄦ澘螣婵犲洦鏅?
                if (expectedMax == userMaxPP) {
                    bestRaised = r;
                }
            }
            raised = bestRaised;
        }
        int maxPP = (raised > 0) ? (int) Math.floor(basePP * (1 + 0.2 * raised)) : basePP;
        int finalPP = (userPP > 0) ? Math.min(userPP, maxPP) : maxPP;
        return new MovePPInfo(raised, finalPP, maxPP);
    }

    // 闂佺儵鏅涢悺銊ф暜鐎靛憡濯奸柛鎾楀懏鐎?MoveSet.moves 闂佽桨鐒︽竟鍡欏垝瀹ュ绀傞柛鎰典簽椤﹂亶鏌ㄥ☉妯煎ⅱ缂侇噮鍋婇弫?add() 闂佸憡锚椤兘宕抽幖浣规櫖閻忕偟鍋撻弳婊堟煙闂€鎰厡婵炴潙顦靛畷銉т沪閹灛妤呮煠鐎圭姵顥夐柛鈺佹湰缁嬪顓奸崨顔尖偓杈ㄤ繆閸欏鐏婄紓宥呮嚇閺?
    // 闂佺绻戞繛濠囧极椤撱垺鏅慨婵堟櫆ves 闂?final Move[]闂佹寧绋戞總鏃傜箔婢舵劖鍤勯柤鎭掑劜濞堣霉閿濆棙灏版繛瀛樼洴楠炴垿濮€閻樺灚鎲诲┑鐐茬墢椤ｎ喚妲愬┑鍥ㄥ闁糕剝顨呯拋鎻捗归悩顔煎姍闁逛究鍔戝銊ф喆閸曨剚顔嶇紓鍌欑鐎氼剟宕㈤幘缁樻櫢?
    // setMove(int, Move) 闂佺娴氶崜娆撳矗?API 婵炴垶姊婚崰鎰ｉ幖浣瑰剮閻庢稒锚婢跺秹鏌涢幇顓犳噭闁哄棛鍠撶槐鎺楀礋椤愩垹绗氱紓浣戒含婵妲愬┑瀣睄闁绘ɑ褰冪粻鐢告煥?
    @SuppressWarnings("unchecked")
    private static boolean writeMovesToInternalArray(Object moveSetObj,
                                                     String[] moveNames, int[] movePPs, int[] moveMaxPPs) {
        // 闂?缂備胶濮崑鎾绘煕閺嶃劎澧柟顔筋殜閺佸秴顫濇潏銊у嚱闂佽浜介崕鎶藉极?setMove(int, Move) 闂佺娴氶崜娆撳矗?API 闂佸憡鍔栭悷銉╁矗閸℃稒鏅悘鐐舵缁犵敻姊洪幓鎺斝℃慨姗堝缁辨帒鈻庤箛锝呮倕闂佽鎸抽ˉ鎾活敂椤掍礁绶為柛鏇ㄥ幗閸?
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
                    PokemonEditorDebug.infoMoves("[MoveWrite] slot {}: setMove('{}') pp={}/{} raised={}",
                            i, name, ppInfo.finalPP, ppInfo.maxPP, ppInfo.raised);
                    setMoveM.invoke(moveSetObj, i, newMove);
                }
                return true;
            }
        } catch (Throwable t) {
            PokemonEditorDebug.errorMoves("[MoveWrite] setMove path failed: {}", t.getMessage());
        }

        // ====== 闂佹悶鍎抽崑銈夊焵椤戣棄浜鹃梺鎸庣⊕濮樸劌煤閸ф绠抽柕澶堝劚閸愨偓闁诲繐绻愮€氼亪骞堥妸鈺傛櫢?moves 闁诲孩绋掗〃鍡涱敊瀹€鍕剭闁告洦鍋勭敮妤呮煥?======
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

    // ========== 闂佸吋鍎抽崲鑼躲亹閸ヮ剙绠ラ柍褜鍓熷鍨箙瀹告悧eTemplate闂佹寧绋戦悧蹇涘极閵堝棛顩查幖绮规缂嶆牠鏌涜箛鎾村櫤閻庢碍鍨甸锝堢疀鎼达絿鎲梺鐟板槻椤︻垶宕归鍡樺仒妞ゎ厽甯炵粈?=========
    private static List<Object> allMoveTemplatesCache = null;
    public static List<String> getAllMoveIds() {
        List<String> result = new ArrayList<>();
        try {
            // 闂佸搫鍊介～澶屾兜?1: 闂?MoveTemplate.Companion 闂佺懓鐏氬畷妯兼崲閹达附鏅?Collection<MoveTemplate> 闂?List<MoveTemplate> 闂佹眹鍔岀€氼參寮婚悢鍏兼櫢?
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

        // 闂佸搫鍊介～澶屾兜?2: 闂?Cobblemon 缂備緡鍋夊畷闈涒枔閹达附顥堟繛鍡樺姀閸嬫挻鎷呴崫銉︽喕濠电偛鐗嗛悘姘珶濡　鏋栭柕濞垮劚閺傗偓闂?
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

        // also try Moves class directly
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

        // ==================== 闂佺粯銇涢弲婵嬶綖?閻熸粓鍋婂渚€骞?====================
        if (pkt.speciesName() != null && !pkt.speciesName().isEmpty()) {
            try {
                Object oldSpecies = pokemon.getSpecies();
                pokemon.setSpecies((com.cobblemon.mod.common.pokemon.Species) lookupSpeciesObj(pkt.speciesName()));
                PokemonEditorDebug.logEditor("species: {} -> {}", oldSpecies, pkt.speciesName());
            } catch (Throwable e) {
                PokemonEditorDebug.warnEditor("setSpecies failed: {}", e.getMessage());
            }
            if (pkt.formName() != null && !pkt.formName().isEmpty()) {
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
        }

        // ==================== 缂備焦绋戦ˇ杈殽?====================
        try {
            int oldLevel = pokemon.getLevel();
            pokemon.setLevel(pkt.level());
            PokemonEditorDebug.logEditor("level: {} -> {}", oldLevel, pkt.level());
        } catch (Throwable e) {
            PokemonEditorDebug.warnEditor("setLevel failed: {}", e.getMessage());
        }

        // ==================== 缂傚倷绀佺换姗€鎮板▎鎾存櫢?====================
        try {
            int expToSet;
            if (pkt.experience() <= 0) {
                // 闂佹椿娼块崝宥夊春濞戙垹瀚夋い蹇撴噹閻﹀綊鎮楃憴鍕殤缂侇喒鍓濋〃銉ョ暆鐎ｎ剛顦╂慨鎺撶⊕椤牓顢?0闂佹寧绋戞總鏃傜箔婢舵劕绀傚ù锝囩摂閸熷懎鈽夐幘瑙勩€冪紒顭掔節瀵即宕滆娴?
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

        // ==================== 婵炲瓨绮嶈ぐ鍐儊閹达附鏅?====================
        try {
            int oldFriendship = pokemon.getFriendship();
            pokemon.setFriendship(Math.max(0, Math.min(255, pkt.friendship())), true);
            PokemonEditorDebug.logEditor("friendship: {} -> {}", oldFriendship, pkt.friendship());
        } catch (Throwable e) {
            PokemonEditorDebug.warnEditor("setFriendship failed: {}", e.getMessage());
        }

        // ==================== 闂傚倸鍋嗘禍婊堝储?====================
        try {
            boolean oldShiny = pokemon.getShiny();
            pokemon.setShiny(pkt.shiny());
            PokemonEditorDebug.logEditor("shiny: {} -> {}", oldShiny, pkt.shiny());
        } catch (Throwable ignored) {}

        // ==================== Nickname ====================
        try {
            if (pkt.displayName() == null || pkt.displayName().isEmpty()) {
                pokemon.setNickname(null);
                PokemonEditorDebug.logEditor("nickname: cleared");
            } else {
                pokemon.setNickname(net.minecraft.network.chat.Component.literal(pkt.displayName()));
                PokemonEditorDebug.logEditor("nickname: {}", pkt.displayName());
            }
        } catch (Throwable ignored) {}

        // ==================== UUID ====================
        String uuidStr = pkt.pokemonUUID();
        if (uuidStr != null && !uuidStr.isEmpty()) {
            try {
                UUID newUuid = UUID.fromString(uuidStr);
                pokemon.setUuid(newUuid);
                PokemonEditorDebug.logEditor("uuid: {}", uuidStr);
            } catch (Throwable ignored) {}
        }

        // ==================== 闂佽鍎搁崘銊у姸 ====================
        try {
            Object genderEnum = resolveGender(pkt.gender());
            if (genderEnum != null) {
                pokemon.setGender((com.cobblemon.mod.common.pokemon.Gender) genderEnum);
                PokemonEditorDebug.logEditor("gender: {}", pkt.gender());
            }
        } catch (Throwable ignored) {}

        // ==================== 闂佽鍎搁崟顓烆棔 ====================
        if (pkt.nature() != null && !pkt.nature().isEmpty()) {
            Object natureObj = lookupRegistryObj(pkt.nature());
            if (natureObj != null) {
                try {
                    pokemon.setNature((com.cobblemon.mod.common.pokemon.Nature) natureObj);
                    PokemonEditorDebug.logEditor("nature: {}", pkt.nature());
                } catch (Throwable ignored) {}
            }
        }

        // ==================== 闂佺粯顨堝▍銏ゅ箯?====================
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

        // ==================== Held Item ====================
        if (pkt.heldItem() != null && !pkt.heldItem().isEmpty()) {
            Item item = findItem(pkt.heldItem());
            if (item != null) {
                ItemStack stack = new ItemStack(item);
                try {
                    pokemon.setHeldItem$common(stack);
                    PokemonEditorDebug.logEditor("heldItem: {}", pkt.heldItem());
                } catch (Throwable ignored) {}
            }
        }

        // ==================== Status ====================
        try {
            if (pkt.status() != null && !pkt.status().isEmpty()) {
                Object statusObj = lookupStatusObj(pkt.status());
                if (statusObj != null) {
                    com.cobblemon.mod.common.pokemon.status.PersistentStatus realStatus =
                            (com.cobblemon.mod.common.pokemon.status.PersistentStatus) statusObj;
                    pokemon.applyStatus(realStatus);
                    PokemonEditorDebug.logEditor("status: applied '{}' via applyStatus", pkt.status());
                } else {
                    PokemonEditorDebug.warnEditor("status: status object is null for '{}', skipping", pkt.status());
                }
            } else {
                pokemon.setStatus(null);
                PokemonEditorDebug.logEditor("status: cleared via setStatus(null)");
            }
        } catch (Throwable t) {
            PokemonEditorDebug.warnEditor("status: error during status operation: {}", t.getMessage());
        }

        // ==================== IVs / EVs ====================
        try {
            Object ivsObj = pokemon.getIvs();
            if (ivsObj != null) {
                writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivHp())), "HP", "hp", "Hp");
                writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivAtk())), "ATK", "attack", "atk", "Atk");
                writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivDef())), "DEF", "defense", "def", "Def");
                writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivSpA())), "SPA", "specialAttack", "spAttack", "spa", "SpA");
                writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivSpD())), "SPD", "specialDefense", "spDefense", "spd", "SpD");
                writeStat(pokemon, ivsObj, Math.max(0, Math.min(31, pkt.ivSpe())), "SPE", "speed", "spe", "Spe");
                PokemonEditorDebug.logEditor("IVs: HP={} ATK={} DEF={} SPA={} SPD={} SPE={}",
                        pkt.ivHp(), pkt.ivAtk(), pkt.ivDef(), pkt.ivSpA(), pkt.ivSpD(), pkt.ivSpe());
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
                PokemonEditorDebug.logEditor("EVs: HP={} ATK={} DEF={} SPA={} SPD={} SPE={} (cap={})",
                        evHp, evAtk, evDef, evSpA, evSpD, evSpe, evCap);
            }
        } catch (Throwable e) {
            PokemonEditorDebug.warnEditor("IV/EV modify failed: {}", e.getMessage());
        }

        // ==================== MoveSet 闂佸憡鍔栭悷銉╁矗?====================
        try {
            Object moveSetObj = callNoArgs(pokemon, "getMoveSet", "moveSet");
            if (moveSetObj != null) {
                String[] moveNames = {pkt.move1(), pkt.move2(), pkt.move3(), pkt.move4()};
                int[] movePPs = {pkt.move1PP(), pkt.move2PP(), pkt.move3PP(), pkt.move4PP()};
                int[] moveMaxPPs = {pkt.move1MaxPP(), pkt.move2MaxPP(), pkt.move3MaxPP(), pkt.move4MaxPP()};
                PokemonEditorDebug.infoMoves("[MoveWrite] writing: slot0='{}' slot1='{}' slot2='{}' slot3='{}'",
                        moveNames[0], moveNames[1], moveNames[2], moveNames[3]);

                // 闂佸憡锚椤兘宕崇粙娆惧晠闁肩⒈鍓氶弳鐘绘倵楠炲灝鐏柛鈺傜〒缁晠顢涘鍏夹熼梺鑽ゅ仜濡妲愬┑瀣剮閻庢稒锚婢跺秹鏌涢幇顓犳噧闁?
                boolean ok = writeMovesToInternalArray(moveSetObj, moveNames, movePPs, moveMaxPPs);
                if (!ok) {
                    // 闂佹悶鍎抽崑銈夊焵椤戣棄浜鹃梺鎸庣⊕椤掔挱tMove(int, Move) 闂佺娴氶崜娆撳矗?API
                    Method setMoveM = null;
                    for (Method m : moveSetObj.getClass().getMethods()) {
                        if (m.getName().equals("setMove") && m.getParameterCount() == 2
                                && m.getParameterTypes()[0] == int.class) {
                            setMoveM = m; break;
                        }
                    }
                    if (setMoveM != null) {
                        for (int i = 0; i < 4; i++) {
                            try { setMoveM.invoke(moveSetObj, i, null); } catch (Throwable ignored) {}
                            String moveName = moveNames[i];
                            if (moveName == null || moveName.isEmpty()) continue;
                            MoveTemplate tpl = lookupMoveTemplate(moveName);
                            if (tpl == null) continue;
                            MovePPInfo ppInfo = calculatePP(tpl, movePPs[i], moveMaxPPs[i]);
                            try { setMoveM.invoke(moveSetObj, i, new Move(tpl, ppInfo.finalPP, ppInfo.raised)); }
                            catch (Throwable ignored) {}
                        }
                    }
                }
            }
        } catch (Throwable t) {
            PokemonEditorDebug.errorMoves("[MoveWrite] error: {}", t.getMessage());
        }

        // HP 闂佸搫鐗冮崑鎾绘煕濮橆剚鍞夋い鏃€娲滅槐鏃堫敋閸℃瑧顦╂慨鎺撶⊕椤牓顢?= 闂佸搫鐗冮崑鎾愁熆妫版繃绠濸闂?
        try {
            int currentMaxHp = pokemon.getMaxHealth();
            int hp = pkt.currentHp();
            if (hp <= 0) {
                hp = currentMaxHp;  // 闂佹椿娼块崝宥夊春濞戙垹瀚夋い蹇撴噹閻﹀綊鏌?HP闂佹寧绋戦惌鍌滃垝椤栨粍濯奸柕鍫濇婵剟鎮跺☉鎺戜壕
            }
            hp = Math.max(0, Math.min(currentMaxHp, hp));
            pokemon.setCurrentHealth(hp);
            PokemonEditorDebug.logEditor("currentHp: {} (max={})", hp, currentMaxHp);
        } catch (Throwable ignored) {}

        PokemonEditorDebug.logEditor("writePokemonFields DONE");
    }

    // 闂佹悶鍎抽崑銈夊焵椤戣棄浜鹃梺鍝勫€介～澶愩€? 闁诲孩绋掗〃鍡涱敊瀹€鍕櫢?API 闁荤姴顑呴崯浼村极閵堝鏅柛顐ｇ矌缁犱粙鏌ｉ敐鍡欐噥缂傚秵姊圭粙澶愬传閸曞灚校闂備緡鍋勯¨鈧紒?
    private static void writePokemonFieldsLegacy(Pokemon pokemon, PokemonEditorNetwork.UpdatePokemonPacket pkt) {

        // ==================== 闂佺粯銇涢弲婵嬶綖?閻熸粓鍋婂渚€骞?====================
        if (pkt.speciesName() != null && !pkt.speciesName().isEmpty()) {
            try {
                pokemon.setSpecies((com.cobblemon.mod.common.pokemon.Species) lookupSpeciesObj(pkt.speciesName()));
            } catch (Throwable e) {
                PokemonEditorDebug.log("setSpecies 婵犮垺鍎肩划鍓ф喆? " + e.getMessage());
            }
        }

        // ==================== 缂備焦绋戦ˇ杈殽?====================
        try {
            pokemon.setLevel(pkt.level());
        } catch (Throwable e) {
            PokemonEditorDebug.log("setLevel 婵犮垺鍎肩划鍓ф喆? " + e.getMessage());
        }

        // ==================== 缂傚倷绀佺换姗€鎮板▎鎾存櫢?====================
        try {
            pokemon.getClass().getMethod("setExperience$common", int.class).invoke(pokemon, pkt.experience());
        } catch (Throwable e) {
            try {
                pokemon.setExperienceAndUpdateLevel(pkt.experience());
            } catch (Throwable ignored) {}
        }

        // ==================== 婵炲瓨绮嶈ぐ鍐儊閹达附鏅?====================
        try {
            pokemon.setFriendship(Math.max(0, Math.min(255, pkt.friendship())), true);
        } catch (Throwable e) {
            PokemonEditorDebug.log("setFriendship 婵犮垺鍎肩划鍓ф喆? " + e.getMessage());
        }

        // ==================== 闂傚倸鍋嗘禍婊堝储?====================
        try {
            pokemon.setShiny(pkt.shiny());
        } catch (Throwable ignored) {}

        // ==================== Nickname ====================
        try {
            if (pkt.displayName() == null || pkt.displayName().isEmpty()) {
                pokemon.setNickname(null);
            } else {
                pokemon.setNickname(net.minecraft.network.chat.Component.literal(pkt.displayName()));
            }
        } catch (Throwable ignored) {}

        // ==================== UUID ====================
        String uuidStr = pkt.pokemonUUID();
        if (uuidStr != null && !uuidStr.isEmpty()) {
            try {
                UUID newUuid = UUID.fromString(uuidStr);
                pokemon.setUuid(newUuid);
            } catch (Throwable ignored) {}
        }

        // ==================== 闂佽鍎搁崘銊у姸 ====================
        try {
            Object genderEnum = resolveGender(pkt.gender());
            if (genderEnum != null)
                pokemon.setGender((com.cobblemon.mod.common.pokemon.Gender) genderEnum);
        } catch (Throwable ignored) {}

        // ==================== 闂佽鍎搁崟顓烆棔 ====================
        if (pkt.nature() != null && !pkt.nature().isEmpty()) {
            Object natureObj = lookupRegistryObj(pkt.nature());
            if (natureObj != null) {
                try {
                    pokemon.setNature((com.cobblemon.mod.common.pokemon.Nature) natureObj);
                } catch (Throwable ignored) {}
            }
        }

        // ==================== 闂佺粯顨堝▍銏ゅ箯?====================
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

        // ==================== Held Item ====================
        if (pkt.heldItem() != null && !pkt.heldItem().isEmpty()) {
            Item item = findItem(pkt.heldItem());
            if (item != null) {
                ItemStack stack = new ItemStack(item);
                try {
                    pokemon.setHeldItem$common(stack);
                } catch (Throwable ignored) {}
            }
        }

        // ==================== Status ====================
        try {
            if (pkt.status() != null && !pkt.status().isEmpty()) {
                Object statusObj = lookupStatusObj(pkt.status());
                if (statusObj != null) {
                    pokemon.applyStatus((com.cobblemon.mod.common.pokemon.status.PersistentStatus) statusObj);
                }
            } else {
                pokemon.setStatus(null);
            }
        } catch (Throwable ignored) {}

        // ==================== IVs / EVs ====================
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

        // ==================== MoveSet 闂佸憡鍔栭悷銉╁矗?====================
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

        // HP 闂佸搫鐗冮崑鎾绘煕濮橆剚鍞夋い鏃€娲滅槐鏃堫敋閸℃瑧顦╂慨鎺撶⊕椤牓顢?= 闂佸搫鐗冮崑鎾绘煥?HP闂?
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

    // ==================== 闁哄鐗嗛幊搴㈡叏椤忓嫷鍟呴柕澶堝劚瀵?====================
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

            // 闂?闂佸搫绉堕…鍫㈢紦閻愵剛鈹嶆い鏃傗拡濡插鏌ㄥ☉娆掑缂佺儵鍋撴俊鐐€涢濠勬崲濮樿埖鍋╂繛鍡樺灍閺佸嫭淇婇崣澶婄亰缂傚秴鎳橀弫宥夊锤椤ょ卑l闂佹寧绋戦ˇ鍗炃庨鈧弫?6 濠碘€冲级閸ㄦ繄绱為崨顖滅＜闁规儳顕埀?闂?
            // PartyStore.toGappyList() 闁哄鏅滈弻銊ッ?List<Pokemon>闂佹寧绋戦悧鍡涘箚?null闂?
            // 濠电偛顦崝宥夊礈娴煎瓨鏅慨婵嗗磱rtyStore 闁诲骸婀遍崑鐔肩嵁閸ヮ剚鏅?Iterable闂佹寧绋戞總鏃傚緤閻愵剚浜ゆい鎾寸箓閺佲晠鏌￠崘顓熺【鐟滄妸鍕氦闁哄倹瀵х粈鈧梺?null 闁诲氦顫夌换鍌濄亹閹绘帩娼栭柨鏃囧亹绾?
            //       婵炴垶鎸哥粔鐑藉礂濮椻偓閺?Iterable闂佹寧绋戦懟顖炲箚娓氣偓瀹曟艾鈻庨幋鐘靛嫎闂佽绻愰悘姘舵偉椤旂晫鈻旂€广儱瀚粣妤呮煛閸曨偄鈷旈柕鍥ㄦ皑缁岸鎮滃Ο鐑嗗亰閺?
            try {
                Method gappyMethod = party.getClass().getMethod("toGappyList");
                List<?> gappyList = (List<?>) gappyMethod.invoke(party);
                if (gappyList != null) {
                    list.addAll(gappyList);  // 婵烇絽娲︾换鍕汲閳?null
                }
            } catch (NoSuchMethodException e1) {
                // 闂佹悶鍎抽崑銈夊焵椤戣棄浜鹃梺鎸庣⊕閼归箖銆呰閺?toGappyList 婵炴垶鎸哥粔鎾偤閵娾晛鎹舵い顓熷笧缁€澶愭煥?size()/get()
                try {
                    Method sizeM = party.getClass().getMethod("size");
                    Method getM = party.getClass().getMethod("get", int.class);
                    int sz = (Integer) sizeM.invoke(party);
                    for (int i = 0; i < sz; i++) {
                        list.add(getM.invoke(party, i));  // 婵炴垶鎸哥粔鐑芥倻閿曞倹鏅?null闂?
                    }
                } catch (Throwable ignored) {
                    // 闂佸搫鐗冮崑鎾绘煕濮橆剟顎楁繛鍙夌墵閺屽懘鍩€椤掑嫭鏅慨姗嗗墻濞层倝鏌＄€ｎ偆鐭嬫俊?List闂佹寧绋戞總鏃傜箔婢跺本宕夐悗鍦Х缁?null
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

    // 闂佸疇妫勯幊蹇曗偓鍨矌濡叉劙濮€閻樼數鈹涢柣鐘叉穿椤曆嗐亹?MoveSet闂佹寧绋掑畝鍛婂閳哄懏鏅?iterator() 闂佸憡鐟崹鐢稿礂濡吋宕夐悗鍦Х缁犳牜绱掑畝鈧崕銈堟綍婵炶揪绲界粔鎾敋闁秵鍤婄紓鍫㈠Х缁夊绱撻崘鎯ф珝闁轰礁锕弫?
    // 闁哄鏅滈弻銊ッ洪弽顓熸櫖婵繄顨憉e 闁荤偞绋忛崝搴ㄥΦ濮樿泛绠板璺鸿嫰閸嬪秶鈧鍠楀ú婵嬵敋娴兼潙鐭楅柡宥冨妼閻忓洭鏌涢弮鍌氭珢缂佽鲸绱礱lse 闁荤偞绋忛崝搴ㄥΦ濮樿泛鐐婇柣鎰€€閸嬫捇鍩€椤掑嫭鏅?iterator
    // outNames[4], outPPs[4], outMaxPPs[4], outRaised[4]闂佹寧绋掓穱铏规椤撱垹绀勯柣鎴炆戝▓鍓佺磽娴ｇ顏电紒杈ㄧ箚缁犳盯宕ㄥǎ顑藉亾韫囨棑绱ｉ柛鏇ㄥ亜鐢儵鏌涢幒鎾剁畵妞ゎ偅鍔欏畷鐘诲冀閿曚胶顦伴梺?0
    private static boolean readMovesByIndex(Object moveSetObj, List<String> outNames,
                                            int[] outPPs, int[] outMaxPPs, int[] outRaised) {
        if (moveSetObj == null) return false;
        PokemonEditorDebug.infoMoves("[MoveRead][Index] probing moveSet methods...");

        // 1. 婵炴潙鍚嬮敋闁告ɑ鐩弫宥咁潩鏉堛劎鍑介梺瑙勪航閸庤尙鏁导瀛樻櫢?get(int) / getMove(int) 闂佸搫鍊介～澶屾兜?
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
                        outMaxPPs[i] = (tpl != null ? tpl.getPp() : move.getMaxPp());
                        outRaised[i] = move.getRaisedPpStages();
                        foundAny = true;
                    } else {
                        // 濠碘€冲级閸ㄦ繄绱為崨顔锯枖闁惧繐鍘滈弫鍕煥濞戞瑧鐓紒缁樺哺楠炴劖鎷呯憴鍕啀缂傚倷绀佺€氭壆鍒掗婊勫闁靛牆鎳夐崑鎾舵兜妞嬪海顦?" 闂?0闂?
                    }
                } catch (Throwable ignored) {}
            }
            if (foundAny || outNames.stream().anyMatch(s -> s != null && !s.isEmpty())) {
                return true;
            }
        }

        // 2. 濠电偛妫寸换婵嬪焵椤掆偓椤р偓缂佽京娈瀘GappyList() 闂佺懓鐡ㄩ悧婊呮偖椤愶絽顕遍柤瑙勫缁犳煡鏌涢妷褍浠滈柟?null 闂佹眹鍔岀€氼剟宕归鍫熸櫢?
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
                        outMaxPPs[i] = (tpl != null ? tpl.getPp() : move.getMaxPp());
                        outRaised[i] = move.getRaisedPpStages();
                    }
                }
                return true;
            }
        } catch (Throwable ignored) {}

        // 3. 闂佽浜介崹铏圭矈閹绢喗鏅慨姗嗗墰濮ｏ箓鎮归崶銊︾闁逞屽墯娣囪櫣鎹㈤崘顏佸亾濞戞瑯娈曟い鏂跨灱閹峰宕稿Δ渚婄椽闂佹寧绋戦悧鍡涖€?moves / slots闂?
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
                                outMaxPPs[i] = (tpl != null ? tpl.getPp() : move.getMaxPp());
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
                                outMaxPPs[i] = (tpl != null ? tpl.getPp() : move.getMaxPp());
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
        // 直接调用 Natures.getNature(String)
        try {
            java.lang.reflect.Method getNatureM = com.cobblemon.mod.common.api.pokemon.Natures.class.getMethod("getNature", String.class);
            Object r = getNatureM.invoke(null, lowerName);
            if (r != null) result = r;
        } catch (Throwable ignored) {}
        // 备用： cobblemon: 前缀
        if (result == null) {
            try {
                java.lang.reflect.Method getNatureM = com.cobblemon.mod.common.api.pokemon.Natures.class.getMethod("getNature", String.class);
                Object r = getNatureM.invoke(null, "cobblemon:" + lowerName);
                if (r != null) result = r;
            } catch (Throwable ignored) {}
        }
        // 备用：遍历 Natures.all() 匹配 name
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
        // 1. 先获取 AbilityTemplate
        Object template = null;
        try {
            java.lang.reflect.Method getM = com.cobblemon.mod.common.api.abilities.Abilities.class.getMethod("get", String.class);
            template = getM.invoke(null, lowerName);
        } catch (Throwable ignored) {}
        // 备用: cobblemon: 前缀
        if (template == null) {
            try {
                java.lang.reflect.Method getM = com.cobblemon.mod.common.api.abilities.Abilities.class.getMethod("get", String.class);
                template = getM.invoke(null, "cobblemon:" + lowerName);
            } catch (Throwable ignored) {}
        }
        // 备用: 通过 AbilityTemplate.Companion 查找
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

        // 2. 从 template 创建 Ability 对象
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
        if (registryCache.containsKey(cacheKey)) return registryCache.get(cacheKey);

        Object result = null;
        PokemonEditorDebug.logEditor("[StatusLookup] searching for status: {}", lowerName);

        // ========== 方案1：使用官方 API Statuses.getStatus(String) （首选） ==========
        try {
            java.lang.reflect.Method getStatusM = com.cobblemon.mod.common.api.pokemon.status.Statuses.class.getMethod("getStatus", String.class);
            Object status = getStatusM.invoke(null, lowerName);
            if (status != null && status instanceof com.cobblemon.mod.common.pokemon.status.PersistentStatus) {
                result = status;
                PokemonEditorDebug.logEditor("[StatusLookup] found via Statuses.getStatus('{}'): {}", lowerName, status.getClass().getSimpleName());
            }
        } catch (Throwable t) {
            PokemonEditorDebug.warnEditor("[StatusLookup] Statuses.getStatus(String) failed: {}", t.getMessage());
        }

        // ========== 方案2：尝试带 cobblemon: 前缀 ==========
        if (result == null) {
            try {
                java.lang.reflect.Method getStatusM = com.cobblemon.mod.common.api.pokemon.status.Statuses.class.getMethod("getStatus", String.class);
                Object status = getStatusM.invoke(null, "cobblemon:" + lowerName);
                if (status != null && status instanceof com.cobblemon.mod.common.pokemon.status.PersistentStatus) {
                    result = status;
                    PokemonEditorDebug.logEditor("[StatusLookup] found via Statuses.getStatus('cobblemon:{}'): {}", lowerName, status.getClass().getSimpleName());
                }
            } catch (Throwable ignored) {}
        }

        // ========== 方案3：名称变体匹配（frozen/freez, poison_badly/bad_poison） ==========
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
                    if (status != null && status instanceof com.cobblemon.mod.common.pokemon.status.PersistentStatus) {
                        result = status;
                        PokemonEditorDebug.logEditor("[StatusLookup] found via variant '{}': {}", variant, status.getClass().getSimpleName());
                        break;
                    }
                } catch (Throwable ignored) {}
            }
        }

        // ========== 方案4：兜底 - 直接通过 Statuses 类的静态字段 ==========
        if (result == null) {
            try {
                for (java.lang.reflect.Field f : com.cobblemon.mod.common.api.pokemon.status.Statuses.class.getFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                        Object val = f.get(null);
                        if (val != null && val instanceof com.cobblemon.mod.common.pokemon.status.PersistentStatus) {
                            try {
                                Object nameRL = val.getClass().getMethod("getName").invoke(val);
                                if (nameRL instanceof ResourceLocation rl) {
                                    if (rl.getPath().equalsIgnoreCase(lowerName) ||
                                            rl.getPath().equalsIgnoreCase(lowerName.replace("_", ""))) {
                                        result = val;
                                        PokemonEditorDebug.logEditor("[StatusLookup] found via Statuses fields: {}", lowerName);
                                        break;
                                    }
                                }
                            } catch (Throwable ignored) {}
                            if (f.getName().toLowerCase().contains(lowerName.replace("_", "")) && result == null) {
                                result = val;
                                PokemonEditorDebug.logEditor("[StatusLookup] found via field name match: {}", f.getName());
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (result == null) {
            PokemonEditorDebug.warnEditor("[StatusLookup] FAILED to find status: {}", lowerName);
        }
        registryCache.put(cacheKey, result);
        return result;
    }

    private static Object lookupSpeciesObj(String name) {
        if (name == null || name.isEmpty()) return null;
        String lowerName = name.toLowerCase().trim();
        String cacheKey = "species#" + lowerName;
        if (registryCache.containsKey(cacheKey)) return registryCache.get(cacheKey);

        Object result = null;
        // 闂佺儵鏅涢悺銊ф暜鐎涙ɑ濯撮悹鎭掑妽閺?PokemonSpecies.getByName(String)
        try {
            Method getByName = PokemonSpecies.class.getMethod("getByName", String.class);
            Object r = getByName.invoke(null, lowerName);
            if (r != null) result = r;
        } catch (Throwable ignored) {}
        if (result == null) {
            // 婵犮垼娉涘ú锝夊船? 闁诲繐绻戠换鍡涙儊?Species.Companion 闂佹眹鍔岀€氭澘顭囬妶澶婄畱濞达絽婀遍崣鈧梺鍦暩閸嬫捇寮婚悢鍏兼櫢?
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

    // Stats 闁荤姴娲╅褔宕幘缁樻櫖闁割偅绮岄埢蹇涙煟椤剙濡肩€殿喚鏅禍鎼佸礋椤忓啰闉嶉梺绋跨箳閸樠囶敊閹邦厾鈻旂€广儱鎳忛崐?Cobblemon 闂佺粯顨呴悧濠傦耿娴煎瓨鏅?
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

        // 闂佸憡绋栧Λ鍕箖閺囩姵濯奸柛褎顨嗛敍鏍煥?
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

        // 濠碘槅鍨界槐鏇犳兜閿曞倸绠ユい鎰剁稻閻撴瑩鏌?
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

        // 闂佺绻愰悧鍡涙儍閸撗勫珰闁哄洨濮甸弳?pokemon.setIV / pokemon.setEV 闂佸搫鍊介～澶屾兜?
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

        // 闂佸憡绋栧Λ鍕箖閺囩姵濯奸柛褎顨嗛敍鏍煥?
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

        // 濠碘槅鍨界槐鏇犳兜閿曞倸绠ユい鎰剁稻閻撴瑩鏌?
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