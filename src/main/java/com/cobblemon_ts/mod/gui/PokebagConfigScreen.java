package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CharmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// [线性-RESP-02] 双列布局修复：左列(label+field/toggle) 与 右列(label+field) 不重叠
//   列A: labelX=centerX-155, fieldX=centerX-80
//   列B: labelX=centerX+10, fieldX=centerX+80
//   白名单/黑名单按钮响应式宽度
public class PokebagConfigScreen extends Screen {

    private EditBox bagSlotsField;
    private EditBox maxStackField;
    private EditBox extractLimitField;
    private Button extractToggleBtn;
    private boolean extractLimitEnabled;

    private int centerX;
    private static final int LABEL_W = 75;
    private static final int FIELD_W = 52;
    private static final int FIELD_H = 16;
    private static final int TOGGLE_W = 80;
    private static final int BTN_H = 20;

    public PokebagConfigScreen() {
        super(Component.translatable("coblemon_ts.pokebag_config.title"));
    }

    @Override
    protected void init() {
        centerX = this.width / 2;

        // [线性-RESP-02] 列A: label=centerX-155, field=centerX-80 (field终点=centerX-28)
        //   列B: label=centerX+10, field=centerX+80 (field终点=centerX+132)
        //   平行标签间距=38px, 不会重叠
        int leftLabelX = centerX - 155;
        int leftFieldX = centerX - 80;
        int rightLabelX = centerX + 10;
        int rightFieldX = centerX + 80;
        int startY = this.height / 2 - 85;
        int rowStep = 26;

        this.extractLimitEnabled = CharmConfig.POKEBAG_EXTRACT_LIMIT_ENABLED.get();

        // ── Row0: 容量 | 堆叠上限 ──
        this.bagSlotsField = addEditBox(leftFieldX, startY, FIELD_W, FIELD_H,
                String.valueOf(CharmConfig.POKEBAG_SLOTS.get()));
        this.maxStackField = addEditBox(rightFieldX, startY, FIELD_W, FIELD_H,
                String.valueOf(CharmConfig.POKEBAG_MAX_STACK_SIZE.get()));

        // ── Row1: 取出限制开关 | 每次取出 ──
        this.extractToggleBtn = this.addRenderableWidget(Button.builder(
                Component.translatable(getExtractToggleKey()),
                btn -> {
                    extractLimitEnabled = !extractLimitEnabled;
                    btn.setMessage(Component.translatable(getExtractToggleKey()));
                })
                .pos(leftFieldX, startY + rowStep).size(TOGGLE_W, BTN_H).build());

        this.extractLimitField = addEditBox(rightFieldX, startY + rowStep, FIELD_W, FIELD_H,
                String.valueOf(CharmConfig.POKEBAG_EXTRACT_LIMIT.get()));

        // ── Row2: 白名单按钮 ── [线性-RESP-02] 响应式宽度
        int listBtnW = Math.min(300, this.width - 30);
        int listBtnX = centerX - listBtnW / 2;
        int whitelistCount = CharmConfig.POKEBAG_WHITELIST_TAGS.get().size();
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.edit_whitelist")
                        .append(" §a[" + whitelistCount + "]"),
                btn -> Minecraft.getInstance().setScreen(
                        new PokebagTagListScreen(true, this)))
                .pos(listBtnX, startY + rowStep * 2 + 8)
                .size(listBtnW, BTN_H).build());

        // ── Row3: 黑名单按钮 ──
        int blacklistCount = CharmConfig.POKEBAG_BLACKLIST_TAGS.get().size();
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.edit_blacklist")
                        .append(" §7[" + blacklistCount + "]"),
                btn -> Minecraft.getInstance().setScreen(
                        new PokebagTagListScreen(false, this)))
                .pos(listBtnX, startY + rowStep * 3 + 8)
                .size(listBtnW, BTN_H).build());

        // ── Row4: 保存 | 返回 ──
        int saveBtnW = 100;
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.save"),
                btn -> saveConfig())
                .pos(centerX - saveBtnW - 4, startY + rowStep * 5 + 5)
                .size(saveBtnW, BTN_H).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.back"),
                btn -> Minecraft.getInstance().setScreen(null))
                .pos(centerX + 4, startY + rowStep * 5 + 5)
                .size(saveBtnW, BTN_H).build());
    }

    private EditBox addEditBox(int x, int y, int w, int h, String val) {
        EditBox box = new EditBox(this.font, x, y, w, h, Component.literal(""));
        box.setValue(val);
        box.setMaxLength(10);
        this.addRenderableWidget(box);
        return box;
    }

    private String getExtractToggleKey() {
        return extractLimitEnabled
                ? "coblemon_ts.pokebag_config.extract_on"
                : "coblemon_ts.pokebag_config.extract_off";
    }

    private void saveConfig() {
        try {
            int bagSlots = Integer.parseInt(this.bagSlotsField.getValue());
            bagSlots = Math.clamp(bagSlots, 1, 54);
            CharmConfig.POKEBAG_SLOTS.set(bagSlots);
            CharmConfig.POKEBAG_SLOTS.save();

            int maxStack = Integer.parseInt(this.maxStackField.getValue());
            maxStack = Math.clamp(maxStack, 1, 8192);
            CharmConfig.POKEBAG_MAX_STACK_SIZE.set(maxStack);
            CharmConfig.POKEBAG_MAX_STACK_SIZE.save();

            CharmConfig.POKEBAG_EXTRACT_LIMIT_ENABLED.set(extractLimitEnabled);
            CharmConfig.POKEBAG_EXTRACT_LIMIT_ENABLED.save();

            int extractLimit = Integer.parseInt(this.extractLimitField.getValue());
            extractLimit = Math.clamp(extractLimit, 1, 1024);
            CharmConfig.POKEBAG_EXTRACT_LIMIT.set(extractLimit);
            CharmConfig.POKEBAG_EXTRACT_LIMIT.save();

            if (Minecraft.getInstance().player != null)
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("coblemon_ts.pokebag_config.saved"), false);
        } catch (NumberFormatException e) {
            if (Minecraft.getInstance().player != null)
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("coblemon_ts.testbuild.input_error"), false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);

        int leftLabelX = centerX - 155;
        int rightLabelX = centerX + 10;
        int startY = this.height / 2 - 85;
        int rowStep = 26;
        int labelY1 = startY + 4;
        int labelY2 = labelY1 + rowStep;

        g.drawCenteredString(this.font, this.title, centerX, this.height / 2 - 105, 0xFFFFFF);

        // ── Row0标签：容量 / 堆叠上限 ──
        g.drawString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.bag_slots").getString(),
                leftLabelX, labelY1, 0xCCCCCC, false);
        g.drawString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.max_stack").getString(),
                rightLabelX, labelY1, 0xCCCCCC, false);

        // ── Row1标签：取出限制 / 每次取出 ──
        g.drawString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.extract_toggle").getString(),
                leftLabelX, labelY2, 0xCCCCCC, false);
        g.drawString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.extract_limit").getString(),
                rightLabelX, labelY2, 0xCCCCCC, false);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}