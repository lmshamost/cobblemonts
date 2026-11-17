package com.cobblemon_ts.mod;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;

public class PokebagInventory extends SimpleInventory {

    public PokebagInventory(int size) {
        super(size);
    }

    @Override
    public int getMaxCountPerStack() {
        return CharmConfig.POKEBAG_MAX_STACK_SIZE.get();
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        int countBefore = stack.getCount();
        super.setStack(slot, stack);
        int maxStack = CharmConfig.POKEBAG_MAX_STACK_SIZE.get();
        if (stack.getCount() < countBefore && stack.getCount() < maxStack) {
            stack.setCount(Math.min(countBefore, maxStack));
        }
    }

    @Override
    public ItemStack addStack(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int maxStack = CharmConfig.POKEBAG_MAX_STACK_SIZE.get();
        ItemStack remaining = stack.copy();

        for (int i = 0; i < this.size(); i++) {
            ItemStack existing = this.getStack(i);
            if (!existing.isEmpty() && ItemStack.areItemsAndComponentsEqual(existing, remaining)) {
                int canAdd = maxStack - existing.getCount();
                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, remaining.getCount());
                    existing.increment(toAdd);
                    remaining.decrement(toAdd);
                    if (remaining.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }

        for (int i = 0; i < this.size(); i++) {
            if (this.getStack(i).isEmpty()) {
                int toMove = Math.min(remaining.getCount(), maxStack);
                ItemStack copy = remaining.copy();
                copy.setCount(toMove);
                this.setStack(i, copy);
                remaining.decrement(toMove);
                if (remaining.isEmpty()) return ItemStack.EMPTY;
            }
        }

        return remaining;
    }
}