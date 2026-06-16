package com.cobblemon_ts.mod.gui.battle;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// [线性-B05v] pkbag GUI：精灵球背包自定义界面
// 92x168，与battle_change_menu同宽(92px)可对齐
// 点击三选一菜单任意按钮后打开
@OnlyIn(Dist.CLIENT)
public class PkBagScreen extends Screen {

    private static final ResourceLocation PKBAG = ResourceLocation.parse("coblemon_ts:textures/gui/pkbag.png");

    // pkbag.png 尺寸 92x168
    private static final int PKBAG_W = 92;
    private static final int PKBAG_H = 168;

    private final Screen parentScreen;

    private int bagX;
    private int bagY;

    public PkBagScreen(Screen parentScreen) {
        super(Component.literal("精灵球背包"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        this.bagX = (this.width - PKBAG_W) / 2;
        this.bagY = (this.height - PKBAG_H) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // [线性-B05v] 渲染父界面（对战界面）作为背景
        if (this.parentScreen != null && this.minecraft != null) {
            this.parentScreen.render(graphics, -1, -1, partialTick);
        }

        // [线性-B05v] 渲染pkbag.png，92px宽与菜单对齐
        graphics.blit(PKBAG, bagX, bagY, 0, 0, PKBAG_W, PKBAG_H, PKBAG_W, PKBAG_H);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // [线性-B05v] 点击pkbag外部区域关闭
        if (mouseX < bagX || mouseX > bagX + PKBAG_W || mouseY < bagY || mouseY > bagY + PKBAG_H) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
    }
}