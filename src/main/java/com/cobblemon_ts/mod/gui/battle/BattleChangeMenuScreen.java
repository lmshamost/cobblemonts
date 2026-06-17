package com.cobblemon_ts.mod.gui.battle;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.lang.reflect.Method;

// [线性-B05] 另一种捕捉模式：三选一菜单（战斗道具/恢复/捕捉）
// 覆盖在Cobblemon捕捉按钮之上，提供3个选项
// [线性-B05s] 精灵图渲染：menu1/menu2各含三个按钮在一张图(92x80)
// 每个按钮 92x25，按区域裁切渲染各自区域
// 悬停按钮区域用menu1，其余用menu2
// 文字中心点(54,39)，文字向左延长
@OnlyIn(Dist.CLIENT)
public class BattleChangeMenuScreen extends Screen {

    private static final ResourceLocation MENU1 = ResourceLocation.parse("coblemon_ts:textures/gui/battle_change_menu1.png");
    private static final ResourceLocation MENU2 = ResourceLocation.parse("coblemon_ts:textures/gui/battle_change_menu2.png");

    // 精灵图纹理尺寸 92x80（3个按钮各92x25，底部5px留白）
    private static final int MENU_W = 92;
    private static final int MENU_H = 80;

    // 每个按钮 92x25
    private static final int BTN_W = 92;
    private static final int BTN_H = 25;

    private static final int BTN1_Y = 0;   // 战斗道具
    private static final int BTN2_Y = 25;  // 恢复
    private static final int BTN3_Y = 50;  // 捕捉

    // 文字中心点（相对于按钮左上角），文字从此点向左延长
    private static final int TEXT_CENTER_X = 54;
    private static final int TEXT_CENTER_Y = 39;

    private final Screen parentScreen;

    private int menuX;
    private int menuY;

    // [线性-B05f] 静态回调存储（kotlin的Function0<Unit>，用Object存+反射invoke）
    private static Object pendingCaptureCallback = null;
    // [线性-O05] 缓存kotlin lambda的invoke方法，避免每次触发都反射查找
    private static Method cachedKotlinInvokeMethod;

    public BattleChangeMenuScreen(Screen parentScreen) {
        super(Component.literal("捕捉菜单"));
        this.parentScreen = parentScreen;
    }

    public static void setPendingCaptureCallback(Object callback) {
        pendingCaptureCallback = callback;
    }

    // [线性-B05g] 触发原始捕捉回调（打开精灵球背包/pkbag GUI）
    public static void triggerCaptureCallback() {
        if (pendingCaptureCallback != null) {
            try {
                // [线性-O05] 缓存invoke方法，避免每次反射查找
                if (cachedKotlinInvokeMethod == null) {
                    cachedKotlinInvokeMethod = pendingCaptureCallback.getClass().getMethod("invoke");
                    cachedKotlinInvokeMethod.setAccessible(true);  // [线性-B05u] 跨模块lambda反射需要setAccessible
                }
                cachedKotlinInvokeMethod.invoke(pendingCaptureCallback);
            } catch (Exception e) {
                com.cobblemon_ts.mod.CobblemonTS.LOGGER.warn("[CaptureMode] 触发捕获回调失败", e);
            }
            pendingCaptureCallback = null;
        }
    }

    public static void clearCaptureCallback() {
        pendingCaptureCallback = null;
    }

    @Override
    protected void init() {
        this.menuX = (this.width - MENU_W) / 2;
        this.menuY = (this.height - MENU_H) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // [线性-B05h] 先渲染父界面（对战界面）作为背景
        if (this.parentScreen != null && this.minecraft != null) {
            this.parentScreen.render(graphics, -1, -1, partialTick);
        }

        // [线性-B05s] 精灵图渲染：三个按钮在一张图里，按区域裁切
        // 悬停按钮区域用menu1，其余区域用menu2
        int hovered = getHoveredButton(mouseX, mouseY);

        // 按钮1区域：战斗道具
        ResourceLocation tex1 = (hovered == 1) ? MENU1 : MENU2;
        graphics.blit(tex1, menuX, menuY + BTN1_Y, 0, BTN1_Y, BTN_W, BTN_H, MENU_W, MENU_H);

        // 按钮2区域：恢复
        ResourceLocation tex2 = (hovered == 2) ? MENU1 : MENU2;
        graphics.blit(tex2, menuX, menuY + BTN2_Y, 0, BTN2_Y, BTN_W, BTN_H, MENU_W, MENU_H);

        // 按钮3区域：捕捉
        ResourceLocation tex3 = (hovered == 3) ? MENU1 : MENU2;
        graphics.blit(tex3, menuX, menuY + BTN3_Y, 0, BTN3_Y, BTN_W, BTN_H, MENU_W, MENU_H);

        // [线性-B05s] 文字渲染：中心点(54,39)，右边界对齐，文字向左延长
        renderButtonText(graphics, "战斗道具", BTN1_Y);
        renderButtonText(graphics, "恢复", BTN2_Y);
        renderButtonText(graphics, "捕捉", BTN3_Y);
    }

    private void renderButtonText(GuiGraphics graphics, String text, int btnY) {
        int textWidth = this.font.width(text);
        int textX = menuX + TEXT_CENTER_X - textWidth;  // 右边界在54，文字向左
        int textY = menuY + btnY + TEXT_CENTER_Y - this.font.lineHeight / 2;
        graphics.drawString(this.font, text, textX, textY, 0xFFFFFF);
    }

    private int getHoveredButton(int mouseX, int mouseY) {
        if (mouseX < menuX || mouseX > menuX + BTN_W) return 0;
        if (mouseY < menuY + BTN1_Y) return 0;
        if (mouseY < menuY + BTN1_Y + BTN_H) return 1;  // 战斗道具
        if (mouseY < menuY + BTN2_Y + BTN_H) return 2;  // 恢复
        if (mouseY < menuY + BTN3_Y + BTN_H) return 3;  // 捕捉
        return 0;
    }

    // [线性-B05s] 三个按钮统一：点击任意一个 → 打开pkbag GUI
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int hovered = getHoveredButton((int) mouseX, (int) mouseY);
        if (hovered == 0) {
            this.onClose();
            return true;
        }

        // [线性-B05v] 打开pkbag GUI（自定义精灵球背包界面）
        if (this.minecraft != null) {
            this.minecraft.setScreen(new PkBagScreen(this.parentScreen));
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
        clearCaptureCallback();
    }
}