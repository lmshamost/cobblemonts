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
    public static final ModConfigSpec.ConfigValue<List<? extends String>> LUCKY_WEIGHTS;
    public static final ModConfigSpec.IntValue LUCK_WEIGHT_PER_LEVEL;

    // ==================== 宝可梦编辑器设置 ====================
    public static final ModConfigSpec.BooleanValue REQUIRE_CREATIVE;
    public static final ModConfigSpec.BooleanValue EDITOR_AUTO_CLOSE;

    static {
        // [线性-C1] 护符设置
        BUILDER.comment("护符设置 | Charm Settings").push("charm_settings");
        
        CATCHING_CHARM_BOOST = BUILDER
            .comment("防晃护符捕获率加成倍数 (1.5 = 提升50%) | Catching Charm catch rate multiplier (1.5 = +50% boost)")
            .defineInRange("catching_charm_boost", 1.5, 1.0, 10.0);
        
        EXP_CHARM_BOOST = BUILDER
            .comment("经验护符经验加成倍数 (1.5 = 提升50%) | Exp. Charm EXP multiplier (1.5 = +50% boost)")
            .defineInRange("exp_charm_boost", 1.5, 1.0, 10.0);
        
        OVAL_CHARM_HATCH_BOOST = BUILDER
            .comment("圆形护符孵化加速倍数 (2.0 = 减少50%时间) | Oval Charm hatch speed multiplier (2.0 = -50% time)")
            .defineInRange("oval_charm_hatch_boost", 2.0, 1.0, 10.0);
        
        SHINY_CHARM_BOOST = BUILDER
            .comment("闪耀护符闪光几率加成倍数 (3.0 = 3倍几率) | Shiny Charm shiny rate multiplier (3.0 = 3x rate)")
            .defineInRange("shiny_charm_boost", 3.0, 1.0, 10000.0);
        
        EXP_ALL_PERCENT = BUILDER
            .comment("高级学习装置经验分配百分比 (50.0 = 未出战宝可梦获得50%经验) | Exp. All EXP share percentage (50.0 = non-battlers get 50% EXP)")
            .defineInRange("exp_all_percent", 50.0, 0.0, 100.0);
        
        SHOW_CHARM_MESSAGES = BUILDER
            .comment("是否显示护符使用提示消息 | Show charm usage messages")
            .define("show_charm_messages", true);

        BUILDER.pop();

        // [线性-C2] 开局道具列表
        BUILDER.comment("开局道具设置 | Starting Items").push("start_items");

        START_ITEMS_ENABLED = BUILDER
            .comment("是否启用开局自动获得道具（关闭后玩家登录不会自动获得任何道具）\n" +
                     "Enable auto-grant items on first join (false = players won't receive any items on login)")
            .define("start_items_enabled", false);

        START_WITH_ITEMS = BUILDER
            .comment("玩家首次登录时自动获得的道具列表（护符栏6个物品：4个护符 + 高级学习装置 + 道具袋）",
                     "可用的道具ID: shiny_charm, oval_charm, exp_charm, catching_charm, exp_all, pokebag\n---\n" +
                     "List of items auto-granted on first login (6 charm slot items: 4 charms + Exp.All + Poké Bag)\n" +
                     "Available item IDs: shiny_charm, oval_charm, exp_charm, catching_charm, exp_all, pokebag")
            .defineList("start_with_items",
                Arrays.asList("shiny_charm", "oval_charm", "exp_charm", "catching_charm", "exp_all", "pokebag"),
                obj -> obj instanceof String);

        BUILDER.pop();

        // [线性-C3] 道具袋设置
        BUILDER.comment("道具袋设置 | Poké Bag Settings").push("pokebag_settings");

        POKEBAG_BLACKLIST_TAGS = BUILDER
            .comment("物品黑名单（支持物品ID如cobblemon:poke_ball，或Tag如#cobblemon:held_items）\n" +
                     "Item blacklist (supports item IDs e.g. cobblemon:poke_ball, or Tags e.g. #cobblemon:held_items)")
            .defineList("blacklist", List.of(), obj -> obj instanceof String);

        POKEBAG_WHITELIST_TAGS = BUILDER
            .comment("物品白名单（支持物品ID如cobblemon:poke_ball，或Tag如#cobblemon:pokeballs）\n" +
                     "Item whitelist (supports item IDs e.g. cobblemon:poke_ball, or Tags e.g. #cobblemon:pokeballs)")
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
                "#cobblemon:vitamins",
                "#coblemon_ts:baguse"
            ), obj -> obj instanceof String);

        POKEBAG_SLOTS = BUILDER
            .comment("道具袋容量（格子数） | Poké Bag capacity (number of slots)")
            .defineInRange("bag_slots", 27, 1, 54);

        POKEBAG_MAX_STACK_SIZE = BUILDER
            .comment("道具袋物品堆叠上限（单个格子最大数量，1-8192） | Max stack size per slot in Poké Bag (1-8192)")
            .defineInRange("max_stack_size", 640, 1, 8192);

        POKEBAG_EXTRACT_LIMIT_ENABLED = BUILDER
            .comment("是否限制shift+左键从道具袋取出的数量（开启则每次最多取extract_limit个，关闭则全部取出）\n" +
                     "Limit shift+click extraction from Poké Bag (enabled = max extract_limit per operation, disabled = extract all)")
            .define("extract_limit_enabled", true);

        POKEBAG_EXTRACT_LIMIT = BUILDER
            .comment("shift+左键从道具袋取出物品时的最大数量 | Max items extracted per shift+click from Poké Bag")
            .defineInRange("extract_limit", 64, 1, 1024);

        BUILDER.pop();

        // [线性-C4] 幸运方块设置
        BUILDER.comment("幸运方块设置 | Lucky Block Settings").push("lucky_blocks");

        LEGENDARY_POKEMON = BUILDER
            .comment("传说宝可梦幸运方块可召唤的传说宝可梦列表 | List of legendary Pokémon spawnable from Legendary Lucky Block")
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
            .comment("究极异兽幸运方块可召唤的究极异兽列表 | List of Ultra Beasts spawnable from Ultra Beast Lucky Block")
            .defineList("ultra_beast_pokemon", Arrays.asList(
                "Nihilego", "Buzzwole", "Pheromosa", "Xurkitree",
                "Celesteela", "Kartana", "Guzzlord", "Poipole",
                "Naganadel", "Stakataka", "Blacephalon"
            ), obj -> obj instanceof String);

        MIN_SPAWN_LEVEL = BUILDER
            .comment("随机宝可梦幸运方块生成的最低等级 | Minimum spawn level for Lucky Block Pokémon")
            .defineInRange("min_spawn_level", 5, 1, 100);

        MAX_SPAWN_LEVEL = BUILDER
            .comment("随机宝可梦幸运方块生成的最高等级 | Maximum spawn level for Lucky Block Pokémon")
            .defineInRange("max_spawn_level", 80, 1, 100);

        LUCKY_WEIGHTS = BUILDER
            .comment("幸运方块权重与保底配置（仿原版loot_tables的quality机制）",
                     "格式: 方块ID|物品列表(逗号分隔)|权重(整数)|保底(整数)",
                     "方块ID: coblemon_ts:item_lucky_block / coblemon_ts:lucky_block / coblemon_ts:legendary_lucky_block / coblemon_ts:ultra_beast_lucky_block / coblemon_ts:shiny_lucky_block",
                     "物品列表: item_id 或 tag:#tag_id 或 nbt:item_id@{components} 或 pokemon英文名",
                     "权重: 整数, 0=跳过加权随机仅靠保底, 数值越大概率越高",
                     "保底: 整数, 0=无保底, 每N次触发必出一次",
                     "动态幸运加成: 见 luck_weight_per_level 配置项 → 给最低权重物品加权",
                     "示例:",
                     "  coblemon_ts:item_lucky_block|coblemon_ts:catching_charm,coblemon_ts:shiny_charm|1|50",
                     "  coblemon_ts:item_lucky_block|coblemon_ts:exp_charm,coblemon_ts:exp_all|2|30",
                     "  coblemon_ts:ultra_beast_lucky_block|nihilego|1|800\n---\n" +
                     "Lucky Block weight & pity config (mimics vanilla loot_tables quality mechanism)",
                     "Format: block_id|item_list(comma separated)|weight(int)|pity(int)",
                     "Block IDs: coblemon_ts:item_lucky_block / coblemon_ts:lucky_block / coblemon_ts:legendary_lucky_block / coblemon_ts:ultra_beast_lucky_block / coblemon_ts:shiny_lucky_block",
                     "Item list: item_id or tag:#tag_id or nbt:item_id@{components} or pokemon_english_name",
                     "Weight: integer, 0=skip weighted random (pity only), higher = more likely",
                     "Pity: integer, 0=no pity, guaranteed every N triggers",
                     "Dynamic luck bonus: see luck_weight_per_level → adds weight to lowest-weight items",
                     "Examples:",
                     "  coblemon_ts:item_lucky_block|coblemon_ts:catching_charm,coblemon_ts:shiny_charm|1|50",
                     "  coblemon_ts:item_lucky_block|coblemon_ts:exp_charm,coblemon_ts:exp_all|2|30",
                     "  coblemon_ts:ultra_beast_lucky_block|nihilego|1|800")
            .defineList("lucky_weights", Arrays.asList(
                // === 物品幸运方块默认配置 ===
                "coblemon_ts:item_lucky_block|coblemon_ts:catching_charm,coblemon_ts:shiny_charm|1|50",
                "coblemon_ts:item_lucky_block|coblemon_ts:exp_charm,coblemon_ts:exp_all|2|30",
                "coblemon_ts:item_lucky_block|coblemon_ts:oval_charm|1|50",
                "coblemon_ts:item_lucky_block|coblemon_ts:gold_crown|2|30",
                "coblemon_ts:item_lucky_block|cobblemon:master_ball|3|0",
                "coblemon_ts:item_lucky_block|tag:#cobblemon:candies|10|0",
                "coblemon_ts:item_lucky_block|minecraft:dragon_egg,minecraft:nether_star|1|0",
                "coblemon_ts:item_lucky_block|minecraft:emerald,minecraft:diamond|15|0",
                "coblemon_ts:item_lucky_block|minecraft:redstone_block,minecraft:gold_ingot,minecraft:iron_ingot,minecraft:coal_block|20|0",
                "coblemon_ts:item_lucky_block|nbt:minecraft:turtle_helmet@components:{\"minecraft:attribute_modifiers\":{modifiers:[{type:\"minecraft:generic.luck\",id:\"minecraft:generic.luck\",amount:10.0d,operation:\"add_value\",slot:\"head\"}],show_in_tooltip:false}}|1|100",
                // === 闪光幸运方块默认配置 ===
                "coblemon_ts:shiny_lucky_block|mewtwo|50|100"
            ), obj -> obj instanceof String);

        LUCK_WEIGHT_PER_LEVEL = BUILDER
            .comment("幸运属性加成（仿原版loot_tables的quality机制）",
                     "每1点幸运值 = 给最低权重物品额外加多少权重值",
                     "公式: 最终权重 = base_weight + floor(luck_weight_per_level * 玩家幸运值)",
                     "加成只作用于当前方块配置中权重最低的物品（多个并列最低则全部加成）",
                     "默认: 1 (即幸运5 → 最低权重物品+5权重)",
                     "设为0即可禁用幸运加成\n---\n" +
                     "Luck stat bonus (mimics vanilla loot_tables quality mechanism)",
                     "Each point of Luck = extra weight added to the lowest-weight item(s)",
                     "Formula: final_weight = base_weight + floor(luck_weight_per_level * player_luck)",
                     "Only applies to item(s) with the lowest weight in the current block config (ties all receive bonus)",
                     "Default: 1 (i.e. Luck 5 → lowest-weight items get +5 weight)",
                     "Set to 0 to disable luck bonus")
            .defineInRange("luck_weight_per_level", 1, 0, 1000);

        BUILDER.pop();

        // [线性-C5] 宝可梦编辑器设置
        BUILDER.comment("宝可梦编辑器设置 | Pokémon Editor Settings").push("pokemon_editor");

        REQUIRE_CREATIVE = BUILDER
            .comment("是否必须在创造模式下才能使用编辑器 (true = 仅创造模式可用, false = OP权限即可用)\n" +
                     "Require creative mode to use editor (true = creative mode only, false = OP permission sufficient)")
            .define("require_creative", true);

        EDITOR_AUTO_CLOSE = BUILDER
            .comment("保存后是否自动关闭编辑器 (true = 自动关闭, false = 保持打开)\n" +
                     "Auto-close editor after saving (true = auto close, false = stay open)")
            .define("editor_auto_close", true);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}