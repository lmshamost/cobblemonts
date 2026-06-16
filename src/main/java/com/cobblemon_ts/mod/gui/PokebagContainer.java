package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.DebugConfig;
import com.cobblemon_ts.mod.PokebagInventory;
import com.cobblemon_ts.mod.PokebagManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PokebagContainer extends AbstractContainerMenu {
    public static final Component TITLE = Component.translatable("container.coblemon_ts.pokebag");
    private final SimpleContainer bagInventory;
    private final Player player;

    public PokebagContainer(int containerId, Inventory playerInv) {
        super(ModContainers.POKEBAG.get(), containerId);
        this.player = playerInv.player;

        // 末影箱模式：创建临时容器，关闭时保存回玩家数据
        PokebagManager manager = CharmPlayerData.get(player).getPokebagManager();
        int bagSlots = manager.getBagInventory().getContainerSize();
        int cfgMaxStack = CharmConfig.POKEBAG_MAX_STACK_SIZE.get();
        if (DebugConfig.DEBUG_POKEBAG.get()) {
            CobblemonTS.LOGGER.info("[Pokebag] 创建临时容器 bagSlots={} cfgMaxStack={}", bagSlots, cfgMaxStack);
        }

        // 使用 PokebagInventory 确保 addItem/setItem 使用正确的堆叠上限
        this.bagInventory = new PokebagInventory(bagSlots);
        if (DebugConfig.DEBUG_POKEBAG.get()) {
            CobblemonTS.LOGGER.info("[Pokebag] 临时容器 getMaxStackSize={}", this.bagInventory.getMaxStackSize());
        }

        // 复制玩家数据到临时容器
        for (int i = 0; i < bagSlots; i++) {
            ItemStack original = manager.getBagInventory().getItem(i);
            if (!original.isEmpty()) {
                ItemStack copy = original.copy();
                if (DebugConfig.DEBUG_POKEBAG.get()) {
                    CobblemonTS.LOGGER.info("[Pokebag] 复制 slot={} item={} count={} -> copy count={}",
                        i, original.getDescriptionId(), original.getCount(), copy.getCount());
                }
                bagInventory.setItem(i, copy);
                if (DebugConfig.DEBUG_POKEBAG.get()) {
                    ItemStack after = bagInventory.getItem(i);
                    CobblemonTS.LOGGER.info("[Pokebag] setItem后 slot={} count={} (是否被截断: {})",
                        i, after.getCount(), after.getCount() != copy.getCount());
                }
            }
        }

        int rows = bagSlots / 9;

        // 道具袋格子（带标签过滤和大堆叠支持）
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                this.addSlot(new Slot(bagInventory, index, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public int getMaxStackSize() {
                        return CharmConfig.POKEBAG_MAX_STACK_SIZE.get();
                    }

                    @Override
                    public int getMaxStackSize(ItemStack stack) {
                        return CharmConfig.POKEBAG_MAX_STACK_SIZE.get();
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        PokebagManager bagManager = CharmPlayerData.get(PokebagContainer.this.player).getPokebagManager();
                        return bagManager.canItemEnterBag(stack);
                    }
                });
            }
        }

        int invY = 18 + rows * 18 + 13;

        // 玩家背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
            }
        }

        // 玩家快捷栏
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, invY + 58));
        }
    }

    public SimpleContainer getBagInventory() {
        return bagInventory;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int bagSize = bagInventory.getContainerSize();

            if (index < bagSize) {
                // 从道具袋移动到玩家背包（shift+click 取物限制）
                if (CharmConfig.POKEBAG_EXTRACT_LIMIT_ENABLED.get()) {
                    int limit = CharmConfig.POKEBAG_EXTRACT_LIMIT.get();
                    if (stack.getCount() > limit) {
                        ItemStack extractStack = stack.copy();
                        extractStack.setCount(limit);
                        stack.shrink(limit);
                        if (!this.moveItemStackTo(extractStack, bagSize, this.slots.size(), true)) {
                            stack.grow(extractStack.getCount());
                            return ItemStack.EMPTY;
                        }
                        result = extractStack.copy();
                        if (stack.isEmpty()) {
                            slot.set(ItemStack.EMPTY);
                        } else {
                            slot.setChanged();
                        }
                        return result;
                    }
                }
                if (!this.moveItemStackTo(stack, bagSize, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移动到道具袋（只允许符合条件的物品）
                PokebagManager manager = CharmPlayerData.get(player).getPokebagManager();
                if (!manager.canItemEnterBag(stack)) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackToBag(stack, bagSize)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    private boolean moveItemStackToBag(ItemStack stack, int bagSize) {
        int originalCount = stack.getCount();

        if (!stack.isStackable()) {
            // 不可堆叠物品：找空位放入
            for (int i = 0; i < bagSize; i++) {
                Slot slot = this.slots.get(i);
                if (!slot.hasItem() && slot.mayPlace(stack)) {
                    slot.set(stack.copy());
                    stack.setCount(0);
                    return true;
                }
            }
            return false;
        }

        int maxStack = CharmConfig.POKEBAG_MAX_STACK_SIZE.get();

        // 先尝试合并到已有同类物品
        for (int i = 0; i < bagSize; i++) {
            Slot slot = this.slots.get(i);
            ItemStack existing = slot.getItem();
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(stack, existing)) {
                int canAdd = maxStack - existing.getCount();
                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, stack.getCount());
                    existing.grow(toAdd);
                    stack.shrink(toAdd);
                    slot.setChanged();
                    if (stack.isEmpty()) return true;
                }
            }
        }

        // 再尝试放入空格
        for (int i = 0; i < bagSize; i++) {
            Slot slot = this.slots.get(i);
            if (!slot.hasItem() && slot.mayPlace(stack)) {
                int toMove = Math.min(stack.getCount(), maxStack);
                ItemStack copy = stack.copy();
                copy.setCount(toMove);
                slot.set(copy);
                stack.shrink(toMove);
                slot.setChanged();
                if (stack.isEmpty()) return true;
            }
        }

        return stack.getCount() < originalCount;
    }

    @Override
    public boolean stillValid(Player player) {
        return bagInventory.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (DebugConfig.DEBUG_POKEBAG.get()) {
            CobblemonTS.LOGGER.info("[Pokebag] removed() 保存 player={}", player.getName().getString());
        }
        PokebagManager manager = CharmPlayerData.get(player).getPokebagManager();
        if (DebugConfig.DEBUG_POKEBAG.get()) {
            CobblemonTS.LOGGER.info("[Pokebag] 玩家容器 getMaxStackSize={}", manager.getBagInventory().getMaxStackSize());
        }
        for (int i = 0; i < bagInventory.getContainerSize(); i++) {
            ItemStack tempStack = bagInventory.getItem(i);
            if (!tempStack.isEmpty()) {
                if (DebugConfig.DEBUG_POKEBAG.get()) {
                    CobblemonTS.LOGGER.info("[Pokebag] 保存 slot={} item={} count={} -> 玩家容器",
                        i, tempStack.getDescriptionId(), tempStack.getCount());
                }
                manager.getBagInventory().setItem(i, tempStack.copy());
                if (DebugConfig.DEBUG_POKEBAG.get()) {
                    ItemStack saved = manager.getBagInventory().getItem(i);
                    CobblemonTS.LOGGER.info("[Pokebag] 保存后玩家容器 slot={} count={}",
                        i, saved.getCount());
                }
            } else {
                manager.getBagInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        if (DebugConfig.DEBUG_POKEBAG.get()) {
            CobblemonTS.LOGGER.info("[Pokebag] removed() 保存完成");
        }
    }
}