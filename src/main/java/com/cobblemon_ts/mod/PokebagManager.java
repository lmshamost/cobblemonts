package com.cobblemon_ts.mod;

import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.Registries;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PokebagManager {
    private boolean autoCollect;
    private SimpleInventory bagInventory;
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

    private SimpleInventory createBagContainer() {
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

    public SimpleInventory getBagInventory() {
        return bagInventory;
    }

    public ItemStack addItemToBag(ItemStack stack) {
        if (stack.isEmpty()) return stack;
        if (!canItemEnterBag(stack)) return stack;
        if (DebugConfig.DEBUG_POKEBAG.get()) {
            CobblemonTS.LOGGER.info("[PokebagMgr] addItemToBag() item={} count={} 容器maxStack={}",
                stack.getItem().getTranslationKey(), stack.getCount(), bagInventory.getMaxCountPerStack());
        }
        ItemStack result = bagInventory.addStack(stack);
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

        String itemId = Registries.ITEM.getId(stack.getItem()).toString();

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
            // NbtElement 匹配: #cobblemon:pokeballs
            String tagId = entry.substring(1);
            for (TagKey<Item> NbtElement : stack.getItem().getRegistryEntry().streamTags().toList()) {
                if (NbtElement.id().toString().equals(tagId)) {
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

    public NbtCompound serializeNBT(RegistryWrapper.WrapperLookup provider) {
        NbtCompound NbtElement = new NbtCompound();
        NbtElement.putBoolean("autoCollect", autoCollect);

        // [线性-B05] 独立 int Count 存储：ItemStack 只存 1，真实数量放 Count 字段，避免 64 拆分冗余
        NbtList items = new NbtList();
        for (int i = 0; i < bagInventory.size(); i++) {
            ItemStack stack = bagInventory.getStack(i);
            if (!stack.isEmpty()) {
                NbtCompound itemTag = new NbtCompound();
                itemTag.putInt("Slot", i);
                itemTag.putInt("Count", stack.getCount());
                ItemStack saveStack = stack.copy();
                saveStack.setCount(1);
                itemTag.put("Item", saveStack.encode(provider));
                items.add(itemTag);
            }
        }
        NbtElement.put("Items", items);
        return NbtElement;
    }

    public void deserializeNBT(NbtCompound NbtElement, RegistryWrapper.WrapperLookup provider) {
        if (NbtElement.contains("autoCollect")) {
            this.autoCollect = NbtElement.getBoolean("autoCollect");
        }

        this.bagSize = CharmConfig.POKEBAG_SLOTS.get();
        this.bagInventory = createBagContainer();
        int maxStack = CharmConfig.POKEBAG_MAX_STACK_SIZE.get();

        if (NbtElement.contains("Items")) {
            NbtList items = NbtElement.getList("Items", 10);
            for (int i = 0; i < items.size(); i++) {
                NbtCompound itemTag = items.getCompound(i);
                int slot = itemTag.getInt("Slot");
                if (slot >= 0 && slot < bagSize) {
                    ItemStack stack = ItemStack.fromNbt(provider, itemTag.getCompound("Item")).orElse(ItemStack.EMPTY);
                    if (!stack.isEmpty()) {
                        if (itemTag.contains("Count")) {
                            // [线性-B05] 新格式：独立 count 直接覆盖，并做上限保护
                            int count = Math.min(Math.max(itemTag.getInt("Count"), 1), maxStack);
                            stack.setCount(count);
                            bagInventory.setStack(slot, stack);
                        } else {
                            // [线性-B05] 旧格式兼容：64 拆分条目合并回同一槽，下次保存自动转新格式
                            ItemStack existing = bagInventory.getStack(slot);
                            if (existing.isEmpty()) {
                                bagInventory.setStack(slot, stack);
                            } else if (ItemStack.areItemsAndComponentsEqual(existing, stack)) {
                                existing.increment(stack.getCount());
                                existing.setCount(Math.min(existing.getCount(), maxStack));
                            }
                        }
                    }
                }
            }
        }
    }
}