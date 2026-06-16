package com.cobblemon_ts.mod.lucky;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.lucky.block.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class LuckyBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CobblemonTS.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CobblemonTS.MODID);

    public static final DeferredBlock<Block> LUCKY_BLOCK = registerBlock("lucky_block",
            () -> new RandomPokemonBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // 石头硬度，镐子加速
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> LEGENDARY_LUCKY_BLOCK = registerBlock("legendary_lucky_block",
            () -> new LegendaryPokemonBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // 泥土硬度，手挖也快
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 7)));

    public static final DeferredBlock<Block> ULTRA_BEAST_LUCKY_BLOCK = registerBlock("ultra_beast_lucky_block",
            () -> new UltraBeastBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // 泥土硬度，手挖也快
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 6)));

    public static final DeferredBlock<Block> SHINY_LUCKY_BLOCK = registerBlock("shiny_lucky_block",
            () -> new ShinyPokemonBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // 泥土硬度，手挖也快
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 10)));

    public static final DeferredBlock<Block> ITEM_LUCKY_BLOCK = registerBlock("item_lucky_block",
            () -> new ItemLuckyBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // 泥土硬度，手挖也快
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> blockSupplier) {
        DeferredBlock<T> block = BLOCKS.register(name, blockSupplier);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}