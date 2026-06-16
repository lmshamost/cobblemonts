package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.gui.PokebagContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenPokebagPacket() implements CustomPacketPayload {
    public static final Type<OpenPokebagPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "open_pokebag"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPokebagPacket> STREAM_CODEC =
            StreamCodec.unit(new OpenPokebagPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServerSide(OpenPokebagPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                sp.openMenu(new SimpleMenuProvider(
                        (containerId, inv, p) -> new PokebagContainer(containerId, inv),
                        PokebagContainer.TITLE
                ));
            }
        });
    }
}