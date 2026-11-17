package com.cobblemon_ts.mod;

/*=================================================================
||  线性依赖: CobblemonTSConfig提供全局游戏性配置
||  线性结构: EV设置 → HUD设置(药水效果→统一彩色条→逐项条→其他) → 战利品/配方
||  线性下游: GuiHealthFoodMixin, InventoryScreenMixin, PokemonEditorScreen, ...
===================================================================*/

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class CobblemonTSConfig {
    // ==================== 努力值设置 ====================
    public static final CharmConfig.BooleanValue ENABLE_UNLIMITED_EVS = new CharmConfig.BooleanValue(false);
    public static final CharmConfig.BooleanValue EV_EFFORT_CAP_252X6 = ENABLE_UNLIMITED_EVS; // 别名，同一引用

    // ==================== HUD 显示设置 ====================
    public static final CharmConfig.BooleanValue SHOW_EFFECT_ICONS = new CharmConfig.BooleanValue(true);
    public static final CharmConfig.BooleanValue SHOW_INVENTORY_EFFECT_ICONS = new CharmConfig.BooleanValue(true);
    public static final CharmConfig.BooleanValue HIDE_TOGGLE_BUTTONS = new CharmConfig.BooleanValue(false);
    // [线性-L04] 自定义血条/饥饿条/盔甲条/经验条开关
    public static final CharmConfig.BooleanValue HUD_CUSTOM_HEALTH_BAR = new CharmConfig.BooleanValue(false);
    public static final CharmConfig.BooleanValue HUD_CUSTOM_FOOD_BAR = new CharmConfig.BooleanValue(false);
    public static final CharmConfig.BooleanValue HUD_CUSTOM_ARMOR_BAR = new CharmConfig.BooleanValue(false);
    public static final CharmConfig.BooleanValue HUD_CUSTOM_XP_BAR = new CharmConfig.BooleanValue(false);
    public static final CharmConfig.BooleanValue HUD_ABSORPTION_OVERLAY = new CharmConfig.BooleanValue(false);
    public static final CharmConfig.BooleanValue HUD_SATURATION_OVERLAY = new CharmConfig.BooleanValue(false);
    // [线性-S14] 界面尺寸2时强制Summary使用guiScale=4
    public static final CharmConfig.BooleanValue HUD_FORCE_SUMMARY_GUISCALE = new CharmConfig.BooleanValue(false);
    // [线性-S01] 精灵信息字体缩放补偿：低界面尺寸时自动放大cobblemon Summary文字
    public static final CharmConfig.BooleanValue HUD_SUMMARY_TEXT_SCALE = new CharmConfig.BooleanValue(true);
    // [线性-S05] 药水图标缩小
    public static final CharmConfig.BooleanValue HUD_POTION_SHRINK = new CharmConfig.BooleanValue(false);
    public static final CharmConfig.DoubleValue HUD_POTION_SHRINK_SCALE = new CharmConfig.DoubleValue(0.6);
    // [线性-P01] 药水闪烁开关：关闭后药水剩少时间时不再闪烁
    public static final CharmConfig.BooleanValue HUD_POTION_HIDE_FLASH = new CharmConfig.BooleanValue(false);

    // [线性-L10] 统一彩色数值条
    public static final CharmConfig.BooleanValue HUD_UNIFIED_COLORED_BARS = new CharmConfig.BooleanValue(true);
    public static final CharmConfig.BooleanValue HUD_UNIFIED_AIR_BAR = new CharmConfig.BooleanValue(true);
    public static final CharmConfig.BooleanValue HUD_UNIFIED_VEHICLE_BAR = new CharmConfig.BooleanValue(true);
    public static final CharmConfig.BooleanValue HUD_UNIFIED_XP_BAR = new CharmConfig.BooleanValue(true);
    // [线性-H03] 数值显示上限
    public static final CharmConfig.IntValue HUD_NUMERIC_DISPLAY_MAX = new CharmConfig.IntValue(65536);

    // [线性-L06] 护符获取与幸运方块配方开关
    public static final CharmConfig.BooleanValue CHARM_LOOT_ENABLED = new CharmConfig.BooleanValue(true);
    public static final CharmConfig.BooleanValue LUCKY_RECIPES_ENABLED_TS = new CharmConfig.BooleanValue(true);

    // 面板主题
    public static final CharmConfig.BooleanValue LIGHT_PANEL_THEME = new CharmConfig.BooleanValue(false);
    public static final CharmConfig.BooleanValue LIGHT_EDITOR_THEME = new CharmConfig.BooleanValue(true);

    // [线性-B05] 另一种捕捉模式
    public static final CharmConfig.BooleanValue ALTERNATIVE_CAPTURE_MODE = new CharmConfig.BooleanValue(true);

    // [线性-R05b] 精灵雷达总开关
    public static final CharmConfig.BooleanValue POKEMON_RADAR_ENABLED = new CharmConfig.BooleanValue(true);

    static { load(); }

    public static void load() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve("cobblemonts/cobblemon_ts_global.json");
        JsonObject json = new JsonObject();
        boolean needSave = false;
        if (Files.exists(configFile)) {
            try {
                json = JsonParser.parseString(Files.readString(configFile)).getAsJsonObject();
            } catch (Exception e) {
                CobblemonTS.LOGGER.error("Failed to load global config", e);
            }
        }

        // EV
        if (json.has("enable_unlimited_evs")) { boolean v=json.get("enable_unlimited_evs").getAsBoolean(); ENABLE_UNLIMITED_EVS.set(v); EV_EFFORT_CAP_252X6.set(v); }
        else { json.addProperty("enable_unlimited_evs", false); needSave=true; }

        // HUD
        if (json.has("show_effect_icons")) SHOW_EFFECT_ICONS.set(json.get("show_effect_icons").getAsBoolean()); else { json.addProperty("show_effect_icons", true); needSave=true; }
        if (json.has("show_inventory_effect_icons")) SHOW_INVENTORY_EFFECT_ICONS.set(json.get("show_inventory_effect_icons").getAsBoolean()); else { json.addProperty("show_inventory_effect_icons", true); needSave=true; }
        if (json.has("hide_toggle_buttons")) HIDE_TOGGLE_BUTTONS.set(json.get("hide_toggle_buttons").getAsBoolean()); else { json.addProperty("hide_toggle_buttons", false); needSave=true; }
        if (json.has("custom_health_bar")) HUD_CUSTOM_HEALTH_BAR.set(json.get("custom_health_bar").getAsBoolean()); else { json.addProperty("custom_health_bar", false); needSave=true; }
        if (json.has("custom_food_bar")) HUD_CUSTOM_FOOD_BAR.set(json.get("custom_food_bar").getAsBoolean()); else { json.addProperty("custom_food_bar", false); needSave=true; }
        if (json.has("custom_armor_bar")) HUD_CUSTOM_ARMOR_BAR.set(json.get("custom_armor_bar").getAsBoolean()); else { json.addProperty("custom_armor_bar", false); needSave=true; }
        if (json.has("custom_xp_bar")) HUD_CUSTOM_XP_BAR.set(json.get("custom_xp_bar").getAsBoolean()); else { json.addProperty("custom_xp_bar", false); needSave=true; }
        if (json.has("absorption_overlay")) HUD_ABSORPTION_OVERLAY.set(json.get("absorption_overlay").getAsBoolean()); else { json.addProperty("absorption_overlay", false); needSave=true; }
        if (json.has("saturation_overlay")) HUD_SATURATION_OVERLAY.set(json.get("saturation_overlay").getAsBoolean()); else { json.addProperty("saturation_overlay", false); needSave=true; }
        if (json.has("hud_force_summary_guiscale")) HUD_FORCE_SUMMARY_GUISCALE.set(json.get("hud_force_summary_guiscale").getAsBoolean()); else { json.addProperty("hud_force_summary_guiscale", false); needSave=true; }
        if (json.has("hud_summary_text_scale")) HUD_SUMMARY_TEXT_SCALE.set(json.get("hud_summary_text_scale").getAsBoolean()); else { json.addProperty("hud_summary_text_scale", true); needSave=true; }
        if (json.has("hud_potion_shrink")) HUD_POTION_SHRINK.set(json.get("hud_potion_shrink").getAsBoolean()); else { json.addProperty("hud_potion_shrink", false); needSave=true; }
        if (json.has("hud_potion_shrink_scale")) HUD_POTION_SHRINK_SCALE.set(json.get("hud_potion_shrink_scale").getAsDouble()); else { json.addProperty("hud_potion_shrink_scale", 0.6); needSave=true; }
        if (json.has("hud_potion_hide_flash")) HUD_POTION_HIDE_FLASH.set(json.get("hud_potion_hide_flash").getAsBoolean()); else { json.addProperty("hud_potion_hide_flash", false); needSave=true; }
        if (json.has("unified_colored_bars")) HUD_UNIFIED_COLORED_BARS.set(json.get("unified_colored_bars").getAsBoolean()); else { json.addProperty("unified_colored_bars", true); needSave=true; }
        if (json.has("unified_air_bar")) HUD_UNIFIED_AIR_BAR.set(json.get("unified_air_bar").getAsBoolean()); else { json.addProperty("unified_air_bar", true); needSave=true; }
        if (json.has("unified_vehicle_bar")) HUD_UNIFIED_VEHICLE_BAR.set(json.get("unified_vehicle_bar").getAsBoolean()); else { json.addProperty("unified_vehicle_bar", true); needSave=true; }
        if (json.has("unified_xp_bar")) HUD_UNIFIED_XP_BAR.set(json.get("unified_xp_bar").getAsBoolean()); else { json.addProperty("unified_xp_bar", true); needSave=true; }
        if (json.has("hud_numeric_max")) HUD_NUMERIC_DISPLAY_MAX.set(json.get("hud_numeric_max").getAsInt()); else { json.addProperty("hud_numeric_max", 65536); needSave=true; }

        // 战利品
        if (json.has("charm_loot_enabled")) CHARM_LOOT_ENABLED.set(json.get("charm_loot_enabled").getAsBoolean()); else { json.addProperty("charm_loot_enabled", true); needSave=true; }
        if (json.has("lucky_recipes_enabled")) LUCKY_RECIPES_ENABLED_TS.set(json.get("lucky_recipes_enabled").getAsBoolean()); else { json.addProperty("lucky_recipes_enabled", true); needSave=true; }
        if (json.has("light_panel_theme")) LIGHT_PANEL_THEME.set(json.get("light_panel_theme").getAsBoolean()); else { json.addProperty("light_panel_theme", false); needSave=true; }
        if (json.has("light_editor_theme")) LIGHT_EDITOR_THEME.set(json.get("light_editor_theme").getAsBoolean()); else { json.addProperty("light_editor_theme", true); needSave=true; }
        if (json.has("alternative_capture_mode")) ALTERNATIVE_CAPTURE_MODE.set(json.get("alternative_capture_mode").getAsBoolean()); else { json.addProperty("alternative_capture_mode", true); needSave=true; }
        if (json.has("pokemon_radar_enabled")) POKEMON_RADAR_ENABLED.set(json.get("pokemon_radar_enabled").getAsBoolean()); else { json.addProperty("pokemon_radar_enabled", true); needSave=true; }

        if (needSave) saveInternal(json);
    }

    // [线性-SAVE] 导出到文件
    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("enable_unlimited_evs", ENABLE_UNLIMITED_EVS.get());
        json.addProperty("show_effect_icons", SHOW_EFFECT_ICONS.get());
        json.addProperty("show_inventory_effect_icons", SHOW_INVENTORY_EFFECT_ICONS.get());
        json.addProperty("hide_toggle_buttons", HIDE_TOGGLE_BUTTONS.get());
        json.addProperty("custom_health_bar", HUD_CUSTOM_HEALTH_BAR.get());
        json.addProperty("custom_food_bar", HUD_CUSTOM_FOOD_BAR.get());
        json.addProperty("custom_armor_bar", HUD_CUSTOM_ARMOR_BAR.get());
        json.addProperty("custom_xp_bar", HUD_CUSTOM_XP_BAR.get());
        json.addProperty("absorption_overlay", HUD_ABSORPTION_OVERLAY.get());
        json.addProperty("saturation_overlay", HUD_SATURATION_OVERLAY.get());
        json.addProperty("hud_force_summary_guiscale", HUD_FORCE_SUMMARY_GUISCALE.get());
        json.addProperty("hud_summary_text_scale", HUD_SUMMARY_TEXT_SCALE.get());
        json.addProperty("hud_potion_shrink", HUD_POTION_SHRINK.get());
        json.addProperty("hud_potion_shrink_scale", HUD_POTION_SHRINK_SCALE.get());
        json.addProperty("hud_potion_hide_flash", HUD_POTION_HIDE_FLASH.get());
        json.addProperty("unified_colored_bars", HUD_UNIFIED_COLORED_BARS.get());
        json.addProperty("unified_air_bar", HUD_UNIFIED_AIR_BAR.get());
        json.addProperty("unified_vehicle_bar", HUD_UNIFIED_VEHICLE_BAR.get());
        json.addProperty("unified_xp_bar", HUD_UNIFIED_XP_BAR.get());
        json.addProperty("hud_numeric_max", HUD_NUMERIC_DISPLAY_MAX.get());
        json.addProperty("charm_loot_enabled", CHARM_LOOT_ENABLED.get());
        json.addProperty("lucky_recipes_enabled", LUCKY_RECIPES_ENABLED_TS.get());
        json.addProperty("light_panel_theme", LIGHT_PANEL_THEME.get());
        json.addProperty("light_editor_theme", LIGHT_EDITOR_THEME.get());
        json.addProperty("alternative_capture_mode", ALTERNATIVE_CAPTURE_MODE.get());
        json.addProperty("pokemon_radar_enabled", POKEMON_RADAR_ENABLED.get());
        saveInternal(json);
    }

    private static void saveInternal(JsonObject json) {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("cobblemonts");
        Path configFile = configDir.resolve("cobblemon_ts_global.json");
        try {
            Files.createDirectories(configDir);
            Files.writeString(configFile, json.toString());
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("Failed to save global config", e);
        }
    }

    public static int getEVTotalCap() {
        return EV_EFFORT_CAP_252X6.get() ? 1512 : 510;
    }
}