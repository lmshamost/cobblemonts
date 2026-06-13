package com.cobblemon_ts.mod.lucky;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;

public class LuckyConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LUCKY_RECIPES_ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> LEGENDARY_POKEMON;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ULTRA_BEAST_POKEMON;
    public static final ModConfigSpec.IntValue MIN_SPAWN_LEVEL;
    public static final ModConfigSpec.IntValue MAX_SPAWN_LEVEL;

    static {
        BUILDER.push("lucky_blocks");

        LUCKY_RECIPES_ENABLED = BUILDER
                .comment("是否启用幸运方块的合成配方（默认关闭）")
                .define("recipes_enabled", false);

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
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}