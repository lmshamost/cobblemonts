package com.cobblemon_ts.mod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncChunkCountS2CPacket(int banTotal, int forceTotal) implements CustomPacketPayload {

    public static final Type<SyncChunkCountS2CPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("cobblemon_ts", "sync_chunk_count"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncChunkCountS2CPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SyncChunkCountS2CPacket::banTotal,
                    ByteBufCodecs.VAR_INT, SyncChunkCountS2CPacket::forceTotal,
                    SyncChunkCountS2CPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // [线性-全局计数] volatile 单次消费：屏幕 init 时 reset，响应到达后写入，tick() 轮询消费
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