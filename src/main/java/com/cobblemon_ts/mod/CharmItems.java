package com.cobblemon_ts.mod;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class CharmItems {
    public static final RegistryHelper.DeferredItem<CharmItem> OVAL_CHARM;
    public static final RegistryHelper.DeferredItem<CharmItem> SHINY_CHARM;
    public static final RegistryHelper.DeferredItem<CharmItem> EXP_CHARM;
    public static final RegistryHelper.DeferredItem<CharmItem> CATCHING_CHARM;
    public static final RegistryHelper.DeferredItem<CharmItem> EXP_ALL;
    public static final RegistryHelper.DeferredItem<Item> GOLD_CROWN;
    public static final RegistryHelper.DeferredItem<TestBuildItem> TEST_BUILD;
    public static final RegistryHelper.DeferredItem<PokebagItem> POKE_BAG;

    // [线性-REG] 注册所有物品到Fabric Registry
    private static <T extends Item> RegistryHelper.DeferredItem<T> reg(String id, T item) {
        Registry.register(Registries.ITEM, Identifier.of(CobblemonTS.MODID, id), item);
        return new RegistryHelper.DeferredItem<>(item);
    }

    static {
        OVAL_CHARM     = reg("oval_charm",       new CharmItem(new Item.Settings().maxCount(1), CharmType.OVAL));
        SHINY_CHARM    = reg("shiny_charm",      new CharmItem(new Item.Settings().maxCount(1), CharmType.SHINY));
        EXP_CHARM      = reg("exp_charm",        new CharmItem(new Item.Settings().maxCount(1), CharmType.EXP));
        CATCHING_CHARM = reg("catching_charm",   new CharmItem(new Item.Settings().maxCount(1), CharmType.CATCHING));
        EXP_ALL        = reg("exp_all",          new CharmItem(new Item.Settings().maxCount(1), CharmType.EXP_ALL));
        GOLD_CROWN     = reg("gold_crown",       new GoldCrownItem(new Item.Settings().maxCount(64)));
        TEST_BUILD     = reg("test_build",       new TestBuildItem(new Item.Settings().maxCount(1)));
        POKE_BAG       = reg("pokebag",          new PokebagItem(new Item.Settings().maxCount(1)));
    }

    public static void init() {} // 触发static块
}