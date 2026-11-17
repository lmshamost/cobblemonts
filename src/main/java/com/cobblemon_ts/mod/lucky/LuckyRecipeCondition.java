package com.cobblemon_ts.mod.lucky;

/*=================================================================
||  线性依赖: LuckyRecipeCondition实现Fabric ResourceCondition接口
||  线性结构: 类型常量定义 → 编解码器 → 条件判定(读取全局配置开关)
||  线性下游: CobblemonTS.onInitialize注册本类型, 5个幸运方块配方JSON引用
||  线性对照: NeoForge版 LuckyRecipeCondition (ICondition) → Fabric版 (ResourceCondition)
===================================================================*/

import com.cobblemon_ts.mod.CobblemonTSConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

public class LuckyRecipeCondition implements ResourceCondition {
    // [线性-L06] 条件ID, 配方JSON中 "condition": "cobblemon_ts:lucky_recipes_enabled"
    public static final Identifier ID = Identifier.of("cobblemon_ts", "lucky_recipes_enabled");

    // 编解码器: 与NeoForge版保持一致, 保留可选enabled字段
    public static final MapCodec<LuckyRecipeCondition> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    com.mojang.serialization.Codec.BOOL.optionalFieldOf("enabled", false).forGetter(c -> c.enabled)
            ).apply(instance, LuckyRecipeCondition::new)
    );

    public static final ResourceConditionType<LuckyRecipeCondition> TYPE =
            ResourceConditionType.create(ID, CODEC);

    private final boolean enabled;

    public LuckyRecipeCondition(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(RegistryWrapper.WrapperLookup registryLookup) {
        return CobblemonTSConfig.LUCKY_RECIPES_ENABLED_TS.get(); // [线性-L06] 检查CobblemonTSConfig的配方开关
    }

    @Override
    public String toString() {
        return "lucky_recipes_enabled(" + CobblemonTSConfig.LUCKY_RECIPES_ENABLED_TS.get() + ")";
    }
}