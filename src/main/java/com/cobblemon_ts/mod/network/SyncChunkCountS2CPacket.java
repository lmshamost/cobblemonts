package com.cobblemon_ts.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// [线性-全局计数] 服务端返回玩家全服区块禁止器计数 S→C
public record SyncChunkCountS2CPacket(int banTotal, int forceTotal) implements CustomPayload {

    public static final Id<SyncChunkCountS2CPacket> TYPE = new Id<>(
            Identifier.of("cobblemon_ts", "sync_chunk_count"));

    public static final PacketCodec<RegistryByteBuf, SyncChunkCountS2CPacket> STREAM_CODEC = new PacketCodec<>() {
        @Override
        public SyncChunkCountS2CPacket decode(RegistryByteBuf buf) {
            return new SyncChunkCountS2CPacket(buf.readVarInt(), buf.readVarInt());
        }

        @Override
        public void encode(RegistryByteBuf buf, SyncChunkCountS2CPacket packet) {
            buf.writeVarInt(packet.banTotal());
            buf.writeVarInt(packet.forceTotal());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    // [线性-全局计数] volatile 单次消费：屏幕 init 时 reset，响应到达后写入，pollLatest() 轮询消费
    private static volatile int[] latest = null;

    public static void resetForNewRequest() {
        latest = null;
    }

    public static int[] pollLatest() {
        return latest;
    }

    public static void handleClientSide(SyncChunkCountS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            latest = new int[]{packet.banTotal(), packet.forceTotal()};
        });
    }

    public static void clearCache() {
        latest = null;
    }
}