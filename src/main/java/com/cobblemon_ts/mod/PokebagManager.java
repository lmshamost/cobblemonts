package com.cobblemon_ts.mod;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PokebagManager {
    private boolean autoCollect;
    private SimpleContainer bagInventory;
    private int bagSize;
    // [线性-B02] 自动保存回调：数据一改即时写persistentData
    private Runnable onDirty = () -> {};

    // [线性-B03] 性能优化：拾取物频繁触发NBT序列化 → 改为脏标记 + 限流保存
    private boolean dirty = false;
    private long lastFlushTime = 0;
    private static final long FLUSH_COOLDOWN_MS = 1000; // 1秒最多保存一次

    public void setOnDirty(Runnable callback) {
        this.onDirty = callback != null ? callback : () -> {};
    }

    public PokebagManager() {
        this.autoCollect = true;
        this.bagSize = CharmConfig.POKEBAG_SLOTS.get();
        this.bagInventory = createBagContainer();
    }

    private SimpleContainer createBagContainer() {
        int maxStack = CharmConfig.POKEBAG_MAX_STACK_SIZE.get();
        if (DebugConfig.DEBUG_POKEBAG.get()) {
            CobblemonTS.LOGGER.info("[PokebagMgr] createBagContainer() bagSize={} maxStack={}", bagSize, maxStack);
        }
        return new PokebagInventory(bagSize);
    }

    public boolean isAutoCollect() {
        return autoCollect;
    }

    public void setAutoCollect(boolean autoCollect) {
        this.autoCollect = autoCollect;
        onDirty.run();
    }

    public void toggleAutoCollect() {
        this.autoCollect = !this.autoCollect;
        onDirty.run();
    }

    public SimpleContainer getBagInventory() {
        return bagInventory;
    }

    public ItemStack addItemToBag(ItemStack stack) {
        if (stack.isEmpty()) return stack;
        if (!canItemEnterBag(stack)) return stack;
        if (DebugConfig.DEBUG_POKEBAG.get()) {
            CobblemonTS.LOGGER.info("[PokebagMgr] addItemToBag() item={} count={} 容器maxStack={}",
                stack.getDescriptionId(), stack.getCount(), bagInventory.getMaxStackSize());
        }
        ItemStack result = bagInventory.addItem(stack);
        if (DebugConfig.DEBUG_POKEBAG.get()) {
            CobblemonTS.LOGGER.info("[PokebagMgr] addItemToBag() 剩余={} (0=全部放入)", result.getCount());
        }
        // [线性-B03] 物品成功放入 → 标记脏数据（由tick限流保存，不再每次拾取都NBT序列化）
        if (result.getCount() != stack.getCount()) {
            dirty = true;
        }
        return result;
    }

    public boolean canItemEnterBag(ItemStack stack) {
        if (stack.isEmpty()) return false;

        List<? extends String> blacklist = CharmConfig.POKEBAG_BLACKLIST_TAGS.get();
        List<? extends String> whitelist = CharmConfig.POKEBAG_WHITELIST_TAGS.get();

        boolean hasWhitelist = !whitelist.isEmpty();
        boolean hasBlacklist = !blacklist.isEmpty();

        if (!hasWhitelist && !hasBlacklist) {
            return true;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        // 黑名单检查（优先级最高）
        if (hasBlacklist) {
            for (String entry : blacklist) {
                if (matchesEntry(stack, itemId, entry)) {
                    return false; // 黑名单命中 → 直接拒绝
                }
            }
        }

        // 白名单检查
        if (hasWhitelist) {
            for (String entry : whitelist) {
                if (matchesEntry(stack, itemId, entry)) {
                    return true; // 白名单命中 → 允许
                }
            }
            return false; // 有白名单但没命中 → 拒绝
        }

        return true; // 只有黑名单且没命中 → 允许
    }

    private static boolean matchesEntry(ItemStack stack, String itemId, String entry) {
        if (entry.startsWith("#")) {
            // Tag 匹配: #cobblemon:pokeballs
            String tagId = entry.substring(1);
            for (TagKey<Item> tag : stack.getTags().toList()) {
                if (tag.location().toString().equals(tagId)) {
                    return true;
                }
            }
            return false;
        } else {
            // 直接物品ID匹配: cobblemon:poke_ball
            return itemId.equals(entry);
        }
    }

    // [线性-B03] 限流保存：每1秒最多落盘一次，避免高频拾取卡服
    public void tryFlush() {
        if (!dirty) return;
        long now = System.currentTimeMillis();
        if (now - lastFlushTime < FLUSH_COOLDOWN_MS) return;
        dirty = false;
        lastFlushTime = now;
        onDirty.run();
    }

    // [线性-B03] 强制立即保存（指令/手动清空等场景）
    public void forceFlush() {
        dirty = true;
        lastFlushTime = 0;
        tryFlush();
    }

    private static final int MAX_SAVE_COUNT = 64;

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("autoCollect", autoCollect);

        ListTag items = new ListTag();
        for (int i = 0; i < bagInventory.getContainerSize(); i++) {
            ItemStack stack = bagInventory.getItem(i);
            if (!stack.isEmpty()) {
                int totalCount = stack.getCount();
                if (DebugConfig.DEBUG_POKEBAG.get()) {
                    CobblemonTS.LOGGER.info("[PokebagMgr] serializeNBT() slot={} item={} totalCount={}",
                        i, stack.getDescriptionId(), totalCount);
                }
                int remaining = totalCount;
                while (remaining > 0) {
                    int subCount = Math.min(remaining, MAX_SAVE_COUNT);
                    ItemStack subStack = stack.copy();
                    subStack.setCount(subCount);
                    CompoundTag itemTag = new CompoundTag();
                    itemTag.putInt("Slot", i);
                    itemTag.put("Item", subStack.save(provider));
                    items.add(itemTag);
                    remaining -= subCount;
                }
                if (DebugConfig.DEBUG_POKEBAG.get()) {
                    CobblemonTS.LOGGER.info("[PokebagMgr] serializeNBT() slot={} 拆分条目数={}",
                        i, (totalCount + MAX_SAVE_COUNT - 1) / MAX_SAVE_COUNT);
                }
            }
        }
        tag.put("Items", items);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains("autoCollect")) {
            this.autoCollect = tag.getBoolean("autoCollect");
        }

        this.bagSize = CharmConfig.POKEBAG_SLOTS.get();
        this.bagInventory = createBagContainer();

        if (tag.contains("Items")) {
            ListTag items = tag.getList("Items", 10);
            if (DebugConfig.DEBUG_POKEBAG.get()) {
                CobblemonTS.LOGGER.info("[PokebagMgr] deserializeNBT() 物品条目数={} 容器maxStack={}",
                    items.size(), bagInventory.getMaxStackSize());
            }
            for (int i = 0; i < items.size(); i++) {
                CompoundTag itemTag = items.getCompound(i);
                int slot = itemTag.getInt("Slot");
                if (slot >= 0 && slot < bagSize) {
                    ItemStack stack = ItemStack.parse(provider, itemTag.getCompound("Item")).orElse(ItemStack.EMPTY);
                    if (DebugConfig.DEBUG_POKEBAG.get()) {
                        CobblemonTS.LOGGER.info("[PokebagMgr] deserializeNBT() slot={} item={} count={}",
                            slot, stack.getDescriptionId(), stack.getCount());
                    }
                    if (!stack.isEmpty()) {
                        ItemStack existing = bagInventory.getItem(slot);
                        if (existing.isEmpty()) {
                            bagInventory.setItem(slot, stack);
                        } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                            existing.grow(stack.getCount());
                            if (DebugConfig.DEBUG_POKEBAG.get()) {
                                CobblemonTS.LOGGER.info("[PokebagMgr] deserializeNBT() slot={} 合并后count={}",
                                    slot, existing.getCount());
                            }
                        }
                    }
                }
            }
        }
    }
}