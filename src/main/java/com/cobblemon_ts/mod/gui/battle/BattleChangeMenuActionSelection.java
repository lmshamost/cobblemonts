package com.cobblemon_ts.mod.gui.battle;

import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleActionSelection;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleBackButton;
import com.cobblemon.mod.common.client.battle.SingleActionRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

// [线性-B05w] 三选一菜单作为BattleActionSelection嵌入BattleGUI
// 使用Cobblemon原生changeActionSelection切换，带返回按键
// 精灵图92x80，每按钮92x25，文字中心(54,12)向左延长
// [线性-B05zc] 位置：x=12左对齐"道具"tile（左下角），以"捕捉"按钮底边对齐"道具"tile底边
// Cobblemon布局: 战斗(0)|宝可梦(1) / 道具(2)|逃走(3)
// 道具tile(index=2): x=12(偶数), y=baseY+29=guiScaledH-56, bottom=guiScaledH-30
// 菜单捕捉按钮: BTN3_Y=50, h=25, 底边=menuY+75 → menuY+75=guiScaledH-30 → menuY=guiScaledH-105
public class BattleChangeMenuActionSelection extends BattleActionSelection {

    private static final ResourceLocation MENU1 = ResourceLocation.parse("coblemon_ts:textures/gui/battle_change_menu1.png");
    private static final ResourceLocation MENU2 = ResourceLocation.parse("coblemon_ts:textures/gui/battle_change_menu2.png");

    private static final int MENU_W = 92;
    private static final int MENU_H = 80;
    private static final int BTN_W = 92;
    private static final int BTN_H = 25;
    private static final int BTN1_Y = 0;   // 战斗道具
    private static final int BTN2_Y = 25;  // 恢复
    private static final int BTN3_Y = 50;  // 捕捉

    // 文字中心点（按钮内），文字向右对齐（向左延长）
    private static final int TEXT_CENTER_X = 54;
    private static final int TEXT_CENTER_Y = 12;  // 25px按钮垂直中心

    // [线性-B05zc] 道具tile(index=2): x=12, y=guiScaledH-56, bottom=guiScaledH-30
    // 菜单捕捉按钮底边(menuY+75)对齐道具tile底边(guiScaledH-30)
    private static final int ITEMS_TILE_X = 12;
    private static final int ITEMS_TILE_BOTTOM_OFFSET = 30;  // 道具tile底边距屏幕底边=85-29-26=30
    private static final int CAPTURE_BTN_BOTTOM = BTN3_Y + BTN_H;  // 捕捉按钮底边(相对菜单)=75

    private final BattleBackButton backButton;
    private final int menuX;
    private final int menuY;

    public BattleChangeMenuActionSelection(BattleGUI battleGUI, SingleActionRequest request) {
        super(battleGUI, request, 0, 0, battleGUI.width, battleGUI.height, Component.literal("捕捉菜单"));
        this.backButton = new BattleBackButton(12.0f, battleGUI.height - 32.0f);

        // [线性-B05zc] 菜单x=12左对齐道具tile(左下角)，捕捉按钮底边对齐道具tile底边
        this.menuX = ITEMS_TILE_X;
        this.menuY = battleGUI.height - ITEMS_TILE_BOTTOM_OFFSET - CAPTURE_BTN_BOTTOM;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int hovered = getHoveredButton(mouseX, mouseY);
        Font font = Minecraft.getInstance().font;

        // 按钮1：战斗道具
        ResourceLocation tex1 = (hovered == 1) ? MENU1 : MENU2;
        graphics.blit(tex1, menuX, menuY + BTN1_Y, 0, BTN1_Y, BTN_W, BTN_H, MENU_W, MENU_H);
        renderButtonText(graphics, font, "战斗道具", BTN1_Y);

        // 按钮2：恢复
        ResourceLocation tex2 = (hovered == 2) ? MENU1 : MENU2;
        graphics.blit(tex2, menuX, menuY + BTN2_Y, 0, BTN2_Y, BTN_W, BTN_H, MENU_W, MENU_H);
        renderButtonText(graphics, font, "恢复", BTN2_Y);

        // 按钮3：捕捉
        ResourceLocation tex3 = (hovered == 3) ? MENU1 : MENU2;
        graphics.blit(tex3, menuX, menuY + BTN3_Y, 0, BTN3_Y, BTN_W, BTN_H, MENU_W, MENU_H);
        renderButtonText(graphics, font, "捕捉", BTN3_Y);

        // 返回按键（Cobblemon原生BattleBackButton，位置x=12, y=screenHeight-32）
        this.backButton.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderButtonText(GuiGraphics graphics, Font font, String text, int btnY) {
        int textWidth = font.width(text);
        int textX = menuX + TEXT_CENTER_X - textWidth;  // 右边界在54，文字向左
        int textY = menuY + btnY + TEXT_CENTER_Y - font.lineHeight / 2;
        graphics.drawString(font, text, textX, textY, 0xFFFFFF);
    }

    private int getHoveredButton(int mouseX, int mouseY) {
        if (mouseX < menuX || mouseX > menuX + BTN_W) return 0;
        if (mouseY < menuY + BTN1_Y) return 0;
        if (mouseY < menuY + BTN1_Y + BTN_H) return 1;
        if (mouseY < menuY + BTN2_Y + BTN_H) return 2;
        if (mouseY < menuY + BTN3_Y + BTN_H) return 3;
        return 0;
    }

    // [线性-B05w] 点击处理：返回按键→根ActionSelection，按钮→pkbag GUI
    @Override
    public boolean mousePrimaryClicked(double mouseX, double mouseY) {
        if (this.backButton.isHovered(mouseX, mouseY)) {
            // 返回根ActionSelection（战斗/道具/宝可梦/逃跑）
            getBattleGUI().changeActionSelection(null);
            return true;
        }

        int hovered = getHoveredButton((int) mouseX, (int) mouseY);
        if (hovered > 0) {
            // [线性-B05ze] 按点击按钮设置pkbag分类：1=战斗道具, 2=恢复, 3=捕捉
            PkBagActionSelection pkBag = new PkBagActionSelection(getBattleGUI(), getRequest());
            switch (hovered) {
                case 1: pkBag.setCurrentCategory(PkBagActionSelection.ItemCategory.BATTLE_ITEMS); break;
                case 2: pkBag.setCurrentCategory(PkBagActionSelection.ItemCategory.RESTORES); break;
                case 3: pkBag.setCurrentCategory(PkBagActionSelection.ItemCategory.CAPTURE); break;
            }
            getBattleGUI().changeActionSelection(pkBag);
            return true;
        }
        return false;
    }
}