package com.cobblemon_ts.mod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiHealthFoodMixin {

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "renderHealthLevel", at = @At("HEAD"), cancellable = true)
    private void onRenderHealthLevel(GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel();

        Player player = (Player) minecraft.getCameraEntity();
        if (player == null) return;

        Font font = minecraft.font;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int barWidth = 81;
        int barHeight = 9;

        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float ratio = currentHealth / maxHealth;
        if (ratio < 0f) ratio = 0f;
        if (ratio > 1f) ratio = 1f;

        int barX = screenWidth / 2 - 91;
        int barY = screenHeight - 39;

        drawBar(guiGraphics, barX, barY, barWidth, barHeight, ratio, 0xFFFF3333);
        String hpText = String.format("%d/%d", (int) currentHealth, (int) maxHealth);
        int hpTextX = barX + (barWidth - font.width(hpText)) / 2;
        guiGraphics.drawString(font, hpText, hpTextX, barY - 10, 0xFFFFFFFF, true);
    }

    @Inject(method = "renderFoodLevel", at = @At("HEAD"), cancellable = true)
    private void onRenderFoodLevel(GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel();

        Player player = (Player) minecraft.getCameraEntity();
        if (player == null) return;

        Font font = minecraft.font;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int barWidth = 81;
        int barHeight = 9;

        int currentFood = player.getFoodData().getFoodLevel();
        int maxFood = 20;
        float foodRatio = (float) currentFood / (float) maxFood;
        if (foodRatio < 0f) foodRatio = 0f;
        if (foodRatio > 1f) foodRatio = 1f;

        int barX = screenWidth / 2 + 10;
        int barY = screenHeight - 39;

        drawBar(guiGraphics, barX, barY, barWidth, barHeight, foodRatio, 0xFFE6B800);
        String foodText = String.format("%d/%d", currentFood, maxFood);
        int foodTextX = barX + (barWidth - font.width(foodText)) / 2;
        guiGraphics.drawString(font, foodText, foodTextX, barY - 10, 0xFFFFFFFF, true);
    }

    private static void drawBar(GuiGraphics g, int x, int y, int width, int height, float ratio, int color) {
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF000000);
        g.fill(x, y, x + width, y + height, 0xFF333333);
        int fillWidth = (int) (width * ratio);
        g.fill(x, y, x + fillWidth, y + height, color);
    }
}