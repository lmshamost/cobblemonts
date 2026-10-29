package com.cobblemon_ts.mod;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CobblemonTSConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ==================== 努力值设置 ====================
    public static final ModConfigSpec.BooleanValue ENABLE_UNLIMITED_EVS;
    public static final ModConfigSpec.BooleanValue EV_EFFORT_CAP_252X6;

    // ==================== HUD 显示设置 ====================
    public static final ModConfigSpec.BooleanValue SHOW_EFFECT_ICONS;
    public static final ModConfigSpec.BooleanValue SHOW_INVENTORY_EFFECT_ICONS;
    public static final ModConfigSpec.BooleanValue HIDE_TOGGLE_BUTTONS;
    // [线性-L04] 自定义血条/饥饿条/盔甲条/经验条开关
    public static final ModConfigSpec.BooleanValue HUD_CUSTOM_HEALTH_BAR;
    public static final ModConfigSpec.BooleanValue HUD_CUSTOM_FOOD_BAR;
    public static final ModConfigSpec.BooleanValue HUD_CUSTOM_ARMOR_BAR;
    public static final ModConfigSpec.BooleanValue HUD_CUSTOM_XP_BAR;
    public static final ModConfigSpec.BooleanValue HUD_ABSORPTION_OVERLAY;
    public static final ModConfigSpec.BooleanValue HUD_SATURATION_OVERLAY;

    // [线性-L10] 统一彩色数值条：总开关→启动后覆盖所有原版HUD，每项独立成条
    public static final ModConfigSpec.BooleanValue HUD_UNIFIED_COLORED_BARS;
    public static final ModConfigSpec.BooleanValue HUD_UNIFIED_AIR_BAR;
    public static final ModConfigSpec.BooleanValue HUD_UNIFIED_VEHICLE_BAR;
    public static final ModConfigSpec.BooleanValue HUD_UNIFIED_XP_BAR;

    // [线性-L06] 护符获取与幸运方块配方开关
    public static final ModConfigSpec.BooleanValue CHARM_LOOT_ENABLED;
    public static final ModConfigSpec.BooleanValue LUCKY_RECIPES_ENABLED_TS;

    // 面板主题：亮色/暗色
    public static final ModConfigSpec.BooleanValue LIGHT_PANEL_THEME;
    public static final ModConfigSpec.BooleanValue LIGHT_EDITOR_THEME;

    // [线性-B05] 另一种捕捉模式：点击捕捉按钮弹出三选一菜单（捕获/恢复/战斗道具）
    public static final ModConfigSpec.BooleanValue ALTERNATIVE_CAPTURE_MODE;

    static {
        BUILDER.comment("努力值设置 | EV Settings").push("ev");
        ENABLE_UNLIMITED_EVS = BUILDER
                .comment("是否开启努力值上限为252*6(true=开启, false=原版限制)\n注意：修改此选项后请重启游戏，否则可能因已有精灵EV超限导致数据异常\n---\n" +
                         "Enable unlimited EVs to 252*6 (true=enabled, false=vanilla limit)\n" +
                         "Note: Restart required after changing. Existing Pokémon with over-limit EVs may have data issues.")
                .define("enable_unlimited_evs", false);
        EV_EFFORT_CAP_252X6 = ENABLE_UNLIMITED_EVS;
        BUILDER.pop();

        BUILDER.comment("HUD显示设置 —— 顺序：药水效果 → 各状态栏 → 其他 | HUD Display Settings (Order: Potion Effects → Status Bars → Others)").push("hud");

        // ==== [HUD-A] 药水效果 ====
        SHOW_EFFECT_ICONS = BUILDER
                .comment("是否在屏幕右上角显示药水buff图标(true=显示, false=隐藏)\n" +
                         "Show potion effect icons at top-right corner (true=show, false=hide)")
                .define("show_effect_icons", false);
        SHOW_INVENTORY_EFFECT_ICONS = BUILDER
                .comment("是否在背包界面显示药水buff图标(true=显示, false=隐藏)\n" +
                         "Show potion effect icons in inventory screen (true=show, false=hide)")
                .define("show_inventory_effect_icons", false);

        // ==== [HUD-B] 各状态栏 — 统一模式 ====
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

        // ==== [HUD-C] 各状态栏 — 逐项开关（统一模式关闭时生效）====
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

        // ==== [HUD-D] 其他 ====
        HIDE_TOGGLE_BUTTONS = BUILDER
                .comment("是否隐藏背包精灵栏和护符栏的开关按钮(true=开关按钮变透明不可见, false=显示绿红小方块正常显示)\n" +
                         "Hide party/charm toggle buttons in inventory (true=transparent/invisible, false=show green/red squares)")
                .define("hide_toggle_buttons", false);

        BUILDER.pop();

        // [线性-L06] 护符获取与幸运方块配方开关
        BUILDER.comment("战利品与配方设置 | Loot & Recipe Settings").push("loot_recipes");
        CHARM_LOOT_ENABLED = BUILDER
                .comment("是否允许护符/高级学习装置/金色王冠从战利品箱子中获取(猪灵堡垒、末地城，低权重随机，true=可获取, false=仅创造模式)\n" +
                         "Allow charms/Exp.All/Gold Crown to appear in loot chests (Bastion Remnants, End Cities, low weight, true=enabled, false=creative only)")
                .define("charm_loot_enabled", true);
        LUCKY_RECIPES_ENABLED_TS = BUILDER
                .comment("是否启用幸运方块合成配方(true=启用配方, false=关闭配方)\n" +
                         "Enable Lucky Block crafting recipes (true=enabled, false=disabled)")
                .define("lucky_recipes_enabled", true);
        BUILDER.pop();
        LIGHT_PANEL_THEME = BUILDER
                .comment("精灵栏与护符栏主题: true=亮色白底, false=暗色深灰\n" +
                         "Party/charm panel theme: true=light/white, false=dark/gray")
                .define("light_panel_theme", false);
        LIGHT_EDITOR_THEME = BUILDER
                .comment("宝可梦编辑器主题: true=亮色, false=暗色\n" +
                         "Pokémon Editor theme: true=light, false=dark")
                .define("light_editor_theme", true);

        // [线性-B05] 另一种捕捉模式
        BUILDER.comment("另一种捕捉模式 | Alternative Capture Mode").push("alternative_capture");
        ALTERNATIVE_CAPTURE_MODE = BUILDER
                .comment("启用另一种捕捉模式：点击捕捉按钮弹出三选一菜单（捕获/恢复/战斗道具），而非直接打开精灵球背包\n" +
                         "Enable alternative capture mode: clicking capture button shows a 3-option menu (Capture/Heal/Battle Items) instead of directly opening the Poké Ball bag")
                .define("alternative_capture_mode", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** 获取努力值上限（供宝可梦编辑器使用）：开启 = 252*6=1512，关闭 = 原版 510 */
    public static int getEVTotalCap() {
        return Boolean.TRUE.equals(EV_EFFORT_CAP_252X6.get()) ? 1512 : 510;
    }
}