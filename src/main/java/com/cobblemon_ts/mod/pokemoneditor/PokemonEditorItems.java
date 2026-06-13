package com.cobblemon_ts.mod.pokemoneditor;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class PokemonEditorItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CobblemonTS.MODID);

    public static final DeferredItem<PokemonEditorItem> POKEMON_EDITOR = ITEMS.register("pokemon_editor",
            () -> new PokemonEditorItem());
}