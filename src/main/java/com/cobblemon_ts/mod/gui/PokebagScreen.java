package com.cobblemon_ts.mod.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.entity.player.PlayerInventory;

public class PokebagScreen extends HandledScreen<PokebagContainer> {
    private static final Identifier CHEST_TEXTURE =
            Identifier.of("minecraft", "textures/gui/container/generic_54.png");

    public PokebagScreen(PokebagContainer container, PlayerInventory inv, Text title) {
        super(container, inv, title);
        int bagSlots = container.getBagInventory().size();
        int rows = Math.max(1, (bagSlots + 8) / 9);
        this.backgroundHeight = 114 + rows * 18;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    public void render(DrawContext DrawContext, int mouseX, int mouseY, float partialTick) {
        super.render(DrawContext, mouseX, mouseY, partialTick);
    }

    @Override
    protected void drawBackground(DrawContext DrawContext, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        int bagSlots = this.getScreenHandler().getBagInventory().size();
        int rows = (bagSlots + 8) / 9;

        DrawContext.drawTexture(CHEST_TEXTURE, x, y, 0, 0, this.backgroundWidth, rows * 18 + 17);
        DrawContext.drawTexture(CHEST_TEXTURE, x, y + rows * 18 + 17, 0, 125, this.backgroundWidth, 97);
    }

    @Override
    protected void drawForeground(DrawContext DrawContext, int mouseX, int mouseY) {
        DrawContext.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);
        DrawContext.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);
    }
}