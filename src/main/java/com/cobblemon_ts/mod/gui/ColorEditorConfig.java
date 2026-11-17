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

    // ==================== HUD 显示设置（客户端） ====================
    public static ModConfigSpec.BooleanValue SHOW_EFFECT_ICONS;
    public static ModConfigSpec.BooleanValue SHOW_INVENTORY_EFFECT_ICONS;
    public static ModConfigSpec.BooleanValue HIDE_TOGGLE_BUTTONS;
    public static ModConfigSpec.BooleanValue HUD_CUSTOM_HEALTH_BAR;
    public static ModConfigSpec.BooleanValue HUD_CUSTOM_FOOD_BAR;
    public static ModConfigSpec.BooleanValue HUD_CUSTOM_ARMOR_BAR;
    public static ModConfigSpec.BooleanValue HUD_CUSTOM_XP_BAR;
    public static ModConfigSpec.BooleanValue HUD_ABSORPTION_OVERLAY;
    public static ModConfigSpec.BooleanValue HUD_SATURATION_OVERLAY;
    public static ModConfigSpec.BooleanValue HUD_UNIFIED_COLORED_BARS;
    public static ModConfigSpec.BooleanValue HUD_UNIFIED_AIR_BAR;
    public static ModConfigSpec.BooleanValue HUD_UNIFIED_VEHICLE_BAR;
    public static ModConfigSpec.BooleanValue HUD_UNIFIED_XP_BAR;
    public static ModConfigSpec.IntValue HUD_NUMERIC_DISPLAY_MAX;
    public static ModConfigSpec.BooleanValue HUD_FORCE_SUMMARY_GUISCALE;
    public static ModConfigSpec.BooleanValue HUD_SUMMARY_TEXT_SCALE;
    public static ModConfigSpec.BooleanValue HUD_POTION_SHRINK;
    public static ModConfigSpec.DoubleValue HUD_POTION_SHRINK_SCALE;
    public static ModConfigSpec.BooleanValue HUD_POTION_HIDE_FLASH;

    // ==================== 面板主题（客户端） ====================
    public static ModConfigSpec.BooleanValue LIGHT_PANEL_THEME;
    public static ModConfigSpec.BooleanValue LIGHT_EDITOR_THEME;

    // ==================== 另一种捕捉模式（客户端） ====================
    public static ModConfigSpec.BooleanValue ALTERNATIVE_CAPTURE_MODE;

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

        // ===== HUD 显示设置（客户端） =====
        BUILDER.comment("HUD显示设置 | HUD Display Settings").push("hud");

        SHOW_EFFECT_ICONS = BUILDER
                .comment("是否在屏幕右上角显示药水buff图标(true=显示, false=隐藏)\n" +
                         "Show potion effect icons at top-right corner (true=show, false=hide)")
                .define("show_effect_icons", true);
        SHOW_INVENTORY_EFFECT_ICONS = BUILDER
                .comment("是否在背包界面显示药水buff图标(true=显示, false=隐藏)\n" +
                         "Show potion effect icons in inventory screen (true=show, false=hide)")
                .define("show_inventory_effect_icons", true);

        HUD_FORCE_SUMMARY_GUISCALE = BUILDER
                .comment("界面尺寸2时强制精灵信息界面使用GUI缩放4(true=临时切换guiScale=4, false=使用当前缩放, 窗口<1920px不生效)\n---\n" +
                         "Force GUI scale 4 for Summary when at scale 2 (true=force scale 4, false=use current, requires window>=1920px)")
                .define("hud_force_summary_guiscale", false);
        HUD_SUMMARY_TEXT_SCALE = BUILDER
                .comment("精灵信息界面字体缩放补偿(true=界面尺寸≤2时自动放大文字, false=不补偿)\n" +
                         "不改变整体guiScale，仅放大cobblemon Summary内的uniform字体文字\n---\n" +
                         "Summary text scale compensation (true=auto-enlarge text when guiScale≤2, false=no compensation)\n" +
                         "Only enlarges cobblemon Summary text without changing global guiScale")
                .define("hud_summary_text_scale", true);
        HUD_POTION_SHRINK = BUILDER
                .comment("是否将药水buff图标缩小(true=缩小, false=原版大图标, 缩放比例见hud_potion_shrink_scale)\n" +
                         "Shrink potion effect icons (true=shrunk, false=vanilla large icons, scale: hud_potion_shrink_scale)")
                .define("hud_potion_shrink", false);
        HUD_POTION_SHRINK_SCALE = BUILDER
                .comment("药水buff图标缩放比例(0.25~1.0, 默认0.6, 仅在hud_potion_shrink=true时生效)\n" +
                         "Potion icon shrink scale (0.25~1.0, default 0.6, only when hud_potion_shrink=true)")
                .defineInRange("hud_potion_shrink_scale", 0.6, 0.25, 1.0);
        HUD_POTION_HIDE_FLASH = BUILDER
                .comment("是否隐藏药水buff闪烁(true=始终显示不闪烁, false=快结束时闪烁提示)\n" +
                         "Hide potion effect flashing when about to expire (true=always show, false=flash when expiring)")
                .define("hud_potion_hide_flash", false);

        HUD_UNIFIED_COLORED_BARS = BUILDER
                .comment("统一彩色数值条总开关(true=全部替换为彩色数值条, false=使用下方逐项开关)\n" +
                         "开启后自动覆盖：生命/伤害吸收/饥饿/饱腹/护甲/韧性/氧气/骑乘生命/经验值\n---\n" +
                         "Master switch for unified colored stat bars (true=replace all vanilla bars with colored bars, false=use per-item switches below)\n" +
                         "When enabled, automatically overrides: Health/Absorption/Hunger/Saturation/Armor/Toughness/Air/Vehicle HP/Experience")
                .define("unified_colored_bars", true);
        HUD_UNIFIED_AIR_BAR = BUILDER
                .comment("是否显示氧气数值条(true=显示, false=隐藏, 仅在统一彩色条总开关开启时有效)\n" +
                         "Show air bar (true=show, false=hide, only effective when unified colored bars master switch is on)")
                .define("unified_air_bar", true);
        HUD_UNIFIED_VEHICLE_BAR = BUILDER
                .comment("是否显示骑乘生物生命数值条(true=显示, false=隐藏, 仅在统一彩色条总开关开启时有效)\n" +
                         "Show vehicle HP bar (true=show, false=hide, only effective when unified colored bars master switch is on)")
                .define("unified_vehicle_bar", true);
        HUD_UNIFIED_XP_BAR = BUILDER
                .comment("是否显示经验值数值条(true=显示, false=隐藏, 仅在统一彩色条总开关开启时有效)\n" +
                         "Show experience bar (true=show, false=hide, only effective when unified colored bars master switch is on)")
                .define("unified_xp_bar", true);

        HUD_CUSTOM_HEALTH_BAR = BUILDER
                .comment("是否启用自定义血条(true=自定义血条, false=原版心形)\n" +
                         "Enable custom health bar (true=custom bar, false=vanilla hearts)")
                .define("custom_health_bar", false);
        HUD_ABSORPTION_OVERLAY = BUILDER
                .comment("是否在血条上显示伤害吸收金条覆盖(true=显示金条, false=仅显示红血条)\n" +
                         "Show absorption golden overlay on health bar (true=show golden, false=red health only)")
                .define("absorption_overlay", false);
        HUD_CUSTOM_ARMOR_BAR = BUILDER
                .comment("是否启用自定义盔甲条(true=自定义盔甲条, false=原版盔甲图标)\n" +
                         "Enable custom armor bar (true=custom bar, false=vanilla armor icons)")
                .define("custom_armor_bar", false);
        HUD_CUSTOM_FOOD_BAR = BUILDER
                .comment("是否启用自定义饥饿条(true=自定义饥饿条, false=原版鸡腿)\n" +
                         "Enable custom food bar (true=custom bar, false=vanilla drumsticks)")
                .define("custom_food_bar", false);
        HUD_SATURATION_OVERLAY = BUILDER
                .comment("是否在饥饿条上显示饱腹值金条覆盖(true=显示金条, false=仅显示饥饿条)\n" +
                         "Show saturation golden overlay on food bar (true=show golden, false=food bar only)")
                .define("saturation_overlay", false);
        HUD_CUSTOM_XP_BAR = BUILDER
                .comment("是否启用自定义经验条(true=自定义经验值条, false=原版经验条)\n" +
                         "Enable custom XP bar (true=custom bar, false=vanilla XP bar)")
                .define("custom_xp_bar", false);

        HIDE_TOGGLE_BUTTONS = BUILDER
                .comment("是否隐藏背包精灵栏和护符栏的开关按钮(true=开关按钮变透明不可见, false=显示绿红小方块正常显示)\n" +
                         "Hide party/charm toggle buttons in inventory (true=transparent/invisible, false=show green/red squares)")
                .define("hide_toggle_buttons", false);
        HUD_NUMERIC_DISPLAY_MAX = BUILDER
                .comment("数值条显示上限：超出此值的护甲/韧性/血量/吸收等属性在数值条上用k/M/G简写\n" +
                         "Numeric display max: values above this limit use k/M/G abbreviation on HUD bars")
                .defineInRange("hud_numeric_max", 65536, 0, 2147483647);

        BUILDER.pop(); // hud

        // ===== 面板主题（客户端） =====
        BUILDER.comment("面板主题 | Panel Theme").push("theme");
        LIGHT_PANEL_THEME = BUILDER
                .comment("精灵栏与护符栏主题: true=亮色白底, false=暗色深灰\n" +
                         "Party/charm panel theme: true=light/white, false=dark/gray")
                .define("light_panel_theme", false);
        LIGHT_EDITOR_THEME = BUILDER
                .comment("宝可梦编辑器主题: true=亮色, false=暗色\n" +
                         "Pokémon Editor theme: true=light, false=dark")
                .define("light_editor_theme", true);
        BUILDER.pop(); // theme

        // ===== 另一种捕捉模式（客户端） =====
        BUILDER.comment("另一种捕捉模式 | Alternative Capture Mode").push("alternative_capture");
        ALTERNATIVE_CAPTURE_MODE = BUILDER
                .comment("启用另一种捕捉模式：点击捕捉按钮弹出三选一菜单（捕获/恢复/战斗道具），而非直接打开精灵球背包\n" +
                         "Enable alternative capture mode: clicking capture button shows a 3-option menu (Capture/Heal/Battle Items) instead of directly opening the Poké Ball bag")
                .define("alternative_capture_mode", true);
        BUILDER.pop(); // alternative_capture

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