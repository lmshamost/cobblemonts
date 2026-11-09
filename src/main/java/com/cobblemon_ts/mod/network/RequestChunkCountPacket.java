package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record RequestChunkCountPacket(UUID ownerId) implements CustomPacketPayload {
    public static final Type<RequestChunkCountPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("cobblemon_ts", "request_chunk_count"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestChunkCountPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RequestChunkCountPacket decode(RegistryFriendlyByteBuf buf) {
            return new RequestChunkCountPacket(buf.readUUID());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, RequestChunkCountPacket p) {
            buf.writeUUID(p.ownerId());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServerSide(RequestChunkCountPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                int[] counts = CobblemonTS.countPlayerChunks(packet.ownerId(), null);
                PacketDistributor.sendToPlayer(sp, new SyncChunkCountS2CPacket(counts[0], counts[1]));
            }
        });
    }
}