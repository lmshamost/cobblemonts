package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CharmConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

// [线性-RESP-02] 双列布局修复：左列(label+field) 与 右列(label+field) 不重叠
//   列A: labelX=centerX-155, fieldX=centerX-80
//   列B: labelX=centerX+10, fieldX=centerX+80
//   白名单/黑名单按钮响应式宽度
public class PokebagConfigScreen extends Screen {

    private TextFieldWidget bagSlotsField;
    private TextFieldWidget maxStackField;

    private int centerX;
    private static final int FIELD_W = 52;
    private static final int FIELD_H = 16;
    private static final int BTN_H = 20;

    public PokebagConfigScreen() {
        super(Text.translatable("cobblemon_ts.pokebag_config.title"));
    }

    @Override
    protected void init() {
        centerX = this.width / 2;

        // [线性-RESP-02] 列A: label=centerX-155, field=centerX-80 (field终点=centerX-28)
        //   列B: label=centerX+10, field=centerX+80 (field终点=centerX+132)
        //   平行标签间距=38px, 不会重叠
        int leftFieldX = centerX - 80;
        int rightFieldX = centerX + 80;
        int startY = this.height / 2 - 85;
        int rowStep = 26;

        // ── Row0: 容量 | 堆叠上限 ──
        this.bagSlotsField = addEditBox(leftFieldX, startY, FIELD_W, FIELD_H,
                String.valueOf(CharmConfig.POKEBAG_SLOTS.get()));
        this.maxStackField = addEditBox(rightFieldX, startY, FIELD_W, FIELD_H,
                String.valueOf(CharmConfig.POKEBAG_MAX_STACK_SIZE.get()));

        // ── Row1: 白名单按钮 ── [线性-RESP-02] 响应式宽度
        int listBtnW = Math.min(300, this.width - 30);
        int listBtnX = centerX - listBtnW / 2;
        int whitelistCount = CharmConfig.POKEBAG_WHITELIST_TAGS.get().size();
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("cobblemon_ts.pokebag_config.edit_whitelist")
                        .append(" §a[" + whitelistCount + "]"),
                btn -> MinecraftClient.getInstance().setScreen(
                        new PokebagTagListScreen(true, this)))
                .position(listBtnX, startY + rowStep + 8)
                .size(listBtnW, BTN_H).build());

        // ── Row2: 黑名单按钮 ──
        int blacklistCount = CharmConfig.POKEBAG_BLACKLIST_TAGS.get().size();
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("cobblemon_ts.pokebag_config.edit_blacklist")
                        .append(" §7[" + blacklistCount + "]"),
                btn -> MinecraftClient.getInstance().setScreen(
                        new PokebagTagListScreen(false, this)))
                .position(listBtnX, startY + rowStep * 2 + 8)
                .size(listBtnW, BTN_H).build());

        // ── Row3: 保存 | 返回 ──
        int saveBtnW = 100;
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("cobblemon_ts.pokebag_config.save"),
                btn -> saveConfig())
                .position(centerX - saveBtnW - 4, startY + rowStep * 4 + 5)
                .size(saveBtnW, BTN_H).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("cobblemon_ts.pokebag_config.back"),
                btn -> MinecraftClient.getInstance().setScreen(null))
                .position(centerX + 4, startY + rowStep * 4 + 5)
                .size(saveBtnW, BTN_H).build());
    }

    private TextFieldWidget addEditBox(int x, int y, int w, int h, String val) {
        TextFieldWidget box = new TextFieldWidget(this.textRenderer, x, y, w, h, Text.literal(""));
        box.setText(val);
        box.setMaxLength(10);
        this.addDrawableChild(box);
        return box;
    }

    private void saveConfig() {
        try {
            int bagSlots = Integer.parseInt(this.bagSlotsField.getText());
            bagSlots = Math.clamp(bagSlots, 1, 54);
            CharmConfig.POKEBAG_SLOTS.set(bagSlots);

            int maxStack = Integer.parseInt(this.maxStackField.getText());
            maxStack = Math.clamp(maxStack, 1, 8192);
            CharmConfig.POKEBAG_MAX_STACK_SIZE.set(maxStack);

            CharmConfig.save();

            if (MinecraftClient.getInstance().player != null)
                MinecraftClient.getInstance().player.sendMessage(
                        Text.translatable("cobblemon_ts.pokebag_config.saved"), false);
        } catch (NumberFormatException e) {
            if (MinecraftClient.getInstance().player != null)
                MinecraftClient.getInstance().player.sendMessage(
                        Text.translatable("cobblemon_ts.testbuild.input_error"), false);
        }
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);

        int leftLabelX = centerX - 155;
        int rightLabelX = centerX + 10;
        int startY = this.height / 2 - 85;
        int labelY1 = startY + 4;

        g.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, this.height / 2 - 105, 0xFFFFFF);

        // ── Row0标签：容量 / 堆叠上限 ──
        g.drawText(this.textRenderer,
                Text.translatable("cobblemon_ts.pokebag_config.bag_slots").getString(),
                leftLabelX, labelY1, 0xCCCCCC, false);
        g.drawText(this.textRenderer,
                Text.translatable("cobblemon_ts.pokebag_config.max_stack").getString(),
                rightLabelX, labelY1, 0xCCCCCC, false);
    }

    @Override
    public boolean shouldPause() { return false; }
}