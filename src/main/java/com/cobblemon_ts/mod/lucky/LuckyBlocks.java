package com.cobblemon_ts.mod.lucky;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.RegistryHelper;
import com.cobblemon_ts.mod.block.ChunkBarrierBlock;
import com.cobblemon_ts.mod.lucky.block.*;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.sound.BlockSoundGroup;

public class LuckyBlocks {
    private static final Block LUCKY_BLOCK_BLOCK = new RandomPokemonBlock(AbstractBlock.Settings.create()
            .strength(0.5F, 6.0F)
            .sounds(BlockSoundGroup.STONE)
            .requiresTool());
    public static final RegistryHelper.DeferredBlock<Block> LUCKY_BLOCK = new RegistryHelper.DeferredBlock<>(LUCKY_BLOCK_BLOCK);
    private static final BlockItem LUCKY_BLOCK_ITEM = new BlockItem(LUCKY_BLOCK_BLOCK, new Item.Settings());

    private static final Block LEGENDARY_LUCKY_BLOCK_BLOCK = new LegendaryPokemonBlock(AbstractBlock.Settings.create()
            .strength(0.5F, 6.0F)
            .sounds(BlockSoundGroup.STONE)
            .requiresTool()
            .luminance(state -> 7));
    public static final RegistryHelper.DeferredBlock<Block> LEGENDARY_LUCKY_BLOCK = new RegistryHelper.DeferredBlock<>(LEGENDARY_LUCKY_BLOCK_BLOCK);
    private static final BlockItem LEGENDARY_LUCKY_BLOCK_ITEM = new BlockItem(LEGENDARY_LUCKY_BLOCK_BLOCK, new Item.Settings());

    private static final Block ULTRA_BEAST_LUCKY_BLOCK_BLOCK = new UltraBeastBlock(AbstractBlock.Settings.create()
            .strength(0.5F, 6.0F)
            .sounds(BlockSoundGroup.STONE)
            .requiresTool()
            .luminance(state -> 6));
    public static final RegistryHelper.DeferredBlock<Block> ULTRA_BEAST_LUCKY_BLOCK = new RegistryHelper.DeferredBlock<>(ULTRA_BEAST_LUCKY_BLOCK_BLOCK);
    private static final BlockItem ULTRA_BEAST_LUCKY_BLOCK_ITEM = new BlockItem(ULTRA_BEAST_LUCKY_BLOCK_BLOCK, new Item.Settings());

    private static final Block SHINY_LUCKY_BLOCK_BLOCK = new ShinyPokemonBlock(AbstractBlock.Settings.create()
            .strength(0.5F, 6.0F)
            .sounds(BlockSoundGroup.STONE)
            .requiresTool()
            .luminance(state -> 10));
    public static final RegistryHelper.DeferredBlock<Block> SHINY_LUCKY_BLOCK = new RegistryHelper.DeferredBlock<>(SHINY_LUCKY_BLOCK_BLOCK);
    private static final BlockItem SHINY_LUCKY_BLOCK_ITEM = new BlockItem(SHINY_LUCKY_BLOCK_BLOCK, new Item.Settings());

    private static final Block ITEM_LUCKY_BLOCK_BLOCK = new ItemLuckyBlock(AbstractBlock.Settings.create()
            .strength(0.5F, 6.0F)
            .sounds(BlockSoundGroup.STONE)
            .requiresTool());
    public static final RegistryHelper.DeferredBlock<Block> ITEM_LUCKY_BLOCK = new RegistryHelper.DeferredBlock<>(ITEM_LUCKY_BLOCK_BLOCK);
    private static final BlockItem ITEM_LUCKY_BLOCK_ITEM = new BlockItem(ITEM_LUCKY_BLOCK_BLOCK, new Item.Settings());

    // [线性-R10] 区块禁止器方块
    private static final Block CHUNK_BARRIER_BLOCK = new ChunkBarrierBlock(AbstractBlock.Settings.create()
            .strength(2.0F, 3.0F)
            .sounds(BlockSoundGroup.WOOD)
            .nonOpaque());
    public static final RegistryHelper.DeferredBlock<Block> CHUNK_BARRIER = new RegistryHelper.DeferredBlock<>(CHUNK_BARRIER_BLOCK);
    private static final BlockItem CHUNK_BARRIER_ITEM = new BlockItem(CHUNK_BARRIER_BLOCK, new Item.Settings());

    static {
        reg("lucky_block", LUCKY_BLOCK_BLOCK, LUCKY_BLOCK_ITEM);
        reg("legendary_lucky_block", LEGENDARY_LUCKY_BLOCK_BLOCK, LEGENDARY_LUCKY_BLOCK_ITEM);
        reg("ultra_beast_lucky_block", ULTRA_BEAST_LUCKY_BLOCK_BLOCK, ULTRA_BEAST_LUCKY_BLOCK_ITEM);
        reg("shiny_lucky_block", SHINY_LUCKY_BLOCK_BLOCK, SHINY_LUCKY_BLOCK_ITEM);
        reg("item_lucky_block", ITEM_LUCKY_BLOCK_BLOCK, ITEM_LUCKY_BLOCK_ITEM);
        reg("chunk_barrier", CHUNK_BARRIER_BLOCK, CHUNK_BARRIER_ITEM);
    }

    private static void reg(String name, Block block, BlockItem item) {
        Registry.register(Registries.BLOCK, Identifier.of(CobblemonTS.MODID, name), block);
        Registry.register(Registries.ITEM, Identifier.of(CobblemonTS.MODID, name), item);
    }

    public static void init() {}
}