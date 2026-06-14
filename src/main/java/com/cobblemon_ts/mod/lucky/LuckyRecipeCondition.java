package com.cobblemon_ts.mod.lucky;

import com.cobblemon_ts.mod.CobblemonTSConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;

public class LuckyRecipeCondition implements ICondition {
    public static final MapCodec<LuckyRecipeCondition> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    com.mojang.serialization.Codec.BOOL.optionalFieldOf("enabled", false).forGetter(c -> CobblemonTSConfig.LUCKY_RECIPES_ENABLED_TS.get())
            ).apply(instance, LuckyRecipeCondition::new)
    );

    private final boolean enabled;

    public LuckyRecipeCondition(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean test(IContext context) {
        return CobblemonTSConfig.LUCKY_RECIPES_ENABLED_TS.get(); // [线性-L06] 检查CobblemonTSConfig的配方开关
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "lucky_recipes_enabled(" + CobblemonTSConfig.LUCKY_RECIPES_ENABLED_TS.get() + ")";
    }
}