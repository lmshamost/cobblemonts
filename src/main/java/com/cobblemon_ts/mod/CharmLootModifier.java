package com.cobblemon_ts.mod;

import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.registry.RegistryKey;

import java.util.List;

public class CharmLootModifier {

    private static final List<ItemStack> CHARM_POOL = List.of(
            new ItemStack(CharmItems.SHINY_CHARM.get()),
            new ItemStack(CharmItems.OVAL_CHARM.get()),
            new ItemStack(CharmItems.EXP_CHARM.get()),
            new ItemStack(CharmItems.CATCHING_CHARM.get()),
            new ItemStack(CharmItems.EXP_ALL.get()),
            new ItemStack(CharmItems.GOLD_CROWN.get())
    );

    public static void onLootTableModify(RegistryKey<LootTable> key, LootTable.Builder tableBuilder,
                                          net.fabricmc.fabric.api.loot.v3.LootTableSource source,
                                          net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        if (!CobblemonTSConfig.CHARM_LOOT_ENABLED.get()) return;
        if (source != net.fabricmc.fabric.api.loot.v3.LootTableSource.VANILLA) return;

        if (key.equals(LootTables.BASTION_TREASURE_CHEST) || key.equals(LootTables.END_CITY_TREASURE_CHEST)) {
            LootPool.Builder poolBuilder = LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1))
                    .conditionally(RandomChanceLootCondition.builder(0.02f));
            for (ItemStack stack : CHARM_POOL) {
                poolBuilder.with(ItemEntry.builder(stack.getItem()).weight(1));
            }
            tableBuilder.pool(poolBuilder);
        }
    }
}