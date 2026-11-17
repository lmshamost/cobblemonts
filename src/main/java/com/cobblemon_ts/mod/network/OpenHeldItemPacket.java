package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.gui.PartyHeldItemContainer;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.text.Text;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;

public record OpenHeldItemPacket() implements CustomPayload {
    public static final Id<OpenHeldItemPacket> TYPE = new Id<>(
            Identifier.of("cobblemon_ts", "open_held_item"));

    public static final PacketCodec<RegistryByteBuf, OpenHeldItemPacket> STREAM_CODEC =
            PacketCodec.unit(new OpenHeldItemPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    public static void handleServerSide(OpenHeldItemPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                        (containerId, inv, p) -> new PartyHeldItemContainer(containerId, inv),
                        PartyHeldItemContainer.TITLE
                ));
            }
        });
    }
}