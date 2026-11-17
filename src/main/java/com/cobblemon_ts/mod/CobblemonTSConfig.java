package com.cobblemon_ts.mod;

/*=================================================================
||  线性依赖: CobblemonTSConfig提供全局游戏性配置
||  线性结构: EV设置 → HUD设置(药水效果→统一彩色条→逐项条→其他) → 战利品/配方
||  线性下游: GuiHealthFoodMixin, InventoryScreenMixin, PokemonEditorScreen, ...
===================================================================*/

import com.cobblemon_ts.mod.gui.ColorEditorConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CobblemonTSConfig {
    // ==================== 努力值设置 ====================
    public static final CharmConfig.BooleanValue ENABLE_UNLIMITED_EVS = new CharmConfig.BooleanValue(false);
    public static final CharmConfig.BooleanValue EV_EFFORT_CAP_252X6 = ENABLE_UNLIMITED_EVS; // 别名，同一引用

    // ==================== HUD 显示设置（→ CLIENT，代理到 ColorEditorConfig） ====================
    public static final CharmConfig.BooleanValue SHOW_EFFECT_ICONS = ColorEditorConfig.SHOW_EFFECT_ICONS;
    public static final CharmConfig.BooleanValue SHOW_INVENTORY_EFFECT_ICONS = ColorEditorConfig.SHOW_INVENTORY_EFFECT_ICONS;
    public static final CharmConfig.BooleanValue HIDE_TOGGLE_BUTTONS = ColorEditorConfig.HIDE_TOGGLE_BUTTONS;
    // [线性-L04] 自定义血条/饥饿条/盔甲条/经验条开关
    public static final CharmConfig.BooleanValue HUD_CUSTOM_HEALTH_BAR = ColorEditorConfig.HUD_CUSTOM_HEALTH_BAR;
    public static final CharmConfig.BooleanValue HUD_CUSTOM_FOOD_BAR = ColorEditorConfig.HUD_CUSTOM_FOOD_BAR;
    public static final CharmConfig.BooleanValue HUD_CUSTOM_ARMOR_BAR = ColorEditorConfig.HUD_CUSTOM_ARMOR_BAR;
    public static final CharmConfig.BooleanValue HUD_CUSTOM_XP_BAR = ColorEditorConfig.HUD_CUSTOM_XP_BAR;
    public static final CharmConfig.BooleanValue HUD_ABSORPTION_OVERLAY = ColorEditorConfig.HUD_ABSORPTION_OVERLAY;
    public static final CharmConfig.BooleanValue HUD_SATURATION_OVERLAY = ColorEditorConfig.HUD_SATURATION_OVERLAY;
    // [线性-S14] 界面尺寸2时强制Summary使用guiScale=4
    public static final CharmConfig.BooleanValue HUD_FORCE_SUMMARY_GUISCALE = ColorEditorConfig.HUD_FORCE_SUMMARY_GUISCALE;
    // [线性-S01] 精灵信息字体缩放补偿：低界面尺寸时自动放大cobblemon Summary文字
    public static final CharmConfig.BooleanValue HUD_SUMMARY_TEXT_SCALE = ColorEditorConfig.HUD_SUMMARY_TEXT_SCALE;
    // [线性-S05] 药水图标缩小
    public static final CharmConfig.BooleanValue HUD_POTION_SHRINK = ColorEditorConfig.HUD_POTION_SHRINK;
    public static final CharmConfig.DoubleValue HUD_POTION_SHRINK_SCALE = ColorEditorConfig.HUD_POTION_SHRINK_SCALE;
    // [线性-P01] 药水闪烁开关：关闭后药水剩少时间时不再闪烁
    public static final CharmConfig.BooleanValue HUD_POTION_HIDE_FLASH = ColorEditorConfig.HUD_POTION_HIDE_FLASH;

    // [线性-L10] 统一彩色数值条
    public static final CharmConfig.BooleanValue HUD_UNIFIED_COLORED_BARS = ColorEditorConfig.HUD_UNIFIED_COLORED_BARS;
    public static final CharmConfig.BooleanValue HUD_UNIFIED_AIR_BAR = ColorEditorConfig.HUD_UNIFIED_AIR_BAR;
    public static final CharmConfig.BooleanValue HUD_UNIFIED_VEHICLE_BAR = ColorEditorConfig.HUD_UNIFIED_VEHICLE_BAR;
    public static final CharmConfig.BooleanValue HUD_UNIFIED_XP_BAR = ColorEditorConfig.HUD_UNIFIED_XP_BAR;
    // [线性-H03] 数值显示上限
    public static final CharmConfig.IntValue HUD_NUMERIC_DISPLAY_MAX = ColorEditorConfig.HUD_NUMERIC_DISPLAY_MAX;

    // [线性-L06] 护符获取与幸运方块配方开关（COMMON）
    public static final CharmConfig.BooleanValue CHARM_LOOT_ENABLED = new CharmConfig.BooleanValue(true);
    public static final CharmConfig.BooleanValue LUCKY_RECIPES_ENABLED_TS = new CharmConfig.BooleanValue(true);

    // 面板主题（→ CLIENT）
    public static final CharmConfig.BooleanValue LIGHT_PANEL_THEME = ColorEditorConfig.LIGHT_PANEL_THEME;
    public static final CharmConfig.BooleanValue LIGHT_EDITOR_THEME = ColorEditorConfig.LIGHT_EDITOR_THEME;

    // [线性-B05] 另一种捕捉模式（→ CLIENT）
    public static final CharmConfig.BooleanValue ALTERNATIVE_CAPTURE_MODE = ColorEditorConfig.ALTERNATIVE_CAPTURE_MODE;

    // [线性-R05b] 精灵雷达总开关（COMMON）
    public static final CharmConfig.BooleanValue POKEMON_RADAR_ENABLED = new CharmConfig.BooleanValue(true);
    // [线性-RDR3] 精灵雷达权限名单：黑名单（玩家名列表，COMMON），名单内玩家禁止使用雷达
    public static final CharmConfig.ConfigValue<List<String>> POKEMON_RADAR_BLACKLIST = new CharmConfig.ConfigValue<>(new ArrayList<>());

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

        // HUD/主题/捕捉模式 已迁移至 ColorEditorConfig（客户端层），此处不再处理

        // 战利品
        if (json.has("charm_loot_enabled")) CHARM_LOOT_ENABLED.set(json.get("charm_loot_enabled").getAsBoolean()); else { json.addProperty("charm_loot_enabled", true); needSave=true; }
        if (json.has("lucky_recipes_enabled")) LUCKY_RECIPES_ENABLED_TS.set(json.get("lucky_recipes_enabled").getAsBoolean()); else { json.addProperty("lucky_recipes_enabled", true); needSave=true; }
        if (json.has("pokemon_radar_enabled")) POKEMON_RADAR_ENABLED.set(json.get("pokemon_radar_enabled").getAsBoolean()); else { json.addProperty("pokemon_radar_enabled", true); needSave=true; }
        if (json.has("radar_blacklist")) POKEMON_RADAR_BLACKLIST.set(jsonArrayToList(json.getAsJsonArray("radar_blacklist")));
        else { json.add("radar_blacklist", new JsonArray()); needSave=true; }

        if (needSave) saveInternal(json);
    }

    // [线性-SAVE] 导出到文件
    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("enable_unlimited_evs", ENABLE_UNLIMITED_EVS.get());
        json.addProperty("charm_loot_enabled", CHARM_LOOT_ENABLED.get());
        json.addProperty("lucky_recipes_enabled", LUCKY_RECIPES_ENABLED_TS.get());
        json.addProperty("pokemon_radar_enabled", POKEMON_RADAR_ENABLED.get());
        json.add("radar_blacklist", listToJsonArray(POKEMON_RADAR_BLACKLIST.get()));
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

    // [线性-UT] JsonArray -> List<String>
    private static List<String> jsonArrayToList(JsonArray arr) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) list.add(arr.get(i).getAsString());
        return list;
    }

    // [线性-UT] List<String> -> JsonArray
    private static JsonArray listToJsonArray(List<String> list) {
        JsonArray arr = new JsonArray();
        for (String s : list) arr.add(s);
        return arr;
    }
}