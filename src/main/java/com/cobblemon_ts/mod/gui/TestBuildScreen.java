// [线性-TB-V2] TestBuild菜单重构：护符红绿按钮 + 道具袋堆叠 + Editor2样式
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

    // [线性-TB01] 护符属性编辑输入框
    private EditBox shinyBoostField;
    private EditBox catchBoostField;
    private EditBox expBoostField;
    private EditBox hatchBoostField;
    private EditBox expAllPercentField;
    // [线性-TB01a] 道具袋堆叠上限输入框
    private EditBox bagStackField;

    // [线性-TB01b] 护符开关按钮（红/绿）——存本地切换状态
    private final Map<String, CharmToggleButton> charmButtons = new LinkedHashMap<>();
    // [线性-TB01c] 是否已从服务端加载过护符状态（只加载一次，之后用本地状态）
    private boolean charmStatesLoaded = false;

    public TestBuildScreen() {
        super(Component.translatable("coblemon_ts.testbuild.title"));
    }

    @Override
    protected void init() {
        int btnW = 100;
        int btnH = 20;
        int centerX = this.width / 2;
        int col0X = centerX - 125;
        int col1X = centerX + 5;
        int startY = this.height / 2 - 80;
        int rowStep = 25;

        // ===== 行0: 初始宝可梦选择 | 统一主题切换 =====
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.testbuild.starter_select"),
                btn -> {
                    PacketDistributor.sendToServer(new TestBuildOpenStarterPacket());
                    this.onClose();
                })
                .pos(col0X, startY)
                .size(btnW, btnH)
                .build());

        this.addRenderableWidget(Button.builder(
                Component.translatable(getThemeToggleKey()),
                btn -> {
                    boolean current = CobblemonTSConfig.LIGHT_PANEL_THEME.get();
                    CobblemonTSConfig.LIGHT_PANEL_THEME.set(!current);
                    CobblemonTSConfig.LIGHT_EDITOR_THEME.set(!current);
                    btn.setMessage(Component.translatable(getThemeToggleKey()));
                })
                .pos(col1X, startY)
                .size(btnW, btnH)
                .build());

        // ===== 行1: 颜色编辑器 | 清除所有护符 =====
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.testbuild.color_editor"),
                btn -> Minecraft.getInstance().setScreen(new ColorEditorScreen()))
                .pos(col0X, startY + rowStep)
                .size(btnW, btnH)
                .build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.testbuild.clear_charms"),
                btn -> {
                    PacketDistributor.sendToServer(new ClearAllCharmsPacket());
                    this.onClose();
                })
                .pos(col1X, startY + rowStep)
                .size(btnW, btnH)
                .build());

        // ===== 行2: 幸运方块权重 | HUD设置 =====
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.testbuild.lucky_weight"),
                btn -> Minecraft.getInstance().setScreen(new com.cobblemon_ts.mod.gui.lucky.LuckyWeightEditorScreen()))
                .pos(col0X, startY + rowStep * 2)
                .size(btnW, btnH)
                .build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.testbuild.hud_settings"),
                btn -> Minecraft.getInstance().setScreen(new HudSettingsScreen()))
                .pos(col1X, startY + rowStep * 2)
                .size(btnW, btnH)
                .build());

        // ===== 护符开关 + 属性编辑区域 =====
        int toggleW = 90;
        int toggleH = 18;
        int fieldW = 48;
        int fieldH = 16;
        int labelW = 85;                                                         // 英文标签足够宽度
        int charmStartY = startY + rowStep * 3 + 5;

        // [线性-TB02] 初始化护符开关按钮 + 数值输入框
        charmButtons.clear();
        int currentRow = 0;

        // 闪光护符 (左) → 闪光概率
        addCharmToggle("shiny_charm", col0X, charmStartY, toggleW, toggleH);
        this.shinyBoostField = addEditBox(col0X + labelW, charmStartY + 1, fieldW, fieldH,
                String.format("%.1f", CharmConfig.SHINY_CHARM_BOOST.get()));

        // 防晃护符 (右) → 捕获倍率
        addCharmToggle("catching_charm", col1X, charmStartY, toggleW, toggleH);
        this.catchBoostField = addEditBox(col1X + labelW, charmStartY + 1, fieldW, fieldH,
                String.format("%.1f", CharmConfig.CATCHING_CHARM_BOOST.get()));

        // 经验护符 (左, row+1) → 经验倍率
        addCharmToggle("exp_charm", col0X, charmStartY + rowStep, toggleW, toggleH);
        this.expBoostField = addEditBox(col0X + labelW, charmStartY + rowStep + 1, fieldW, fieldH,
                String.format("%.1f", CharmConfig.EXP_CHARM_BOOST.get()));

        // 圆形护符 (右, row+1) → 孵化倍率
        addCharmToggle("oval_charm", col1X, charmStartY + rowStep, toggleW, toggleH);
        this.hatchBoostField = addEditBox(col1X + labelW, charmStartY + rowStep + 1, fieldW, fieldH,
                String.format("%.1f", CharmConfig.OVAL_CHARM_HATCH_BOOST.get()));

        // Exp.All (左, row+2) → 经验共享%
        addCharmToggle("exp_all", col0X, charmStartY + rowStep * 2, toggleW, toggleH);
        this.expAllPercentField = addEditBox(col0X + labelW, charmStartY + rowStep * 2 + 1, fieldW, fieldH,
                String.valueOf(CharmConfig.EXP_ALL_PERCENT.get().intValue()));

        // 道具袋 (右, row+2) → 道具袋堆叠
        addCharmToggle("pokebag", col1X, charmStartY + rowStep * 2, toggleW, toggleH);
        this.bagStackField = addEditBox(col1X + labelW, charmStartY + rowStep * 2 + 1, fieldW, fieldH,
                String.valueOf(CharmConfig.POKEBAG_MAX_STACK_SIZE.get()));

        // ===== 保存按钮 (row+3) =====
        this.addRenderableWidget(Button.builder(
                Component.translatable("coblemon_ts.testbuild.save_charm_config"),
                btn -> saveAll())
                .pos(centerX - btnW / 2, charmStartY + rowStep * 3 + 5)
                .size(btnW, btnH)
                .build());

        // [线性-TB03] 发起新的护符状态请求（清空旧响应，避免脏读）
        SyncCharmStatesS2CPacket.resetForNewRequest();
        PacketDistributor.sendToServer(new RequestCharmStatesPacket());
    }

    // [线性-TB04] 创建护符红/绿开关按钮
    private void addCharmToggle(String charmId, int x, int y, int w, int h) {
        CharmToggleButton btn = new CharmToggleButton(x, y, w, h, charmId);
        charmButtons.put(charmId, btn);
        this.addRenderableWidget(btn);
    }

    // [线性-TB05] 创建数值编辑框
    private EditBox addEditBox(int x, int y, int w, int h, String val) {
        EditBox box = new EditBox(this.font, x, y, w, h, Component.literal(""));
        box.setValue(val);
        this.addRenderableWidget(box);
        return box;
    }

    // [线性-TB06] 主题切换键
    private String getThemeToggleKey() {
        boolean light = CobblemonTSConfig.LIGHT_PANEL_THEME.get();
        return light ? "coblemon_ts.testbuild.theme_light" : "coblemon_ts.testbuild.theme_dark";
    }

    // [线性-TB07] 保存全部：Config + 护符状态同步到服务端
    private void saveAll() {
        try {
            // [线性-TB07a] 保存护符数值配置（与旧逻辑一致）
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

            // [线性-TB07b] 道具袋堆叠
            int bagStack = Integer.parseInt(this.bagStackField.getValue());
            CharmConfig.POKEBAG_MAX_STACK_SIZE.set(bagStack);
            CharmConfig.POKEBAG_MAX_STACK_SIZE.save();

            // [线性-TB07c] 同步护符状态到服务端
            for (Map.Entry<String, CharmToggleButton> entry : charmButtons.entrySet()) {
                PacketDistributor.sendToServer(
                        new UpdateCharmStatePacket(entry.getKey(), entry.getValue().isOwned()));
            }

            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("coblemon_ts.testbuild.charm_saved"), false);
            }
        } catch (NumberFormatException e) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("coblemon_ts.testbuild.input_error"), false);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 80;
        int rowStep = 25;
        int col0X = centerX - 125;
        int col1X = centerX + 5;
        int labelW = 85;
        int charmStartY = startY + rowStep * 3 + 5;
        int labelColor = 0xFF_AAAAAA;
        int fieldLabelY = charmStartY + 5;

        // [线性-TB08] 标题
        graphics.drawCenteredString(this.font, this.title, centerX, this.height / 2 - 100, 0xFFFFFF);

        // [线性-TB08a] 护符编辑区域标签（与init对齐）
        graphics.drawString(this.font, Component.translatable("coblemon_ts.testbuild.shiny_boost").getString(),
                col0X + labelW + 2, fieldLabelY, labelColor, false);
        graphics.drawString(this.font, Component.translatable("coblemon_ts.testbuild.catch_boost").getString(),
                col1X + labelW + 2, fieldLabelY, labelColor, false);
        graphics.drawString(this.font, Component.translatable("coblemon_ts.testbuild.exp_boost").getString(),
                col0X + labelW + 2, fieldLabelY + rowStep, labelColor, false);
        graphics.drawString(this.font, Component.translatable("coblemon_ts.testbuild.hatch_boost").getString(),
                col1X + labelW + 2, fieldLabelY + rowStep, labelColor, false);
        graphics.drawString(this.font, Component.translatable("coblemon_ts.testbuild.exp_all_percent").getString(),
                col0X + labelW + 2, fieldLabelY + rowStep * 2, labelColor, false);
        // [线性-TB08b] 道具袋堆叠标签
        graphics.drawString(this.font, Component.translatable("coblemon_ts.testbuild.bag_stack").getString(),
                col1X + labelW + 2, fieldLabelY + rowStep * 2, labelColor, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        // [线性-TB09] 轮询服务端响应：一次性消费，不缓存不保留
        if (!charmStatesLoaded) {
            Map<String, Boolean> states = SyncCharmStatesS2CPacket.pollLatestStates();
            if (states != null) {
                for (Map.Entry<String, CharmToggleButton> entry : charmButtons.entrySet()) {
                    Boolean owned = states.get(entry.getKey());
                    if (owned != null) {
                        entry.getValue().setOwned(owned);
                    }
                }
                charmStatesLoaded = true;
            }
        }
        super.tick();
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    // ==================== 护符开关按钮内部类 ====================

    // [线性-TB10] 红/绿护符开关按钮：红=未装备 绿=已装备
    private static class CharmToggleButton extends net.minecraft.client.gui.components.Button {
        private boolean owned;
        private final String charmId;

        public CharmToggleButton(int x, int y, int w, int h, String charmId) {
            super(x, y, w, h, Component.empty(), b -> {
                // [线性-TB10a] 点击切换状态
                if (b instanceof CharmToggleButton cb) {
                    cb.owned = !cb.owned;
                }
            }, DEFAULT_NARRATION);
            this.charmId = charmId;
            this.owned = false; // 初始默认未装备，等待服务端同步
        }

        public boolean isOwned() { return owned; }
        public void setOwned(boolean o) { this.owned = o; }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            int bgColor = owned ? 0xEE_40A040 : 0xEE_C04040; // [线性-TB10b] 绿/红
            int borderColor = owned ? 0xFF_60D060 : 0xFF_D06060;
            int x = getX(), y = getY();

            // [线性-TB10c] 背景 + 边框
            graphics.fill(x, y, x + width, y + height, bgColor);
            graphics.fill(x, y, x + width, y + 1, borderColor);
            graphics.fill(x, y + height - 1, x + width, y + height, borderColor);
            graphics.fill(x, y, x + 1, y + height, borderColor);
            graphics.fill(x + width - 1, y, x + width, y + height, borderColor);

            // [线性-TB10d] 文字：护符名
            String charmName = Component.translatable("item.coblemon_ts." + charmId).getString();
            graphics.drawCenteredString(Minecraft.getInstance().font,
                    charmName, x + width / 2, y + (height - 8) / 2, 0xFFFFFF);

            // [线性-TB10e] 焦点边框
            if (isFocused()) {
                graphics.fill(x - 1, y - 1, x + width + 1, y, 0xFF_FFFFFF);
                graphics.fill(x - 1, y + height, x + width + 1, y + height + 1, 0xFF_FFFFFF);
                graphics.fill(x - 1, y, x, y + height, 0xFF_FFFFFF);
                graphics.fill(x + width, y, x + width + 1, y + height, 0xFF_FFFFFF);
            }
        }
    }
}