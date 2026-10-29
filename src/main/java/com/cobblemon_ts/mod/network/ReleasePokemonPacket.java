package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.SecurityHelper;
import com.cobblemon_ts.mod.pokemoneditor.CobblemonBridge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ReleasePokemonPacket(int slot) implements CustomPacketPayload {

    public static final Type<ReleasePokemonPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "release_pokemon"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReleasePokemonPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ReleasePokemonPacket decode(RegistryFriendlyByteBuf buf) {
            return new ReleasePokemonPacket(buf.readInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ReleasePokemonPacket packet) {
            buf.writeInt(packet.slot());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServerSide(ReleasePokemonPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                ServerPlayer player = (ServerPlayer) ctx.player();
                // [线性-SEC] 权限校验：仅创造模式或OP
                if (!SecurityHelper.isTrusted(player)) {
                    CobblemonTS.LOGGER.warn("[Security] Unauthorized ReleasePokemon from {}",
                            player.getName().getString());
                    return;
                }
                CobblemonTS.LOGGER.info("[Release] RECEIVED slot={} player={} uuid={}",
                        pkt.slot(), player.getName().getString(), player.getUUID());
                CobblemonTS.LOGGER.info("[Release] → calling CobblemonBridge.deletePokemonFromSlot(player, null, {})", pkt.slot());
                CobblemonBridge.deletePokemonFromSlot(player, null, pkt.slot());
                CobblemonTS.LOGGER.info("[Release] deletePokemonFromSlot returned successfully");
                player.displayClientMessage(
                        Component.literal("已放生精灵"), false);
            } catch (Exception e) {
                CobblemonTS.LOGGER.error("[Release] FAILED: " + e.getClass().getName() + ": " + e.getMessage(), e);
            }
        });
    }
}