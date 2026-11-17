package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.gui.PokebagContainer;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.text.Text;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;

public record OpenPokebagPacket() implements CustomPayload {
    public static final Id<OpenPokebagPacket> TYPE = new Id<>(
            Identifier.of("cobblemon_ts", "open_pokebag"));

    public static final PacketCodec<RegistryByteBuf, OpenPokebagPacket> STREAM_CODEC =
            PacketCodec.unit(new OpenPokebagPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    public static void handleServerSide(OpenPokebagPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                        (containerId, inv, p) -> new PokebagContainer(containerId, inv, p),
                        PokebagContainer.TITLE
                ));
            }
        });
    }
}