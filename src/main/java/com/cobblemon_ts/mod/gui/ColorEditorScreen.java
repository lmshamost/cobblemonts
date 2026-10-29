package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CobblemonTSConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// [线性-L1] 颜色编辑器GUI：精灵栏/护符栏/宝可梦编辑器 颜色自定义
//   Tab布局：亮色一行(精灵栏→护符栏→编辑器) / 暗色一行(精灵栏→护符栏→编辑器)
//   竖向RGB输入 + 十六进制可编辑输入框 + 颜色预览 + 应用按钮
// [线性-RESP-01] 面板尺寸响应式：根据屏幕尺寸动态计算，支持GUI缩放
public class ColorEditorScreen extends Screen {

    private static final int BASE_PANEL_W = 620;
    private static final int BASE_PANEL_H = 380;
    private static final int MIN_PANEL_W = 400;
    private static final int MIN_PANEL_H = 260;
    private int PANEL_W, PANEL_H;
    private int panelX, panelY;

    // [线性-L10] tabLayout {{0,2,4},{1,3,5}} → 上排全亮色, 下排全暗色, 每排固定: 精灵栏→护符栏→编辑器
    private int activeTab = 0;
    // [线性-L10] tab翻译键 | tab translation keys: party/charm/editor × light/dark
    private static final String[] TAB_KEYS = {
        "coblemon_ts.coloreditor.tab.party_light",
        "coblemon_ts.coloreditor.tab.party_dark",
        "coblemon_ts.coloreditor.tab.charm_light",
        "coblemon_ts.coloreditor.tab.charm_dark",
        "coblemon_ts.coloreditor.tab.editor_light",
        "coblemon_ts.coloreditor.tab.editor_dark"
    };

    // 当前组内选中的颜色项索引
    private int selectedColorIndex = 0;

    // RGB输入框 + 十六进制输入框
    private EditBox rField, gField, bField, hexField;
    private Button applyBtn;

    // 每组颜色项翻译键 | color item translation keys per group
    private static final String[][] COLOR_ITEM_KEYS = {
        // 0: 精灵栏亮色
        {"coblemon_ts.coloreditor.item.panel_bg","coblemon_ts.coloreditor.item.panel_border","coblemon_ts.coloreditor.item.panel_text",
         "coblemon_ts.coloreditor.item.slot_bg","coblemon_ts.coloreditor.item.slot_hover_bg","coblemon_ts.coloreditor.item.slot_border",
         "coblemon_ts.coloreditor.item.slot_selected_border","coblemon_ts.coloreditor.item.slot_hover_border"},
        // 1: 精灵栏暗色
        {"coblemon_ts.coloreditor.item.panel_outer","coblemon_ts.coloreditor.item.panel_inner","coblemon_ts.coloreditor.item.panel_text"},
        // 2: 护符栏亮色
        {"coblemon_ts.coloreditor.item.panel_bg","coblemon_ts.coloreditor.item.panel_border","coblemon_ts.coloreditor.item.panel_text",
         "coblemon_ts.coloreditor.item.slot_bg","coblemon_ts.coloreditor.item.slot_hover_bg","coblemon_ts.coloreditor.item.slot_border",
         "coblemon_ts.coloreditor.item.slot_selected_border","coblemon_ts.coloreditor.item.slot_hover_border"},
        // 3: 护符栏暗色
        {"coblemon_ts.coloreditor.item.panel_outer","coblemon_ts.coloreditor.item.panel_inner","coblemon_ts.coloreditor.item.panel_text"},
        // 4: 编辑器亮色
        {"coblemon_ts.coloreditor.item.container_bg","coblemon_ts.coloreditor.item.container_border","coblemon_ts.coloreditor.item.label_text",
         "coblemon_ts.coloreditor.item.input_bg","coblemon_ts.coloreditor.item.input_border","coblemon_ts.coloreditor.item.input_text",
         "coblemon_ts.coloreditor.item.tab_active"},
        // 5: 编辑器暗色
        {"coblemon_ts.coloreditor.item.container_bg","coblemon_ts.coloreditor.item.container_border","coblemon_ts.coloreditor.item.label_text",
         "coblemon_ts.coloreditor.item.input_bg","coblemon_ts.coloreditor.item.input_border","coblemon_ts.coloreditor.item.input_text",
         "coblemon_ts.coloreditor.item.tab_active"},
    };

    public ColorEditorScreen() {
        super(Component.translatable("coblemon_ts.coloreditor.title"));
    }

    @Override
    protected void init() {
        // [线性-RESP-01] 响应式面板尺寸：取基础值和屏幕可用空间的较小值，不低于最小值
        PANEL_W = Math.min(BASE_PANEL_W, Math.max(MIN_PANEL_W, this.width - 20));
        PANEL_H = Math.min(BASE_PANEL_H, Math.max(MIN_PANEL_H, this.height - 10));
        panelX = (this.width - PANEL_W) / 2;
        panelY = (this.height - PANEL_H) / 2;

        // Tab按钮（6个Tab，分2行）
        int tabW = 90;
        int tabH = 18;
        int tabY1 = panelY + 5;
        int tabY2 = tabY1 + tabH + 3;
        int[][] tabLayout = {{0, 2, 4}, {1, 3, 5}}; // 第1行=亮色(精灵栏→护符栏→编辑器) / 第2行=暗色
        for (int row = 0; row < tabLayout.length; row++) {
            for (int col = 0; col < tabLayout[row].length; col++) {
                int i = tabLayout[row][col];
                int tabX = panelX + 10 + col * (tabW + 4);
                int tabY = row == 0 ? tabY1 : tabY2;
                int finalI = i;
                this.addRenderableWidget(Button.builder(
                        Component.translatable(TAB_KEYS[i]),
                        btn -> { activeTab = finalI; selectedColorIndex = 0; refreshRGBFields(); clearAndRebuild(); })
                        .pos(tabX, tabY)
                        .size(tabW, tabH)
                        .build());
            }
        }

        // 颜色项列表（左侧）
        int listX = panelX + 10;
        int listY = tabY2 + tabH + 8;
        int listW = 120;
        int itemH = 16;
        String[] items = COLOR_ITEM_KEYS[activeTab];
        for (int i = 0; i < items.length; i++) {
            int idx = i;
            this.addRenderableWidget(Button.builder(
                    Component.translatable(items[i]),
                    btn -> { selectedColorIndex = idx; refreshRGBFields(); })
                    .pos(listX, listY + i * (itemH + 2))
                    .size(listW, itemH)
                    .build());
        }

        // ===== RGB输入区域：竖向排列 =====
        int fieldX = listX + listW + 20;
        int fieldW = 52;
        int fieldH = 16;
        int labelGap = 14;
        int rowSpacing = fieldH + 6;

        int rY = listY;
        int gY = rY + rowSpacing;
        int bY = gY + rowSpacing;
        int hexY = bY + rowSpacing + 4; // 十六进制行与B行之间留4px间距

        rField = new EditBox(this.font, fieldX + labelGap, rY, fieldW, fieldH, Component.literal("R"));
        rField.setMaxLength(3);
        rField.setFilter(s -> s.matches("\\d{0,3}"));
        rField.setResponder(s -> { updateColorPreview(); updateHexFromRGB(); });
        this.addRenderableWidget(rField);

        gField = new EditBox(this.font, fieldX + labelGap, gY, fieldW, fieldH, Component.literal("G"));
        gField.setMaxLength(3);
        gField.setFilter(s -> s.matches("\\d{0,3}"));
        gField.setResponder(s -> { updateColorPreview(); updateHexFromRGB(); });
        this.addRenderableWidget(gField);

        bField = new EditBox(this.font, fieldX + labelGap, bY, fieldW, fieldH, Component.literal("B"));
        bField.setMaxLength(3);
        bField.setFilter(s -> s.matches("\\d{0,3}"));
        bField.setResponder(s -> { updateColorPreview(); updateHexFromRGB(); });
        this.addRenderableWidget(bField);

        // [线性-L3] 十六进制颜色码输入框（可编辑）
        hexField = new EditBox(this.font, fieldX + labelGap + 6, hexY, fieldW + 26, fieldH, Component.literal("0x"));
        hexField.setMaxLength(10); // 0xRRGGBBAA = 10字符
        hexField.setFilter(s -> s.matches("0[xX][0-9a-fA-F]{0,8}") || s.matches("[0-9a-fA-F]{0,8}"));
        hexField.setResponder(s -> { updateRGBFromHex(); updateColorPreview(); });
        this.addRenderableWidget(hexField);

        // 应用按钮（放在十六进制输入框下面）
        int btnY = hexY + rowSpacing + 2;
        applyBtn = Button.builder(Component.translatable("coblemon_ts.coloreditor.apply"), btn -> applyColor())
                .pos(fieldX + labelGap, btnY)
                .size(66, 18)
                .build();
        this.addRenderableWidget(applyBtn);

        // 返回按钮
        this.addRenderableWidget(Button.builder(Component.translatable("coblemon_ts.coloreditor.back"),
                btn -> this.onClose())
                .pos(panelX + PANEL_W - 55, panelY + PANEL_H - 25)
                .size(45, 20)
                .build());

        refreshRGBFields();
    }

    /** 切换Tab时重建界面 */
    private void clearAndRebuild() {
        this.clearWidgets();
        this.init();
    }

    private void refreshRGBFields() {
        int color = getCurrentColor();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        if (rField != null) rField.setValue(String.valueOf(r));
        if (gField != null) gField.setValue(String.valueOf(g));
        if (bField != null) bField.setValue(String.valueOf(b));
        if (hexField != null) hexField.setValue(String.format("0x%08X", color));
    }

    /** [线性-L3] RGB字段变化时同步更新十六进制输入框 */
    private void updateHexFromRGB() {
        try {
            String newHex = String.format("0x%08X",
                    0xFF000000 | (clamp(rField.getValue()) << 16) | (clamp(gField.getValue()) << 8) | clamp(bField.getValue()));
            if (newHex.equals(hexField.getValue())) return; // 值相同不触发setValue，避免循环
            if (hexField != null) hexField.setValue(newHex);
        } catch (NumberFormatException ignored) {}
    }

    /** [线性-L3] 十六进制输入框变化时同步更新RGB字段 */
    private void updateRGBFromHex() {
        try {
            String hex = hexField.getValue();
            if (hex.isEmpty()) return;
            if (hex.startsWith("0x") || hex.startsWith("0X")) hex = hex.substring(2);
            if (hex.isEmpty()) return;
            long val = Long.parseLong(hex, 16);
            int r = (int) ((val >> 16) & 0xFF);
            int g = (int) ((val >> 8) & 0xFF);
            int b = (int) (val & 0xFF);
            String sr = String.valueOf(r), sg = String.valueOf(g), sb = String.valueOf(b);
            if (sr.equals(rField.getValue()) && sg.equals(gField.getValue()) && sb.equals(bField.getValue())) return; // 值相同跳过
            if (rField != null) rField.setValue(sr);
            if (gField != null) gField.setValue(sg);
            if (bField != null) bField.setValue(sb);
        } catch (NumberFormatException ignored) {}
    }

    private int getCurrentColor() {
        return switch (activeTab) {
            // 精灵栏亮色
            case 0 -> switch (selectedColorIndex) {
                case 0 -> ColorEditorConfig.panelLightBg();
                case 1 -> ColorEditorConfig.panelLightBorder();
                case 2 -> ColorEditorConfig.panelLightText();
                case 3 -> ColorEditorConfig.panelLightSlotBg();
                case 4 -> ColorEditorConfig.panelLightSlotHoverBg();
                case 5 -> ColorEditorConfig.panelLightSlotBorder();
                case 6 -> ColorEditorConfig.panelLightSlotSelectedBorder();
                case 7 -> ColorEditorConfig.panelLightSlotHoverBorder();
                default -> 0;
            };
            // 精灵栏暗色
            case 1 -> switch (selectedColorIndex) {
                case 0 -> ColorEditorConfig.panelDarkOuter();
                case 1 -> ColorEditorConfig.panelDarkInner();
                case 2 -> ColorEditorConfig.panelDarkText();
                default -> 0;
            };
            // [线性-L2-1] 护符栏亮色（与精灵栏共用颜色配置）
            case 2 -> switch (selectedColorIndex) {
                case 0 -> ColorEditorConfig.panelLightBg();
                case 1 -> ColorEditorConfig.panelLightBorder();
                case 2 -> ColorEditorConfig.panelLightText();
                case 3 -> ColorEditorConfig.panelLightSlotBg();
                case 4 -> ColorEditorConfig.panelLightSlotHoverBg();
                case 5 -> ColorEditorConfig.panelLightSlotBorder();
                case 6 -> ColorEditorConfig.panelLightSlotSelectedBorder();
                case 7 -> ColorEditorConfig.panelLightSlotHoverBorder();
                default -> 0;
            };
            // [线性-L2-2] 护符栏暗色（与精灵栏共用颜色配置）
            case 3 -> switch (selectedColorIndex) {
                case 0 -> ColorEditorConfig.panelDarkOuter();
                case 1 -> ColorEditorConfig.panelDarkInner();
                case 2 -> ColorEditorConfig.panelDarkText();
                default -> 0;
            };
            // 编辑器亮色
            case 4 -> switch (selectedColorIndex) {
                case 0 -> ColorEditorConfig.editorLightContainerBg();
                case 1 -> ColorEditorConfig.editorLightContainerBorder();
                case 2 -> ColorEditorConfig.editorLightLabel();
                case 3 -> ColorEditorConfig.editorLightInputBg();
                case 4 -> ColorEditorConfig.editorLightInputBorder();
                case 5 -> ColorEditorConfig.editorLightInputText();
                case 6 -> ColorEditorConfig.editorLightTabActive();
                default -> 0;
            };
            // 编辑器暗色
            case 5 -> switch (selectedColorIndex) {
                case 0 -> ColorEditorConfig.editorDarkContainerBg();
                case 1 -> ColorEditorConfig.editorDarkContainerBorder();
                case 2 -> ColorEditorConfig.editorDarkLabel();
                case 3 -> ColorEditorConfig.editorDarkInputBg();
                case 4 -> ColorEditorConfig.editorDarkInputBorder();
                case 5 -> ColorEditorConfig.editorDarkInputText();
                case 6 -> ColorEditorConfig.editorDarkTabActive();
                default -> 0;
            };
            default -> 0;
        };
    }

    private void setCurrentColor(int color) {
        switch (activeTab) {
            // 精灵栏亮色
            case 0 -> {
                switch (selectedColorIndex) {
                    case 0 -> ColorEditorConfig.PANEL_LIGHT_BG.set(color);
                    case 1 -> ColorEditorConfig.PANEL_LIGHT_BORDER.set(color);
                    case 2 -> ColorEditorConfig.PANEL_LIGHT_TEXT.set(color);
                    case 3 -> ColorEditorConfig.PANEL_LIGHT_SLOT_BG.set(color);
                    case 4 -> ColorEditorConfig.PANEL_LIGHT_SLOT_HOVER_BG.set(color);
                    case 5 -> ColorEditorConfig.PANEL_LIGHT_SLOT_BORDER.set(color);
                    case 6 -> ColorEditorConfig.PANEL_LIGHT_SLOT_SELECTED_BORDER.set(color);
                    case 7 -> ColorEditorConfig.PANEL_LIGHT_SLOT_HOVER_BORDER.set(color);
                }
            }
            // 精灵栏暗色
            case 1 -> {
                switch (selectedColorIndex) {
                    case 0 -> ColorEditorConfig.PANEL_DARK_OUTER.set(color);
                    case 1 -> ColorEditorConfig.PANEL_DARK_INNER.set(color);
                    case 2 -> ColorEditorConfig.PANEL_DARK_TEXT.set(color);
                }
            }
            // [线性-L2-3] 护符栏亮色
            case 2 -> {
                switch (selectedColorIndex) {
                    case 0 -> ColorEditorConfig.PANEL_LIGHT_BG.set(color);
                    case 1 -> ColorEditorConfig.PANEL_LIGHT_BORDER.set(color);
                    case 2 -> ColorEditorConfig.PANEL_LIGHT_TEXT.set(color);
                    case 3 -> ColorEditorConfig.PANEL_LIGHT_SLOT_BG.set(color);
                    case 4 -> ColorEditorConfig.PANEL_LIGHT_SLOT_HOVER_BG.set(color);
                    case 5 -> ColorEditorConfig.PANEL_LIGHT_SLOT_BORDER.set(color);
                    case 6 -> ColorEditorConfig.PANEL_LIGHT_SLOT_SELECTED_BORDER.set(color);
                    case 7 -> ColorEditorConfig.PANEL_LIGHT_SLOT_HOVER_BORDER.set(color);
                }
            }
            // [线性-L2-4] 护符栏暗色
            case 3 -> {
                switch (selectedColorIndex) {
                    case 0 -> ColorEditorConfig.PANEL_DARK_OUTER.set(color);
                    case 1 -> ColorEditorConfig.PANEL_DARK_INNER.set(color);
                    case 2 -> ColorEditorConfig.PANEL_DARK_TEXT.set(color);
                }
            }
            // 编辑器亮色
            case 4 -> {
                switch (selectedColorIndex) {
                    case 0 -> ColorEditorConfig.EDITOR_LIGHT_CONTAINER_BG.set(color);
                    case 1 -> ColorEditorConfig.EDITOR_LIGHT_CONTAINER_BORDER.set(color);
                    case 2 -> ColorEditorConfig.EDITOR_LIGHT_LABEL.set(color);
                    case 3 -> ColorEditorConfig.EDITOR_LIGHT_INPUT_BG.set(color);
                    case 4 -> ColorEditorConfig.EDITOR_LIGHT_INPUT_BORDER.set(color);
                    case 5 -> ColorEditorConfig.EDITOR_LIGHT_INPUT_TEXT.set(color);
                    case 6 -> ColorEditorConfig.EDITOR_LIGHT_TAB_ACTIVE.set(color);
                }
            }
            // 编辑器暗色
            case 5 -> {
                switch (selectedColorIndex) {
                    case 0 -> ColorEditorConfig.EDITOR_DARK_CONTAINER_BG.set(color);
                    case 1 -> ColorEditorConfig.EDITOR_DARK_CONTAINER_BORDER.set(color);
                    case 2 -> ColorEditorConfig.EDITOR_DARK_LABEL.set(color);
                    case 3 -> ColorEditorConfig.EDITOR_DARK_INPUT_BG.set(color);
                    case 4 -> ColorEditorConfig.EDITOR_DARK_INPUT_BORDER.set(color);
                    case 5 -> ColorEditorConfig.EDITOR_DARK_INPUT_TEXT.set(color);
                    case 6 -> ColorEditorConfig.EDITOR_DARK_TAB_ACTIVE.set(color);
                }
            }
        }
    }

    private void updateColorPreview() {
        // 预览由 render 中的颜色方块实时显示
    }

    private void applyColor() {
        try {
            int r = clamp(rField.getValue());
            int g = clamp(gField.getValue());
            int b = clamp(bField.getValue());
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;
            setCurrentColor(color);
            ColorEditorConfig.PANEL_LIGHT_BG.save(); // 触发保存
        } catch (NumberFormatException ignored) {}
    }

    private int clamp(String s) {
        int v = Integer.parseInt(s.isEmpty() ? "0" : s);
        return Math.max(0, Math.min(255, v));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // 面板背景
        graphics.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xDD1E1E2E);
        graphics.fill(panelX + 1, panelY + 1, panelX + PANEL_W - 1, panelY + PANEL_H - 1, 0xFF2A2A3E);

        super.render(graphics, mouseX, mouseY, partialTick);

        // ===== RGB输入区竖向标签 =====
        int listX = panelX + 10;
        int listW = 120;
        int fieldX = listX + listW + 20;
        int labelGap = 14;
        int fieldH = 16;
        int rowSpacing = fieldH + 6;
        int tabY2 = panelY + 5 + 18 + 3; // 第二行Tab底部
        int listY = tabY2 + 18 + 8;

        int rY = listY;
        int gY = rY + rowSpacing;
        int bY = gY + rowSpacing;

        graphics.drawString(this.font, "R:", fieldX, rY + 2, 0xFFFF5555);
        graphics.drawString(this.font, "G:", fieldX, gY + 2, 0xFF55FF55);
        graphics.drawString(this.font, "B:", fieldX, bY + 2, 0xFF5555FF);

        // ===== 十六进制输入框标签 =====
        int hexY = bY + rowSpacing + 4;
        graphics.drawString(this.font, "0x:", fieldX, hexY + 2, 0xFFFFAA55);

        // ===== 颜色预览方块（在应用按钮下面） =====
        int btnY = hexY + rowSpacing + 2;
        int previewX = fieldX;
        int previewY = btnY + 24;
        int previewSize = 36;
        int currentColor = 0xFF000000;
        try {
            int r = clamp(rField.getValue());
            int g = clamp(gField.getValue());
            int b = clamp(bField.getValue());
            currentColor = 0xFF000000 | (r << 16) | (g << 8) | b;
        } catch (NumberFormatException ignored) {}
        graphics.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, 0xFF000000);
        graphics.fill(previewX + 1, previewY + 1, previewX + previewSize - 1, previewY + previewSize - 1, currentColor);

        // ===== 右侧预览区 ===== [线性-RESP-01] 使用动态PANEL_W计算，微面板也能显示
        int fieldAreaW = 206; // 标签14 + 输入52 + 间距 ~ 206
        int previewAreaX = fieldX + fieldAreaW + 10;
        if (previewAreaX > panelX + PANEL_W - 60) previewAreaX = panelX + PANEL_W - 60;
        int previewAreaY = panelY + 35;
        int previewAreaW = panelX + PANEL_W - previewAreaX - 10;
        int previewAreaH = PANEL_H - 65;
        drawPreviewPanel(graphics, previewAreaX, previewAreaY, previewAreaW, previewAreaH);
    }

    /** 绘制UI预览（框架渲染，无功能） */
    private void drawPreviewPanel(GuiGraphics graphics, int x, int y, int w, int h) {
        // 预览区外框
        graphics.fill(x, y, x + w, y + h, 0xFF333344);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF222233);

        if (activeTab <= 3) {
            // 精灵栏/护符栏预览
            boolean isCharm = (activeTab == 2 || activeTab == 3);
            drawPanelPreview(graphics, x + 5, y + 5, w - 10, h - 10, isCharm);
        } else {
            // 编辑器预览
            drawEditorPreview(graphics, x + 5, y + 5, w - 10, h - 10);
        }
    }

    /** 精灵栏/护符栏预览 */
    private void drawPanelPreview(GuiGraphics graphics, int x, int y, int w, int h, boolean isCharm) {
        boolean light = (activeTab == 0 || activeTab == 2);
        int bg = light ? ColorEditorConfig.panelLightBg() : ColorEditorConfig.panelDarkOuter();
        int inner = light ? ColorEditorConfig.panelLightBg() : ColorEditorConfig.panelDarkInner();
        int border = light ? ColorEditorConfig.panelLightBorder() : ColorEditorConfig.panelDarkOuter();
        int textColor = light ? ColorEditorConfig.panelLightText() : ColorEditorConfig.panelDarkText();

        // 面板底色
        graphics.fill(x, y, x + 60, y + h, border);
        graphics.fill(x + 1, y + 1, x + 59, y + h - 1, light ? bg : inner);
        if (!light) {
            graphics.fill(x + 5, y + 5, x + 55, y + h - 5, inner);
        }

        // 标题 | Title
        graphics.drawCenteredString(this.font,
                Component.translatable(isCharm ? "coblemon_ts.ui.charms" : "coblemon_ts.ui.party").getString(),
                x + 30, y + 4, textColor);

        // 槽位预览
        int slotY = y + 18;
        int slotBg = light ? ColorEditorConfig.panelLightSlotBg() : 0xCC161616;
        int slotBorder = light ? ColorEditorConfig.panelLightSlotBorder() : 0xFF555555;
        for (int i = 0; i < 6; i++) {
            int sy = slotY + i * 31;
            graphics.fill(x + 3, sy, x + 57, sy + 28, slotBorder);
            graphics.fill(x + 4, sy + 1, x + 56, sy + 27, slotBg);
            // 护符栏显示"C-编号"，精灵栏显示"Lv.50"
            graphics.drawString(this.font, isCharm ? ("C-" + (i + 1)) : "Lv.50", x + 20, sy + 8, textColor);
        }
    }

    /** 编辑器预览 */
    private void drawEditorPreview(GuiGraphics graphics, int x, int y, int w, int h) {
        boolean light = (activeTab == 4);
        int cBg = light ? ColorEditorConfig.editorLightContainerBg() : ColorEditorConfig.editorDarkContainerBg();
        int cBorder = light ? ColorEditorConfig.editorLightContainerBorder() : ColorEditorConfig.editorDarkContainerBorder();
        int label = light ? ColorEditorConfig.editorLightLabel() : ColorEditorConfig.editorDarkLabel();
        int inputBg = light ? ColorEditorConfig.editorLightInputBg() : ColorEditorConfig.editorDarkInputBg();
        int inputBorder = light ? ColorEditorConfig.editorLightInputBorder() : ColorEditorConfig.editorDarkInputBorder();
        int inputText = light ? ColorEditorConfig.editorLightInputText() : ColorEditorConfig.editorDarkInputText();
        int tabActive = light ? ColorEditorConfig.editorLightTabActive() : ColorEditorConfig.editorDarkTabActive();

        // 容器
        graphics.fill(x, y, x + w, y + h, cBorder);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, cBg);

        // Tab栏
        int tabY = y - 20;
        String[] tabs = {"总览", "技能", "高级"};
        for (int i = 0; i < 3; i++) {
            int tx = x + 5 + i * 62;
            graphics.fill(tx, tabY, tx + 58, tabY + 18, i == 0 ? tabActive : cBorder);
            graphics.drawCenteredString(this.font, tabs[i], tx + 29, tabY + 5, 0xFFFFFFFF);
        }

        // 标签
        int rowY = y + 10;
        String[] labels = {"物种:", "等级:", "昵称:", "性格:", "特性:"};
        for (int i = 0; i < labels.length; i++) {
            graphics.drawString(this.font, labels[i], x + 8, rowY + i * 20 + 4, label);
        }

        // 输入框
        for (int i = 0; i < labels.length; i++) {
            int ix = x + 55;
            int iy = rowY + i * 20;
            graphics.fill(ix, iy, ix + 100, iy + 18, inputBorder);
            graphics.fill(ix + 1, iy + 1, ix + 99, iy + 17, inputBg);
            if (i == 0) graphics.drawString(this.font, "皮卡丘", ix + 4, iy + 4, inputText);
            if (i == 1) graphics.drawString(this.font, "50", ix + 4, iy + 4, inputText);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }
}