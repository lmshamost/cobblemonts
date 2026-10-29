package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CharmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PokebagConfigScreen extends Screen {

    private EditBox bagSlotsField;
    private EditBox maxStackField;
    private EditBox extractLimitField;
    private EditBox whitelistField;
    private EditBox blacklistField;
    private Button extractToggleBtn;

    private boolean extractLimitEnabled;

    private int centerX, leftLabelX, leftFieldX, rightLabelX, rightFieldX, startY, rowStep;
    private static final int LABEL_W = 75;
    private static final int FIELD_W = 52;
    private static final int FIELD_H = 16;
    private static final int WIDE_FIELD_W = 230;
    private static final int WIDE_FIELD_H = 32;
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
        rightLabelX = centerX + 5;
        rightFieldX = rightLabelX + LABEL_W + 3;
        startY = this.height / 2 - 80;
        rowStep = 25;

        this.extractLimitEnabled = CharmConfig.POKEBAG_EXTRACT_LIMIT_ENABLED.get();

        // 行0: 容量 | 堆叠上限
        this.bagSlotsField = addEditBox(leftFieldX, startY, FIELD_W, FIELD_H,
                String.valueOf(CharmConfig.POKEBAG_SLOTS.get()));
        this.maxStackField = addEditBox(leftFieldX, startY + rowStep, FIELD_W, FIELD_H,
                String.valueOf(CharmConfig.POKEBAG_MAX_STACK_SIZE.get()));

        // 行1: 取出限制开关
        this.extractToggleBtn = this.addRenderableWidget(Button.builder(
                Component.translatable(getExtractToggleKey()),
                btn -> {
                    extractLimitEnabled = !extractLimitEnabled;
                    btn.setMessage(Component.translatable(getExtractToggleKey()));
                })
                .pos(leftFieldX, startY + rowStep * 2).size(BTN_W, BTN_H).build());

        // 行1: 取出数量
        this.extractLimitField = addEditBox(leftFieldX, startY + rowStep * 3, FIELD_W, FIELD_H,
                String.valueOf(CharmConfig.POKEBAG_EXTRACT_LIMIT.get()));

        // 行2+: 白名单（宽字段）
        this.whitelistField = addEditBox(centerX - WIDE_FIELD_W / 2, startY + rowStep * 4 + 5, WIDE_FIELD_W, WIDE_FIELD_H,
                listToCsv(CharmConfig.POKEBAG_WHITELIST_TAGS.get()));

        // 行3+: 黑名单（宽字段）
        this.blacklistField = addEditBox(centerX - WIDE_FIELD_W / 2, startY + rowStep * 5 + 20, WIDE_FIELD_W, WIDE_FIELD_H,
                listToCsv(CharmConfig.POKEBAG_BLACKLIST_TAGS.get()));

        // 底部按钮: 保存 | 返回
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.save"),
                btn -> saveConfig())
                .pos(centerX - BTN_W - 5, startY + rowStep * 7 + 10).size(BTN_W, BTN_H).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.pokebag_config.back"),
                btn -> Minecraft.getInstance().setScreen(null))
                .pos(centerX + 5, startY + rowStep * 7 + 10).size(BTN_W, BTN_H).build());
    }

    private EditBox addEditBox(int x, int y, int w, int h, String val) {
        EditBox box = new EditBox(this.font, x, y, w, h, Component.literal(""));
        box.setValue(val);
        box.setMaxLength(2048);
        this.addRenderableWidget(box);
        return box;
    }

    private String getExtractToggleKey() {
        return extractLimitEnabled
                ? "coblemon_ts.pokebag_config.extract_on"
                : "coblemon_ts.pokebag_config.extract_off";
    }

    private static String listToCsv(List<? extends String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(",", list);
    }

    private static List<String> csvToList(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
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

            List<String> whitelist = csvToList(this.whitelistField.getValue());
            CharmConfig.POKEBAG_WHITELIST_TAGS.set(whitelist);
            CharmConfig.POKEBAG_WHITELIST_TAGS.save();

            List<String> blacklist = csvToList(this.blacklistField.getValue());
            CharmConfig.POKEBAG_BLACKLIST_TAGS.set(blacklist);
            CharmConfig.POKEBAG_BLACKLIST_TAGS.save();

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

        // 标题
        g.drawCenteredString(this.font, this.title, centerX, this.height / 2 - 100, 0xFFFFFF);

        // 行0: 容量
        g.drawString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.bag_slots").getString(),
                leftLabelX, labelY, 0xCCCCCC, false);
        // 堆叠上限
        g.drawString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.max_stack").getString(),
                leftLabelX, labelY + rowStep, 0xCCCCCC, false);

        // 行1: 取出限制
        g.drawString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.extract_toggle").getString(),
                leftLabelX, labelY + rowStep * 2, 0xCCCCCC, false);
        // 取出数量
        g.drawString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.extract_limit").getString(),
                leftLabelX, labelY + rowStep * 3, 0xCCCCCC, false);

        // 行2: 白名单
        g.drawString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.whitelist").getString(),
                centerX - WIDE_FIELD_W / 2, startY + rowStep * 4 - 10, 0xCCCCCC, false);

        // 行3: 黑名单
        g.drawString(this.font,
                Component.translatable("coblemon_ts.pokebag_config.blacklist").getString(),
                centerX - WIDE_FIELD_W / 2, startY + rowStep * 5 + 5, 0xCCCCCC, false);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}