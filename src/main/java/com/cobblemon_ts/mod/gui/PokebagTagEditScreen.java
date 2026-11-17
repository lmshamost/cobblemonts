package com.cobblemon_ts.mod.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

// [线性-RESP-04] 响应式布局：输入框/按钮按屏幕宽度动态计算，支持GUI缩放
@Environment(EnvType.CLIENT)
public class PokebagTagEditScreen extends Screen {

    private final boolean isWhitelist;
    private final int entryIndex;
    private final String originalValue;
    private final PokebagTagListScreen parent;

    private TextFieldWidget tagField;

    public PokebagTagEditScreen(boolean isWhitelist, int entryIndex, String value, PokebagTagListScreen parent) {
        super(Text.translatable(isWhitelist
                ? "cobblemon_ts.pokebag_config.edit_wl_title"
                : "cobblemon_ts.pokebag_config.edit_bl_title"));
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
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("cobblemon_ts.pokebag_config.edit_hint"),
                btn -> {})
                .position(fieldX, 30)
                .size(contentWidth, 14)
                .build());

        // 输入框
        tagField = new TextFieldWidget(this.textRenderer, fieldX, y, contentWidth, 20,
                Text.literal("entry"));
        tagField.setMaxLength(200);
        tagField.setText(originalValue != null ? originalValue : "");
        this.addDrawableChild(tagField);

        y += 35;

        // [线性-RESP-04] 底部3按钮响应式排列
        int btnW = Math.min(80, contentWidth / 3 - 4);
        int btnGap = 6;
        int totalBtnW = btnW * 3 + btnGap * 2;
        int btnStart = centerX - totalBtnW / 2;

        // 保存
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("cobblemon_ts.pokebag_config.save_entry"),
                btn -> saveEntry())
                .position(btnStart, y)
                .size(btnW, 20)
                .build());

        // 删除
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("cobblemon_ts.pokebag_config.delete_entry"),
                btn -> {
                    if (parent != null) {
                        parent.entries.remove(entryIndex);
                    }
                    MinecraftClient.getInstance().setScreen(parent);
                })
                .position(btnStart + btnW + btnGap, y)
                .size(btnW, 20)
                .build());

        // 返回 → [线性-S02] 取消时清理空白条目
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("cobblemon_ts.pokebag_config.cancel_entry"),
                btn -> {
                    removeIfBlank();
                    MinecraftClient.getInstance().setScreen(parent);
                })
                .position(btnStart + (btnW + btnGap) * 2, y)
                .size(btnW, 20)
                .build());
    }

    // [线性-S02] 取消/返回时检查当前条目是否空白，是则移除
    private void removeIfBlank() {
        if (parent != null && entryIndex >= 0 && entryIndex < parent.entries.size()) {
            String val = parent.entries.get(entryIndex);
            if (val == null || val.isBlank()) {
                parent.entries.remove(entryIndex);
            }
        }
    }

    private void saveEntry() {
        String newValue = tagField.getText().trim();
        if (parent != null && entryIndex >= 0 && entryIndex < parent.entries.size()) {
            // [线性-S02] 空白内容不保留，直接清理
            if (newValue.isEmpty()) {
                parent.entries.remove(entryIndex);
            } else {
                parent.entries.set(entryIndex, newValue);
            }
        }
        MinecraftClient.getInstance().setScreen(parent);
    }

    // [线性-S02-ESC] ESC键关闭时也清理空白条目
    @Override
    public void close() {
        removeIfBlank();
        super.close();
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);
        g.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() { return false; }
}