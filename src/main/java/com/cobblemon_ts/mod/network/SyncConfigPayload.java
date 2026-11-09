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

public record SyncConfigPayload(BlockPos pos, Map<Long, Byte> rules) implements CustomPacketPayload {

    public static final Type<SyncConfigPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("cobblemon_ts", "chunk_barrier_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncConfigPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SyncConfigPayload decode(RegistryFriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            int size = buf.readVarInt();
            Map<Long, Byte> rules = new HashMap<>();
            for (int i = 0; i < size; i++) {
                rules.put(buf.readLong(), buf.readByte());
            }
            return new SyncConfigPayload(pos, rules);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SyncConfigPayload p) {
            buf.writeBlockPos(p.pos());
            buf.writeVarInt(p.rules().size());
            for (var e : p.rules().entrySet()) {
                buf.writeLong(e.getKey());
                buf.writeByte(e.getValue());
            }
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
                ChunkBarrierConfig newConfig = new ChunkBarrierConfig();
                newConfig.chunkRules.putAll(packet.rules());
                be.setConfig(newConfig);
            }
        });
    }
}