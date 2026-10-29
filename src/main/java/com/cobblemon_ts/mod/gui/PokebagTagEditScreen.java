package com.cobblemon_ts.mod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// [线性-PTE-01] 道具袋白名单/黑名单单条编辑界面（仿LuckyWeightEditEntryScreen按键模式）
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
        int y = 50;

        // 提示文字
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.edit_hint"),
                btn -> {})
                .pos(centerX - 180, 30)
                .size(360, 14)
                .build());

        // 输入框
        tagField = new EditBox(this.font, centerX - 175, y, 350, 20,
                Component.literal("entry"));
        tagField.setMaxLength(200);
        tagField.setValue(originalValue != null ? originalValue : "");
        this.addRenderableWidget(tagField);

        y += 35;

        // 保存
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.save_entry"),
                btn -> saveEntry())
                .pos(centerX - 100, y)
                .size(80, 20)
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
                .pos(centerX - 10, y)
                .size(80, 20)
                .build());

        // 返回
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.cancel_entry"),
                btn -> Minecraft.getInstance().setScreen(parent))
                .pos(centerX + 80, y)
                .size(60, 20)
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