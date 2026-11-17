package com.cobblemon_ts.mod.gui;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// [线性-R10] 区块禁止器屏幕打开时的扩展同步数据
public record ChunkBarrierSyncData(
        BlockPos pos,
        Map<Long, Byte> rules,
        UUID ownerId,
        String ownerName,
        boolean banLuckyBlock,
        boolean banShinyBlock,
        boolean banLegendaryBlock,
        boolean banUltraBlock,
        Map<String, Boolean> playerWhitelist,
        Map<String, Boolean> luckyPlayerWhitelist
) {
    public static final PacketCodec<RegistryByteBuf, ChunkBarrierSyncData> PACKET_CODEC = new PacketCodec<>() {
        @Override
        public ChunkBarrierSyncData decode(RegistryByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            int ruleSize = buf.readVarInt();
            Map<Long, Byte> rules = new HashMap<>();
            for (int i = 0; i < ruleSize; i++) {
                rules.put(buf.readLong(), buf.readByte());
            }
            UUID ownerId = buf.readUuid();
            String ownerName = buf.readString();
            boolean banLuckyBlock = buf.readBoolean();
            boolean banShinyBlock = buf.readBoolean();
            boolean banLegendaryBlock = buf.readBoolean();
            boolean banUltraBlock = buf.readBoolean();
            int ps = buf.readVarInt();
            Map<String, Boolean> playerWhitelist = new HashMap<>();
            for (int i = 0; i < ps; i++) {
                playerWhitelist.put(buf.readString(), buf.readBoolean());
            }
            int lps = buf.readVarInt();
            Map<String, Boolean> luckyPlayerWhitelist = new HashMap<>();
            for (int i = 0; i < lps; i++) {
                luckyPlayerWhitelist.put(buf.readString(), buf.readBoolean());
            }
            return new ChunkBarrierSyncData(pos, rules, ownerId, ownerName,
                    banLuckyBlock, banShinyBlock, banLegendaryBlock, banUltraBlock,
                    playerWhitelist, luckyPlayerWhitelist);
        }

        @Override
        public void encode(RegistryByteBuf buf, ChunkBarrierSyncData data) {
            buf.writeBlockPos(data.pos());
            buf.writeVarInt(data.rules().size());
            for (var e : data.rules().entrySet()) {
                buf.writeLong(e.getKey());
                buf.writeByte(e.getValue());
            }
            buf.writeUuid(data.ownerId() != null ? data.ownerId() : new UUID(0, 0));
            buf.writeString(data.ownerName() != null ? data.ownerName() : "");
            buf.writeBoolean(data.banLuckyBlock());
            buf.writeBoolean(data.banShinyBlock());
            buf.writeBoolean(data.banLegendaryBlock());
            buf.writeBoolean(data.banUltraBlock());
            buf.writeVarInt(data.playerWhitelist().size());
            for (var e : data.playerWhitelist().entrySet()) {
                buf.writeString(e.getKey());
                buf.writeBoolean(e.getValue());
            }
            buf.writeVarInt(data.luckyPlayerWhitelist().size());
            for (var e : data.luckyPlayerWhitelist().entrySet()) {
                buf.writeString(e.getKey());
                buf.writeBoolean(e.getValue());
            }
        }
    };
}