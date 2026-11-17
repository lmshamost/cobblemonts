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

    // [线性-B06] 容器堆叠上限带参覆写：对齐 SimpleInventory 的大堆叠存储，避免 640 被按物品原生 64 截断
    @Override
    public int getMaxCount(ItemStack stack) {
        return CharmConfig.POKEBAG_MAX_STACK_SIZE.get();
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