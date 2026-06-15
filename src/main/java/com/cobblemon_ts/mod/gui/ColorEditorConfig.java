package com.cobblemon_ts.mod.gui;

import net.neoforged.neoforge.common.ModConfigSpec;

// [线性-L1] 颜色编辑器配置：精灵栏/护符栏/宝可梦编辑器 所有可自定义颜色值
// 用于 ColorEditorScreen 读取和修改，同时被 PartyUIState、InventoryScreenMixin、PokemonEditorScreen 引用
public class ColorEditorConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // ===== 精灵栏/护符栏 亮色主题 =====
    public static ModConfigSpec.ConfigValue<Integer> PANEL_LIGHT_BG;           // 面板底色
    public static ModConfigSpec.ConfigValue<Integer> PANEL_LIGHT_BORDER;       // 面板边框
    public static ModConfigSpec.ConfigValue<Integer> PANEL_LIGHT_TEXT;         // 面板文字
    public static ModConfigSpec.ConfigValue<Integer> PANEL_LIGHT_SLOT_BG;      // 槽位普通底色
    public static ModConfigSpec.ConfigValue<Integer> PANEL_LIGHT_SLOT_HOVER_BG;// 槽位悬停底色
    public static ModConfigSpec.ConfigValue<Integer> PANEL_LIGHT_SLOT_BORDER;  // 槽位普通边框
    public static ModConfigSpec.ConfigValue<Integer> PANEL_LIGHT_SLOT_SELECTED_BORDER; // 槽位选中边框
    public static ModConfigSpec.ConfigValue<Integer> PANEL_LIGHT_SLOT_HOVER_BORDER;    // 槽位悬停边框

    // ===== 精灵栏/护符栏 暗色主题 =====
    public static ModConfigSpec.ConfigValue<Integer> PANEL_DARK_OUTER;         // 暗色面板外层
    public static ModConfigSpec.ConfigValue<Integer> PANEL_DARK_INNER;         // 暗色面板内层
    public static ModConfigSpec.ConfigValue<Integer> PANEL_DARK_TEXT;          // 暗色面板文字
    public static ModConfigSpec.ConfigValue<Integer> PANEL_DARK_SLOT_BG;       // 暗色槽位普通底色
    public static ModConfigSpec.ConfigValue<Integer> PANEL_DARK_SLOT_HOVER_BG; // 暗色槽位悬停底色
    public static ModConfigSpec.ConfigValue<Integer> PANEL_DARK_SLOT_BORDER;   // 暗色槽位普通边框
    public static ModConfigSpec.ConfigValue<Integer> PANEL_DARK_SLOT_SELECTED_BORDER; // 暗色槽位选中边框
    public static ModConfigSpec.ConfigValue<Integer> PANEL_DARK_SLOT_HOVER_BORDER;    // 暗色槽位悬停边框

    // ===== 宝可梦编辑器 亮色主题 =====
    public static ModConfigSpec.ConfigValue<Integer> EDITOR_LIGHT_CONTAINER_BG;    // 容器背景
    public static ModConfigSpec.ConfigValue<Integer> EDITOR_LIGHT_CONTAINER_BORDER;// 容器边框
    public static ModConfigSpec.ConfigValue<Integer> EDITOR_LIGHT_LABEL;           // 标签文字
    public static ModConfigSpec.ConfigValue<Integer> EDITOR_LIGHT_INPUT_BG;        // 输入框背景
    public static ModConfigSpec.ConfigValue<Integer> EDITOR_LIGHT_INPUT_BORDER;    // 输入框边框
    public static ModConfigSpec.ConfigValue<Integer> EDITOR_LIGHT_INPUT_TEXT;      // 输入框文字
    public static ModConfigSpec.ConfigValue<Integer> EDITOR_LIGHT_TAB_ACTIVE;      // Tab活跃色

    // ===== 宝可梦编辑器 暗色主题 =====
    public static ModConfigSpec.ConfigValue<Integer> EDITOR_DARK_CONTAINER_BG;
    public static ModConfigSpec.ConfigValue<Integer> EDITOR_DARK_CONTAINER_BORDER;
    public static ModConfigSpec.ConfigValue<Integer> EDITOR_DARK_LABEL;
    public static ModConfigSpec.ConfigValue<Integer> EDITOR_DARK_INPUT_BG;
    public static ModConfigSpec.ConfigValue<Integer> EDITOR_DARK_INPUT_BORDER;
    public static ModConfigSpec.ConfigValue<Integer> EDITOR_DARK_INPUT_TEXT;
    public static ModConfigSpec.ConfigValue<Integer> EDITOR_DARK_TAB_ACTIVE;

    static {
        BUILDER.comment("颜色编辑器配置 —— 精灵栏/护符栏/宝可梦编辑器颜色自定义").push("color_editor");

        // ===== 精灵栏/护符栏 亮色 =====
        BUILDER.comment("精灵栏/护符栏 亮色主题颜色").push("panel_light");
        PANEL_LIGHT_BG = BUILDER.define("panel_light_bg", 0xFFF0F0F0);
        PANEL_LIGHT_BORDER = BUILDER.define("panel_light_border", 0xFFAAAAAA);
        PANEL_LIGHT_TEXT = BUILDER.define("panel_light_text", 0xFF333333);
        PANEL_LIGHT_SLOT_BG = BUILDER.define("panel_light_slot_bg", 0xFFF5F5F5);
        PANEL_LIGHT_SLOT_HOVER_BG = BUILDER.define("panel_light_slot_hover_bg", 0xFFE0E0E0);
        PANEL_LIGHT_SLOT_BORDER = BUILDER.define("panel_light_slot_border", 0xFFAAAAAA);
        PANEL_LIGHT_SLOT_SELECTED_BORDER = BUILDER.define("panel_light_slot_selected_border", 0xFF00AAFF);
        PANEL_LIGHT_SLOT_HOVER_BORDER = BUILDER.define("panel_light_slot_hover_border", 0xFF777777);
        BUILDER.pop();

        // ===== 精灵栏/护符栏 暗色 =====
        BUILDER.comment("精灵栏/护符栏 暗色主题颜色").push("panel_dark");
        PANEL_DARK_OUTER = BUILDER.define("panel_dark_outer", 0xCC0B0B0B);
        PANEL_DARK_INNER = BUILDER.define("panel_dark_inner", 0xCC161616);
        PANEL_DARK_TEXT = BUILDER.define("panel_dark_text", 0xFFFFFFFF);
        PANEL_DARK_SLOT_BG = BUILDER.define("panel_dark_slot_bg", 0xFF2A2A2A);
        PANEL_DARK_SLOT_HOVER_BG = BUILDER.define("panel_dark_slot_hover_bg", 0xFF3A3A3A);
        PANEL_DARK_SLOT_BORDER = BUILDER.define("panel_dark_slot_border", 0xFF444444);
        PANEL_DARK_SLOT_SELECTED_BORDER = BUILDER.define("panel_dark_slot_selected_border", 0xFF00AAFF);
        PANEL_DARK_SLOT_HOVER_BORDER = BUILDER.define("panel_dark_slot_hover_border", 0xFF777777);
        BUILDER.pop();

        // ===== 宝可梦编辑器 亮色 =====
        BUILDER.comment("宝可梦编辑器 亮色主题颜色").push("editor_light");
        EDITOR_LIGHT_CONTAINER_BG = BUILDER.define("editor_light_container_bg", 0xFFFFFFFF);
        EDITOR_LIGHT_CONTAINER_BORDER = BUILDER.define("editor_light_container_border", 0xFFB8C7DA);
        EDITOR_LIGHT_LABEL = BUILDER.define("editor_light_label", 0xFF3B82F6);
        EDITOR_LIGHT_INPUT_BG = BUILDER.define("editor_light_input_bg", 0xFFFCFDFE);
        EDITOR_LIGHT_INPUT_BORDER = BUILDER.define("editor_light_input_border", 0xFFB8C7DA);
        EDITOR_LIGHT_INPUT_TEXT = BUILDER.define("editor_light_input_text", 0xFF0F172A);
        EDITOR_LIGHT_TAB_ACTIVE = BUILDER.define("editor_light_tab_active", 0xFF3B82F6);
        BUILDER.pop();

        // ===== 宝可梦编辑器 暗色 =====
        BUILDER.comment("宝可梦编辑器 暗色主题颜色").push("editor_dark");
        EDITOR_DARK_CONTAINER_BG = BUILDER.define("editor_dark_container_bg", 0xFF141414);
        EDITOR_DARK_CONTAINER_BORDER = BUILDER.define("editor_dark_container_border", 0xFF475569);
        EDITOR_DARK_LABEL = BUILDER.define("editor_dark_label", 0xFF0032B4);
        EDITOR_DARK_INPUT_BG = BUILDER.define("editor_dark_input_bg", 0xFF1E293B);
        EDITOR_DARK_INPUT_BORDER = BUILDER.define("editor_dark_input_border", 0xFF475569);
        EDITOR_DARK_INPUT_TEXT = BUILDER.define("editor_dark_input_text", 0xFFF1F5F9);
        EDITOR_DARK_TAB_ACTIVE = BUILDER.define("editor_dark_tab_active", 0xFF2563EB);
        BUILDER.pop();

        BUILDER.pop(); // color_editor
        SPEC = BUILDER.build();
    }

    // ===== 便捷获取方法 =====

    /** 精灵栏/护符栏：亮色面板底色 */
    public static int panelLightBg() { return PANEL_LIGHT_BG.get(); }
    /** 精灵栏/护符栏：亮色面板边框 */
    public static int panelLightBorder() { return PANEL_LIGHT_BORDER.get(); }
    /** 精灵栏/护符栏：亮色面板文字 */
    public static int panelLightText() { return PANEL_LIGHT_TEXT.get(); }
    /** 精灵栏/护符栏：亮色槽位普通底色 */
    public static int panelLightSlotBg() { return PANEL_LIGHT_SLOT_BG.get(); }
    /** 精灵栏/护符栏：亮色槽位悬停底色 */
    public static int panelLightSlotHoverBg() { return PANEL_LIGHT_SLOT_HOVER_BG.get(); }
    /** 精灵栏/护符栏：亮色槽位普通边框 */
    public static int panelLightSlotBorder() { return PANEL_LIGHT_SLOT_BORDER.get(); }
    /** 精灵栏/护符栏：亮色槽位选中边框 */
    public static int panelLightSlotSelectedBorder() { return PANEL_LIGHT_SLOT_SELECTED_BORDER.get(); }
    /** 精灵栏/护符栏：亮色槽位悬停边框 */
    public static int panelLightSlotHoverBorder() { return PANEL_LIGHT_SLOT_HOVER_BORDER.get(); }

    /** 精灵栏/护符栏：暗色面板外层 */
    public static int panelDarkOuter() { return PANEL_DARK_OUTER.get(); }
    /** 精灵栏/护符栏：暗色面板内层 */
    public static int panelDarkInner() { return PANEL_DARK_INNER.get(); }
    /** 精灵栏/护符栏：暗色面板文字 */
    public static int panelDarkText() { return PANEL_DARK_TEXT.get(); }
    /** 精灵栏/护符栏：暗色槽位普通底色 */
    public static int panelDarkSlotBg() { return PANEL_DARK_SLOT_BG.get(); }
    /** 精灵栏/护符栏：暗色槽位悬停底色 */
    public static int panelDarkSlotHoverBg() { return PANEL_DARK_SLOT_HOVER_BG.get(); }
    /** 精灵栏/护符栏：暗色槽位普通边框 */
    public static int panelDarkSlotBorder() { return PANEL_DARK_SLOT_BORDER.get(); }
    /** 精灵栏/护符栏：暗色槽位选中边框 */
    public static int panelDarkSlotSelectedBorder() { return PANEL_DARK_SLOT_SELECTED_BORDER.get(); }
    /** 精灵栏/护符栏：暗色槽位悬停边框 */
    public static int panelDarkSlotHoverBorder() { return PANEL_DARK_SLOT_HOVER_BORDER.get(); }

    /** 编辑器：亮色容器背景 */
    public static int editorLightContainerBg() { return EDITOR_LIGHT_CONTAINER_BG.get(); }
    /** 编辑器：亮色容器边框 */
    public static int editorLightContainerBorder() { return EDITOR_LIGHT_CONTAINER_BORDER.get(); }
    /** 编辑器：亮色标签 */
    public static int editorLightLabel() { return EDITOR_LIGHT_LABEL.get(); }
    /** 编辑器：亮色输入框背景 */
    public static int editorLightInputBg() { return EDITOR_LIGHT_INPUT_BG.get(); }
    /** 编辑器：亮色输入框边框 */
    public static int editorLightInputBorder() { return EDITOR_LIGHT_INPUT_BORDER.get(); }
    /** 编辑器：亮色输入框文字 */
    public static int editorLightInputText() { return EDITOR_LIGHT_INPUT_TEXT.get(); }
    /** 编辑器：亮色Tab活跃色 */
    public static int editorLightTabActive() { return EDITOR_LIGHT_TAB_ACTIVE.get(); }

    /** 编辑器：暗色容器背景 */
    public static int editorDarkContainerBg() { return EDITOR_DARK_CONTAINER_BG.get(); }
    /** 编辑器：暗色容器边框 */
    public static int editorDarkContainerBorder() { return EDITOR_DARK_CONTAINER_BORDER.get(); }
    /** 编辑器：暗色标签 */
    public static int editorDarkLabel() { return EDITOR_DARK_LABEL.get(); }
    /** 编辑器：暗色输入框背景 */
    public static int editorDarkInputBg() { return EDITOR_DARK_INPUT_BG.get(); }
    /** 编辑器：暗色输入框边框 */
    public static int editorDarkInputBorder() { return EDITOR_DARK_INPUT_BORDER.get(); }
    /** 编辑器：暗色输入框文字 */
    public static int editorDarkInputText() { return EDITOR_DARK_INPUT_TEXT.get(); }
    /** 编辑器：暗色Tab活跃色 */
    public static int editorDarkTabActive() { return EDITOR_DARK_TAB_ACTIVE.get(); }
}