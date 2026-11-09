package com.cobblemon_ts.mod;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ChunkBarrierServerConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue DEFAULT_RANGE;
    public static final ModConfigSpec.IntValue MAX_RANGE;

    static {
        BUILDER.comment("区块禁止器设置 | Chunk Barrier Settings").push("chunk_barrier");

        DEFAULT_RANGE = BUILDER
                .comment("Default GUI range in chunks (odd number, min 3, max 16)\nGUI 默认显示区块范围（奇数）")
                .defineInRange("default_range", 9, 3, 16);

        MAX_RANGE = BUILDER
                .comment("Max allowed GUI range (odd, hard cap 16, server owner only)\nGUI 允许的最大区块范围（奇数，硬上限 16，仅服主可改）")
                .defineInRange("max_range", 9, 3, 16);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static int getDefaultRange() {
        int v = DEFAULT_RANGE.get();
        return v % 2 == 0 ? v + 1 : v;
    }

    public static int getMaxRange() {
        int v = MAX_RANGE.get();
        if (v > 16) v = 16;
        return v % 2 == 0 ? v + 1 : v;
    }
}