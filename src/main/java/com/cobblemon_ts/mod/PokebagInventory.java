package com.cobblemon_ts.mod;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class PokebagInventory extends SimpleContainer {

    public PokebagInventory(int size) {
        super(size);
    }

    @Override
    public int getMaxStackSize() {
        return CharmConfig.POKEBAG_MAX_STACK_SIZE.get();
    }

    // [线性-B06] 容器堆叠上限带参覆写：SOP同款，让SimpleContainer.setItem的limitSize不再按物品原生64截断，否则大堆叠(如640)存入即被砍成64
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return CharmConfig.POKEBAG_MAX_STACK_SIZE.get();
    }

    @Override
    public ItemStack addItem(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int maxStack = CharmConfig.POKEBAG_MAX_STACK_SIZE.get();
        ItemStack remaining = stack.copy();

        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack existing = this.getItem(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)) {
                int canAdd = maxStack - existing.getCount();
                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, remaining.getCount());
                    existing.grow(toAdd);
                    remaining.shrink(toAdd);
                    if (remaining.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }

        for (int i = 0; i < this.getContainerSize(); i++) {
            if (this.getItem(i).isEmpty()) {
                int toMove = Math.min(remaining.getCount(), maxStack);
                ItemStack copy = remaining.copy();
                copy.setCount(toMove);
                this.setItem(i, copy);
                remaining.shrink(toMove);
                if (remaining.isEmpty()) return ItemStack.EMPTY;
            }
        }

        return remaining;
    }
}