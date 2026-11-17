package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.SecurityHelper;
import com.cobblemon_ts.mod.pokemoneditor.CobblemonBridge;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.text.Text;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;

public record ReleasePokemonPacket(int slot) implements CustomPayload {

    public static final Id<ReleasePokemonPacket> TYPE = new Id<>(
            Identifier.of("cobblemon_ts", "release_pokemon"));

    public static final PacketCodec<RegistryByteBuf, ReleasePokemonPacket> STREAM_CODEC = new PacketCodec<>() {
        @Override
        public ReleasePokemonPacket decode(RegistryByteBuf buf) {
            return new ReleasePokemonPacket(buf.readInt());
        }

        @Override
        public void encode(RegistryByteBuf buf, ReleasePokemonPacket packet) {
            buf.writeInt(packet.slot());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    public static void handleServerSide(ReleasePokemonPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                ServerPlayerEntity player = (ServerPlayerEntity) ctx.player();
                // [线性-SEC] 权限校验：仅创造模式或OP
                if (!SecurityHelper.isTrusted(player)) {
                    CobblemonTS.LOGGER.warn("[Security] Unauthorized ReleasePokemon from {}",
                            player.getName().getString());
                    return;
                }
                CobblemonTS.LOGGER.info("[Release] RECEIVED slot={} player={} uuid={}",
                        pkt.slot(), player.getName().getString(), player.getUuid());
                CobblemonTS.LOGGER.info("[Release] → calling CobblemonBridge.deletePokemonFromSlot(player, null, {})", pkt.slot());
                CobblemonBridge.deletePokemonFromSlot(player, null, pkt.slot());
                CobblemonTS.LOGGER.info("[Release] deletePokemonFromSlot returned successfully");
                player.sendMessage(
                        Text.literal("已放生精灵"), false);
            } catch (Exception e) {
                CobblemonTS.LOGGER.error("[Release] FAILED: " + e.getClass().getName() + ": " + e.getMessage(), e);
            }
        });
    }
}