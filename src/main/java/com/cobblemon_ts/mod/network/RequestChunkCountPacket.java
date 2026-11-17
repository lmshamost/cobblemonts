package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.UUID;

// [线性-全局计数] 客户端请求玩家全服区块禁止器计数 C→S
public record RequestChunkCountPacket(UUID ownerId) implements CustomPayload {
    public static final Id<RequestChunkCountPacket> TYPE = new Id<>(
            Identifier.of("cobblemon_ts", "request_chunk_count"));

    public static final PacketCodec<RegistryByteBuf, RequestChunkCountPacket> STREAM_CODEC = new PacketCodec<>() {
        @Override
        public RequestChunkCountPacket decode(RegistryByteBuf buf) {
            return new RequestChunkCountPacket(buf.readUuid());
        }

        @Override
        public void encode(RegistryByteBuf buf, RequestChunkCountPacket p) {
            buf.writeUuid(p.ownerId());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    public static void handleServerSide(RequestChunkCountPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayerEntity sp) {
                int[] counts = CobblemonTS.countPlayerChunks(packet.ownerId(), null);
                PacketDistributor.sendToPlayer(sp, new SyncChunkCountS2CPacket(counts[0], counts[1]));
            }
        });
    }
}