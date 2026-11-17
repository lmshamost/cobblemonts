package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.CharmEventListener;
import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.PokebagInventory;
import com.cobblemon_ts.mod.PokebagManager;
import net.minecraft.text.Text;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;

public class PokebagContainer extends ScreenHandler {
    public static final Text TITLE = Text.translatable("container.cobblemon_ts.pokebag");
    // [线性-B06] shift+alt 全取的按钮标记：客户端Mixin检测到 Alt 时把 mouseButton 改写为该值，服务端据此识别
    public static final int EXTRACT_ALL_BUTTON = 100;
    private final SimpleInventory bagInventory;
    private final PlayerEntity dataOwner;

    // [线性-B09] 客户端构造：menuType.create 工厂路径，只创建「本地副本视图」，数据由服务端同步包(initializeContents)填充。
    // 与真实容器隔离，客户端本地预测点击只改副本，不与服务端真实执行叠加(否则放入翻倍/左键拿起消失)。
    public PokebagContainer(int containerId, PlayerInventory playerInv) {
        this(containerId, playerInv, playerInv.player, new PokebagInventory(CharmConfig.POKEBAG_SLOTS.get()));
    }

    // [线性-BAG02] 服务端构造(invsee)：打开者(openerInv)与数据源(dataOwner)分离，OP可打开目标玩家道具袋，
    // 引用数据源玩家的真实共享容器，玩家与OP同看一份数据，实时互通，无需复制/写回。
    public PokebagContainer(int containerId, PlayerInventory openerInv, PlayerEntity dataOwner) {
        this(containerId, openerInv, dataOwner, CharmPlayerData.get(dataOwner).getPokebagManager().getBagInventory());
    }

    private PokebagContainer(int containerId, PlayerInventory openerInv, PlayerEntity dataOwner, SimpleInventory bagInventory) {
        super(ModContainers.POKEBAG, containerId);
        this.dataOwner = dataOwner;
        this.bagInventory = bagInventory;

        int bagSlots = bagInventory.size();
        int rows = bagSlots / 9;

        // 道具袋格子（带标签过滤和大堆叠支持）
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                this.addSlot(new Slot(bagInventory, index, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public int getMaxItemCount() {
                        return CharmConfig.POKEBAG_MAX_STACK_SIZE.get();
                    }

                    @Override
                    public int getMaxItemCount(ItemStack stack) {
                        return CharmConfig.POKEBAG_MAX_STACK_SIZE.get();
                    }

                    @Override
                    public boolean canInsert(ItemStack stack) {
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

    public SimpleInventory getBagInventory() {
        return bagInventory;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            int bagSize = bagInventory.size();

            if (index < bagSize) {
                // [线性-B07] 从道具袋移到玩家背包：直接走原版 insertItem，由其按目标槽正常堆叠(64)自动拆分，
                // 单次shift+点击只移动一个堆叠。不再手动 shrink+move 副本，避免与原版 onSlotClick 点击逻辑重复导致数量翻倍/物品消失。
                this.insertItem(stack, bagSize, this.slots.size(), true);
                if (stack.isEmpty()) {
                    slot.setStack(ItemStack.EMPTY);
                } else {
                    slot.markDirty();
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
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
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
                if (!slot.hasStack() && slot.canInsert(stack)) {
                    slot.setStack(stack.copy());
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
            ItemStack existing = slot.getStack();
            if (!existing.isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, existing)) {
                int canAdd = maxStack - existing.getCount();
                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, stack.getCount());
                    existing.increment(toAdd);
                    stack.decrement(toAdd);
                    slot.markDirty();
                    if (stack.isEmpty()) return true;
                }
            }
        }

        // 再尝试放入空格
        for (int i = 0; i < bagSize; i++) {
            Slot slot = this.slots.get(i);
            if (!slot.hasStack() && slot.canInsert(stack)) {
                int toMove = Math.min(stack.getCount(), maxStack);
                ItemStack copy = stack.copy();
                copy.setCount(toMove);
                slot.setStack(copy);
                stack.decrement(toMove);
                slot.markDirty();
                if (stack.isEmpty()) return true;
            }
        }

        return stack.getCount() < originalCount;
    }

    // [线性-B06] shift+alt 全取：客户端Mixin检测到Alt时把mouseButton改写为EXTRACT_ALL_BUTTON，服务端据此识别并执行
    @Override
    public void onSlotClick(int slotId, int button, SlotActionType clickType, PlayerEntity player) {
        if (clickType == SlotActionType.QUICK_MOVE && button == EXTRACT_ALL_BUTTON) {
            this.extractAllFromSlot(player, slotId);
            return;
        }
        super.onSlotClick(slotId, button, clickType, player);
    }

    // [线性-B06] 全取该槽：复制整堆→清槽→用insertItem按玩家背包正常堆叠(64)分批移动，放不下的回填原槽
    private void extractAllFromSlot(PlayerEntity player, int slotId) {
        int bagSize = bagInventory.size();
        if (slotId < 0 || slotId >= bagSize) return;
        Slot slot = this.slots.get(slotId);
        if (slot == null || !slot.hasStack()) return;
        if (!slot.canTakeItems(player)) return;

        ItemStack stack = slot.getStack().copy();
        slot.setStack(ItemStack.EMPTY);

        // insertItem 每次只填一个空槽/合并同类，天然按目标槽64拆分；循环直到移完或背包满
        while (!stack.isEmpty() && this.insertItem(stack, bagSize, this.slots.size(), true)) {
            // 已移动一部分，继续
        }

        if (!stack.isEmpty()) {
            slot.setStack(stack);
        }
        slot.markDirty();
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return bagInventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        // [线性-BAG04] 共享视图：数据直接引用真实容器，无需写回，关闭时仅立即落盘
        CharmPlayerData ownerData = CharmEventListener.getPlayerDataIfPresent(dataOwner);
        if (ownerData != null) {
            ownerData.getPokebagManager().forceFlush();
        }
    }
}