package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CharmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PokebagConfigScreen extends Screen {

    private EditBox bagSlotsField;
    private EditBox maxStackField;
    private EditBox extractLimitField;
    private Button extractToggleBtn;
    private boolean extractLimitEnabled;

    private int centerX, leftLabelX, leftFieldX, startY, rowStep;
    private static final int LABEL_W = 75;
    private static final int FIELD_W = 52;
    private static final int FIELD_H = 16;
    private static final int BTN_W = 100;
    private static final int BTN_H = 20;

    public PokebagConfigScreen() {
        super(Component.translatable("coblemon_ts.pokebag_config.title"));
    }

    @Override
    protected void init() {
        centerX = this.width / 2;
        leftLabelX = centerX - 130;
        leftFieldX = leftLabelX + LABEL_W + 3;
        startY = this.height / 2 - 80;
        rowStep = 25;

        this.extractLimitEnabled = CharmConfig.POKEBAG_EXTRACT_LIMIT_ENABLED.get();

        // 行0: 容量 + 堆叠上限
        this.bagSlotsField = addEditBox(leftFieldX, startY, FIELD_W, FIELD_H,
                String.valueOf(CharmConfig.POKEBAG_SLOTS.get()));
        this.maxStackField = addEditBox(leftFieldX + 120, startY, FIELD_W, FIELD_H,
                String.valueOf(CharmConfig.POKEBAG_MAX_STACK_SIZE.get()));

        // 行1: 取出限制开关 + 每次取出
        this.extractToggleBtn = this.addRenderableWidget(Button.builder(
                Component.translatable(getExtractToggleKey()),
                btn -> {
                    extractLimitEnabled = !extractLimitEnabled;
                    btn.setMessage(Component.translatable(getExtractToggleKey()));
                })
                .pos(leftFieldX, startY + rowStep).size(BTN_W, BTN_H).build());

        this.extractLimitField = addEditBox(leftFieldX + 120, startY + rowStep, FIELD_W, FIELD_H,
                String.valueOf(CharmConfig.POKEBAG_EXTRACT_LIMIT.get()));

        // 行2: 编辑白名单按钮（含条目数）
        int whitelistCount = CharmConfig.POKEBAG_WHITELIST_TAGS.get().size();
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.edit_whitelist")
                        .append(" §a[" + whitelistCount + "]"),
                btn -> Minecraft.getInstance().setScreen(
                        new PokebagTagListScreen(true, this)))
                .pos(centerX - 115, startY + rowStep * 2 + 10)
                .size(230, BTN_H).build());

        // 行3: 编辑黑名单按钮（含条目数）
        int blacklistCount = CharmConfig.POKEBAG_BLACKLIST_TAGS.get().size();
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.edit_blacklist")
                        .append(" §7[" + blacklistCount + "]"),
                btn -> Minecraft.getInstance().setScreen(
                        new PokebagTagListScreen(false, this)))
                .pos(centerX - 115, startY + rowStep * 3 + 10)
                .size(230, BTN_H).build());

        // 底部: 保存 | 返回
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.save"),
                btn -> saveConfig())
                .pos(centerX - BTN_W - 5, startY + rowStep * 5 + 5)
                .size(BTN_W, BTN_H).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.back"),
                btn -> Minecraft.getInstance().setScreen(null))
                .pos(centerX + 5, startY + rowStep * 5 + 5)
                .size(BTN_W, BTN_H).build());
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

        int labelY = startY + 4;

        g.drawCenteredString(this.font, this.title, centerX, this.height / 2 - 100, 0xFFFFFF);

        // 容量 + 堆叠上限
        g.drawString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.bag_slots").getString(),
                leftLabelX, labelY, 0xCCCCCC, false);
        g.drawString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.max_stack").getString(),
                leftLabelX + 120, labelY, 0xCCCCCC, false);

        // 取出限制 + 每次取出
        g.drawString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.extract_toggle").getString(),
                leftLabelX, labelY + rowStep, 0xCCCCCC, false);
        g.drawString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.extract_limit").getString(),
                leftLabelX + 120, labelY + rowStep, 0xCCCCCC, false);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}