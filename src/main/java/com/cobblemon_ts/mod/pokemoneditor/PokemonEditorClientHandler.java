package com.cobblemon_ts.mod.pokemoneditor;

import com.cobblemon_ts.mod.network.IPayloadContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;


import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
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
            MinecraftClient.getInstance().setScreen(screen);
            PokemonEditorDebug.log("CLIENT PokemonEditorScreen opened");
        });
    }

    public static void handlePokemonData(PokemonEditorNetwork.PokemonDataPacket packet, IPayloadContext context) {
        PokemonEditorDebug.log("CLIENT handlePokemonData() slot=" + packet.slot()
                + " exists=" + packet.exists() + " species=" + packet.speciesName());
        context.enqueueWork(() -> {
            if (MinecraftClient.getInstance().currentScreen instanceof PokemonEditorScreen editorScreen) {
                editorScreen.updatePokemonData(packet);
            } else {
                PokemonEditorDebug.log("CLIENT handlePokemonData() - screen is not PokemonEditorScreen, ignoring");
            }
        });
    }
}