package com.cobblemon_ts.mod.pokemoneditor;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// ========================================================================
// PokemonEditorNetwork - 使用直接 Cobblemon API（反射）读写宝可梦数据
// 不使用 NBT 作为中间层，避免序列化/反序列化错误。
// ========================================================================
public class PokemonEditorNetwork {

    public static final ResourceLocation PKT_OPEN_EDITOR = ResourceLocation.fromNamespaceAndPath("coblemon_ts", "open_editor");
    public static final ResourceLocation PKT_POKEMON_DATA = ResourceLocation.fromNamespaceAndPath("coblemon_ts", "pokemon_data");
    public static final ResourceLocation PKT_UPDATE_POKEMON = ResourceLocation.fromNamespaceAndPath("coblemon_ts", "update_pokemon");
    public static final ResourceLocation PKT_DELETE_POKEMON = ResourceLocation.fromNamespaceAndPath("coblemon_ts", "delete_pokemon");

    // ---------- 1. OpenEditorPacket (Server→Client) ----------
    public record OpenEditorPacket(int partySize, String playerName, UUID playerUUID) implements CustomPacketPayload {
        public static final Type<OpenEditorPacket> TYPE = new Type<>(PKT_OPEN_EDITOR);
        public static final StreamCodec<ByteBuf, OpenEditorPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, OpenEditorPacket::partySize,
                ByteBufCodecs.STRING_UTF8, OpenEditorPacket::playerName,
                ByteBufCodecs.stringUtf8(36).map(UUID::fromString, UUID::toString), OpenEditorPacket::playerUUID,
                OpenEditorPacket::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ---------- 2. PokemonDataPacket (Server→Client) ----------
    // 关键：StreamCodec.composite 最多只支持 6 个参数（composite1~composite6）
    // 策略：将大数据包拆分为多层嵌套 record，每一层 composite 都不超过 6 个组件
    // ---- 共享的子分组定义 ----
    public record Stats6(int hp, int atk, int def, int spA, int spD, int spe) {}
    public record Moves4(String m1, String m2, String m3, String m4) {}

    private static final StreamCodec<ByteBuf, Stats6> STATS_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Stats6::hp,
            ByteBufCodecs.VAR_INT, Stats6::atk,
            ByteBufCodecs.VAR_INT, Stats6::def,
            ByteBufCodecs.VAR_INT, Stats6::spA,
            ByteBufCodecs.VAR_INT, Stats6::spD,
            ByteBufCodecs.VAR_INT, Stats6::spe,
            Stats6::new
    );
    private static final StreamCodec<ByteBuf, Moves4> MOVES_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Moves4::m1,
            ByteBufCodecs.STRING_UTF8, Moves4::m2,
            ByteBufCodecs.STRING_UTF8, Moves4::m3,
            ByteBufCodecs.STRING_UTF8, Moves4::m4,
            Moves4::new
    );
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = ByteBufCodecs.stringUtf8(36)
            .map(s -> s.isEmpty() ? null : UUID.fromString(s), u -> u == null ? "" : u.toString());

    // PokemonDataPacket：定义扁平字段，内部通过嵌套分组实现序列化
    // movePPs 编码: hp=move1PP, atk=move2PP, def=move3PP, spA=move4PP,
    //              spD=move1MaxPP*1000 + move2MaxPP, spe=move3MaxPP*1000 + move4MaxPP
    public record PokemonDataPacket(int slot, boolean exists, UUID pokemonUUID,
                                    String speciesName, String displayName, String formName,
                                    int level, int experience, int friendship, boolean shiny,
                                    int ivHp, int ivAtk, int ivDef, int ivSpA, int ivSpD, int ivSpe,
                                    int evHp, int evAtk, int evDef, int evSpA, int evSpD, int evSpe,
                                    String nature, String ability,
                                    String move1, String move2, String move3, String move4,
                                    int move1PP, int move2PP, int move3PP, int move4PP,
                                    int move1MaxPP, int move2MaxPP, int move3MaxPP, int move4MaxPP,
                                    int currentHp, int maxHp, String heldItem, int gender,
                                    String possibleAbilities) implements CustomPacketPayload {
        public static final Type<PokemonDataPacket> TYPE = new Type<>(PKT_POKEMON_DATA);

        // ---- 内部嵌套分组（仅用于序列化，不暴露给外部 API）----
        // Group1: 槽位标识信息 (3 项)
        private record Group1(int slot, boolean exists, UUID pokemonUUID) {}
        // Group2: 物种信息 (3 项)
        private record Group2(String speciesName, String displayName, String formName) {}
        // Group3: 等级/经验/亲密度/闪光 (4 项)
        private record Group3(int level, int experience, int friendship, boolean shiny) {}
        // Group4: 能力/招式 (nature + ability + moves4 = 6 项)
        private record Group4(String nature, String ability, Moves4 moves) {}
        // Group5: HP/道具/性别/特性列表/movePP (6 项)
        private record Group5(int currentHp, int maxHp, String heldItem, int gender, String possibleAbilities, Stats6 movePPs) {}

        // ---- Pack/Unpack 工具方法 ----
        private static int packTwo(int a, int b) { return a * 1000 + b; }
        private static int unpackFirst(int packed) { return packed / 1000; }
        private static int unpackSecond(int packed) { return packed % 1000; }

        private static final StreamCodec<ByteBuf, Group1> G1_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Group1::slot,
                ByteBufCodecs.BOOL, Group1::exists,
                UUID_CODEC, Group1::pokemonUUID,
                Group1::new
        );
        private static final StreamCodec<ByteBuf, Group2> G2_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Group2::speciesName,
                ByteBufCodecs.STRING_UTF8, Group2::displayName,
                ByteBufCodecs.STRING_UTF8, Group2::formName,
                Group2::new
        );
        private static final StreamCodec<ByteBuf, Group3> G3_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Group3::level,
                ByteBufCodecs.VAR_INT, Group3::experience,
                ByteBufCodecs.VAR_INT, Group3::friendship,
                ByteBufCodecs.BOOL, Group3::shiny,
                Group3::new
        );
        // Group4 内部再嵌套一层：nature + ability + (moves4 的 StreamCodec)
        // 通过 composite 组合 nature + ability 为一个小 record，再和 moves4 组合
        private record Group4a(String nature, String ability) {}
        private static final StreamCodec<ByteBuf, Group4a> G4A_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Group4a::nature,
                ByteBufCodecs.STRING_UTF8, Group4a::ability,
                Group4a::new
        );
        // Group4: (g4a:2) + (moves:4 包装为 Moves4) = 使用 composite 组合 g4a_record + moves4_record
        private static final StreamCodec<ByteBuf, Group4> G4_CODEC = StreamCodec.composite(
                G4A_CODEC, g -> new Group4a(g.nature, g.ability),
                MOVES_CODEC, g -> new Moves4(g.moves.m1, g.moves.m2, g.moves.m3, g.moves.m4),
                (g4a, mv) -> new Group4(g4a.nature, g4a.ability, mv)
        );
        private static final StreamCodec<ByteBuf, Group5> G5_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Group5::currentHp,
                ByteBufCodecs.VAR_INT, Group5::maxHp,
                ByteBufCodecs.STRING_UTF8, Group5::heldItem,
                ByteBufCodecs.VAR_INT, Group5::gender,
                ByteBufCodecs.STRING_UTF8, Group5::possibleAbilities,
                STATS_CODEC, Group5::movePPs,
                Group5::new
        );

        // 顶层 composite：6 个组件 (g1, g2, g3, ivs, evs, g4+g5 合并)
        // 再做一层合并：把 g4 和 g5 合并为 Group45
        private record Group45(Group4 g4, Group5 g5) {}
        private static final StreamCodec<ByteBuf, Group45> G45_CODEC = StreamCodec.composite(
                G4_CODEC, Group45::g4,
                G5_CODEC, Group45::g5,
                Group45::new
        );

        // 现在顶层有 6 个组件：g1, g2, g3, ivs, evs, g45 → 刚好 6 个！
        public static final StreamCodec<ByteBuf, PokemonDataPacket> STREAM_CODEC = StreamCodec.composite(
                G1_CODEC, p -> new Group1(p.slot, p.exists, p.pokemonUUID),
                G2_CODEC, p -> new Group2(p.speciesName, p.displayName, p.formName),
                G3_CODEC, p -> new Group3(p.level, p.experience, p.friendship, p.shiny),
                STATS_CODEC, p -> new Stats6(p.ivHp, p.ivAtk, p.ivDef, p.ivSpA, p.ivSpD, p.ivSpe),
                STATS_CODEC, p -> new Stats6(p.evHp, p.evAtk, p.evDef, p.evSpA, p.evSpD, p.evSpe),
                G45_CODEC, p -> new Group45(
                        new Group4(p.nature, p.ability, new Moves4(p.move1, p.move2, p.move3, p.move4)),
                        new Group5(p.currentHp, p.maxHp, p.heldItem, p.gender, p.possibleAbilities,
                                new Stats6(p.move1PP, p.move2PP, p.move3PP, p.move4PP,
                                        packTwo(p.move1MaxPP, p.move2MaxPP),
                                        packTwo(p.move3MaxPP, p.move4MaxPP)))
                ),
                (g1, g2, g3, iv, ev, g45) -> new PokemonDataPacket(
                        g1.slot, g1.exists, g1.pokemonUUID,
                        g2.speciesName, g2.displayName, g2.formName,
                        g3.level, g3.experience, g3.friendship, g3.shiny,
                        iv.hp, iv.atk, iv.def, iv.spA, iv.spD, iv.spe,
                        ev.hp, ev.atk, ev.def, ev.spA, ev.spD, ev.spe,
                        g45.g4.nature, g45.g4.ability,
                        g45.g4.moves.m1, g45.g4.moves.m2, g45.g4.moves.m3, g45.g4.moves.m4,
                        g45.g5.movePPs.hp, g45.g5.movePPs.atk, g45.g5.movePPs.def, g45.g5.movePPs.spA,
                        unpackFirst(g45.g5.movePPs.spD), unpackSecond(g45.g5.movePPs.spD),
                        unpackFirst(g45.g5.movePPs.spe), unpackSecond(g45.g5.movePPs.spe),
                        g45.g5.currentHp, g45.g5.maxHp, g45.g5.heldItem, g45.g5.gender,
                        g45.g5.possibleAbilities
                )
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ---------- 3. UpdatePokemonPacket (Client→Server) ----------
    // 使用和 PokemonDataPacket 相同的嵌套分组策略
    // movePPs 编码: hp=move1PP, atk=move2PP, def=move3PP, spA=move4PP,
    //              spD=move1MaxPP*1000+move2MaxPP, spe=move3MaxPP*1000+move4MaxPP
    public record UpdatePokemonPacket(int slot, UUID targetPlayerUUID,
                                      String speciesName, String formName,
                                      int level, int experience, String displayName, boolean shiny, int friendship,
                                      int ivHp, int ivAtk, int ivDef, int ivSpA, int ivSpD, int ivSpe,
                                      int evHp, int evAtk, int evDef, int evSpA, int evSpD, int evSpe,
                                      String nature, String ability,
                                      String move1, String move2, String move3, String move4,
                                      int move1PP, int move2PP, int move3PP, int move4PP,
                                      int move1MaxPP, int move2MaxPP, int move3MaxPP, int move4MaxPP,
                                      int currentHp, int maxHp, String heldItem, int gender,
                                      String pokemonUUID, String rawJsonData) implements CustomPacketPayload {
        public static final Type<UpdatePokemonPacket> TYPE = new Type<>(PKT_UPDATE_POKEMON);

        // ---- 内部嵌套分组 ----
        private record UGroup1(int slot, UUID targetPlayerUUID, String pokemonUUID, String rawJsonData) {}
        private record UGroup2(String speciesName, String formName) {}
        private record UGroup3(int level, int experience, String displayName, boolean shiny, int friendship) {}
        private record UGroup4(String nature, String ability, Moves4 moves) {}
        private record UGroup4a(String nature, String ability) {}
        private record UGroup5(int currentHp, int maxHp, String heldItem, int gender, Stats6 movePPs) {}
        private record UGroup45(UGroup4 g4, UGroup5 g5) {}

        private static int packTwoU(int a, int b) { return a * 1000 + b; }
        private static int unpackFirstU(int packed) { return packed / 1000; }
        private static int unpackSecondU(int packed) { return packed % 1000; }

        private static final StreamCodec<ByteBuf, UGroup1> UG1_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, UGroup1::slot,
                UUID_CODEC, UGroup1::targetPlayerUUID,
                ByteBufCodecs.STRING_UTF8, UGroup1::pokemonUUID,
                ByteBufCodecs.STRING_UTF8, UGroup1::rawJsonData,
                UGroup1::new
        );
        private static final StreamCodec<ByteBuf, UGroup2> UG2_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, UGroup2::speciesName,
                ByteBufCodecs.STRING_UTF8, UGroup2::formName,
                UGroup2::new
        );
        private static final StreamCodec<ByteBuf, UGroup3> UG3_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, UGroup3::level,
                ByteBufCodecs.VAR_INT, UGroup3::experience,
                ByteBufCodecs.STRING_UTF8, UGroup3::displayName,
                ByteBufCodecs.BOOL, UGroup3::shiny,
                ByteBufCodecs.VAR_INT, UGroup3::friendship,
                UGroup3::new
        );
        private static final StreamCodec<ByteBuf, UGroup4a> UG4A_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, UGroup4a::nature,
                ByteBufCodecs.STRING_UTF8, UGroup4a::ability,
                UGroup4a::new
        );
        private static final StreamCodec<ByteBuf, UGroup4> UG4_CODEC = StreamCodec.composite(
                UG4A_CODEC, g -> new UGroup4a(g.nature, g.ability),
                MOVES_CODEC, g -> new Moves4(g.moves.m1, g.moves.m2, g.moves.m3, g.moves.m4),
                (g4a, mv) -> new UGroup4(g4a.nature, g4a.ability, mv)
        );
        private static final StreamCodec<ByteBuf, UGroup5> UG5_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, UGroup5::currentHp,
                ByteBufCodecs.VAR_INT, UGroup5::maxHp,
                ByteBufCodecs.STRING_UTF8, UGroup5::heldItem,
                ByteBufCodecs.VAR_INT, UGroup5::gender,
                STATS_CODEC, UGroup5::movePPs,
                UGroup5::new
        );
        private static final StreamCodec<ByteBuf, UGroup45> UG45_CODEC = StreamCodec.composite(
                UG4_CODEC, UGroup45::g4,
                UG5_CODEC, UGroup45::g5,
                UGroup45::new
        );

        // 顶层 composite：6 个组件 (ug1, ug2, ug3, ivs, evs, ug45)
        public static final StreamCodec<ByteBuf, UpdatePokemonPacket> STREAM_CODEC = StreamCodec.composite(
                UG1_CODEC, p -> new UGroup1(p.slot, p.targetPlayerUUID, p.pokemonUUID, p.rawJsonData),
                UG2_CODEC, p -> new UGroup2(p.speciesName, p.formName),
                UG3_CODEC, p -> new UGroup3(p.level, p.experience, p.displayName, p.shiny, p.friendship),
                STATS_CODEC, p -> new Stats6(p.ivHp, p.ivAtk, p.ivDef, p.ivSpA, p.ivSpD, p.ivSpe),
                STATS_CODEC, p -> new Stats6(p.evHp, p.evAtk, p.evDef, p.evSpA, p.evSpD, p.evSpe),
                UG45_CODEC, p -> new UGroup45(
                        new UGroup4(p.nature, p.ability, new Moves4(p.move1, p.move2, p.move3, p.move4)),
                        new UGroup5(p.currentHp, p.maxHp, p.heldItem, p.gender,
                                new Stats6(p.move1PP, p.move2PP, p.move3PP, p.move4PP,
                                        packTwoU(p.move1MaxPP, p.move2MaxPP),
                                        packTwoU(p.move3MaxPP, p.move4MaxPP)))
                ),
                (g1, g2, g3, iv, ev, g45) -> new UpdatePokemonPacket(
                        g1.slot, g1.targetPlayerUUID,
                        g2.speciesName, g2.formName,
                        g3.level, g3.experience, g3.displayName, g3.shiny, g3.friendship,
                        iv.hp, iv.atk, iv.def, iv.spA, iv.spD, iv.spe,
                        ev.hp, ev.atk, ev.def, ev.spA, ev.spD, ev.spe,
                        g45.g4.nature, g45.g4.ability,
                        g45.g4.moves.m1, g45.g4.moves.m2, g45.g4.moves.m3, g45.g4.moves.m4,
                        g45.g5.movePPs.hp, g45.g5.movePPs.atk, g45.g5.movePPs.def, g45.g5.movePPs.spA,
                        unpackFirstU(g45.g5.movePPs.spD), unpackSecondU(g45.g5.movePPs.spD),
                        unpackFirstU(g45.g5.movePPs.spe), unpackSecondU(g45.g5.movePPs.spe),
                        g45.g5.currentHp, g45.g5.maxHp, g45.g5.heldItem, g45.g5.gender,
                        g1.pokemonUUID, g1.rawJsonData
                )
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ---------- 4. DeletePokemonPacket (Client→Server) ----------
    public record DeletePokemonPacket(int slot, UUID targetPlayerUUID) implements CustomPacketPayload {
        public static final Type<DeletePokemonPacket> TYPE = new Type<>(PKT_DELETE_POKEMON);
        public static final StreamCodec<ByteBuf, DeletePokemonPacket> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, DeletePokemonPacket::slot,
                ByteBufCodecs.stringUtf8(36).map(s -> s.isEmpty() ? null : UUID.fromString(s), u -> u == null ? "" : u.toString()), DeletePokemonPacket::targetPlayerUUID,
                DeletePokemonPacket::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // =====================================================================
    // 服务端工具：打开编辑器 + 发送每个槽位的宝可梦数据
    // =====================================================================
    public static void sendOpenEditorPacket(ServerPlayer clientPlayer, UUID targetUUID, String targetName) {
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
                    data.ivHp, data.ivAtk, data.ivDef, data.ivSpA, data.ivSpD, data.ivSpe,
                    data.evHp, data.evAtk, data.evDef, data.evSpA, data.evSpD, data.evSpe,
                    data.nature, data.ability,
                    data.move1, data.move2, data.move3, data.move4,
                    data.move1PP, data.move2PP, data.move3PP, data.move4PP,
                    data.move1MaxPP, data.move2MaxPP, data.move3MaxPP, data.move4MaxPP,
                    data.currentHp, data.maxHp, data.heldItem, data.gender,
                    String.join(";", data.possibleAbilities)
            ));
        }
    }

    // =====================================================================
    // 服务端：处理 Client→Server 的 UpdatePokemonPacket
    // =====================================================================
    public static void handleUpdatePokemon(UpdatePokemonPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                ServerPlayer serverPlayer = (ServerPlayer) ctx.player();
                if (!PokemonEditorItem.checkPermission(serverPlayer)) {
                    serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("你没有权限修改宝可梦！"));
                    return;
                }
                UUID target = pkt.targetPlayerUUID() != null ? pkt.targetPlayerUUID() : serverPlayer.getUUID();
                PokemonEditorDebug.log("handleUpdatePokemon() - slot=" + pkt.slot() + " target=" + target + " species=" + pkt.speciesName());

                // 特殊情况：物种名为空 → 视为删除该槽位（清空）
                if (pkt.speciesName() == null || pkt.speciesName().isEmpty()) {
                    CobblemonBridge.deletePokemonFromSlot(serverPlayer, target, pkt.slot());
                    // 删除后也回发数据，客户端刷新为空槽位
                    PokemonData empty = CobblemonBridge.readPokemonFromSlot(serverPlayer, target, pkt.slot());
                    if (empty == null) empty = new PokemonData();
                    UUID uuid = empty.pokemonUUID;
                    PacketDistributor.sendToPlayer(serverPlayer, new PokemonDataPacket(
                            pkt.slot(), empty.exists, uuid,
                            empty.speciesName, empty.displayName, empty.formName,
                            empty.level, empty.experience, empty.friendship, empty.shiny,
                            empty.ivHp, empty.ivAtk, empty.ivDef, empty.ivSpA, empty.ivSpD, empty.ivSpe,
                            empty.evHp, empty.evAtk, empty.evDef, empty.evSpA, empty.evSpD, empty.evSpe,
                            empty.nature, empty.ability,
                            empty.move1, empty.move2, empty.move3, empty.move4,
                            empty.move1PP, empty.move2PP, empty.move3PP, empty.move4PP,
                            empty.move1MaxPP, empty.move2MaxPP, empty.move3MaxPP, empty.move4MaxPP,
                            empty.currentHp, empty.maxHp, empty.heldItem, empty.gender,
                            String.join(";", empty.possibleAbilities)
                    ));
                    serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("[宝可梦编辑器] 已删除队伍 " + (pkt.slot() + 1)));
                    return;
                }

                // 正常情况：写入/更新宝可梦数据
                CobblemonBridge.writePokemonToSlot(serverPlayer, target, pkt);

                // ★ 关键修复：写入后重新从宝可梦对象读取，回发给客户端，GUI 即时刷新
                PokemonData refreshed = CobblemonBridge.readPokemonFromSlot(serverPlayer, target, pkt.slot());
                if (refreshed == null) refreshed = new PokemonData();
                UUID rUuid = refreshed.pokemonUUID;
                PacketDistributor.sendToPlayer(serverPlayer, new PokemonDataPacket(
                        pkt.slot(), refreshed.exists, rUuid,
                        refreshed.speciesName, refreshed.displayName, refreshed.formName,
                        refreshed.level, refreshed.experience, refreshed.friendship, refreshed.shiny,
                        refreshed.ivHp, refreshed.ivAtk, refreshed.ivDef, refreshed.ivSpA, refreshed.ivSpD, refreshed.ivSpe,
                        refreshed.evHp, refreshed.evAtk, refreshed.evDef, refreshed.evSpA, refreshed.evSpD, refreshed.evSpe,
                        refreshed.nature, refreshed.ability,
                        refreshed.move1, refreshed.move2, refreshed.move3, refreshed.move4,
                        refreshed.move1PP, refreshed.move2PP, refreshed.move3PP, refreshed.move4PP,
                        refreshed.move1MaxPP, refreshed.move2MaxPP, refreshed.move3MaxPP, refreshed.move4MaxPP,
                        refreshed.currentHp, refreshed.maxHp, refreshed.heldItem, refreshed.gender,
                        String.join(";", refreshed.possibleAbilities)
                ));

                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("[宝可梦编辑器] 已保存队伍 " + (pkt.slot() + 1)));
            } catch (Exception e) {
                PokemonEditorDebug.logError("handleUpdatePokemon() error: " + e.getMessage(), e);
                if (ctx.player() instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("[宝可梦编辑器] 保存失败：" + e.getMessage()));
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
                ServerPlayer serverPlayer = (ServerPlayer) ctx.player();
                // 权限检查
                if (!PokemonEditorItem.checkPermission(serverPlayer)) {
                    serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("你没有权限删除宝可梦！"));
                    return;
                }
                // 确定目标玩家 UUID
                UUID target = pkt.targetPlayerUUID() != null ? pkt.targetPlayerUUID() : serverPlayer.getUUID();
                PokemonEditorDebug.log("handleDeletePokemon() - slot=" + pkt.slot() + " target=" + target);

                // 执行删除（清空槽位）
                CobblemonBridge.deletePokemonFromSlot(serverPlayer, target, pkt.slot());

                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("[宝可梦编辑器] 已删除队伍 " + (pkt.slot() + 1)));
            } catch (Exception e) {
                PokemonEditorDebug.logError("handleDeletePokemon() error: " + e.getMessage(), e);
            }
        });
    }
}