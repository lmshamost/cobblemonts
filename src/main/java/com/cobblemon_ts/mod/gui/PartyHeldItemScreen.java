package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.lang.reflect.Method;

public class PartyHeldItemScreen extends AbstractContainerScreen<PartyHeldItemContainer> {

    private static boolean iconReflectionReady;
    private static Method getIconMethod;
    private static Method guiBlitkMethod;

    public PartyHeldItemScreen(PartyHeldItemContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 133;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        drawPokemonIcons(guiGraphics);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xC0101010);
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 13, 0xC0000000);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    private void drawPokemonIcons(GuiGraphics guiGraphics) {
        if (!iconReflectionReady) setupIconReflection();
        if (!iconReflectionReady || minecraft == null || minecraft.player == null) return;

        try {
            var cobblemonInstance = Class.forName("com.cobblemon.mod.common.Cobblemon")
                    .getField("INSTANCE").get(null);
            var storage = cobblemonInstance.getClass().getMethod("getStorage").invoke(cobblemonInstance);
            var party = storage.getClass().getMethod("getParty",
                    Class.forName("net.minecraft.world.entity.player.Player"))
                    .invoke(storage, minecraft.player);
            if (party == null) return;

            var sizeMethod = party.getClass().getMethod("size");
            var getMethod = party.getClass().getMethod("get", int.class);
            int size = (Integer) sizeMethod.invoke(party);

            for (int i = 0; i < Math.min(size, 6); i++) {
                var pokemon = getMethod.invoke(party, i);
                if (pokemon == null) continue;

                try {
                    Object icon = getIconMethod.invoke(pokemon);
                    if (icon instanceof ResourceLocation iconRL) {
                        int sx = leftPos + 80 + i * 20 + 2;
                        int sy = topPos + 20 + 1;
                        guiBlitkMethod.invoke(null, guiGraphics.pose(), iconRL, sx, sy);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            CobblemonTS.LOGGER.warn("[HeldItemScreen] drawIcons failed: {}", e.toString());
        }
    }

    private static void setupIconReflection() {
        try {
            Class<?> pokemonClass = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
            getIconMethod = pokemonClass.getMethod("getIcon");
            Class<?> guiUtilsClass = Class.forName("com.cobblemon.mod.common.api.gui.GuiUtilsKt");
            guiBlitkMethod = guiUtilsClass.getMethod("blitk",
                    com.mojang.blaze3d.vertex.PoseStack.class,
                    ResourceLocation.class,
                    Number.class, Number.class);
            iconReflectionReady = true;
        } catch (Exception e) {
            CobblemonTS.LOGGER.warn("[HeldItemScreen] Icon reflection failed: {}", e.getMessage());
        }
    }
}