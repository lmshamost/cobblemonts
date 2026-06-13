package com.cobblemon_ts.mod.lucky.block;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class ItemLuckyBlock extends BaseLuckyBlock {
    private static final TagKey<Item> LUCKY_DROPS_TAG = ItemTags.create(ResourceLocation.fromNamespaceAndPath(CobblemonTS.MODID, "lucky_drops"));
    private static final Random RANDOM = new Random();
    private static volatile List<Item> luckyDropCache = null;

    public ItemLuckyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void triggerEffect(ServerLevel level, BlockPos pos, Player player) {
        List<Item> items = getLuckyDropItems(level);
        if (items.isEmpty()) {
            CobblemonTS.LOGGER.warn("[LuckyBlock] lucky_drops标签为空, 掉落钻石作为保底");
            ItemStack fallback = new ItemStack(Items.DIAMOND, 1 + RANDOM.nextInt(3));
            dropItem(level, pos, fallback);
            return;
        }

        int itemCount = 1 + RANDOM.nextInt(5);
        for (int i = 0; i < itemCount; i++) {
            Item item = items.get(RANDOM.nextInt(items.size()));
            ItemStack stack = new ItemStack(item, 1 + RANDOM.nextInt(3));
            if (stack.getMaxStackSize() < stack.getCount()) {
                stack.setCount(stack.getMaxStackSize());
            }
            dropItem(level, pos, stack);
        }
        CobblemonTS.LOGGER.info("[LuckyBlock] 掉落{}个随机物品 at {}", itemCount, pos);
    }

    private void dropItem(ServerLevel level, BlockPos pos, ItemStack stack) {
        double offsetX = RANDOM.nextDouble() * 0.5 + 0.25;
        double offsetZ = RANDOM.nextDouble() * 0.5 + 0.25;
        ItemEntity entity = new ItemEntity(level,
                pos.getX() + offsetX, pos.getY() + 1.0, pos.getZ() + offsetZ, stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    private List<Item> getLuckyDropItems(ServerLevel level) {
        if (luckyDropCache != null) {
            return luckyDropCache;
        }

        List<Item> items = new ArrayList<>();
        Optional<HolderSet.Named<Item>> tagHolders = BuiltInRegistries.ITEM.getTag(LUCKY_DROPS_TAG);

        if (tagHolders.isPresent()) {
            for (Holder<Item> holder : tagHolders.get()) {
                items.add(holder.value());
            }
        }

        luckyDropCache = items;
        return items;
    }
}