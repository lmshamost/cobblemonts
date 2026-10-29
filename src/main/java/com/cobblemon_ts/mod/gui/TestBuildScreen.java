// [线性-TB-V3] TestBuild重构：移除独立toggle按钮，标签自身红/绿着色+点击切换
package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import com.cobblemon_ts.mod.network.ClearAllCharmsPacket;
import com.cobblemon_ts.mod.network.TestBuildOpenStarterPacket;
import com.cobblemon_ts.mod.network.UpdateCharmStatePacket;
import com.cobblemon_ts.mod.network.RequestCharmStatesPacket;
import com.cobblemon_ts.mod.network.SyncCharmStatesS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestBuildScreen extends Screen {

    // [线性-TB-V3a] 护符属性编辑输入框
    private EditBox shinyBoostField;
    private EditBox catchBoostField;
    private EditBox expBoostField;
    private EditBox hatchBoostField;
    private EditBox expAllPercentField;
    private EditBox bagStackField;

    // [线性-TB-V3b] 护符状态（本地切换，红/绿着色）——key=charmId, value=是否装备
    private final Map<String, Boolean> charmStates = new LinkedHashMap<>();
    private boolean charmStatesLoaded = false;

    // [线性-TB-V3c] 布局常量（init时计算）
    private int centerX, col0X, col1X, charmStartY, rowStep;
    private int labelW = 75; // 标签文字区域宽度（点击区域）
    private int fieldW = 52;
    private int fieldH = 16;

    public TestBuildScreen() {
        super(Component.translatable("coblemon_ts.testbuild.title"));
    }

    @Override
    protected void init() {
        int btnW = 100, btnH = 20;
        centerX = this.width / 2;
        col0X = centerX - 125;
        col1X = centerX + 5;
        int startY = this.height / 2 - 80;
        rowStep = 25;

        // ===== 行0: 初始宝可梦选择 | 统一主题切换 =====
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.testbuild.starter_select"),
                btn -> { PacketDistributor.sendToServer(new TestBuildOpenStarterPacket()); this.onClose(); })
                .pos(col0X, startY).size(btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable(getThemeToggleKey()),
                btn -> {
                    boolean c = CobblemonTSConfig.LIGHT_PANEL_THEME.get();
                    CobblemonTSConfig.LIGHT_PANEL_THEME.set(!c);
                    CobblemonTSConfig.LIGHT_EDITOR_THEME.set(!c);
                    btn.setMessage(Component.translatable(getThemeToggleKey()));
                }).pos(col1X, startY).size(btnW, btnH).build());

        // ===== 行1: 颜色编辑器 | 清除所有护符 =====
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.testbuild.color_editor"),
                btn -> Minecraft.getInstance().setScreen(new ColorEditorScreen()))
                .pos(col0X, startY + rowStep).size(btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.testbuild.clear_charms"),
                btn -> { PacketDistributor.sendToServer(new ClearAllCharmsPacket()); this.onClose(); })
                .pos(col1X, startY + rowStep).size(btnW, btnH).build());

        // ===== 行2: 幸运方块权重 | HUD设置 =====
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.testbuild.lucky_weight"),
                btn -> Minecraft.getInstance().setScreen(new com.cobblemon_ts.mod.gui.lucky.LuckyWeightEditorScreen()))
                .pos(col0X, startY + rowStep * 2).size(btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.testbuild.hud_settings"),
                btn -> Minecraft.getInstance().setScreen(new HudSettingsScreen()))
                .pos(col1X, startY + rowStep * 2).size(btnW, btnH).build());

        // ===== 护符编辑区域（无独立toggle，标签即开关） =====
        charmStartY = startY + rowStep * 3 + 5;
        int fieldX_off = labelW + 3; // 字段与标签之间的偏移

        // [线性-TB-V3d] 初始化护符状态默认值
        String[] ids = {"shiny_charm", "catching_charm", "exp_charm", "oval_charm", "exp_all", "pokebag"};
        for (String id : ids) charmStates.putIfAbsent(id, false);

        // 闪光护符 (左) → 闪光概率
        this.shinyBoostField = addEditBox(col0X + fieldX_off, charmStartY + 1, fieldW, fieldH,
                String.format("%.1f", CharmConfig.SHINY_CHARM_BOOST.get()));

        // 防晃护符 (右) → 捕获倍率
        this.catchBoostField = addEditBox(col1X + fieldX_off, charmStartY + 1, fieldW, fieldH,
                String.format("%.1f", CharmConfig.CATCHING_CHARM_BOOST.get()));

        // 经验护符 (左, row+1) → 经验倍率
        this.expBoostField = addEditBox(col0X + fieldX_off, charmStartY + rowStep + 1, fieldW, fieldH,
                String.format("%.1f", CharmConfig.EXP_CHARM_BOOST.get()));

        // 圆形护符 (右, row+1) → 孵化倍率
        this.hatchBoostField = addEditBox(col1X + fieldX_off, charmStartY + rowStep + 1, fieldW, fieldH,
                String.format("%.1f", CharmConfig.OVAL_CHARM_HATCH_BOOST.get()));

        // Exp.All (左, row+2) → 经验共享%
        this.expAllPercentField = addEditBox(col0X + fieldX_off, charmStartY + rowStep * 2 + 1, fieldW, fieldH,
                String.valueOf(CharmConfig.EXP_ALL_PERCENT.get().intValue()));

        // 道具袋 (右, row+2) → 堆叠上限
        this.bagStackField = addEditBox(col1X + fieldX_off, charmStartY + rowStep * 2 + 1, fieldW, fieldH,
                String.valueOf(CharmConfig.POKEBAG_MAX_STACK_SIZE.get()));

        // ===== 保存按钮 (row+3) =====
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.testbuild.save_charm_config"),
                btn -> saveAll())
                .pos(centerX - btnW / 2, charmStartY + rowStep * 3 + 5)
                .size(btnW, btnH).build());

        // [线性-TB-V3e] 请求服务端护符状态
        SyncCharmStatesS2CPacket.resetForNewRequest();
        PacketDistributor.sendToServer(new RequestCharmStatesPacket());
    }

    // [线性-TB-V3f] 标签点击区域定义（6个护符的label区域）
    // 布局: 左列 [col0X, col0X+labelW], 右列 [col1X, col1X+labelW]
    // 行: charmStartY, +rowStep, +rowStep*2
    private int[] labelRect(String charmId) {
        return switch (charmId) {
            case "shiny_charm"  -> new int[]{col0X, charmStartY, col0X + labelW, charmStartY + fieldH + 2};
            case "catching_charm" -> new int[]{col1X, charmStartY, col1X + labelW, charmStartY + fieldH + 2};
            case "exp_charm"   -> new int[]{col0X, charmStartY + rowStep, col0X + labelW, charmStartY + rowStep + fieldH + 2};
            case "oval_charm"  -> new int[]{col1X, charmStartY + rowStep, col1X + labelW, charmStartY + rowStep + fieldH + 2};
            case "exp_all"     -> new int[]{col0X, charmStartY + rowStep * 2, col0X + labelW, charmStartY + rowStep * 2 + fieldH + 2};
            case "pokebag"     -> new int[]{col1X, charmStartY + rowStep * 2, col1X + labelW, charmStartY + rowStep * 2 + fieldH + 2};
            default -> new int[]{0, 0, 0, 0};
        };
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) { // 左键
            for (String charmId : charmStates.keySet()) {
                int[] r = labelRect(charmId);
                if (mx >= r[0] && mx <= r[2] && my >= r[1] && my <= r[3]) {
                    charmStates.put(charmId, !charmStates.get(charmId));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private EditBox addEditBox(int x, int y, int w, int h, String val) {
        EditBox box = new EditBox(this.font, x, y, w, h, Component.literal(""));
        box.setValue(val);
        this.addRenderableWidget(box);
        return box;
    }

    private String getThemeToggleKey() {
        boolean light = CobblemonTSConfig.LIGHT_PANEL_THEME.get();
        return light ? "coblemon_ts.testbuild.theme_light" : "coblemon_ts.testbuild.theme_dark";
    }

    private void saveAll() {
        try {
            double shiny = Double.parseDouble(this.shinyBoostField.getValue());
            CharmConfig.SHINY_CHARM_BOOST.set(shiny); CharmConfig.SHINY_CHARM_BOOST.save();

            double catchV = Double.parseDouble(this.catchBoostField.getValue());
            CharmConfig.CATCHING_CHARM_BOOST.set(catchV); CharmConfig.CATCHING_CHARM_BOOST.save();

            double exp = Double.parseDouble(this.expBoostField.getValue());
            CharmConfig.EXP_CHARM_BOOST.set(exp); CharmConfig.EXP_CHARM_BOOST.save();

            double hatch = Double.parseDouble(this.hatchBoostField.getValue());
            CharmConfig.OVAL_CHARM_HATCH_BOOST.set(hatch); CharmConfig.OVAL_CHARM_HATCH_BOOST.save();

            int expAll = Integer.parseInt(this.expAllPercentField.getValue());
            CharmConfig.EXP_ALL_PERCENT.set((double) expAll); CharmConfig.EXP_ALL_PERCENT.save();

            int bagStack = Integer.parseInt(this.bagStackField.getValue());
            CharmConfig.POKEBAG_MAX_STACK_SIZE.set(bagStack); CharmConfig.POKEBAG_MAX_STACK_SIZE.save();

            // [线性-TB-V3g] 同步护符状态到服务端（遍历charmStates）
            for (Map.Entry<String, Boolean> e : charmStates.entrySet()) {
                PacketDistributor.sendToServer(new UpdateCharmStatePacket(e.getKey(), e.getValue()));
            }

            if (Minecraft.getInstance().player != null)
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("coblemon_ts.testbuild.charm_saved"), false);
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

        int labelY_base = charmStartY + 4; // 标签文字的Y基线

        // [线性-TB-V3h] 标题
        g.drawCenteredString(this.font, this.title, centerX, this.height / 2 - 100, 0xFFFFFF);

        // [线性-TB-V3i] 护符标签着色渲染（红=未装备 / 绿=已装备）
        renderCharmLabel(g, "shiny_charm",   "coblemon_ts.testbuild.shiny_boost",   col0X, labelY_base);
        renderCharmLabel(g, "catching_charm","coblemon_ts.testbuild.catch_boost",   col1X, labelY_base);
        renderCharmLabel(g, "exp_charm",     "coblemon_ts.testbuild.exp_boost",     col0X, labelY_base + rowStep);
        renderCharmLabel(g, "oval_charm",    "coblemon_ts.testbuild.hatch_boost",   col1X, labelY_base + rowStep);
        renderCharmLabel(g, "exp_all",       "coblemon_ts.testbuild.exp_all_percent", col0X, labelY_base + rowStep * 2);
        renderCharmLabel(g, "pokebag",       "coblemon_ts.testbuild.bag_stack",     col1X, labelY_base + rowStep * 2);
    }

    private void renderCharmLabel(GuiGraphics g, String charmId, String translationKey, int x, int y) {
        boolean on = charmStates.getOrDefault(charmId, false);
        int color = on ? 0xFF_40FF40 : 0xFF_FF4040; // 绿/红
        String text = Component.translatable(translationKey).getString();
        g.drawString(this.font, text, x + 2, y, color, false);
    }

    @Override
    public void tick() {
        if (!charmStatesLoaded) {
            Map<String, Boolean> ss = SyncCharmStatesS2CPacket.pollLatestStates();
            if (ss != null) {
                for (Map.Entry<String, Boolean> e : ss.entrySet())
                    charmStates.put(e.getKey(), e.getValue());
                charmStatesLoaded = true;
            }
        }
        super.tick();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}