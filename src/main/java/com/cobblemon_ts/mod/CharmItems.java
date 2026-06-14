package com.cobblemon_ts.mod;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CharmItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CobblemonTS.MODID);

    public static final DeferredItem<CharmItem> OVAL_CHARM = ITEMS.register("oval_charm", 
            () -> new CharmItem(new Item.Properties().stacksTo(1), CharmType.OVAL));
    
    public static final DeferredItem<CharmItem> SHINY_CHARM = ITEMS.register("shiny_charm", 
            () -> new CharmItem(new Item.Properties().stacksTo(1), CharmType.SHINY));
    
    public static final DeferredItem<CharmItem> EXP_CHARM = ITEMS.register("exp_charm", 
            () -> new CharmItem(new Item.Properties().stacksTo(1), CharmType.EXP));
    
    public static final DeferredItem<CharmItem> CATCHING_CHARM = ITEMS.register("catching_charm", 
            () -> new CharmItem(new Item.Properties().stacksTo(1), CharmType.CATCHING));
    
    public static final DeferredItem<CharmItem> EXP_ALL = ITEMS.register("exp_all", 
            () -> new CharmItem(new Item.Properties().stacksTo(1), CharmType.EXP_ALL));

    public static final DeferredItem<Item> GOLD_CROWN = ITEMS.register("gold_crown",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<TestBuildItem> TEST_BUILD = ITEMS.register("test_build",
            () -> new TestBuildItem(new Item.Properties().stacksTo(1)));
}