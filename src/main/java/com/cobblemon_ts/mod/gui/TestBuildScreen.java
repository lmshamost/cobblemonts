package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import com.cobblemon_ts.mod.network.ClearAllCharmsPacket;
import com.cobblemon_ts.mod.network.TestBuildOpenStarterPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class TestBuildScreen extends Screen {

    // [线性-T1] 护符属性编辑输入框
    private EditBox shinyBoostField;
    private EditBox catchBoostField;
    private EditBox expBoostField;
    private EditBox hatchBoostField;
    private EditBox expAllPercentField;

    public TestBuildScreen() {
        super(Component.literal("测试功能选择"));
    }

    @Override
    protected void init() {
        int btnW = 100;
        int btnH = 20;
        int centerX = this.width / 2;
        int col0X = centerX - 110;
        int col1X = centerX + 10;
        int startY = this.height / 2 - 60;
        int rowStep = 25;

        // [线性-T1-1] 按钮1: 初始宝可梦选择 (col 0)
        this.addRenderableWidget(Button.builder(
                Component.literal("宝可梦选择"),
                btn -> {
                    PacketDistributor.sendToServer(new TestBuildOpenStarterPacket());
                    this.onClose();
                })
                .pos(col0X, startY)
                .size(btnW, btnH)
                .build());

        // [线性-T1-2] 按钮2: 统一主题切换 (col 1)
        this.addRenderableWidget(Button.builder(
                Component.literal(getThemeToggleLabel()),
                btn -> {
                    boolean current = CobblemonTSConfig.LIGHT_PANEL_THEME.get();
                    CobblemonTSConfig.LIGHT_PANEL_THEME.set(!current);
                    CobblemonTSConfig.LIGHT_EDITOR_THEME.set(!current);
                    btn.setMessage(Component.literal(getThemeToggleLabel()));
                })
                .pos(col1X, startY)
                .size(btnW, btnH)
                .build());

        // [线性-T1-3] 按钮3: 颜色编辑器 (col 0, row 1)
        this.addRenderableWidget(Button.builder(
                Component.literal("颜色编辑器"),
                btn -> Minecraft.getInstance().setScreen(new ColorEditorScreen()))
                .pos(col0X, startY + rowStep)
                .size(btnW, btnH)
                .build());

        // [线性-T1-4] 按钮4: 清除所有护符 (col 1, row 1)
        this.addRenderableWidget(Button.builder(
                Component.literal("清除护符"),
                btn -> {
                    PacketDistributor.sendToServer(new ClearAllCharmsPacket());
                    this.onClose();
                })
                .pos(col1X, startY + rowStep)
                .size(btnW, btnH)
                .build());

        // [线性-T1-5] 按钮5: 幸运方块权重编辑器 (col 0, row 2)
        this.addRenderableWidget(Button.builder(
                Component.literal("幸运方块权重"),
                btn -> Minecraft.getInstance().setScreen(new com.cobblemon_ts.mod.gui.lucky.LuckyWeightEditorScreen()))
                .pos(col0X, startY + rowStep * 2)
                .size(btnW, btnH)
                .build());

        // [线性-T1-6] 按钮6: HUD设置 (col 1, row 2)
        this.addRenderableWidget(Button.builder(
                Component.literal("HUD设置"),
                btn -> Minecraft.getInstance().setScreen(new HudSettingsScreen()))
                .pos(col1X, startY + rowStep * 2)
                .size(btnW, btnH)
                .build());

        // ===== 护符属性编辑区域 =====
        int fieldW = 50;
        int fieldH = 16;
        int labelW = 48;
        int editStartY = startY + rowStep * 3 + 5;

        // Row 0: 闪光概率 | 捕获倍率
        String shinyVal = String.format("%.1f", CharmConfig.SHINY_CHARM_BOOST.get());
        this.shinyBoostField = new EditBox(this.font, col0X + labelW, editStartY, fieldW, fieldH, Component.literal(""));
        this.shinyBoostField.setValue(shinyVal);
        this.addRenderableWidget(this.shinyBoostField);

        String catchVal = String.format("%.1f", CharmConfig.CATCHING_CHARM_BOOST.get());
        this.catchBoostField = new EditBox(this.font, col1X + labelW, editStartY, fieldW, fieldH, Component.literal(""));
        this.catchBoostField.setValue(catchVal);
        this.addRenderableWidget(this.catchBoostField);

        // Row 1: 经验倍率 | 孵化倍率
        String expVal = String.format("%.1f", CharmConfig.EXP_CHARM_BOOST.get());
        this.expBoostField = new EditBox(this.font, col0X + labelW, editStartY + rowStep, fieldW, fieldH, Component.literal(""));
        this.expBoostField.setValue(expVal);
        this.addRenderableWidget(this.expBoostField);

        String hatchVal = String.format("%.1f", CharmConfig.OVAL_CHARM_HATCH_BOOST.get());
        this.hatchBoostField = new EditBox(this.font, col1X + labelW, editStartY + rowStep, fieldW, fieldH, Component.literal(""));
        this.hatchBoostField.setValue(hatchVal);
        this.addRenderableWidget(this.hatchBoostField);

        // Row 2: 经验共享%
        String expAllVal = String.valueOf(CharmConfig.EXP_ALL_PERCENT.get().intValue());
        this.expAllPercentField = new EditBox(this.font, col0X + labelW, editStartY + rowStep * 2, fieldW, fieldH, Component.literal(""));
        this.expAllPercentField.setValue(expAllVal);
        this.addRenderableWidget(this.expAllPercentField);

        // Row 3: 保存按钮
        this.addRenderableWidget(Button.builder(
                Component.literal("保存护符配置"),
                btn -> saveCharmConfig())
                .pos(col0X + labelW + fieldW + 5, editStartY + rowStep * 2)
                .size(btnW, btnH)
                .build());
    }

    // [线性-T2] 主题切换标签
    private String getThemeToggleLabel() {
        boolean light = CobblemonTSConfig.LIGHT_PANEL_THEME.get();
        return light ? "亮色主题" : "暗色主题";
    }

    // [线性-T3] 保存护符配置到 Config + 持久化
    private void saveCharmConfig() {
        try {
            double shiny = Double.parseDouble(this.shinyBoostField.getValue());
            CharmConfig.SHINY_CHARM_BOOST.set(shiny);
            CharmConfig.SHINY_CHARM_BOOST.save();

            double catchV = Double.parseDouble(this.catchBoostField.getValue());
            CharmConfig.CATCHING_CHARM_BOOST.set(catchV);
            CharmConfig.CATCHING_CHARM_BOOST.save();

            double exp = Double.parseDouble(this.expBoostField.getValue());
            CharmConfig.EXP_CHARM_BOOST.set(exp);
            CharmConfig.EXP_CHARM_BOOST.save();

            double hatch = Double.parseDouble(this.hatchBoostField.getValue());
            CharmConfig.OVAL_CHARM_HATCH_BOOST.set(hatch);
            CharmConfig.OVAL_CHARM_HATCH_BOOST.save();

            int expAll = Integer.parseInt(this.expAllPercentField.getValue());
            CharmConfig.EXP_ALL_PERCENT.set((double) expAll);
            CharmConfig.EXP_ALL_PERCENT.save();

            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§a护符配置已保存"), false);
            }
        } catch (NumberFormatException e) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§c输入格式错误，请检查数值"), false);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 60;
        int rowStep = 25;
        int col0X = centerX - 110;
        int col1X = centerX + 10;
        int labelW = 48;
        int editStartY = startY + rowStep * 3 + 5;

        // 标题
        graphics.drawCenteredString(this.font, this.title, centerX, this.height / 2 - 80, 0xFFFFFF);

        // [线性-T4] 护符编辑区域标签 (灰色提示)
        int labelColor = 0xFF_AAAAAA;
        int fieldLabelY = editStartY + 4;
        graphics.drawString(this.font, "闪光概率:", col0X, fieldLabelY, labelColor, false);
        graphics.drawString(this.font, "捕获倍率:", col1X, fieldLabelY, labelColor, false);
        graphics.drawString(this.font, "经验倍率:", col0X, fieldLabelY + rowStep, labelColor, false);
        graphics.drawString(this.font, "孵化倍率:", col1X, fieldLabelY + rowStep, labelColor, false);
        graphics.drawString(this.font, "经验共享%:", col0X, fieldLabelY + rowStep * 2, labelColor, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}