package com.cobblemon_ts.mod.pokemoneditor;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@OnlyIn(Dist.CLIENT)
public class PokemonEditorClientHandler {

    public static void handleOpenEditor(PokemonEditorNetwork.OpenEditorPacket packet, IPayloadContext context) {
        PokemonEditorDebug.log("CLIENT handleOpenEditor() - player=" + packet.playerName()
                + " partySize=" + packet.partySize());
        context.enqueueWork(() -> {
            PokemonEditorScreen screen = new PokemonEditorScreen(
                    packet.playerUUID(),
                    packet.playerName(),
                    packet.partySize()
            );
            Minecraft.getInstance().setScreen(screen);
            PokemonEditorDebug.log("CLIENT PokemonEditorScreen opened");
        });
    }

    public static void handlePokemonData(PokemonEditorNetwork.PokemonDataPacket packet, IPayloadContext context) {
        PokemonEditorDebug.log("CLIENT handlePokemonData() slot=" + packet.slot()
                + " exists=" + packet.exists() + " species=" + packet.speciesName());
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof PokemonEditorScreen editorScreen) {
                editorScreen.updatePokemonData(packet);
            } else {
                PokemonEditorDebug.log("CLIENT handlePokemonData() - screen is not PokemonEditorScreen, ignoring");
            }
        });
    }
}