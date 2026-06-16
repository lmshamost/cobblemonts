package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModContainers {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, CobblemonTS.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<PartyHeldItemContainer>> PARTY_HELD_ITEM =
            MENU_TYPES.register("party_held_item",
                    () -> IMenuTypeExtension.create(
                            (id, inv, buf) -> new PartyHeldItemContainer(id, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<PokebagContainer>> POKEBAG =
            MENU_TYPES.register("pokebag",
                    () -> IMenuTypeExtension.create(
                            (id, inv, buf) -> new PokebagContainer(id, inv)));
}