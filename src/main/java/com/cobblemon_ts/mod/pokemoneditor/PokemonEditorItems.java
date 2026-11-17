package com.cobblemon_ts.mod.pokemoneditor;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.RegistryHelper;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class PokemonEditorItems {
    public static final RegistryHelper.DeferredItem<PokemonEditorItem> POKEMON_EDITOR;

    static {
        PokemonEditorItem item = new PokemonEditorItem();
        Registry.register(Registries.ITEM, Identifier.of(CobblemonTS.MODID, "pokemon_editor"), item);
        POKEMON_EDITOR = new RegistryHelper.DeferredItem<>(item);
    }

    public static void init() {}
}