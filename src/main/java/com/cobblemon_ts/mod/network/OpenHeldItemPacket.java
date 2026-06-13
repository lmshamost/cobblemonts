package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.gui.PartyHeldItemContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenHeldItemPacket() implements CustomPacketPayload {
    public static final Type<OpenHeldItemPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "open_held_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenHeldItemPacket> STREAM_CODEC =
            StreamCodec.unit(new OpenHeldItemPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServerSide(OpenHeldItemPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                sp.openMenu(new SimpleMenuProvider(
                        (containerId, inv, p) -> new PartyHeldItemContainer(containerId, inv),
                        PartyHeldItemContainer.TITLE
                ));
            }
        });
    }
}