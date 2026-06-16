package com.cobblemon_ts.mod;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;

public class CharmConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // ==================== 护符设置 ====================
    public static final ModConfigSpec.DoubleValue CATCHING_CHARM_BOOST;
    public static final ModConfigSpec.DoubleValue EXP_CHARM_BOOST;
    public static final ModConfigSpec.DoubleValue OVAL_CHARM_HATCH_BOOST;
    public static final ModConfigSpec.DoubleValue SHINY_CHARM_BOOST;
    public static final ModConfigSpec.DoubleValue EXP_ALL_PERCENT;
    public static final ModConfigSpec.BooleanValue SHOW_CHARM_MESSAGES;

    // ==================== 开局道具列表 ====================
    public static final ModConfigSpec.BooleanValue START_ITEMS_ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> START_WITH_ITEMS;

    // ==================== 道具袋设置 ====================
    public static final ModConfigSpec.ConfigValue<List<? extends String>> POKEBAG_BLACKLIST_TAGS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> POKEBAG_WHITELIST_TAGS;
    public static final ModConfigSpec.IntValue POKEBAG_SLOTS;
    public static final ModConfigSpec.IntValue POKEBAG_MAX_STACK_SIZE;
    public static final ModConfigSpec.BooleanValue POKEBAG_EXTRACT_LIMIT_ENABLED;
    public static final ModConfigSpec.IntValue POKEBAG_EXTRACT_LIMIT;

    // ==================== 幸运方块设置 ====================
    public static final ModConfigSpec.ConfigValue<List<? extends String>> LEGENDARY_POKEMON;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ULTRA_BEAST_POKEMON;
    public static final ModConfigSpec.IntValue MIN_SPAWN_LEVEL;
    public static final ModConfigSpec.IntValue MAX_SPAWN_LEVEL;

    // ==================== 宝可梦编辑器设置 ====================
    public static final ModConfigSpec.BooleanValue REQUIRE_CREATIVE;
    public static final ModConfigSpec.BooleanValue EDITOR_AUTO_CLOSE;

    static {
        // [线性-C1] 护符设置
        BUILDER.comment("护符设置").push("charm_settings");
        
        CATCHING_CHARM_BOOST = BUILDER
            .comment("防晃护符捕获率加成倍数 (1.5 = 提升50%)")
            .defineInRange("catching_charm_boost", 1.5, 1.0, 10.0);
        
        EXP_CHARM_BOOST = BUILDER
            .comment("经验护符经验加成倍数 (1.5 = 提升50%)")
            .defineInRange("exp_charm_boost", 1.5, 1.0, 10.0);
        
        OVAL_CHARM_HATCH_BOOST = BUILDER
            .comment("圆形护符孵化加速倍数 (2.0 = 减少50%时间)")
            .defineInRange("oval_charm_hatch_boost", 2.0, 1.0, 10.0);
        
        SHINY_CHARM_BOOST = BUILDER
            .comment("闪耀护符闪光几率加成倍数 (3.0 = 3倍几率)")
            .defineInRange("shiny_charm_boost", 3.0, 1.0, 100.0);
        
        EXP_ALL_PERCENT = BUILDER
            .comment("高级学习装置经验分配百分比 (50.0 = 未出战宝可梦获得50%经验)")
            .defineInRange("exp_all_percent", 50.0, 0.0, 100.0);
        
        SHOW_CHARM_MESSAGES = BUILDER
            .comment("是否显示护符使用提示消息")
            .define("show_charm_messages", true);

        BUILDER.pop();

        // [线性-C2] 开局道具列表
        BUILDER.comment("开局道具设置").push("start_items");

        START_ITEMS_ENABLED = BUILDER
            .comment("是否启用开局自动获得道具（关闭后玩家登录不会自动获得任何道具）")
            .define("start_items_enabled", false);

        START_WITH_ITEMS = BUILDER
            .comment("玩家首次登录时自动获得的道具列表（护符栏6个物品：4个护符 + 高级学习装置 + 道具袋）",
                     "可用的道具ID: shiny_charm, oval_charm, exp_charm, catching_charm, exp_all, pokebag")
            .defineList("start_with_items",
                Arrays.asList("shiny_charm", "oval_charm", "exp_charm", "catching_charm", "exp_all", "pokebag"),
                obj -> obj instanceof String);

        BUILDER.pop();

        // [线性-C3] 道具袋设置
        BUILDER.comment("道具袋设置").push("pokebag_settings");

        POKEBAG_BLACKLIST_TAGS = BUILDER
            .comment("物品黑名单（支持物品ID如cobblemon:poke_ball，或Tag如#cobblemon:held_items）")
            .defineList("blacklist", List.of(), obj -> obj instanceof String);

        POKEBAG_WHITELIST_TAGS = BUILDER
            .comment("物品白名单（支持物品ID如cobblemon:poke_ball，或Tag如#cobblemon:pokeballs）")
            .defineList("whitelist", Arrays.asList(
                "#cobblemon:poke_balls",
                "#cobblemon:item_ethers",
                "#cobblemon:potions",
                "#cobblemon:restores",
                "#cobblemon:revives",
                "#cobblemon:battle_items",
                "#cobblemon:poke_food",
                "#cobblemon:feathers",
                "#cobblemon:mints",
                "#cobblemon:candies",
                "#cobblemon:vitamins"
            ), obj -> obj instanceof String);

        POKEBAG_SLOTS = BUILDER
            .comment("道具袋容量（格子数）")
            .defineInRange("bag_slots", 27, 1, 54);

        POKEBAG_MAX_STACK_SIZE = BUILDER
            .comment("道具袋物品堆叠上限（单个格子最大数量，1-8192）")
            .defineInRange("max_stack_size", 640, 1, 8192);

        POKEBAG_EXTRACT_LIMIT_ENABLED = BUILDER
            .comment("是否限制shift+左键从道具袋取出的数量（开启则每次最多取extract_limit个，关闭则全部取出）")
            .define("extract_limit_enabled", true);

        POKEBAG_EXTRACT_LIMIT = BUILDER
            .comment("shift+左键从道具袋取出物品时的最大数量")
            .defineInRange("extract_limit", 64, 1, 1024);

        BUILDER.pop();

        // [线性-C4] 幸运方块设置
        BUILDER.comment("幸运方块设置").push("lucky_blocks");

        LEGENDARY_POKEMON = BUILDER
            .comment("传说宝可梦幸运方块可召唤的传说宝可梦列表")
            .defineList("legendary_pokemon", Arrays.asList(
                "Articuno", "Zapdos", "Moltres", "Mew",
                "Raikou", "Entei", "Suicune", "Lugia", "HoOh", "Celebi",
                "Regirock", "Regice", "Registeel", "Latias", "Latios",
                "Uxie", "Mesprit", "Azelf", "Heatran", "Regigigas",
                "Cresselia", "Darkrai", "Shaymin", "Arceus",
                "Cobalion", "Terrakion", "Virizion", "Tornadus",
                "Thundurus", "Landorus", "Meloetta", "Genesect",
                "Xerneas", "Yveltal", "Zygarde", "Diancie", "Hoopa", "Volcanion",
                "TypeNull", "Silvally", "TapuKoko", "TapuLele",
                "TapuBulu", "TapuFini", "Cosmog", "Cosmoem",
                "Solgaleo", "Lunala", "Necrozma", "Magearna", "Marshadow", "Zeraora",
                "Zacian", "Zamazenta", "Eternatus", "Kubfu", "Urshifu",
                "Regieleki", "Regidrago", "Glastrier", "Spectrier", "Calyrex"
            ), obj -> obj instanceof String);

        ULTRA_BEAST_POKEMON = BUILDER
            .comment("究极异兽幸运方块可召唤的究极异兽列表")
            .defineList("ultra_beast_pokemon", Arrays.asList(
                "Nihilego", "Buzzwole", "Pheromosa", "Xurkitree",
                "Celesteela", "Kartana", "Guzzlord", "Poipole",
                "Naganadel", "Stakataka", "Blacephalon"
            ), obj -> obj instanceof String);

        MIN_SPAWN_LEVEL = BUILDER
            .comment("随机宝可梦幸运方块生成的最低等级")
            .defineInRange("min_spawn_level", 5, 1, 100);

        MAX_SPAWN_LEVEL = BUILDER
            .comment("随机宝可梦幸运方块生成的最高等级")
            .defineInRange("max_spawn_level", 80, 1, 100);

        BUILDER.pop();

        // [线性-C5] 宝可梦编辑器设置
        BUILDER.comment("宝可梦编辑器设置").push("pokemon_editor");

        REQUIRE_CREATIVE = BUILDER
            .comment("是否必须在创造模式下才能使用编辑器 (true = 仅创造模式可用, false = OP权限即可用)")
            .define("require_creative", true);

        EDITOR_AUTO_CLOSE = BUILDER
            .comment("保存后是否自动关闭编辑器 (true = 自动关闭, false = 保持打开)")
            .define("editor_auto_close", true);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}