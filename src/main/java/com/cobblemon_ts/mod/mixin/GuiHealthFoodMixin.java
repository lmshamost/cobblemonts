package com.cobblemon_ts.mod.mixin;

import com.cobblemon_ts.mod.CobblemonTSConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// [线性-L05] 自定义HUD：血条+吸收心金条覆盖、饥饿条+饱腹金条覆盖、盔甲条位置修正
@Mixin(Gui.class)
public abstract class GuiHealthFoodMixin {

    @Shadow @Final private Minecraft minecraft;

    // ================================================================
    // [线性-L05a] 血条渲染：红色基底 + 金色吸收心覆盖层
    // ================================================================
    @Inject(method = "renderHealthLevel", at = @At("HEAD"), cancellable = true)
    private void onRenderHealthLevel(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (!CobblemonTSConfig.HUD_CUSTOM_HEALTH_BAR.get()) return; // 配置关闭→原版渲染
        ci.cancel();

        Player player = (Player) minecraft.getCameraEntity();
        if (player == null) return;

        Font font = minecraft.font;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int barWidth = 81;
        int barHeight = 8;
        int barX = screenWidth / 2 - 91;
        int barY = screenHeight - 39;

        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float absorption = player.getAbsorptionAmount();

        // 绘制背景（黑色边框 + 深灰底色）
        drawBarBackground(guiGraphics, barX, barY, barWidth, barHeight);

        // [线性-L05a-1] 红色血条基底：填充 currentHealth / maxHealth
        float healthRatio = Math.min(1.0f, Math.max(0.0f, currentHealth / maxHealth));
        drawBarFill(guiGraphics, barX, barY, barWidth, barHeight, healthRatio, 0xFFFF3333);

        // [线性-L05a-2] 金色吸收心覆盖层：在血条上层从healthRatio位置延伸到(health+absorption)/maxHealth
        if (CobblemonTSConfig.HUD_ABSORPTION_OVERLAY.get() && absorption > 0.0f) {
            float totalRatio = Math.min(1.0f, (currentHealth + absorption) / maxHealth);
            float absorptionStart = healthRatio * barWidth;
            float absorptionWidth = (totalRatio - healthRatio) * barWidth;
            if (absorptionWidth > 0) {
                int startX = barX + (int) absorptionStart;
                guiGraphics.fill(startX, barY, startX + (int) absorptionWidth, barY + barHeight, 0xFFFFD700);
            }
        }

        // [线性-L05a-3] 血条文字居中在条上
        String hpText = String.format("%d/%d", (int) currentHealth, (int) maxHealth);
        int hpTextX = barX + (barWidth - font.width(hpText)) / 2;
        int hpTextY = barY + (barHeight - font.lineHeight) / 2;
        guiGraphics.drawString(font, hpText, hpTextX, hpTextY, 0xFFFFFFFF, true);
    }

    // ================================================================
    // [线性-L05b] 饥饿条渲染：鸡腿色基底 + 金色饱腹条覆盖层
    // 数值显示：(饥饿值+饱腹值)/饥饿值，类似40/20
    // ================================================================
    @Inject(method = "renderFoodLevel", at = @At("HEAD"), cancellable = true)
    private void onRenderFoodLevel(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (!CobblemonTSConfig.HUD_CUSTOM_FOOD_BAR.get()) return; // 配置关闭→原版渲染
        ci.cancel();

        Player player = (Player) minecraft.getCameraEntity();
        if (player == null) return;

        Font font = minecraft.font;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int barWidth = 81;
        int barHeight = 8;
        int barX = screenWidth / 2 + 10;
        int barY = screenHeight - 39;

        FoodData foodData = player.getFoodData();
        int currentFood = foodData.getFoodLevel();
        int maxFood = 20;
        float saturation = foodData.getSaturationLevel();

        // 绘制背景
        drawBarBackground(guiGraphics, barX, barY, barWidth, barHeight);

        // [线性-L05b-1] 鸡腿色饥饿条基底：填充 currentFood / 20
        // 参考原版鸡腿颜色：暖棕色 0xFFE69A3C
        float foodRatio = Math.min(1.0f, Math.max(0.0f, (float) currentFood / (float) maxFood));
        drawBarFill(guiGraphics, barX, barY, barWidth, barHeight, foodRatio, 0xFFE69A3C);

        // [线性-L05b-2] 金色饱腹条覆盖层：在饥饿条上层从foodRatio位置延伸到(food+saturation)/20
        // 参考原版金鸡腿颜色：金色 0xFFFFD700
        if (CobblemonTSConfig.HUD_SATURATION_OVERLAY.get() && saturation > 0.0f) {
            float totalRatio = Math.min(1.0f, (currentFood + saturation) / (float) maxFood);
            float saturationStart = foodRatio * barWidth;
            float saturationWidth = (totalRatio - foodRatio) * barWidth;
            if (saturationWidth > 0) {
                int startX = barX + (int) saturationStart;
                guiGraphics.fill(startX, barY, startX + (int) saturationWidth, barY + barHeight, 0xFFFFD700);
            }
        }

        // [线性-L05b-3] 饥饿条数值居中在条上：(饥饿值+饱腹值)/饥饿值，如40/20
        int totalFood = currentFood + (int) saturation;
        String foodText = String.format("%d/%d", totalFood, currentFood);
        int foodTextX = barX + (barWidth - font.width(foodText)) / 2;
        int foodTextY = barY + (barHeight - font.lineHeight) / 2;
        guiGraphics.drawString(font, foodText, foodTextX, foodTextY, 0xFFFFFFFF, true);
    }

    // ================================================================
    // [线性-L05c] 盔甲条渲染：修复错位，放在血条上方
    // 数值显示：盔甲值/韧性值
    // 取消原版renderArmor并自定义渲染
    // ================================================================
    @Inject(method = "renderArmorLevel", at = @At("HEAD"), cancellable = true)
    private void onRenderArmorLevel(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (!CobblemonTSConfig.HUD_CUSTOM_ARMOR_BAR.get()) return; // 配置关闭→原版渲染
        ci.cancel();

        Player player = (Player) minecraft.getCameraEntity();
        if (player == null) return;

        int armorValue = player.getArmorValue();
        if (armorValue <= 0) return; // 无盔甲不显示

        Font font = minecraft.font;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int barWidth = 81;
        int barHeight = 8; // [线性-L05c-1] 与其他条统一高度
        int barX = screenWidth / 2 - 91;
        int barY = screenHeight - 49;

        // [线性-L05c-2] 获取盔甲韧性值
        int armorToughness = (int) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        int maxArmor = 20; // 最大盔甲值（全钻石装）
        float armorRatio = Math.min(1.0f, Math.max(0.0f, (float) armorValue / (float) maxArmor));

        // 绘制背景
        drawBarBackground(guiGraphics, barX, barY, barWidth, barHeight);
        // 灰色盔甲填充
        drawBarFill(guiGraphics, barX, barY, barWidth, barHeight, armorRatio, 0xFF888888);

        // [线性-L05c-3] 盔甲数值居中在条上：盔甲值/韧性值
        String armorText = String.format("%d/%d", armorValue, armorToughness);
        int armorTextX = barX + (barWidth - font.width(armorText)) / 2;
        int armorTextY = barY + (barHeight - font.lineHeight) / 2;
        guiGraphics.drawString(font, armorText, armorTextX, armorTextY, 0xFFFFFFFF, true);
    }

    // ================================================================
    // [线性-L05d] 通用绘制工具方法
    // ================================================================
    private static void drawBarBackground(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF000000); // 黑色边框
        g.fill(x, y, x + width, y + height, 0xFF333333);                  // 深灰底色
    }

    private static void drawBarFill(GuiGraphics g, int x, int y, int width, int height, float ratio, int color) {
        int fillWidth = (int) (width * ratio);
        if (fillWidth > 0) {
            g.fill(x, y, x + fillWidth, y + height, color);
        }
    }
}