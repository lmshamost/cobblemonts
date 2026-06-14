package com.cobblemon_ts.mod.pokemoneditor;

import net.neoforged.neoforge.common.ModConfigSpec;

public class PokemonEditorConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // ★调试模式已集中到 CobblemonTSConfig.debug.debug_editor（全局 debug 组）
    public static final ModConfigSpec.BooleanValue REQUIRE_CREATIVE;
    public static final ModConfigSpec.BooleanValue BRIGHT_THEME;
    public static final ModConfigSpec.BooleanValue EDITOR_AUTO_CLOSE;

    static {
        BUILDER.comment("宝可梦编辑器设置").push("pokemon_editor");

        REQUIRE_CREATIVE = BUILDER
                .comment("是否必须在创造模式下才能使用编辑器 (true = 仅创造模式可用, false = OP权限即可用)")
                .define("require_creative", true);

        BRIGHT_THEME = BUILDER
                .comment("是否使用亮色主题 (true = 亮蓝色标签, false = 深色灰色标签)")
                .define("bright_theme", true);

        EDITOR_AUTO_CLOSE = BUILDER
                .comment("保存后是否自动关闭编辑器 (true = 自动关闭, false = 保持打开)")
                .define("editor_auto_close", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}