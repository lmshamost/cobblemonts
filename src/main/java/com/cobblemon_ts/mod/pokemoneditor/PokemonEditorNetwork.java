package com.cobblemon_ts.mod.pokemoneditor;

import com.cobblemon_ts.mod.network.IPayloadContext;
import com.cobblemon_ts.mod.network.PacketDistributor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// ========================================================================
// PokemonEditorNetwork - 使用直接 Cobblemon API（反射）读写宝可梦数据
// 不使用 NBT 作为中间层，避免序列化/反序列化错误。
// ========================================================================
public class PokemonEditorNetwork {

    public static final Identifier PKT_OPEN_EDITOR = Identifier.of("cobblemon_ts", "open_editor");
    public static final Identifier PKT_POKEMON_DATA = Identifier.of("cobblemon_ts", "pokemon_data");
    public static final Identifier PKT_UPDATE_POKEMON = Identifier.of("cobblemon_ts", "update_pokemon");
    public static final Identifier PKT_DELETE_POKEMON = Identifier.of("cobblemon_ts", "delete_pokemon");

    // ---------- 1. OpenEditorPacket (Server→Client) ----------
    public record OpenEditorPacket(int partySize, String playerName, UUID playerUUID) implements CustomPayload {
        public static final Id<OpenEditorPacket> TYPE = new Id<>(PKT_OPEN_EDITOR);
        public static final PacketCodec<ByteBuf, OpenEditorPacket> STREAM_CODEC = PacketCodecHelper.composite(
                PacketCodecs.VAR_INT, OpenEditorPacket::partySize,
                PacketCodecs.STRING, OpenEditorPacket::playerName,
                PacketCodecHelper.mapped(PacketCodecs.string(36), UUID::fromString, UUID::toString), OpenEditorPacket::playerUUID,
                OpenEditorPacket::new
        );
        @Override public Id<? extends CustomPayload> getId() { return TYPE; }
    }

    // ---------- 2. PokemonDataPacket (Server→Client) ----------
    // 关键：PacketCodec.composite 最多只支持 6 个参数（composite1~composite6）
    // 策略：将大数据包拆分为多层嵌套 record，每一层 composite 都不超过 6 个组件
    // ---- 共享的子分组定义 ----
    public record Stats6(int hp, int atk, int def, int spA, int spD, int spe) {}
    public record Moves4(String m1, String m2, String m3, String m4) {}

    private static final PacketCodec<ByteBuf, Stats6> STATS_CODEC = PacketCodecHelper.composite(
            PacketCodecs.VAR_INT, Stats6::hp,
            PacketCodecs.VAR_INT, Stats6::atk,
            PacketCodecs.VAR_INT, Stats6::def,
            PacketCodecs.VAR_INT, Stats6::spA,
            PacketCodecs.VAR_INT, Stats6::spD,
            PacketCodecs.VAR_INT, Stats6::spe,
            Stats6::new
    );
    private static final PacketCodec<ByteBuf, Moves4> MOVES_CODEC = PacketCodecHelper.composite(
            PacketCodecs.STRING, Moves4::m1,
            PacketCodecs.STRING, Moves4::m2,
            PacketCodecs.STRING, Moves4::m3,
            PacketCodecs.STRING, Moves4::m4,
            Moves4::new
    );
    private static final PacketCodec<ByteBuf, UUID> UUID_CODEC = PacketCodecHelper.mapped(
            PacketCodecs.string(36),
            s -> s.isEmpty() ? null : UUID.fromString(s),
            u -> u == null ? "" : u.toString());

    // PokemonDataPacket：定义扁平字段，内部通过嵌套分组实现序列化
    // movePPs 编码: hp=move1PP, atk=move2PP, def=move3PP, spA=move4PP,
    //              spD=move1MaxPP*1000 + move2MaxPP, spe=move3MaxPP*1000 + move4MaxPP
    public record PokemonDataPacket(int slot, boolean exists, UUID pokemonUUID,
                                    String speciesName, String displayName, String formName,
                                    int level, int experience, int friendship, boolean shiny,
                                    boolean goldenBottleCap,
                                    int ivHp, int ivAtk, int ivDef, int ivSpA, int ivSpD, int ivSpe,
                                    int evHp, int evAtk, int evDef, int evSpA, int evSpD, int evSpe,
                                    String nature, String ability,
                                    String move1, String move2, String move3, String move4,
                                    int move1PP, int move2PP, int move3PP, int move4PP,
                                    int move1MaxPP, int move2MaxPP, int move3MaxPP, int move4MaxPP,
                                    int currentHp, int maxHp, String heldItem, int gender,
                                    String possibleAbilities, String status) implements CustomPayload {
        public static final Id<PokemonDataPacket> TYPE = new Id<>(PKT_POKEMON_DATA);

        // ---- 内部嵌套分组（仅用于序列化，不暴露给外部 API）----
        // Group1: 槽位标识信息 (3 项)
        private record Group1(int slot, boolean exists, UUID pokemonUUID) {}
        // Group2: 物种信息 (3 项)
        private record Group2(String speciesName, String displayName, String formName) {}
        // Group3: 等级/经验/亲密度/闪光/金色王冠 (5 项)
        private record Group3(int level, int experience, int friendship, boolean shiny, boolean goldenBottleCap) {}
        // Group4: 能力/招式 (nature + ability + moves4 = 6 项)
        private record Group4(String nature, String ability, Moves4 moves) {}
        // Group5: HP/道具/性别/特性列表/movePP/status
        // 重新组织：statusItem(2) + currentHp(1) + maxHp(1) + gender(1) + possibleAbilities(1) + movePPs(1)
        //  → 7 项？需要重新分组，用 statusItem 子组后 Group5 变成 6 项
        private record Group5StatusItem(String status, String heldItem) {}
        private record Group5(Group5StatusItem statusItem, int currentHp, int maxHp,
                              int gender, String possibleAbilities, Stats6 movePPs) {}

        // ---- Pack/Unpack 工具方法 ----
        private static int packTwo(int a, int b) { return a * 1000 + b; }
        private static int unpackFirst(int packed) { return packed / 1000; }
        private static int unpackSecond(int packed) { return packed % 1000; }

        private static final PacketCodec<ByteBuf, Group1> G1_CODEC = PacketCodecHelper.composite(
                PacketCodecs.VAR_INT, Group1::slot,
                PacketCodecs.BOOL, Group1::exists,
                UUID_CODEC, Group1::pokemonUUID,
                Group1::new
        );
        private static final PacketCodec<ByteBuf, Group2> G2_CODEC = PacketCodecHelper.composite(
                PacketCodecs.STRING, Group2::speciesName,
                PacketCodecs.STRING, Group2::displayName,
                PacketCodecs.STRING, Group2::formName,
                Group2::new
        );
        private static final PacketCodec<ByteBuf, Group3> G3_CODEC = PacketCodecHelper.composite(
                PacketCodecs.VAR_INT, Group3::level,
                PacketCodecs.VAR_INT, Group3::experience,
                PacketCodecs.VAR_INT, Group3::friendship,
                PacketCodecs.BOOL, Group3::shiny,
                PacketCodecs.BOOL, Group3::goldenBottleCap,
                Group3::new
        );
        // Group4 内部再嵌套一层：nature + ability + (moves4 的 PacketCodec)
        private record Group4a(String nature, String ability) {}
        private static final PacketCodec<ByteBuf, Group4a> G4A_CODEC = PacketCodecHelper.composite(
                PacketCodecs.STRING, Group4a::nature,
                PacketCodecs.STRING, Group4a::ability,
                Group4a::new
        );
        private static final PacketCodec<ByteBuf, Group4> G4_CODEC = PacketCodecHelper.composite(
                G4A_CODEC, g -> new Group4a(g.nature, g.ability),
                MOVES_CODEC, g -> new Moves4(g.moves.m1, g.moves.m2, g.moves.m3, g.moves.m4),
                (g4a, mv) -> new Group4(g4a.nature, g4a.ability, mv)
        );
        // Group5: statusItem(2) + currentHp + maxHp + gender + possibleAbilities + movePPs = 6 项
        private static final PacketCodec<ByteBuf, Group5StatusItem> G5SI_CODEC = PacketCodecHelper.composite(
                PacketCodecs.STRING, Group5StatusItem::status,
                PacketCodecs.STRING, Group5StatusItem::heldItem,
                Group5StatusItem::new
        );
        private static final PacketCodec<ByteBuf, Group5> G5_CODEC = PacketCodecHelper.composite(
                G5SI_CODEC, g -> new Group5StatusItem(g.statusItem.status, g.statusItem.heldItem),
                PacketCodecs.VAR_INT, Group5::currentHp,
                PacketCodecs.VAR_INT, Group5::maxHp,
                PacketCodecs.VAR_INT, Group5::gender,
                PacketCodecs.STRING, Group5::possibleAbilities,
                STATS_CODEC, Group5::movePPs,
                Group5::new
        );

        // 顶层 composite：6 个组件 (g1, g2, g3, ivs, evs, g4+g5 合并)
        private record Group45(Group4 g4, Group5 g5) {}
        private static final PacketCodec<ByteBuf, Group45> G45_CODEC = PacketCodecHelper.composite(
                G4_CODEC, Group45::g4,
                G5_CODEC, Group45::g5,
                Group45::new
        );

        // 顶层有 6 个组件：g1, g2, g3, ivs, evs, g45 → 刚好 6 个！
        public static final PacketCodec<ByteBuf, PokemonDataPacket> STREAM_CODEC = PacketCodecHelper.composite(
                G1_CODEC, p -> new Group1(p.slot, p.exists, p.pokemonUUID),
                G2_CODEC, p -> new Group2(p.speciesName, p.displayName, p.formName),
                G3_CODEC, p -> new Group3(p.level, p.experience, p.friendship, p.shiny, p.goldenBottleCap),
                STATS_CODEC, p -> new Stats6(p.ivHp, p.ivAtk, p.ivDef, p.ivSpA, p.ivSpD, p.ivSpe),
                STATS_CODEC, p -> new Stats6(p.evHp, p.evAtk, p.evDef, p.evSpA, p.evSpD, p.evSpe),
                G45_CODEC, p -> new Group45(
                        new Group4(p.nature, p.ability, new Moves4(p.move1, p.move2, p.move3, p.move4)),
                        new Group5(new Group5StatusItem(p.status, p.heldItem),
                                p.currentHp, p.maxHp, p.gender, p.possibleAbilities,
                                new Stats6(p.move1PP, p.move2PP, p.move3PP, p.move4PP,
                                        packTwo(p.move1MaxPP, p.move2MaxPP),
                                        packTwo(p.move3MaxPP, p.move4MaxPP)))
                ),
                (g1, g2, g3, iv, ev, g45) -> new PokemonDataPacket(
                        g1.slot, g1.exists, g1.pokemonUUID,
                        g2.speciesName, g2.displayName, g2.formName,
                        g3.level, g3.experience, g3.friendship, g3.shiny,
                        g3.goldenBottleCap,
                        iv.hp, iv.atk, iv.def, iv.spA, iv.spD, iv.spe,
                        ev.hp, ev.atk, ev.def, ev.spA, ev.spD, ev.spe,
                        g45.g4.nature, g45.g4.ability,
                        g45.g4.moves.m1, g45.g4.moves.m2, g45.g4.moves.m3, g45.g4.moves.m4,
                        g45.g5.movePPs.hp, g45.g5.movePPs.atk, g45.g5.movePPs.def, g45.g5.movePPs.spA,
                        unpackFirst(g45.g5.movePPs.spD), unpackSecond(g45.g5.movePPs.spD),
                        unpackFirst(g45.g5.movePPs.spe), unpackSecond(g45.g5.movePPs.spe),
                        g45.g5.currentHp, g45.g5.maxHp, g45.g5.statusItem.heldItem, g45.g5.gender,
                        g45.g5.possibleAbilities, g45.g5.statusItem.status
                )
        );
        @Override public Id<? extends CustomPayload> getId() { return TYPE; }
    }

    // ---------- 3. UpdatePokemonPacket (Client→Server) ----------
    // 使用和 PokemonDataPacket 相同的嵌套分组策略
    // movePPs 编码: hp=move1PP, atk=move2PP, def=move3PP, spA=move4PP,
    //              spD=move1MaxPP*1000+move2MaxPP, spe=move3MaxPP*1000+move4MaxPP
    public record UpdatePokemonPacket(int slot, UUID targetPlayerUUID,
                                      String speciesName, String formName,
                                      int level, int experience, String displayName, boolean shiny, int friendship,
                                      boolean goldenBottleCap,
                                      int ivHp, int ivAtk, int ivDef, int ivSpA, int ivSpD, int ivSpe,
                                      int evHp, int evAtk, int evDef, int evSpA, int evSpD, int evSpe,
                                      String nature, String ability,
                                      String move1, String move2, String move3, String move4,
                                      int move1PP, int move2PP, int move3PP, int move4PP,
                                      int move1MaxPP, int move2MaxPP, int move3MaxPP, int move4MaxPP,
                                      int currentHp, int maxHp, String heldItem, int gender,
                                      String pokemonUUID, String rawJsonData, String status) implements CustomPayload {
        public static final Id<UpdatePokemonPacket> TYPE = new Id<>(PKT_UPDATE_POKEMON);

        // ---- 内部嵌套分组 ----
        private record UGroup1(int slot, UUID targetPlayerUUID, String pokemonUUID, String rawJsonData) {}
        private record UGroup2(String speciesName, String formName) {}
        private record UGroup3(int level, int experience, String displayName, boolean shiny, int friendship, boolean goldenBottleCap) {}
        private record UGroup4(String nature, String ability, Moves4 moves) {}
        private record UGroup4a(String nature, String ability) {}
        private record UGroup5StatusItem(String status, String heldItem) {}
        private record UGroup5(UGroup5StatusItem statusItem, int currentHp, int maxHp, int gender, Stats6 movePPs) {}
        private record UGroup45(UGroup4 g4, UGroup5 g5) {}

        private static int packTwoU(int a, int b) { return a * 1000 + b; }
        private static int unpackFirstU(int packed) { return packed / 1000; }
        private static int unpackSecondU(int packed) { return packed % 1000; }

        private static final PacketCodec<ByteBuf, UGroup1> UG1_CODEC = PacketCodecHelper.composite(
                PacketCodecs.VAR_INT, UGroup1::slot,
                UUID_CODEC, UGroup1::targetPlayerUUID,
                PacketCodecs.STRING, UGroup1::pokemonUUID,
                PacketCodecs.STRING, UGroup1::rawJsonData,
                UGroup1::new
        );
        private static final PacketCodec<ByteBuf, UGroup2> UG2_CODEC = PacketCodecHelper.composite(
                PacketCodecs.STRING, UGroup2::speciesName,
                PacketCodecs.STRING, UGroup2::formName,
                UGroup2::new
        );
        private static final PacketCodec<ByteBuf, UGroup3> UG3_CODEC = PacketCodecHelper.composite(
                PacketCodecs.VAR_INT, UGroup3::level,
                PacketCodecs.VAR_INT, UGroup3::experience,
                PacketCodecs.STRING, UGroup3::displayName,
                PacketCodecs.BOOL, UGroup3::shiny,
                PacketCodecs.VAR_INT, UGroup3::friendship,
                PacketCodecs.BOOL, UGroup3::goldenBottleCap,
                UGroup3::new
        );
        private static final PacketCodec<ByteBuf, UGroup4a> UG4A_CODEC = PacketCodecHelper.composite(
                PacketCodecs.STRING, UGroup4a::nature,
                PacketCodecs.STRING, UGroup4a::ability,
                UGroup4a::new
        );
        private static final PacketCodec<ByteBuf, UGroup4> UG4_CODEC = PacketCodecHelper.composite(
                UG4A_CODEC, g -> new UGroup4a(g.nature, g.ability),
                MOVES_CODEC, g -> new Moves4(g.moves.m1, g.moves.m2, g.moves.m3, g.moves.m4),
                (g4a, mv) -> new UGroup4(g4a.nature, g4a.ability, mv)
        );
        // UGroup5: statusItem(2) + currentHp + maxHp + gender + movePPs = 6 项
        private static final PacketCodec<ByteBuf, UGroup5StatusItem> UG5SI_CODEC = PacketCodecHelper.composite(
                PacketCodecs.STRING, UGroup5StatusItem::status,
                PacketCodecs.STRING, UGroup5StatusItem::heldItem,
                UGroup5StatusItem::new
        );
        private static final PacketCodec<ByteBuf, UGroup5> UG5_CODEC = PacketCodecHelper.composite(
                UG5SI_CODEC, g -> new UGroup5StatusItem(g.statusItem.status, g.statusItem.heldItem),
                PacketCodecs.VAR_INT, UGroup5::currentHp,
                PacketCodecs.VAR_INT, UGroup5::maxHp,
                PacketCodecs.VAR_INT, UGroup5::gender,
                STATS_CODEC, UGroup5::movePPs,
                UGroup5::new
        );
        private static final PacketCodec<ByteBuf, UGroup45> UG45_CODEC = PacketCodecHelper.composite(
                UG4_CODEC, UGroup45::g4,
                UG5_CODEC, UGroup45::g5,
                UGroup45::new
        );

        // 顶层 composite：6 个组件 (ug1, ug2, ug3, ivs, evs, ug45)
        public static final PacketCodec<ByteBuf, UpdatePokemonPacket> STREAM_CODEC = PacketCodecHelper.composite(
                UG1_CODEC, p -> new UGroup1(p.slot, p.targetPlayerUUID, p.pokemonUUID, p.rawJsonData),
                UG2_CODEC, p -> new UGroup2(p.speciesName, p.formName),
                UG3_CODEC, p -> new UGroup3(p.level, p.experience, p.displayName, p.shiny, p.friendship, p.goldenBottleCap),
                STATS_CODEC, p -> new Stats6(p.ivHp, p.ivAtk, p.ivDef, p.ivSpA, p.ivSpD, p.ivSpe),
                STATS_CODEC, p -> new Stats6(p.evHp, p.evAtk, p.evDef, p.evSpA, p.evSpD, p.evSpe),
                UG45_CODEC, p -> new UGroup45(
                        new UGroup4(p.nature, p.ability, new Moves4(p.move1, p.move2, p.move3, p.move4)),
                        new UGroup5(new UGroup5StatusItem(p.status, p.heldItem),
                                p.currentHp, p.maxHp, p.gender,
                                new Stats6(p.move1PP, p.move2PP, p.move3PP, p.move4PP,
                                        packTwoU(p.move1MaxPP, p.move2MaxPP),
                                        packTwoU(p.move3MaxPP, p.move4MaxPP)))
                ),
                (g1, g2, g3, iv, ev, g45) -> new UpdatePokemonPacket(
                        g1.slot, g1.targetPlayerUUID,
                        g2.speciesName, g2.formName,
                        g3.level, g3.experience, g3.displayName, g3.shiny, g3.friendship,
                        g3.goldenBottleCap,
                        iv.hp, iv.atk, iv.def, iv.spA, iv.spD, iv.spe,
                        ev.hp, ev.atk, ev.def, ev.spA, ev.spD, ev.spe,
                        g45.g4.nature, g45.g4.ability,
                        g45.g4.moves.m1, g45.g4.moves.m2, g45.g4.moves.m3, g45.g4.moves.m4,
                        g45.g5.movePPs.hp, g45.g5.movePPs.atk, g45.g5.movePPs.def, g45.g5.movePPs.spA,
                        unpackFirstU(g45.g5.movePPs.spD), unpackSecondU(g45.g5.movePPs.spD),
                        unpackFirstU(g45.g5.movePPs.spe), unpackSecondU(g45.g5.movePPs.spe),
                        g45.g5.currentHp, g45.g5.maxHp, g45.g5.statusItem.heldItem, g45.g5.gender,
                        g1.pokemonUUID, g1.rawJsonData, g45.g5.statusItem.status
                )
        );
        @Override public Id<? extends CustomPayload> getId() { return TYPE; }
    }

    // ---------- 4. DeletePokemonPacket (Client→Server) ----------
    public record DeletePokemonPacket(int slot, UUID targetPlayerUUID) implements CustomPayload {
        public static final Id<DeletePokemonPacket> TYPE = new Id<>(PKT_DELETE_POKEMON);
        public static final PacketCodec<ByteBuf, DeletePokemonPacket> STREAM_CODEC = PacketCodecHelper.composite(
                PacketCodecs.VAR_INT, DeletePokemonPacket::slot,
                PacketCodecHelper.mapped(PacketCodecs.string(36), s -> s.isEmpty() ? null : UUID.fromString(s), u -> u == null ? "" : u.toString()), DeletePokemonPacket::targetPlayerUUID,
                DeletePokemonPacket::new
        );
        @Override public Id<? extends CustomPayload> getId() { return TYPE; }
    }

    // =====================================================================
    // 服务端工具：打开编辑器 + 发送每个槽位的宝可梦数据
    // =====================================================================
    public static void sendOpenEditorPacket(ServerPlayerEntity clientPlayer, UUID targetUUID, String targetName) {
        int partySize = CobblemonBridge.getPartySize(clientPlayer, targetUUID);
        PokemonEditorDebug.log("sendOpenEditorPacket() - target=" + targetName + " partySize=" + partySize);

        // 1. 发送 OpenEditorPacket 给客户端
        PacketDistributor.sendToPlayer(clientPlayer, new OpenEditorPacket(partySize, targetName, targetUUID));

        // 2. 为每个槽位发送 PokemonDataPacket
        for (int i = 0; i < partySize; i++) {
            PokemonData data = CobblemonBridge.readPokemonFromSlot(clientPlayer, targetUUID, i);
            if (data == null) {
                data = new PokemonData();
            }
            UUID uuid = data.pokemonUUID;
            PacketDistributor.sendToPlayer(clientPlayer, new PokemonDataPacket(
                    i, data.exists, uuid,
                    data.speciesName, data.displayName, data.formName,
                    data.level, data.experience, data.friendship, data.shiny,
                    data.goldenBottleCap,
                    data.ivHp, data.ivAtk, data.ivDef, data.ivSpA, data.ivSpD, data.ivSpe,
                    data.evHp, data.evAtk, data.evDef, data.evSpA, data.evSpD, data.evSpe,
                    data.nature, data.ability,
                    data.move1, data.move2, data.move3, data.move4,
                    data.move1PP, data.move2PP, data.move3PP, data.move4PP,
                    data.move1MaxPP, data.move2MaxPP, data.move3MaxPP, data.move4MaxPP,
                    data.currentHp, data.maxHp, data.heldItem, data.gender,
                    String.join(";", data.possibleAbilities), data.status
            ));
        }
    }

    // =====================================================================
    // 服务端：处理 Client→Server 的 UpdatePokemonPacket
    // =====================================================================
    public static void handleUpdatePokemon(UpdatePokemonPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                ServerPlayerEntity player = (ServerPlayerEntity) ctx.player();
                if (!PokemonEditorItem.checkPermission(player)) {
                    player.sendMessage(net.minecraft.text.Text.literal("你没有权限修改宝可梦！"));
                    return;
                }
                UUID target = pkt.targetPlayerUUID() != null ? pkt.targetPlayerUUID() : player.getUuid();
                PokemonEditorDebug.log("handleUpdatePokemon() - slot=" + pkt.slot() + " target=" + target + " species=" + pkt.speciesName());

                // 特殊情况：物种名为空 → 视为删除该槽位（清空）
                if (pkt.speciesName() == null || pkt.speciesName().isEmpty()) {
                    CobblemonBridge.deletePokemonFromSlot(player, target, pkt.slot());
                    // 删除后也回发数据，客户端刷新为空槽位
                    PokemonData empty = CobblemonBridge.readPokemonFromSlot(player, target, pkt.slot());
                    if (empty == null) empty = new PokemonData();
                    UUID uuid = empty.pokemonUUID;
                    PacketDistributor.sendToPlayer(player, new PokemonDataPacket(
                            pkt.slot(), empty.exists, uuid,
                            empty.speciesName, empty.displayName, empty.formName,
                            empty.level, empty.experience, empty.friendship, empty.shiny,
                            empty.goldenBottleCap,
                            empty.ivHp, empty.ivAtk, empty.ivDef, empty.ivSpA, empty.ivSpD, empty.ivSpe,
                            empty.evHp, empty.evAtk, empty.evDef, empty.evSpA, empty.evSpD, empty.evSpe,
                            empty.nature, empty.ability,
                            empty.move1, empty.move2, empty.move3, empty.move4,
                            empty.move1PP, empty.move2PP, empty.move3PP, empty.move4PP,
                            empty.move1MaxPP, empty.move2MaxPP, empty.move3MaxPP, empty.move4MaxPP,
                            empty.currentHp, empty.maxHp, empty.heldItem, empty.gender,
                            String.join(";", empty.possibleAbilities), empty.status
                    ));
                    player.sendMessage(net.minecraft.text.Text.literal("[宝可梦编辑器] 已删除队伍 " + (pkt.slot() + 1)));
                    return;
                }

                // 正常情况：写入/更新宝可梦数据
                CobblemonBridge.writePokemonToSlot(player, target, pkt);

                // ★ 关键修复：写入后重新从宝可梦对象读取，回发给客户端，GUI 即时刷新
                PokemonData refreshed = CobblemonBridge.readPokemonFromSlot(player, target, pkt.slot());
                if (refreshed == null) refreshed = new PokemonData();
                UUID rUuid = refreshed.pokemonUUID;
                PacketDistributor.sendToPlayer(player, new PokemonDataPacket(
                        pkt.slot(), refreshed.exists, rUuid,
                        refreshed.speciesName, refreshed.displayName, refreshed.formName,
                        refreshed.level, refreshed.experience, refreshed.friendship, refreshed.shiny,
                        refreshed.goldenBottleCap,
                        refreshed.ivHp, refreshed.ivAtk, refreshed.ivDef, refreshed.ivSpA, refreshed.ivSpD, refreshed.ivSpe,
                        refreshed.evHp, refreshed.evAtk, refreshed.evDef, refreshed.evSpA, refreshed.evSpD, refreshed.evSpe,
                        refreshed.nature, refreshed.ability,
                        refreshed.move1, refreshed.move2, refreshed.move3, refreshed.move4,
                        refreshed.move1PP, refreshed.move2PP, refreshed.move3PP, refreshed.move4PP,
                        refreshed.move1MaxPP, refreshed.move2MaxPP, refreshed.move3MaxPP, refreshed.move4MaxPP,
                        refreshed.currentHp, refreshed.maxHp, refreshed.heldItem, refreshed.gender,
                        String.join(";", refreshed.possibleAbilities), refreshed.status
                ));

                player.sendMessage(net.minecraft.text.Text.literal("[宝可梦编辑器] 已保存队伍 " + (pkt.slot() + 1)));
            } catch (Exception e) {
                PokemonEditorDebug.logError("handleUpdatePokemon() error: " + e.getMessage(), e);
                if (ctx.player() instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.sendMessage(net.minecraft.text.Text.literal("[宝可梦编辑器] 保存失败：" + e.getMessage()));
                }
            }
        });
    }

    // =====================================================================
    // 服务端：处理 Client→Server 的 DeletePokemonPacket
    // =====================================================================
    public static void handleDeletePokemon(DeletePokemonPacket pkt, IPayloadContext ctx) {
        // 【服务端】处理客户端的"删除宝可梦"请求
        // 流程：检查权限 → 确定目标玩家 → 转发给 CobblemonBridge.deletePokemonFromSlot 执行删除
        ctx.enqueueWork(() -> {
            try {
                ServerPlayerEntity player = (ServerPlayerEntity) ctx.player();
                // 权限检查
                if (!PokemonEditorItem.checkPermission(player)) {
                    player.sendMessage(net.minecraft.text.Text.literal("你没有权限删除宝可梦！"));
                    return;
                }
                // 确定目标玩家 UUID
                UUID target = pkt.targetPlayerUUID() != null ? pkt.targetPlayerUUID() : player.getUuid();
                PokemonEditorDebug.log("handleDeletePokemon() - slot=" + pkt.slot() + " target=" + target);

                // 执行删除（清空槽位）
                CobblemonBridge.deletePokemonFromSlot(player, target, pkt.slot());

                player.sendMessage(net.minecraft.text.Text.literal("[宝可梦编辑器] 已删除队伍 " + (pkt.slot() + 1)));
            } catch (Exception e) {
                PokemonEditorDebug.logError("handleDeletePokemon() error: " + e.getMessage(), e);
            }
        });
    }
}