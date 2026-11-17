package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.CharmEventListener;
import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.PokebagInventory;
import com.cobblemon_ts.mod.PokebagManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PokebagContainer extends AbstractContainerMenu {
    public static final Component TITLE = Component.translatable("container.cobblemon_ts.pokebag");
    // [线性-B06] shift+alt 全取的按钮标记：客户端Mixin检测到 Alt 时把 mouseButton 改写为该值，服务端据此识别
    public static final int EXTRACT_ALL_BUTTON = 100;
    private final SimpleContainer bagInventory;
    private final Player dataOwner;

    // [线性-B09] 客户端构造：menuType.create 工厂路径，只创建「本地副本视图」，数据由服务端同步包(initializeContents)填充。
    // 与真实容器隔离，客户端本地预测点击只改副本，不与服务端真实执行叠加(否则放入翻倍/左键拿起消失)。
    public PokebagContainer(int containerId, Inventory playerInv) {
        this(containerId, playerInv, playerInv.player, new PokebagInventory(CharmConfig.POKEBAG_SLOTS.get()));
    }

    // [线性-BAG02] 服务端构造(invsee)：打开者(openerInv)与数据源(dataOwner)分离，OP可打开目标玩家道具袋，
    // 引用数据源玩家的真实共享容器，玩家与OP同看一份数据，实时互通，无需复制/写回。
    public PokebagContainer(int containerId, Inventory openerInv, Player dataOwner) {
        this(containerId, openerInv, dataOwner, CharmPlayerData.get(dataOwner).getPokebagManager().getBagInventory());
    }

    private PokebagContainer(int containerId, Inventory openerInv, Player dataOwner, SimpleContainer bagInventory) {
        super(ModContainers.POKEBAG.get(), containerId);
        this.dataOwner = dataOwner;
        this.bagInventory = bagInventory;

        int bagSlots = bagInventory.getContainerSize();
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
                        PokebagManager bagManager = CharmPlayerData.get(PokebagContainer.this.dataOwner).getPokebagManager();
                        return bagManager.canItemEnterBag(stack);
                    }
                });
            }
        }

        int invY = 18 + rows * 18 + 13;

        // 玩家背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(openerInv, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
            }
        }

        // 玩家快捷栏
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(openerInv, col, 8 + col * 18, invY + 58));
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
                // [线性-B07] 从道具袋移到玩家背包：直接走原版 moveItemStackTo，由其按目标槽正常堆叠(64)自动拆分，
                // 单次shift+点击只移动一个堆叠。不再手动 shrink+move 副本，避免与原版 doClick 点击逻辑重复导致数量翻倍/物品消失。
                this.moveItemStackTo(stack, bagSize, this.slots.size(), true);
                if (stack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
                return ItemStack.EMPTY;
            } else {
                // 从玩家背包移动到道具袋（按数据源玩家的准入规则判断）
                PokebagManager manager = CharmPlayerData.get(dataOwner).getPokebagManager();
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

    // [线性-B06] shift+alt 全取：客户端Mixin检测到Alt时把mouseButton改写为EXTRACT_ALL_BUTTON，服务端据此识别并执行
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.QUICK_MOVE && button == EXTRACT_ALL_BUTTON) {
            this.extractAllFromSlot(player, slotId);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    // [线性-B06] 全取该槽：复制整堆→清槽→用moveItemStackTo按玩家背包正常堆叠(64)分批移动，放不下的回填原槽
    private void extractAllFromSlot(Player player, int slotId) {
        int bagSize = bagInventory.getContainerSize();
        if (slotId < 0 || slotId >= bagSize) return;
        Slot slot = this.slots.get(slotId);
        if (slot == null || !slot.hasItem()) return;
        if (!slot.mayPickup(player)) return;

        ItemStack stack = slot.getItem().copy();
        slot.set(ItemStack.EMPTY);

        // moveItemStackTo 每次只填一个空槽/合并同类，天然按目标槽64拆分；循环直到移完或背包满
        while (!stack.isEmpty() && this.moveItemStackTo(stack, bagSize, this.slots.size(), true)) {
            // 已移动一部分，继续
        }

        if (!stack.isEmpty()) {
            slot.set(stack);
        }
        slot.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return bagInventory.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (false) {
            CobblemonTS.LOGGER.info("[Pokebag] removed() 保存 player={}", player.getName().getString());
        }
        // [线性-BAG04] 共享视图：数据直接引用真实容器，无需写回，关闭时仅立即落盘
        CharmPlayerData ownerData = CharmEventListener.getPlayerDataIfPresent(dataOwner);
        if (ownerData != null) {
            ownerData.getPokebagManager().forceFlush();
        }
    }
}