package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity;
import com.cobblemon_ts.mod.block.ChunkBarrierConfig;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

// [线性-R10] 区块禁止器配置同步包：客户端→服务端，保存整个配置
public record SyncConfigPayload(BlockPos pos, Map<Long, Byte> rules, Map<String, Boolean> playerList, Map<String, Boolean> luckyPlayerList, boolean banLuckyBlock) implements CustomPayload {

    public static final Id<SyncConfigPayload> TYPE = new Id<>(
            Identifier.of("cobblemon_ts", "chunk_barrier_sync"));

    public static final PacketCodec<RegistryByteBuf, SyncConfigPayload> STREAM_CODEC = new PacketCodec<>() {
        @Override
        public SyncConfigPayload decode(RegistryByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            int ruleSize = buf.readVarInt();
            Map<Long, Byte> rules = new HashMap<>();
            for (int i = 0; i < ruleSize; i++) {
                rules.put(buf.readLong(), buf.readByte());
            }
            int playerSize = buf.readVarInt();
            Map<String, Boolean> playerList = new HashMap<>();
            for (int i = 0; i < playerSize; i++) {
                playerList.put(buf.readString(), buf.readBoolean());
            }
            int luckySize = buf.readVarInt();
            Map<String, Boolean> luckyPlayerList = new HashMap<>();
            for (int i = 0; i < luckySize; i++) {
                luckyPlayerList.put(buf.readString(), buf.readBoolean());
            }
            boolean banLuckyBlock = buf.readBoolean();
            return new SyncConfigPayload(pos, rules, playerList, luckyPlayerList, banLuckyBlock);
        }

        @Override
        public void encode(RegistryByteBuf buf, SyncConfigPayload p) {
            buf.writeBlockPos(p.pos());
            buf.writeVarInt(p.rules().size());
            for (var e : p.rules().entrySet()) {
                buf.writeLong(e.getKey());
                buf.writeByte(e.getValue());
            }
            buf.writeVarInt(p.playerList().size());
            for (var e : p.playerList().entrySet()) {
                buf.writeString(e.getKey());
                buf.writeBoolean(e.getValue());
            }
            buf.writeVarInt(p.luckyPlayerList().size());
            for (var e : p.luckyPlayerList().entrySet()) {
                buf.writeString(e.getKey());
                buf.writeBoolean(e.getValue());
            }
            buf.writeBoolean(p.banLuckyBlock());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    public static void handleServerSide(SyncConfigPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayerEntity sp)) return;
            if (sp.getWorld().getBlockEntity(packet.pos()) instanceof ChunkBarrierBlockEntity be) {
                // [线性-权限] 只有 owner 能改 BE 配置，服务端强校验
                if (be.getOwnerId() != null
                        && !sp.getUuid().equals(be.getOwnerId())
                        && !sp.getServer().getPlayerManager().isOperator(sp.getGameProfile())) {
                    return;
                }
                ChunkBarrierConfig newConfig = new ChunkBarrierConfig();
                newConfig.chunkRules.putAll(packet.rules());
                newConfig.playerWhitelist.putAll(packet.playerList());
                newConfig.luckyPlayerWhitelist.putAll(packet.luckyPlayerList());
                newConfig.banLuckyBlock = packet.banLuckyBlock();
                be.setConfig(newConfig);
            }
        });
    }
}