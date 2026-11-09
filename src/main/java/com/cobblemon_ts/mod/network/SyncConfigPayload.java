package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity;
import com.cobblemon_ts.mod.block.ChunkBarrierConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record SyncConfigPayload(BlockPos pos, Map<Long, Byte> rules, Map<String, Boolean> playerList, Map<String, Boolean> luckyPlayerList, boolean banLuckyBlock) implements CustomPacketPayload {

    public static final Type<SyncConfigPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("cobblemon_ts", "chunk_barrier_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncConfigPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SyncConfigPayload decode(RegistryFriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            int ruleSize = buf.readVarInt();
            Map<Long, Byte> rules = new HashMap<>();
            for (int i = 0; i < ruleSize; i++) {
                rules.put(buf.readLong(), buf.readByte());
            }
            int playerSize = buf.readVarInt();
            Map<String, Boolean> playerList = new HashMap<>();
            for (int i = 0; i < playerSize; i++) {
                playerList.put(buf.readUtf(), buf.readBoolean());
            }
            int luckySize = buf.readVarInt();
            Map<String, Boolean> luckyPlayerList = new HashMap<>();
            for (int i = 0; i < luckySize; i++) {
                luckyPlayerList.put(buf.readUtf(), buf.readBoolean());
            }
            boolean banLuckyBlock = buf.readBoolean();
            return new SyncConfigPayload(pos, rules, playerList, luckyPlayerList, banLuckyBlock);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SyncConfigPayload p) {
            buf.writeBlockPos(p.pos());
            buf.writeVarInt(p.rules().size());
            for (var e : p.rules().entrySet()) {
                buf.writeLong(e.getKey());
                buf.writeByte(e.getValue());
            }
            buf.writeVarInt(p.playerList().size());
            for (var e : p.playerList().entrySet()) {
                buf.writeUtf(e.getKey());
                buf.writeBoolean(e.getValue());
            }
            buf.writeVarInt(p.luckyPlayerList().size());
            for (var e : p.luckyPlayerList().entrySet()) {
                buf.writeUtf(e.getKey());
                buf.writeBoolean(e.getValue());
            }
            buf.writeBoolean(p.banLuckyBlock());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServerSide(SyncConfigPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) return;
            if (sp.level().getBlockEntity(packet.pos()) instanceof ChunkBarrierBlockEntity be) {
                // [线性-权限] 只有 owner 能改 BE 配置，服务端强校验
                if (be.getOwnerId() != null
                        && !sp.getUUID().equals(be.getOwnerId())
                        && !sp.getServer().getPlayerList().isOp(sp.getGameProfile())) {
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