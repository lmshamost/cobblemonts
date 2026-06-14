package com.cobblemon_ts.mod;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;

// [线性-L06a] 护符战利品修饰器：猪灵堡垒/末地城箱子低权重随机掉落护符
public class CharmLootModifier extends LootModifier {

    public static final MapCodec<CharmLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            LootModifier.codecStart(instance).apply(instance, CharmLootModifier::new));

    private static final List<ItemStack> CHARM_POOL = List.of(
            new ItemStack(CharmItems.SHINY_CHARM.get()),
            new ItemStack(CharmItems.OVAL_CHARM.get()),
            new ItemStack(CharmItems.EXP_CHARM.get()),
            new ItemStack(CharmItems.CATCHING_CHARM.get()),
            new ItemStack(CharmItems.EXP_ALL.get()),
            new ItemStack(CharmItems.GOLD_CROWN.get())
    );

    public CharmLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // [线性-L06a-1] 检查配置开关
        if (!CobblemonTSConfig.CHARM_LOOT_ENABLED.get()) {
            return generatedLoot;
        }

        RandomSource random = context.getRandom();
        // [线性-L06a-2] 低权重：2%概率掉落随机护符（护符池5选1）
        if (random.nextInt(100) < 2) {
            int idx = random.nextInt(CHARM_POOL.size());
            generatedLoot.add(CHARM_POOL.get(idx).copy());
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}