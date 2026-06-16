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

    @Override
    public void setItem(int slot, ItemStack stack) {
        int countBefore = stack.getCount();
        super.setItem(slot, stack);
        int maxStack = CharmConfig.POKEBAG_MAX_STACK_SIZE.get();
        if (stack.getCount() < countBefore && stack.getCount() < maxStack) {
            stack.setCount(Math.min(countBefore, maxStack));
        }
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