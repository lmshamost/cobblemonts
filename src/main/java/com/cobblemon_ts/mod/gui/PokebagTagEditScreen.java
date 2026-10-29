package com.cobblemon_ts.mod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// [线性-RESP-04] 响应式布局：输入框/按钮按屏幕宽度动态计算，支持GUI缩放
@OnlyIn(Dist.CLIENT)
public class PokebagTagEditScreen extends Screen {

    private final boolean isWhitelist;
    private final int entryIndex;
    private final String originalValue;
    private final PokebagTagListScreen parent;

    private EditBox tagField;

    public PokebagTagEditScreen(boolean isWhitelist, int entryIndex, String value, PokebagTagListScreen parent) {
        super(Component.translatable(isWhitelist
                ? "coblemon_ts.pokebag_config.edit_wl_title"
                : "coblemon_ts.pokebag_config.edit_bl_title"));
        this.isWhitelist = isWhitelist;
        this.entryIndex = entryIndex;
        this.originalValue = value;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        // [线性-RESP-04] 内容宽度响应式
        int contentWidth = Math.min(360, this.width - 30);
        int fieldX = centerX - contentWidth / 2;

        int y = 48;

        // 提示文字
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.edit_hint"),
                btn -> {})
                .pos(fieldX, 30)
                .size(contentWidth, 14)
                .build());

        // 输入框
        tagField = new EditBox(this.font, fieldX, y, contentWidth, 20,
                Component.literal("entry"));
        tagField.setMaxLength(200);
        tagField.setValue(originalValue != null ? originalValue : "");
        this.addRenderableWidget(tagField);

        y += 35;

        // [线性-RESP-04] 底部3按钮响应式排列
        int btnW = Math.min(80, contentWidth / 3 - 4);
        int btnGap = 6;
        int totalBtnW = btnW * 3 + btnGap * 2;
        int btnStart = centerX - totalBtnW / 2;

        // 保存
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.save_entry"),
                btn -> saveEntry())
                .pos(btnStart, y)
                .size(btnW, 20)
                .build());

        // 删除
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.delete_entry"),
                btn -> {
                    if (parent != null) {
                        parent.entries.remove(entryIndex);
                    }
                    Minecraft.getInstance().setScreen(parent);
                })
                .pos(btnStart + btnW + btnGap, y)
                .size(btnW, 20)
                .build());

        // 返回
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.cancel_entry"),
                btn -> Minecraft.getInstance().setScreen(parent))
                .pos(btnStart + (btnW + btnGap) * 2, y)
                .size(btnW, 20)
                .build());
    }

    private void saveEntry() {
        String newValue = tagField.getValue().trim();
        if (parent != null && entryIndex >= 0 && entryIndex < parent.entries.size()) {
            parent.entries.set(entryIndex, newValue);
        }
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);
        g.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}