package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CobblemonTSConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

// [线性-H1] HUD设置面板：两列布局，所有开关按钮
public class HudSettingsScreen extends Screen {

    private static final int BTN_W = 155;
    private static final int BTN_H = 18;
    private static final int BTN_GAP = 2;
    private static final int COL_GAP = 4;
    private static final int ROW_STEP = BTN_H + BTN_GAP;

    public HudSettingsScreen() {
        super(Component.literal("HUD设置"));
    }

    @Override
    protected void init() {
        int leftX  = this.width / 2 - BTN_W - COL_GAP / 2;
        int rightX = this.width / 2 + COL_GAP / 2;
        int y = 28;

        // ==== 第1行: 药水效果 ====
        addToggleBtn(leftX,  y, "药水buff(右上)",   CobblemonTSConfig.SHOW_EFFECT_ICONS);
        addToggleBtn(rightX, y, "药水buff(背包内)",  CobblemonTSConfig.SHOW_INVENTORY_EFFECT_ICONS);
        y += ROW_STEP;

        // ==== 第2行: 统一彩色条总开关 ====
        addToggleBtn(leftX,  y, "统一彩色条总开关",   CobblemonTSConfig.HUD_UNIFIED_COLORED_BARS);
        addToggleBtn(rightX, y, "隐藏背包切换按钮",   CobblemonTSConfig.HIDE_TOGGLE_BUTTONS);
        y += ROW_STEP;

        // ==== 第3行: 逐项-血条/饥饿 ====
        addToggleBtn(leftX,  y, "逐项·自定义血条",   CobblemonTSConfig.HUD_CUSTOM_HEALTH_BAR);
        addToggleBtn(rightX, y, "逐项·自定义饥饿条", CobblemonTSConfig.HUD_CUSTOM_FOOD_BAR);
        y += ROW_STEP;

        // ==== 第4行: 覆盖层 ====
        addToggleBtn(leftX,  y, "吸收值金条覆盖",     CobblemonTSConfig.HUD_ABSORPTION_OVERLAY);
        addToggleBtn(rightX, y, "饱腹值金条覆盖",     CobblemonTSConfig.HUD_SATURATION_OVERLAY);
        y += ROW_STEP;

        // ==== 第5行: 盔甲/经验 ====
        addToggleBtn(leftX,  y, "逐项·自定义盔甲条",  CobblemonTSConfig.HUD_CUSTOM_ARMOR_BAR);
        addToggleBtn(rightX, y, "逐项·自定义经验条",  CobblemonTSConfig.HUD_CUSTOM_XP_BAR);
        y += ROW_STEP;

        // ==== 返回按钮 ====
        y += 6;
        int cx = this.width / 2;
        this.addRenderableWidget(Button.builder(
                Component.literal("← 返回"),
                btn -> this.onClose())
                .pos(cx - BTN_W / 2, y)
                .size(BTN_W, BTN_H)
                .build());
    }

    // [线性-H2] 开关按钮: 标签 + §a开/§c关
    private void addToggleBtn(int x, int y, String label, ModConfigSpec.BooleanValue config) {
        Button btn = Button.builder(
                Component.literal(formatLabel(label, config.get())),
                b -> {
                    config.set(!config.get());
                    config.save();
                    b.setMessage(Component.literal(formatLabel(label, config.get())));
                })
                .pos(x, y)
                .size(BTN_W, BTN_H)
                .build();
        this.addRenderableWidget(btn);
    }

    // [线性-H3] 格式化: "标签: §a开" / "标签: §c关"
    private static String formatLabel(String label, boolean on) {
        return label + ": " + (on ? "§a开" : "§c关");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title,
                this.width / 2, 10, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}