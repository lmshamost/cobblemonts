package com.cobblemon_ts.mod;

import com.cobblemon_ts.mod.gui.ColorEditorConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

// [线性-CFG] 配置主类（COMMON 层，common.toml）
//  留在 COMMON：努力值（EV）、战利品、幸运方块配方、精灵雷达总开关
//  迁移到 CLIENT：HUD、面板主题、另一种捕捉（→ ColorEditorConfig，client.toml）
//  客户端专属项此处保留同名字段转发，让全项目既有引用零改动继续使用。
public class CobblemonTSConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ==================== 努力值设置（COMMON） ====================
    public static final ModConfigSpec.BooleanValue ENABLE_UNLIMITED_EVS;
    public static final ModConfigSpec.BooleanValue EV_EFFORT_CAP_252X6;

    // ==================== HUD 显示设置（→ CLIENT） ====================
    public static final ModConfigSpec.BooleanValue SHOW_EFFECT_ICONS = ColorEditorConfig.SHOW_EFFECT_ICONS;
    public static final ModConfigSpec.BooleanValue SHOW_INVENTORY_EFFECT_ICONS = ColorEditorConfig.SHOW_INVENTORY_EFFECT_ICONS;
    public static final ModConfigSpec.BooleanValue HIDE_TOGGLE_BUTTONS = ColorEditorConfig.HIDE_TOGGLE_BUTTONS;
    // [线性-L04] 自定义血条/饥饿条/盔甲条/经验条开关
    public static final ModConfigSpec.BooleanValue HUD_CUSTOM_HEALTH_BAR = ColorEditorConfig.HUD_CUSTOM_HEALTH_BAR;
    public static final ModConfigSpec.BooleanValue HUD_CUSTOM_FOOD_BAR = ColorEditorConfig.HUD_CUSTOM_FOOD_BAR;
    public static final ModConfigSpec.BooleanValue HUD_CUSTOM_ARMOR_BAR = ColorEditorConfig.HUD_CUSTOM_ARMOR_BAR;
    public static final ModConfigSpec.BooleanValue HUD_CUSTOM_XP_BAR = ColorEditorConfig.HUD_CUSTOM_XP_BAR;
    public static final ModConfigSpec.BooleanValue HUD_ABSORPTION_OVERLAY = ColorEditorConfig.HUD_ABSORPTION_OVERLAY;
    public static final ModConfigSpec.BooleanValue HUD_SATURATION_OVERLAY = ColorEditorConfig.HUD_SATURATION_OVERLAY;
    // [线性-L10] 统一彩色数值条：总开关→启动后覆盖所有原版HUD，每项独立成条
    public static final ModConfigSpec.BooleanValue HUD_UNIFIED_COLORED_BARS = ColorEditorConfig.HUD_UNIFIED_COLORED_BARS;
    public static final ModConfigSpec.BooleanValue HUD_UNIFIED_AIR_BAR = ColorEditorConfig.HUD_UNIFIED_AIR_BAR;
    public static final ModConfigSpec.BooleanValue HUD_UNIFIED_VEHICLE_BAR = ColorEditorConfig.HUD_UNIFIED_VEHICLE_BAR;
    public static final ModConfigSpec.BooleanValue HUD_UNIFIED_XP_BAR = ColorEditorConfig.HUD_UNIFIED_XP_BAR;
    // [线性-H03] 数值显示上限：护甲/韧性/血量等无上限属性条的最大显示值，超出时用k/M/G简写
    public static final ModConfigSpec.IntValue HUD_NUMERIC_DISPLAY_MAX = ColorEditorConfig.HUD_NUMERIC_DISPLAY_MAX;
    // [线性-S01] 精灵信息GUI缩放强制4
    public static final ModConfigSpec.BooleanValue HUD_FORCE_SUMMARY_GUISCALE = ColorEditorConfig.HUD_FORCE_SUMMARY_GUISCALE;
    // [线性-S01] 精灵信息字体缩放补偿：低界面尺寸时自动放大cobblemon Summary文字
    public static final ModConfigSpec.BooleanValue HUD_SUMMARY_TEXT_SCALE = ColorEditorConfig.HUD_SUMMARY_TEXT_SCALE;
    // [线性-S05] 药水图标缩小
    public static final ModConfigSpec.BooleanValue HUD_POTION_SHRINK = ColorEditorConfig.HUD_POTION_SHRINK;
    public static final ModConfigSpec.DoubleValue HUD_POTION_SHRINK_SCALE = ColorEditorConfig.HUD_POTION_SHRINK_SCALE;
    // [线性-P01] 药水闪烁开关：关闭后药水剩少时间时不再闪烁
    public static final ModConfigSpec.BooleanValue HUD_POTION_HIDE_FLASH = ColorEditorConfig.HUD_POTION_HIDE_FLASH;

    // [线性-L06] 护符获取与幸运方块配方开关（COMMON）
    public static final ModConfigSpec.BooleanValue CHARM_LOOT_ENABLED;
    public static final ModConfigSpec.BooleanValue LUCKY_RECIPES_ENABLED_TS;

    // ==================== 面板主题（→ CLIENT） ====================
    public static final ModConfigSpec.BooleanValue LIGHT_PANEL_THEME = ColorEditorConfig.LIGHT_PANEL_THEME;
    public static final ModConfigSpec.BooleanValue LIGHT_EDITOR_THEME = ColorEditorConfig.LIGHT_EDITOR_THEME;

    // [线性-B05] 另一种捕捉模式（→ CLIENT）
    public static final ModConfigSpec.BooleanValue ALTERNATIVE_CAPTURE_MODE = ColorEditorConfig.ALTERNATIVE_CAPTURE_MODE;

    // [线性-R01] 精灵雷达总开关（COMMON）
    public static final ModConfigSpec.BooleanValue POKEMON_RADAR_ENABLED;
    // [线性-RDR3] 精灵雷达权限名单：黑名单(玩家名列表,COMMON)，名单内玩家禁止使用雷达
    public static final ModConfigSpec.ConfigValue<List<? extends String>> POKEMON_RADAR_BLACKLIST;

    static {
        BUILDER.comment("努力值设置 | EV Settings").push("ev");
        ENABLE_UNLIMITED_EVS = BUILDER
                .comment("是否开启努力值上限为252*6(true=开启, false=原版限制)\n注意：修改此选项后请重启游戏，否则可能因已有精灵EV超限导致数据异常\n---\n" +
                         "Enable unlimited EVs to 252*6 (true=enabled, false=vanilla limit)\n" +
                         "Note: Restart required after changing. Existing Pokémon with over-limit EVs may have data issues.")
                .define("enable_unlimited_evs", false);
        EV_EFFORT_CAP_252X6 = ENABLE_UNLIMITED_EVS;
        BUILDER.pop();

        // [线性-L06] 护符获取与幸运方块配方开关（COMMON）
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

        // [线性-R01] 精灵雷达总开关（COMMON）
        BUILDER.comment("精灵雷达设置 | Pokemon Radar Settings").push("pokemon_radar");
        POKEMON_RADAR_ENABLED = BUILDER
                .comment("是否开启精灵雷达功能(true=开启, false=关闭)\n---\n" +
                         "Enable Pokemon Radar feature (true=enabled, false=disabled)")
                .define("pokemon_radar_enabled", true);
        // [线性-RDR3] 雷达权限名单(黑名单)：玩家名列表，逗号分隔；名单内玩家无法使用雷达，留空则所有玩家可用
        POKEMON_RADAR_BLACKLIST = BUILDER
                .comment("精灵雷达黑名单(玩家名,逗号分隔)，名单内玩家无法使用雷达；留空则所有玩家可用\n---\n" +
                         "Pokemon Radar blacklist (player names, comma-separated). Players in this list cannot use the radar; empty = everyone allowed.")
                .defineList("radar_blacklist", List.of(), obj -> obj instanceof String);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** 获取努力值上限（供宝可梦编辑器使用）：开启 = 252*6=1512，关闭 = 原版 510 */
    public static int getEVTotalCap() {
        return Boolean.TRUE.equals(EV_EFFORT_CAP_252X6.get()) ? 1512 : 510;
    }
}