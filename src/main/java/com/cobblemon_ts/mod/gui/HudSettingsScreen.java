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
        super(Component.translatable("coblemon_ts.hud.title"));
    }

    @Override
    protected void init() {
        int leftX  = this.width / 2 - BTN_W - COL_GAP / 2;
        int rightX = this.width / 2 + COL_GAP / 2;
        int y = 28;

        // ==== 第1行: 药水效果 ====
        addToggleBtn(leftX,  y, "coblemon_ts.hud.effect_icons_top",      CobblemonTSConfig.SHOW_EFFECT_ICONS);
        addToggleBtn(rightX, y, "coblemon_ts.hud.effect_icons_inv",       CobblemonTSConfig.SHOW_INVENTORY_EFFECT_ICONS);
        y += ROW_STEP;

        // ==== 第2行: 统一彩色条总开关 ====
        addToggleBtn(leftX,  y, "coblemon_ts.hud.colored_bars",           CobblemonTSConfig.HUD_UNIFIED_COLORED_BARS);
        addToggleBtn(rightX, y, "coblemon_ts.hud.hide_toggle_btns",       CobblemonTSConfig.HIDE_TOGGLE_BUTTONS);
        y += ROW_STEP;

        // ==== 第3行: 逐项-血条/饥饿 ====
        addToggleBtn(leftX,  y, "coblemon_ts.hud.custom_health_bar",      CobblemonTSConfig.HUD_CUSTOM_HEALTH_BAR);
        addToggleBtn(rightX, y, "coblemon_ts.hud.custom_food_bar",        CobblemonTSConfig.HUD_CUSTOM_FOOD_BAR);
        y += ROW_STEP;

        // ==== 第4行: 覆盖层 ====
        addToggleBtn(leftX,  y, "coblemon_ts.hud.absorption_overlay",     CobblemonTSConfig.HUD_ABSORPTION_OVERLAY);
        addToggleBtn(rightX, y, "coblemon_ts.hud.saturation_overlay",     CobblemonTSConfig.HUD_SATURATION_OVERLAY);
        y += ROW_STEP;

        // ==== 第5行: 盔甲/经验 ====
        addToggleBtn(leftX,  y, "coblemon_ts.hud.custom_armor_bar",       CobblemonTSConfig.HUD_CUSTOM_ARMOR_BAR);
        addToggleBtn(rightX, y, "coblemon_ts.hud.custom_xp_bar",          CobblemonTSConfig.HUD_CUSTOM_XP_BAR);
        y += ROW_STEP;

        // ==== 返回按钮 ====
        y += 6;
        int cx = this.width / 2;
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.hud.back"),
                btn -> this.onClose())
                .pos(cx - BTN_W / 2, y)
                .size(BTN_W, BTN_H)
                .build());
    }

    // [线性-H2] 开关按钮: 翻译键标签 + §aON/§cOFF
    private void addToggleBtn(int x, int y, String labelKey, ModConfigSpec.BooleanValue config) {
        Button btn = Button.builder(
                Component.literal(formatLabel(labelKey, config.get())),
                b -> {
                    config.set(!config.get());
                    config.save();
                    b.setMessage(Component.literal(formatLabel(labelKey, config.get())));
                })
                .pos(x, y)
                .size(BTN_W, BTN_H)
                .build();
        this.addRenderableWidget(btn);
    }

    // [线性-H3] 格式化: "标签: §aON" / "标签: §cOFF"
    private static String formatLabel(String labelKey, boolean on) {
        String label = Component.translatable(labelKey).getString();
        String status = Component.translatable(on ? "coblemon_ts.hud.on" : "coblemon_ts.hud.off").getString();
        return label + ": " + (on ? "§a" : "§c") + status;
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