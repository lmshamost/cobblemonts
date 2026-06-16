package com.cobblemon_ts.mod.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PokebagScreen extends AbstractContainerScreen<PokebagContainer> {
    private static final ResourceLocation CHEST_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

    public PokebagScreen(PokebagContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        int bagSlots = container.getBagInventory().getContainerSize();
        int rows = Math.max(1, (bagSlots + 8) / 9);
        this.imageHeight = 114 + rows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int bagSlots = this.menu.getBagInventory().getContainerSize();
        int rows = (bagSlots + 8) / 9;

        guiGraphics.blit(CHEST_TEXTURE, x, y, 0, 0, this.imageWidth, rows * 18 + 17);
        guiGraphics.blit(CHEST_TEXTURE, x, y + rows * 18 + 17, 0, 125, this.imageWidth, 97);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}