package com.cobblemon_ts.mod;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CharmConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue CATCHING_CHARM_BOOST;
    public static final ModConfigSpec.DoubleValue EXP_CHARM_BOOST;
    public static final ModConfigSpec.DoubleValue OVAL_CHARM_HATCH_BOOST;
    public static final ModConfigSpec.DoubleValue SHINY_CHARM_BOOST;
    public static final ModConfigSpec.DoubleValue EXP_ALL_PERCENT;
    public static final ModConfigSpec.BooleanValue SHOW_CHARM_MESSAGES;
    // ★调试模式已集中到 CobblemonTSConfig.debug.debug_charm（全局 debug 组）

    static {
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
        SPEC = BUILDER.build();
    }
}