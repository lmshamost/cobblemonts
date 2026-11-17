package com.cobblemon_ts.mod;

/*=================================================================
||  线性依赖: CharmConfig提供决策所需的配置参数
||  线性结构: C1(护符) → C2(开局道具) → C3(道具袋) → C4(幸运方块) → C5(编辑器)
||  线性作用: 所有CharmItem/Pokebag/LuckyBlock/Editor读取阈值/列表的入口
||  线性下游: CharmEventListener.init(), CharmEventHandler, LuckyWeightManager, ...
===================================================================*/

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CharmConfig {
    // ========== config keys ==========
    static final String CATCHING_CHARM_BOOST_K = "catching_charm_boost";
    static final String EXP_CHARM_BOOST_K = "exp_charm_boost";
    static final String OVAL_CHARM_HATCH_BOOST_K = "oval_charm_hatch_boost";
    static final String SHINY_CHARM_BOOST_K = "shiny_charm_boost";
    static final String EXP_ALL_PERCENT_K = "exp_all_percent";
    static final String SHOW_CHARM_MESSAGES_K = "show_charm_messages";
    static final String START_ITEMS_ENABLED_K = "start_items_enabled";
    static final String START_WITH_ITEMS_K = "start_with_items";
    static final String POKEBAG_BLACKLIST_TAGS_K = "pokebag_blacklist_tags";
    static final String POKEBAG_WHITELIST_TAGS_K = "pokebag_whitelist_tags";
    static final String POKEBAG_SLOTS_K = "pokebag_slots";
    static final String POKEBAG_MAX_STACK_SIZE_K = "pokebag_max_stack_size";
    static final String LEGENDARY_POKEMON_K = "legendary_pokemon";
    static final String ULTRA_BEAST_POKEMON_K = "ultra_beast_pokemon";
    static final String MIN_SPAWN_LEVEL_K = "min_spawn_level";
    static final String MAX_SPAWN_LEVEL_K = "max_spawn_level";
    static final String LUCKY_WEIGHTS_K = "lucky_weights";
    static final String LUCK_WEIGHT_PER_LEVEL_K = "luck_weight_per_level";
    static final String REQUIRE_CREATIVE_K = "require_creative";
    static final String EDITOR_AUTO_CLOSE_K = "editor_auto_close";
    // [线性-C6] 区块禁止器设置
    static final String CB_DEFAULT_RANGE_K = "cb_default_range";
    static final String CB_MAX_RANGE_K = "cb_max_range";
    static final String CB_BAN_CHUNK_LIMIT_K = "cb_ban_chunk_limit";
    static final String CB_FORCELOAD_CHUNK_LIMIT_K = "cb_forceload_chunk_limit";

    // ========== value wrappers (compatible with old .get()/.set() calls) ==========
    public static class DoubleValue { double v; public DoubleValue(double v){this.v=v;} public double get(){return v;} public void set(double v){this.v=v;} }
    public static class IntValue { int v; public IntValue(int v){this.v=v;} public int get(){return v;} public void set(int v){this.v=v;} }
    public static class BooleanValue { boolean v; public BooleanValue(boolean v){this.v=v;} public boolean get(){return v;} public void set(boolean v){this.v=v;} }
    public static class ConfigValue<T> { T v; public ConfigValue(T v){this.v=v;} public T get(){return v;} public void set(T v){this.v=v;} }

    // ==================== 护符设置 ====================
    public static final DoubleValue CATCHING_CHARM_BOOST = new DoubleValue(1.5);
    public static final DoubleValue EXP_CHARM_BOOST = new DoubleValue(1.5);
    public static final DoubleValue OVAL_CHARM_HATCH_BOOST = new DoubleValue(2.0);
    public static final DoubleValue SHINY_CHARM_BOOST = new DoubleValue(3.0);
    public static final DoubleValue EXP_ALL_PERCENT = new DoubleValue(50.0);
    public static final BooleanValue SHOW_CHARM_MESSAGES = new BooleanValue(true);

    // ==================== 开局道具列表 ====================
    public static final BooleanValue START_ITEMS_ENABLED = new BooleanValue(false);
    public static final ConfigValue<List<String>> START_WITH_ITEMS = new ConfigValue<>(
        Arrays.asList("shiny_charm","oval_charm","exp_charm","catching_charm","exp_all","pokebag"));

    // ==================== 道具袋设置 ====================
    public static final ConfigValue<List<String>> POKEBAG_BLACKLIST_TAGS = new ConfigValue<>(new ArrayList<>());
    public static final ConfigValue<List<String>> POKEBAG_WHITELIST_TAGS = new ConfigValue<>(Arrays.asList(
        "#cobblemon:poke_balls","#cobblemon:item_ethers","#cobblemon:potions",
        "#cobblemon:restores","#cobblemon:revives","#cobblemon:battle_items",
        "#cobblemon:poke_food","#cobblemon:feathers","#cobblemon:mints",
        "#cobblemon:candies","#cobblemon:vitamins","#cobblemon_ts:baguse"));
    public static final IntValue POKEBAG_SLOTS = new IntValue(27);
    public static final IntValue POKEBAG_MAX_STACK_SIZE = new IntValue(640);

    // ==================== 幸运方块设置 ====================
    public static final ConfigValue<List<String>> LEGENDARY_POKEMON = new ConfigValue<>(Arrays.asList(
        "Articuno","Zapdos","Moltres","Mew","Raikou","Entei","Suicune","Lugia","HoOh","Celebi",
        "Regirock","Regice","Registeel","Latias","Latios","Uxie","Mesprit","Azelf","Heatran","Regigigas",
        "Cresselia","Darkrai","Shaymin","Arceus","Cobalion","Terrakion","Virizion","Tornadus",
        "Thundurus","Landorus","Meloetta","Genesect","Xerneas","Yveltal","Zygarde","Diancie","Hoopa","Volcanion",
        "TypeNull","Silvally","TapuKoko","TapuLele","TapuBulu","TapuFini","Cosmog","Cosmoem",
        "Solgaleo","Lunala","Necrozma","Magearna","Marshadow","Zeraora","Zacian","Zamazenta","Eternatus",
        "Kubfu","Urshifu","Regieleki","Regidrago","Glastrier","Spectrier","Calyrex"));
    public static final ConfigValue<List<String>> ULTRA_BEAST_POKEMON = new ConfigValue<>(Arrays.asList(
        "Nihilego","Buzzwole","Pheromosa","Xurkitree","Celesteela","Kartana","Guzzlord",
        "Poipole","Naganadel","Stakataka","Blacephalon"));
    public static final IntValue MIN_SPAWN_LEVEL = new IntValue(5);
    public static final IntValue MAX_SPAWN_LEVEL = new IntValue(80);
    public static final ConfigValue<List<String>> LUCKY_WEIGHTS = new ConfigValue<>(Arrays.asList(
        "cobblemon_ts:item_lucky_block|cobblemon_ts:catching_charm,cobblemon_ts:shiny_charm|1|50",
        "cobblemon_ts:item_lucky_block|cobblemon_ts:exp_charm,cobblemon_ts:exp_all|2|30",
        "cobblemon_ts:item_lucky_block|cobblemon_ts:oval_charm|1|50",
        "cobblemon_ts:item_lucky_block|cobblemon_ts:gold_crown|2|30",
        "cobblemon_ts:item_lucky_block|cobblemon:master_ball|3|0",
        "cobblemon_ts:item_lucky_block|NbtElement:#cobblemon:candies|10|0",
        "cobblemon_ts:item_lucky_block|minecraft:dragon_egg,minecraft:nether_star|1|0",
        "cobblemon_ts:item_lucky_block|minecraft:emerald,minecraft:diamond|15|0",
        "cobblemon_ts:item_lucky_block|minecraft:redstone_block,minecraft:gold_ingot,minecraft:iron_ingot,minecraft:coal_block|20|0",
        "cobblemon_ts:item_lucky_block|nbt:minecraft:turtle_helmet@components:{\"minecraft:attribute_modifiers\":{modifiers:[{type:\"minecraft:generic.luck\",id:\"minecraft:generic.luck\",amount:10.0d,operation:\"add_value\",slot:\"head\"}],show_in_tooltip:false}}|1|100",
        "cobblemon_ts:shiny_lucky_block|mewtwo|50|100"));
    public static final IntValue LUCK_WEIGHT_PER_LEVEL = new IntValue(1);

    // ==================== 宝可梦编辑器设置 ====================
    public static final BooleanValue REQUIRE_CREATIVE = new BooleanValue(true);
    public static final BooleanValue EDITOR_AUTO_CLOSE = new BooleanValue(true);

    // ==================== 区块禁止器设置 ====================
    public static final IntValue CB_DEFAULT_RANGE = new IntValue(9);
    public static final IntValue CB_MAX_RANGE = new IntValue(9);
    public static final IntValue CB_BAN_CHUNK_LIMIT = new IntValue(100);
    public static final IntValue CB_FORCELOAD_CHUNK_LIMIT = new IntValue(50);

    static { load(); }

    // [线性-L1] 从Gson JSON加载配置
    @SuppressWarnings("unchecked")
    public static void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("cobblemonts");
        Path configFile = configDir.resolve("cobblemon_ts.json");
        JsonObject json = new JsonObject();
        if (Files.exists(configFile)) {
            try {
                json = JsonParser.parseString(Files.readString(configFile)).getAsJsonObject();
            } catch (Exception e) {
                CobblemonTS.LOGGER.error("Failed to load cobblemon_ts.json", e);
            }
        }
        boolean needSave = false;
        // [线性-W3] 缺少注释时补充写入（保留现有值，仅注入 _comment）
        if (!json.has("_comment")) needSave = true;

        // [线性-C1] 护符设置
        if (json.has(CATCHING_CHARM_BOOST_K)) CATCHING_CHARM_BOOST.v = json.get(CATCHING_CHARM_BOOST_K).getAsDouble();
        else { json.addProperty(CATCHING_CHARM_BOOST_K, 1.5); needSave=true; }
        if (json.has(EXP_CHARM_BOOST_K)) EXP_CHARM_BOOST.v = json.get(EXP_CHARM_BOOST_K).getAsDouble();
        else { json.addProperty(EXP_CHARM_BOOST_K, 1.5); needSave=true; }
        if (json.has(OVAL_CHARM_HATCH_BOOST_K)) OVAL_CHARM_HATCH_BOOST.v = json.get(OVAL_CHARM_HATCH_BOOST_K).getAsDouble();
        else { json.addProperty(OVAL_CHARM_HATCH_BOOST_K, 2.0); needSave=true; }
        if (json.has(SHINY_CHARM_BOOST_K)) SHINY_CHARM_BOOST.v = json.get(SHINY_CHARM_BOOST_K).getAsDouble();
        else { json.addProperty(SHINY_CHARM_BOOST_K, 3.0); needSave=true; }
        if (json.has(EXP_ALL_PERCENT_K)) EXP_ALL_PERCENT.v = json.get(EXP_ALL_PERCENT_K).getAsDouble();
        else { json.addProperty(EXP_ALL_PERCENT_K, 50.0); needSave=true; }
        if (json.has(SHOW_CHARM_MESSAGES_K)) SHOW_CHARM_MESSAGES.v = json.get(SHOW_CHARM_MESSAGES_K).getAsBoolean();
        else { json.addProperty(SHOW_CHARM_MESSAGES_K, true); needSave=true; }

        // [线性-C2] 开局道具
        if (json.has(START_ITEMS_ENABLED_K)) START_ITEMS_ENABLED.v = json.get(START_ITEMS_ENABLED_K).getAsBoolean();
        else { json.addProperty(START_ITEMS_ENABLED_K, false); needSave=true; }
        if (json.has(START_WITH_ITEMS_K)) START_WITH_ITEMS.v = jsonArrayToList(json.getAsJsonArray(START_WITH_ITEMS_K));
        else { json.add(START_WITH_ITEMS_K, listToJsonArray(START_WITH_ITEMS.v)); needSave=true; }

        // [线性-C3] 道具袋
        if (json.has(POKEBAG_BLACKLIST_TAGS_K)) POKEBAG_BLACKLIST_TAGS.v = jsonArrayToList(json.getAsJsonArray(POKEBAG_BLACKLIST_TAGS_K));
        else { json.add(POKEBAG_BLACKLIST_TAGS_K, listToJsonArray(POKEBAG_BLACKLIST_TAGS.v)); needSave=true; }
        if (json.has(POKEBAG_WHITELIST_TAGS_K)) POKEBAG_WHITELIST_TAGS.v = jsonArrayToList(json.getAsJsonArray(POKEBAG_WHITELIST_TAGS_K));
        else { json.add(POKEBAG_WHITELIST_TAGS_K, listToJsonArray(POKEBAG_WHITELIST_TAGS.v)); needSave=true; }
        if (json.has(POKEBAG_SLOTS_K)) POKEBAG_SLOTS.v = json.get(POKEBAG_SLOTS_K).getAsInt();
        else { json.addProperty(POKEBAG_SLOTS_K, 27); needSave=true; }
        if (json.has(POKEBAG_MAX_STACK_SIZE_K)) POKEBAG_MAX_STACK_SIZE.v = json.get(POKEBAG_MAX_STACK_SIZE_K).getAsInt();
        else { json.addProperty(POKEBAG_MAX_STACK_SIZE_K, 640); needSave=true; }

        // [线性-C4] 幸运方块
        if (json.has(LEGENDARY_POKEMON_K)) LEGENDARY_POKEMON.v = jsonArrayToList(json.getAsJsonArray(LEGENDARY_POKEMON_K));
        else { json.add(LEGENDARY_POKEMON_K, listToJsonArray(LEGENDARY_POKEMON.v)); needSave=true; }
        if (json.has(ULTRA_BEAST_POKEMON_K)) ULTRA_BEAST_POKEMON.v = jsonArrayToList(json.getAsJsonArray(ULTRA_BEAST_POKEMON_K));
        else { json.add(ULTRA_BEAST_POKEMON_K, listToJsonArray(ULTRA_BEAST_POKEMON.v)); needSave=true; }
        if (json.has(MIN_SPAWN_LEVEL_K)) MIN_SPAWN_LEVEL.v = json.get(MIN_SPAWN_LEVEL_K).getAsInt();
        else { json.addProperty(MIN_SPAWN_LEVEL_K, 5); needSave=true; }
        if (json.has(MAX_SPAWN_LEVEL_K)) MAX_SPAWN_LEVEL.v = json.get(MAX_SPAWN_LEVEL_K).getAsInt();
        else { json.addProperty(MAX_SPAWN_LEVEL_K, 80); needSave=true; }
        if (json.has(LUCKY_WEIGHTS_K)) LUCKY_WEIGHTS.v = jsonArrayToList(json.getAsJsonArray(LUCKY_WEIGHTS_K));
        else { json.add(LUCKY_WEIGHTS_K, listToJsonArray(LUCKY_WEIGHTS.v)); needSave=true; }
        if (json.has(LUCK_WEIGHT_PER_LEVEL_K)) LUCK_WEIGHT_PER_LEVEL.v = json.get(LUCK_WEIGHT_PER_LEVEL_K).getAsInt();
        else { json.addProperty(LUCK_WEIGHT_PER_LEVEL_K, 1); needSave=true; }

        // [线性-C5] 宝可梦编辑器
        if (json.has(REQUIRE_CREATIVE_K)) REQUIRE_CREATIVE.v = json.get(REQUIRE_CREATIVE_K).getAsBoolean();
        else { json.addProperty(REQUIRE_CREATIVE_K, true); needSave=true; }
        if (json.has(EDITOR_AUTO_CLOSE_K)) EDITOR_AUTO_CLOSE.v = json.get(EDITOR_AUTO_CLOSE_K).getAsBoolean();
        else { json.addProperty(EDITOR_AUTO_CLOSE_K, true); needSave=true; }

        // [线性-C6] 区块禁止器
        if (json.has(CB_DEFAULT_RANGE_K)) CB_DEFAULT_RANGE.v = json.get(CB_DEFAULT_RANGE_K).getAsInt();
        else { json.addProperty(CB_DEFAULT_RANGE_K, 9); needSave=true; }
        if (json.has(CB_MAX_RANGE_K)) CB_MAX_RANGE.v = json.get(CB_MAX_RANGE_K).getAsInt();
        else { json.addProperty(CB_MAX_RANGE_K, 9); needSave=true; }
        if (json.has(CB_BAN_CHUNK_LIMIT_K)) CB_BAN_CHUNK_LIMIT.v = json.get(CB_BAN_CHUNK_LIMIT_K).getAsInt();
        else { json.addProperty(CB_BAN_CHUNK_LIMIT_K, 100); needSave=true; }
        if (json.has(CB_FORCELOAD_CHUNK_LIMIT_K)) CB_FORCELOAD_CHUNK_LIMIT.v = json.get(CB_FORCELOAD_CHUNK_LIMIT_K).getAsInt();
        else { json.addProperty(CB_FORCELOAD_CHUNK_LIMIT_K, 50); needSave=true; }

        // [线性-S1] 保存默认配置（带注释，格式化输出）
        if (needSave) {
            writeConfig(configFile, json);
        }
    }

    // [线性-S2] 保存当前配置到JSON（供Cloth Config保存回调统一落盘）
    public static void save() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve("cobblemonts").resolve("cobblemon_ts.json");
        JsonObject json = new JsonObject();

        // [线性-C1] 护符设置
        json.addProperty(CATCHING_CHARM_BOOST_K, CATCHING_CHARM_BOOST.v);
        json.addProperty(EXP_CHARM_BOOST_K, EXP_CHARM_BOOST.v);
        json.addProperty(OVAL_CHARM_HATCH_BOOST_K, OVAL_CHARM_HATCH_BOOST.v);
        json.addProperty(SHINY_CHARM_BOOST_K, SHINY_CHARM_BOOST.v);
        json.addProperty(EXP_ALL_PERCENT_K, EXP_ALL_PERCENT.v);
        json.addProperty(SHOW_CHARM_MESSAGES_K, SHOW_CHARM_MESSAGES.v);

        // [线性-C2] 开局道具
        json.addProperty(START_ITEMS_ENABLED_K, START_ITEMS_ENABLED.v);
        json.add(START_WITH_ITEMS_K, listToJsonArray(START_WITH_ITEMS.v));

        // [线性-C3] 道具袋
        json.add(POKEBAG_BLACKLIST_TAGS_K, listToJsonArray(POKEBAG_BLACKLIST_TAGS.v));
        json.add(POKEBAG_WHITELIST_TAGS_K, listToJsonArray(POKEBAG_WHITELIST_TAGS.v));
        json.addProperty(POKEBAG_SLOTS_K, POKEBAG_SLOTS.v);
        json.addProperty(POKEBAG_MAX_STACK_SIZE_K, POKEBAG_MAX_STACK_SIZE.v);

        // [线性-C4] 幸运方块
        json.add(LEGENDARY_POKEMON_K, listToJsonArray(LEGENDARY_POKEMON.v));
        json.add(ULTRA_BEAST_POKEMON_K, listToJsonArray(ULTRA_BEAST_POKEMON.v));
        json.addProperty(MIN_SPAWN_LEVEL_K, MIN_SPAWN_LEVEL.v);
        json.addProperty(MAX_SPAWN_LEVEL_K, MAX_SPAWN_LEVEL.v);
        json.add(LUCKY_WEIGHTS_K, listToJsonArray(LUCKY_WEIGHTS.v));
        json.addProperty(LUCK_WEIGHT_PER_LEVEL_K, LUCK_WEIGHT_PER_LEVEL.v);

        // [线性-C5] 宝可梦编辑器
        json.addProperty(REQUIRE_CREATIVE_K, REQUIRE_CREATIVE.v);
        json.addProperty(EDITOR_AUTO_CLOSE_K, EDITOR_AUTO_CLOSE.v);

        // [线性-C6] 区块禁止器
        json.addProperty(CB_DEFAULT_RANGE_K, CB_DEFAULT_RANGE.v);
        json.addProperty(CB_MAX_RANGE_K, CB_MAX_RANGE.v);
        json.addProperty(CB_BAN_CHUNK_LIMIT_K, CB_BAN_CHUNK_LIMIT.v);
        json.addProperty(CB_FORCELOAD_CHUNK_LIMIT_K, CB_FORCELOAD_CHUNK_LIMIT.v);

        writeConfig(configFile, json);
    }

    // [线性-W1] 统一写盘：注入 _comment 注释 + 格式化输出
    private static void writeConfig(Path configFile, JsonObject json) {
        json.add("_comment", buildCommentArray());
        String pretty = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(json);
        try {
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, pretty);
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("Failed to save cobblemon_ts.json", e);
        }
    }

    // [线性-W2] 生成配置注释说明（JSON 无原生注释，用 _comment 数组承载）
    private static JsonArray buildCommentArray() {
        JsonArray arr = new JsonArray();
        arr.add("CobblemonTS 配置文件");
        arr.add("修改后重启游戏生效");
        arr.add("");
        arr.add("== 护符设置 ==");
        arr.add("catching_charm_boost: 捕捉护符的捕捉成功率倍率 (默认 1.5)");
        arr.add("exp_charm_boost: 经验护符的经验获取倍率 (默认 1.5)");
        arr.add("oval_charm_hatch_boost: 圆形护符孵化加速倍率 (默认 2.0)");
        arr.add("shiny_charm_boost: 闪光护符的闪光概率倍率 (默认 3.0)");
        arr.add("exp_all_percent: 学习装置分享给全队的经验百分比 (默认 50.0)");
        arr.add("show_charm_messages: 是否显示护符生效提示消息 (默认 true)");
        arr.add("");
        arr.add("== 开局道具 ==");
        arr.add("start_items_enabled: 玩家首次进入时是否发放开局道具 (默认 false)");
        arr.add("start_with_items: 开局道具列表，用宝可梦/物品 ID 字符串 (默认 6 件护符道具)");
        arr.add("");
        arr.add("== 道具袋 ==");
        arr.add("pokebag_blacklist_tags: 道具袋禁止放入的物品标签列表");
        arr.add("pokebag_whitelist_tags: 道具袋允许放入的物品标签列表");
        arr.add("pokebag_slots: 道具袋容量槽位 (默认 27)");
        arr.add("pokebag_max_stack_size: 道具袋单格最大堆叠数量 (默认 640)");
        arr.add("");
        arr.add("== 幸运方块 ==");
        arr.add("legendary_pokemon: 传说宝可梦名单，用于幸运方块传说掉落池");
        arr.add("ultra_beast_pokemon: 究极异兽名单，用于幸运方块异兽掉落池");
        arr.add("min_spawn_level: 幸运方块生成的宝可梦最低等级 (默认 5)");
        arr.add("max_spawn_level: 幸运方块生成的宝可梦最高等级 (默认 80)");
        arr.add("lucky_weights: 幸运方块奖励权重表，格式: 掉落物|权重|保底次数");
        arr.add("luck_weight_per_level: 每级增加的基础幸运权重 (默认 1)");
        arr.add("");
        arr.add("== 宝可梦编辑器 ==");
        arr.add("require_creative: 编辑器是否仅限创造模式使用 (默认 true)");
        arr.add("editor_auto_close: 编辑完成后是否自动关闭界面 (默认 true)");
        arr.add("");
        arr.add("== 区块禁止器 ==");
        arr.add("cb_default_range: 区块禁止器默认半径 (默认 9)");
        arr.add("cb_max_range: 区块禁止器最大半径 (默认 9)");
        arr.add("cb_ban_chunk_limit: 单玩家最多禁止区块数 (默认 100)");
        arr.add("cb_forceload_chunk_limit: 单玩家最多强加载区块数 (默认 50)");
        return arr;
    }

    // [线性-UT1] JsonArray -> List<String>
    private static List<String> jsonArrayToList(JsonArray arr) {
        List<String> list = new ArrayList<>();
        for (int i=0; i<arr.size(); i++) list.add(arr.get(i).getAsString());
        return list;
    }

    // [线性-UT2] List<String> -> JsonArray
    private static JsonArray listToJsonArray(List<String> list) {
        JsonArray arr = new JsonArray();
        for (String s : list) arr.add(s);
        return arr;
    }

    // ======== 区块禁止器 getter（钳位保护） ========
    public static int getCbDefaultRange() {
        int v = CB_DEFAULT_RANGE.get();
        if (v < 3) v = 3;
        if (v > 16) v = 16;
        return v % 2 == 0 ? v + 1 : v;
    }
    public static int getCbMaxRange() {
        int v = CB_MAX_RANGE.get();
        if (v < 3) v = 3;
        if (v > 16) v = 16;
        return v % 2 == 0 ? v + 1 : v;
    }
    public static int getCbBanChunkLimit() { return CB_BAN_CHUNK_LIMIT.get(); }
    public static int getCbForceloadChunkLimit() { return CB_FORCELOAD_CHUNK_LIMIT.get(); }
}