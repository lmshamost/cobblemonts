package com.cobblemon_ts.mod.mixin;

import com.cobblemon_ts.mod.CobblemonTSConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// [线性-L05] 自定义HUD双模式：
//   模式A: HUD_UNIFIED_COLORED_BARS=true → 统一彩色数值条（注入各独立图层方法）
//   模式B: HUD_UNIFIED_COLORED_BARS=false → 旧版逐项开关（HUD_CUSTOM_HEALTH_BAR等）
//   NeoForge 1.21图层系统：每个状态项为独立LayeredDraw层，需逐个注入取消
@Mixin(Gui.class)
public abstract class GuiHealthFoodMixin {

    @Shadow @Final private Minecraft minecraft;

    // ---- [线性-L10] 统一彩色条颜色常量 ----
    private static final int C_HEALTH     = 0xFFFF3333; // 红 - 生命值
    private static final int C_ABSORPTION = 0xFFFFD700; // 金 - 伤害吸收
    private static final int C_ARMOR      = 0xFFC0C0C0; // 银灰 - 护甲值
    private static final int C_TOUGHNESS  = 0xFF5CB1D6; // 钻石蓝 - 韧性值
    private static final int C_FOOD       = 0xFFE69A3C; // 棕 - 饥饿值
    private static final int C_SATURATION = 0xFFFFD700; // 金 - 饱腹值
    private static final int C_AIR        = 0xFF3399FF; // 蓝 - 氧气值
    private static final int C_MOUNT      = 0xFF55FF55; // 绿 - 骑乘生命
    private static final int C_XP         = 0xFF80FF20; // 亮绿 - 经验值
    private static final int BAR_W = 81;
    private static final int BAR_H = 7;
    private static final int BAR_GAP = 3;
    private static final int STEP = BAR_H + BAR_GAP; // 10
    private static final float PX_PER_UNIT = BAR_W / 20.0f; // 无上限项像素比: 1单位≈4px

    // ================================================================
    // [线性-L10a]: renderHealthLevel → 生命值 + 伤害吸收（左列底部两条）
    //   由PLAYER_HEALTH图层调用，统一模式下取消原生→渲染自定义条
    // ================================================================
    @Inject(method = "renderHealthLevel", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRenderHealthLevel(GuiGraphics g, CallbackInfo ci) {
        if (CobblemonTSConfig.HUD_UNIFIED_COLORED_BARS.get()) {
            ci.cancel();
            renderUnifiedHealth(g);
            return;
        }
        if (CobblemonTSConfig.HUD_CUSTOM_HEALTH_BAR.get()) {
            ci.cancel();
            renderIndividualHealth(g);
        }
    }

    // ================================================================
    // [线性-L10b]: renderArmorLevel → 护甲值 + 韧性值（左列顶部两条）
    //   由ARMOR_LEVEL图层调用，统一模式下取消原生→渲染自定义条
    // ================================================================
    @Inject(method = "renderArmorLevel", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRenderArmorLevel(GuiGraphics g, CallbackInfo ci) {
        if (CobblemonTSConfig.HUD_UNIFIED_COLORED_BARS.get()) {
            ci.cancel();
            renderUnifiedArmor(g);
            return;
        }
        if (CobblemonTSConfig.HUD_CUSTOM_ARMOR_BAR.get()) {
            ci.cancel();
            renderUnifiedArmor(g); // 盔甲无覆盖层开关，直接复用统一渲染
        }
    }

    // ================================================================
    // [线性-L10c]: renderFoodLevel → 饥饿值 + 饱腹值（右列底部两条）
    //   由FOOD_LEVEL图层调用，统一模式下取消原生→渲染自定义条
    // ================================================================
    @Inject(method = "renderFoodLevel", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRenderFoodLevel(GuiGraphics g, CallbackInfo ci) {
        if (CobblemonTSConfig.HUD_UNIFIED_COLORED_BARS.get()) {
            ci.cancel();
            renderUnifiedFood(g);
            return;
        }
        if (CobblemonTSConfig.HUD_CUSTOM_FOOD_BAR.get()) {
            ci.cancel();
            renderIndividualFood(g);
        }
    }

    // ================================================================
    // [线性-L10d]: renderAirLevel → 氧气值（右列第三条）
    //   由AIR_LEVEL图层调用，统一模式下取消原生→渲染自定义条
    //   条件：仅当玩家在水下/缺氧时（airSupply < maxAirSupply）
    // ================================================================
    @Inject(method = "renderAirLevel", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRenderAirLevel(GuiGraphics g, CallbackInfo ci) {
        if (CobblemonTSConfig.HUD_UNIFIED_COLORED_BARS.get()) {
            ci.cancel();
            renderUnifiedAir(g);
        }
        // 无逐项氧气开关；统一OFF时走原版气泡
    }

    // ================================================================
    // [线性-L10e]: renderVehicleHealth → 骑乘生物生命值（右列第四条/顶部）
    //   由VEHICLE_HEALTH图层调用，统一模式下取消原生→渲染自定义条
    //   条件：仅当玩家骑乘中
    // ================================================================
    @Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRenderVehicleHealth(GuiGraphics g, CallbackInfo ci) {
        if (CobblemonTSConfig.HUD_UNIFIED_COLORED_BARS.get()) {
            ci.cancel();
            renderUnifiedMount(g);
        }
        // 无逐项骑乘生命开关；统一OFF时走原版心形
    }

    // ================================================================
    // [线性-L10f]: renderExperienceBar → 经验值条
    //   格式: "总经验值 | Lv.等级"
    // ================================================================
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true, require = 0)
    private void ts$renderExperienceBarUnified(GuiGraphics g, int xPos, CallbackInfo ci) {
        if (CobblemonTSConfig.HUD_UNIFIED_COLORED_BARS.get()) {
            ci.cancel();
            renderUnifiedXP(g);
            return;
        }
        if (CobblemonTSConfig.HUD_CUSTOM_XP_BAR.get()) {
            ci.cancel();
            renderUnifiedXP(g);
        }
    }

    // ================================================================
    // [线性-L10g]: renderExperienceLevel → 取消原版等级数字
    // ================================================================
    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true, require = 0)
    private void ts$renderExperienceLevelUnified(GuiGraphics g, DeltaTracker dt, CallbackInfo ci) {
        if (CobblemonTSConfig.HUD_UNIFIED_COLORED_BARS.get()) {
            ci.cancel();
            return;
        }
        if (CobblemonTSConfig.HUD_CUSTOM_XP_BAR.get()) {
            ci.cancel(); // 自定义经验条ON → 隐藏原版等级数字
        }
    }

    // ================================================================
    // ==== 统一模式渲染方法（每个图层独立渲染各自的条）====
    // ================================================================

    // [线性-L20a] 生命值 + 伤害吸收（左列: y=-39, y=-49）
    private void renderUnifiedHealth(GuiGraphics g) {
        Player player = (Player) minecraft.getCameraEntity();
        if (player == null) return;

        Font font = minecraft.font;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        int leftX = sw / 2 - 91;
        int baseY = sh - 39;

        float hp = player.getHealth();
        float maxHp = player.getMaxHealth();
        float abs = player.getAbsorptionAmount();

        // 生命值条（红）— 始终显示
        renderUnifiedBar(g, font, leftX, baseY, BAR_W, BAR_H,
                hp / maxHp, C_HEALTH,
                String.format("%.0f/%.0f", hp, maxHp));

        // [改] 伤害吸收条（金）— 无上限，条长=值×像素比，无空白段
        if (abs > 0.0f) {
            int bw = Math.min((int)(abs * PX_PER_UNIT), BAR_W);
            renderValueBar(g, font, leftX, baseY - STEP, bw, C_ABSORPTION,
                    String.format("%.0f", abs));
        }
    }

    // [修复] 护甲+韧性合并条（Y动态：有吸收在-59，无吸收在-49）
    private void renderUnifiedArmor(GuiGraphics g) {
        Player player = (Player) minecraft.getCameraEntity();
        if (player == null) return;

        int armor = player.getArmorValue();
        int toughness = (int) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        if (armor <= 0 && toughness <= 0) return;

        Font font = minecraft.font;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        int leftX = sw / 2 - 91;
        // [修复] 伤害吸收条不存在时上移一位(-49)，存在时保持原位(-59)
        float abs = player.getAbsorptionAmount();
        int y = sh - 39 - (abs > 0.0f ? 2 : 1) * STEP;

        drawBg(g, leftX, y, BAR_W, BAR_H);
        int ty = y + (BAR_H - font.lineHeight) / 2;

        if (armor > 0 && toughness > 0) {
            // 固定均分：左半盔甲(银灰)+右半韧性(钻石蓝)，各占BAR_W/2
            int midX = leftX + BAR_W / 2;
            g.fill(leftX, y, midX, y + BAR_H, C_ARMOR);
            g.fill(midX, y, leftX + BAR_W, y + BAR_H, C_TOUGHNESS);
            // 各自居中于自己那半
            String at = String.valueOf(armor);
            String tt = String.valueOf(toughness);
            int atx = leftX + (BAR_W / 2 - font.width(at)) / 2;
            int ttx = midX + (BAR_W / 2 - font.width(tt)) / 2;
            g.drawString(font, at, atx, ty, 0xFFFFFFFF, true);
            g.drawString(font, tt, ttx, ty, 0xFFFFFFFF, true);
        } else if (armor > 0) {
            g.fill(leftX, y, leftX + Math.min((int)(armor * PX_PER_UNIT), BAR_W), y + BAR_H, C_ARMOR);
            String text = String.valueOf(armor);
            int tx = leftX + (BAR_W - font.width(text)) / 2;
            g.drawString(font, text, tx, ty, 0xFFFFFFFF, true);
        } else {
            g.fill(leftX, y, leftX + Math.min((int)(toughness * PX_PER_UNIT), BAR_W), y + BAR_H, C_TOUGHNESS);
            String text = String.valueOf(toughness);
            int tx = leftX + (BAR_W - font.width(text)) / 2;
            g.drawString(font, text, tx, ty, 0xFFFFFFFF, true);
        }
    }

    // [线性-L20c] 饥饿值 + 饱腹值（右列: y=-39, y=-49）
    private void renderUnifiedFood(GuiGraphics g) {
        Player player = (Player) minecraft.getCameraEntity();
        if (player == null) return;

        Font font = minecraft.font;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        int rightX = sw / 2 + 10;
        int baseY = sh - 39;

        FoodData fd = player.getFoodData();
        int food = fd.getFoodLevel();
        float sat = fd.getSaturationLevel();

        // 饥饿值条（棕）— 始终显示
        renderUnifiedBar(g, font, rightX, baseY, BAR_W, BAR_H,
                food / 20.0f, C_FOOD,
                String.format("%d/20", food));

        // 饱腹值条（金）— 条件: sat > 0, 格式: 饱腹值/饥饿值
        if (sat > 0.0f) {
            renderUnifiedBar(g, font, rightX, baseY - STEP, BAR_W, BAR_H,
                    Math.min(1.0f, sat / (float) food), C_SATURATION,
                    String.format("%.0f/%d", sat, food));
        }
    }

    // [线性-L20d] 氧气值（右列: y=-59）— 条件: 缺氧时显示
    private void renderUnifiedAir(GuiGraphics g) {
        Player player = (Player) minecraft.getCameraEntity();
        if (player == null) return;

        int air = player.getAirSupply();
        int maxAir = player.getMaxAirSupply();
        if (air >= maxAir) return; // 不缺氧不显示

        Font font = minecraft.font;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        int rightX = sw / 2 + 10;
        int baseY = sh - 39;

        renderUnifiedBar(g, font, rightX, baseY - 2 * STEP, BAR_W, BAR_H,
                (float) air / (float) maxAir, C_AIR,
                String.format("%d/%d", air, maxAir));
    }

    // [线性-L20e] 骑乘生物生命值（右列: y=-69）— 条件: 骑乘中
    private void renderUnifiedMount(GuiGraphics g) {
        Player player = (Player) minecraft.getCameraEntity();
        if (player == null) return;
        if (!(player.getVehicle() instanceof LivingEntity mount)) return;

        float mHp = mount.getHealth();
        float mMax = mount.getMaxHealth();

        Font font = minecraft.font;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        int rightX = sw / 2 + 10;
        int baseY = sh - 39;

        renderUnifiedBar(g, font, rightX, baseY - 3 * STEP, BAR_W, BAR_H,
                mHp / mMax, C_MOUNT,
                String.format("%.0f/%.0f", mHp, mMax));
    }

    // ================================================================
    // ==== 通用绘制工具 ====
    // ================================================================

    // [线性-UT01] 统一条: 背景+填充+文字居中
    private static void renderUnifiedBar(GuiGraphics g, Font font, int x, int y, int w, int h,
                                          float ratio, int color, String text) {
        drawBg(g, x, y, w, h);
        drawFill(g, x, y, w, h, Math.min(1.0f, Math.max(0.0f, ratio)), color);
        int tx = x + (w - font.width(text)) / 2;
        int ty = y + (h - font.lineHeight) / 2;
        g.drawString(font, text, tx, ty, 0xFFFFFFFF, true);
    }

    // [线性-UT02] 条背景: 黑边+深灰底
    private static void drawBg(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + w, y + h, 0xFF333333);
    }

    // [线性-UT03] 条填充: 按ratio绘制颜色填充
    private static void drawFill(GuiGraphics g, int x, int y, int w, int h, float ratio, int color) {
        int fw = (int) (w * ratio);
        if (fw > 0) g.fill(x, y, x + fw, y + h, color);
    }

    // [线性-UT04] 无上限数值条: 条长=值×像素比，无空白段，无固定分母
    //   用于盔甲/韧性/伤害吸收等无上限属性
    private static void renderValueBar(GuiGraphics g, Font font, int x, int y, int bw,
                                        int color, String text) {
        if (bw <= 0) return;
        drawBg(g, x, y, bw, BAR_H);
        g.fill(x, y, x + bw, y + BAR_H, color);
        int tx = x + (bw - font.width(text)) / 2;
        int ty = y + (BAR_H - font.lineHeight) / 2;
        g.drawString(font, text, tx, ty, 0xFFFFFFFF, true);
    }

    // [线性-L20g] 统一经验值条（底部居中，宽182高6）
    private void renderUnifiedXP(GuiGraphics g) {
        Player player = (Player) minecraft.getCameraEntity();
        if (player == null) return;

        Font font = minecraft.font;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();

        int xpX = sw / 2 - 91;
        int xpW = 182;
        int xpH = 6;
        int xpY = sh - 27;

        int totalXp = player.totalExperience;
        int level = player.experienceLevel;
        float progress = player.experienceProgress;

        drawBg(g, xpX, xpY, xpW, xpH);
        drawFill(g, xpX, xpY, xpW, xpH, progress, C_XP);

        String text = String.format("%d | Lv.%d", totalXp, level);
        int tx = xpX + (xpW - font.width(text)) / 2;
        int ty = xpY + (xpH - font.lineHeight) / 2;
        g.drawString(font, text, tx, ty, 0xFFFFFFFF, true);
    }

    // ================================================================
    // ==== 逐项模式渲染方法 ====
    // ================================================================

    // [线性-ID03] 逐项·血条（含吸收覆盖开关检查）— Y坐标与统一模式一致
    private void renderIndividualHealth(GuiGraphics g) {
        Player player = (Player) minecraft.getCameraEntity();
        if (player == null) return;

        Font font = minecraft.font;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        int leftX = sw / 2 - 91;
        int baseY = sh - 39;

        float hp = player.getHealth();
        float maxHp = player.getMaxHealth();
        float abs = player.getAbsorptionAmount();

        renderUnifiedBar(g, font, leftX, baseY, BAR_W, BAR_H,
                Math.min(1.0f, hp / maxHp), C_HEALTH,
                String.format("%d/%d", (int) hp, (int) maxHp));

        if (CobblemonTSConfig.HUD_ABSORPTION_OVERLAY.get() && abs > 0.0f) {
            int bw = Math.min((int) (abs * PX_PER_UNIT), BAR_W);
            renderValueBar(g, font, leftX, baseY - STEP, bw, C_ABSORPTION,
                    String.format("%.0f", abs));
        }
    }

    // [线性-ID04] 逐项·饥饿条（含饱腹覆盖开关检查）— Y坐标与统一模式一致(baseY)
    private void renderIndividualFood(GuiGraphics g) {
        Player player = (Player) minecraft.getCameraEntity();
        if (player == null) return;

        FoodData fd = player.getFoodData();
        int food = fd.getFoodLevel();
        float sat = fd.getSaturationLevel();

        Font font = minecraft.font;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        int rightX = sw / 2 + 10;
        int baseY = sh - 39;

        renderUnifiedBar(g, font, rightX, baseY, BAR_W, BAR_H,
                Math.min(1.0f, (float) food / 20.0f), C_FOOD,
                String.format("%d/%d", food, 20));

        if (CobblemonTSConfig.HUD_SATURATION_OVERLAY.get() && sat > 0.0f) {
            renderUnifiedBar(g, font, rightX, baseY - STEP, BAR_W, BAR_H,
                    Math.min(1.0f, sat / (float) food), C_SATURATION,
                    String.format("%.0f/%d", sat, food));
        }
    }
}