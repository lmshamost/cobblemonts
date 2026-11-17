package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CobblemonTSConfig;
import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.pokemonradar.PokemonRadarScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

// [线性-H1] HUD设置面板：两列布局，所有开关按钮
public class HudSettingsScreen extends Screen {

    private static final int BTN_W = 155;
    private static final int BTN_H = 18;
    private static final int BTN_GAP = 2;
    private static final int COL_GAP = 4;
    private static final int ROW_STEP = BTN_H + BTN_GAP;

    public HudSettingsScreen() {
        super(Text.translatable("cobblemon_ts.hud.title"));
    }

    @Override
    protected void init() {
        int leftX  = this.width / 2 - BTN_W - COL_GAP / 2;
        int rightX = this.width / 2 + COL_GAP / 2;
        int y = 28;

        // [线性-P01-reorder] ==== 第1行: 药水buff背包 + 左上角 ====
        addToggleBtn(leftX,  y, "cobblemon_ts.hud.effect_icons_inv",       CobblemonTSConfig.SHOW_INVENTORY_EFFECT_ICONS);
        addToggleBtn(rightX, y, "cobblemon_ts.hud.effect_icons_top",      CobblemonTSConfig.SHOW_EFFECT_ICONS);
        y += ROW_STEP;

        // [线性-P01-reorder] ==== 第2行: 药水隐藏闪烁 + 药水缩小 ====
        addToggleBtn(leftX,  y, "cobblemon_ts.hud.potion_hide_flash",     CobblemonTSConfig.HUD_POTION_HIDE_FLASH);
        addToggleBtn(rightX, y, "cobblemon_ts.hud.potion_shrink",          CobblemonTSConfig.HUD_POTION_SHRINK);
        y += ROW_STEP;

        // [线性-P01-reorder] ==== 第3行: 雷达编辑器 + 精灵信息缩放 ====
        addRadarEditorBtn(leftX, y);
        addToggleBtn(rightX, y, "cobblemon_ts.hud.summary_guiscale",       CobblemonTSConfig.HUD_FORCE_SUMMARY_GUISCALE);
        y += ROW_STEP;

        // ==== 第4行: 统一彩色条总开关 ====
        addToggleBtn(leftX,  y, "cobblemon_ts.hud.colored_bars",           CobblemonTSConfig.HUD_UNIFIED_COLORED_BARS);
        addToggleBtn(rightX, y, "cobblemon_ts.hud.hide_toggle_btns",       CobblemonTSConfig.HIDE_TOGGLE_BUTTONS);
        y += ROW_STEP;

        // ==== 第5行: 逐项-血条/饥饿 ====
        addToggleBtn(leftX,  y, "cobblemon_ts.hud.custom_health_bar",      CobblemonTSConfig.HUD_CUSTOM_HEALTH_BAR);
        addToggleBtn(rightX, y, "cobblemon_ts.hud.custom_food_bar",        CobblemonTSConfig.HUD_CUSTOM_FOOD_BAR);
        y += ROW_STEP;

        // ==== 第6行: 覆盖层 ====
        addToggleBtn(leftX,  y, "cobblemon_ts.hud.absorption_overlay",     CobblemonTSConfig.HUD_ABSORPTION_OVERLAY);
        addToggleBtn(rightX, y, "cobblemon_ts.hud.saturation_overlay",     CobblemonTSConfig.HUD_SATURATION_OVERLAY);
        y += ROW_STEP;

        // ==== 第7行: 盔甲/经验 ====
        addToggleBtn(leftX,  y, "cobblemon_ts.hud.custom_armor_bar",       CobblemonTSConfig.HUD_CUSTOM_ARMOR_BAR);
        addToggleBtn(rightX, y, "cobblemon_ts.hud.custom_xp_bar",          CobblemonTSConfig.HUD_CUSTOM_XP_BAR);
        y += ROW_STEP;

        // ==== 返回按钮 ====
        y += 6;
        int cx = this.width / 2;
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("cobblemon_ts.hud.close"),
                btn -> this.close())
                .position(cx - BTN_W / 2, y)
                .size(BTN_W, BTN_H)
                .build());
    }

    // [线性-P01] 雷达编辑器按钮：打开精灵雷达编辑界面
    private void addRadarEditorBtn(int x, int y) {
        Text label = Text.translatable("cobblemon_ts.hud.radar_editor");
        this.addDrawableChild(ButtonWidget.builder(label, btn -> {
            MinecraftClient.getInstance().setScreen(new PokemonRadarScreen());
        }).position(x, y).size(BTN_W, BTN_H).build());
    }

    // [线性-H2] 开关按钮: 翻译键标签 + §aON/§cOFF
    private void addToggleBtn(int x, int y, String labelKey, CharmConfig.BooleanValue config) {
        ButtonWidget btn = ButtonWidget.builder(
                Text.literal(formatLabel(labelKey, config.get())),
                b -> {
                    config.set(!config.get());
                    // CobblemonTSConfig.toNbt();
                    b.setMessage(Text.literal(formatLabel(labelKey, config.get())));
                })
                .position(x, y)
                .size(BTN_W, BTN_H)
                .build();
        this.addDrawableChild(btn);
    }

    // [线性-H3] 格式化: "标签: §aON" / "标签: §cOFF"
    private static String formatLabel(String labelKey, boolean on) {
        String label = Text.translatable(labelKey).getString();
        String status = Text.translatable(on ? "cobblemon_ts.hud.on" : "cobblemon_ts.hud.off").getString();
        return label + ": " + (on ? "§a" : "§c") + status;
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, 10, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}