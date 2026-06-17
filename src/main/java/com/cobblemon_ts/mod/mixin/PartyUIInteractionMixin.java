package com.cobblemon_ts.mod.mixin;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.ui.PartyUIState;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 队伍面板交互 Mixin
 * Mixin 到 InventoryScreen 和 EffectRenderingInventoryScreen (NeoForge 添加的效果图标的子类)
 * 因为游戏在有/无药水效果时创建不同的实例
 */
@Mixin({net.minecraft.client.gui.screens.inventory.InventoryScreen.class,
         net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen.class,
         net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class})
public abstract class PartyUIInteractionMixin extends AbstractContainerScreen {

    private PartyUIInteractionMixin() {
        super(null, null, null);
    }

    // ==================== mouseClicked ====================
    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void partyui_mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().player == null) return;

        Object screen = this;
        if (!PartyUIState.isValidScreen(screen)) return;

        PartyUIState.State state = PartyUIState.getState(screen);
        if (state == null) return;

        int leftPos = this.leftPos;
        int topPos = this.topPos;

        // ===== 先检测两个开关按钮点击（使用固定偏移，确保与 render 坐标完全一致） =====
        int partyPanelX = PartyUIState.getPartyPanelX(leftPos);
        int TOGGLE_SIZE = 12;
        int TOGGLE_Y = topPos + 2;

        // 精灵栏开关：精灵栏右侧
        int partyToggleX = partyPanelX + PartyUIState.PARTY_WIDTH - TOGGLE_SIZE - 4;

        // 护符开关：贴着精灵栏左边外侧
        int charmToggleX = partyPanelX - TOGGLE_SIZE - 2;

        // 精灵栏开关
        if (mouseX >= partyToggleX && mouseX <= partyToggleX + TOGGLE_SIZE
                && mouseY >= TOGGLE_Y && mouseY <= TOGGLE_Y + TOGGLE_SIZE) {
            PartyUIState.setPersistentPartyHidden(!state.partyHidden);
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        // 护符开关
        if (mouseX >= charmToggleX && mouseX <= charmToggleX + TOGGLE_SIZE
                && mouseY >= TOGGLE_Y && mouseY <= TOGGLE_Y + TOGGLE_SIZE) {
            PartyUIState.setPersistentCharmDrawerOut(!state.charmDrawerOut);
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        // 1. 右键菜单优先拦截
        if (state.contextMenuOpen) {
            PartyUIState.safeHandleContextMenu(mouseX, mouseY, button, state, screen);
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        // 2. 不在精灵面板区域 → 先检查道具袋槽位，再检查护符抽屉
        if (!PartyUIState.isInPartyArea(state, leftPos, topPos, mouseX, mouseY)) {
            if (PartyUIState.handlePokebagSlotClick(state, leftPos, topPos, mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
            PartyUIState.safeHandleCharmClick(state, leftPos, topPos, mouseX, mouseY);
            return;
        }

        // 精灵栏隐藏时不响应精灵区域内的点击
        if (state.partyHidden) return;

        // 点击冷却
        if (state.ticksTillClick > 0 && button == 0) {
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        int slotSize = PartyUIState.SLOT_SIZE;
        int slotSpacing = PartyUIState.SLOT_SPACING;
        int partyWidth = PartyUIState.PARTY_WIDTH;

        // 4. 先检测携带物槽位点击
        for (int i = 0; i < 6; i++) {
            int sY = topPos + PartyUIState.HEADER_HEIGHT + i * (slotSize + slotSpacing);
            int itemSlotX = partyPanelX + 3 + PartyUIState.SLOT_CONTENT_WIDTH - 18;
            int itemSlotY = sY + 6;
            if (mouseX >= itemSlotX - 1 && mouseX <= itemSlotX + 17
                    && mouseY >= itemSlotY - 1 && mouseY <= itemSlotY + 17) {
                if (button == 0) {
                    PartyUIState.safeHandleHeldItemClick(i, state);
                } else if (button == 1) {
                    Pokemon pkm = PartyUIState.getPartyPokemon(i);
                    if (pkm != null) {
                        state.contextMenuX = (int) mouseX;
                        state.contextMenuY = (int) mouseY;
                        PartyUIState.safeHandleRightClick(i, state);
                    }
                }
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }

        // 5. 精灵槽位点击
        for (int i = 0; i < 6; i++) {
            int sY = topPos + PartyUIState.HEADER_HEIGHT + i * (slotSize + slotSpacing);
            int slotX = partyPanelX + 3;
            if (mouseX >= slotX && mouseX <= slotX + partyWidth - 6
                    && mouseY >= sY && mouseY <= sY + slotSize) {
                state.contextMenuX = (int) mouseX;
                state.contextMenuY = (int) mouseY;
                if (button == 0) {
                    PartyUIState.safeHandleLeftClick(i, state, screen);
                } else if (button == 1) {
                    Pokemon pkm = PartyUIState.getPartyPokemon(i);
                    if (pkm != null) {
                        PartyUIState.safeHandleRightClick(i, state);
                    }
                }
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }

        // 在面板区域内但不在槽位上 → 阻断原版点击
        cir.setReturnValue(true);
        cir.cancel();
    }

    // ==================== mouseReleased ====================
    @Inject(method = "mouseReleased(DDI)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void partyui_mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().player == null) return;
        Object screen = this;
        if (!PartyUIState.isValidScreen(screen)) return;
        PartyUIState.State state = PartyUIState.getState(screen);
        if (state == null) return;

        int leftPos = this.leftPos;
        int topPos = this.topPos;

        if (!PartyUIState.isInPartyArea(state, leftPos, topPos, mouseX, mouseY)) {
            if (state.dragSourceSlot >= 0) {
                state.dragSourceSlot = -1;
                state.selectedPartySlot = -1;
            }
            return;
        }

        if (state.dragSourceSlot >= 0 && !state.contextMenuOpen) {
            int slotSize = PartyUIState.SLOT_SIZE;
            int slotSpacing = PartyUIState.SLOT_SPACING;
            int partyX = PartyUIState.getPartyPanelX(leftPos);
            for (int i = 0; i < 6; i++) {
                int slotX = partyX + 3;
                int sY = topPos + PartyUIState.HEADER_HEIGHT + i * (slotSize + slotSpacing);
                if (mouseX >= slotX && mouseX <= slotX + PartyUIState.PARTY_WIDTH - 6
                        && mouseY >= sY && mouseY <= sY + slotSize) {
                    if (i != state.dragSourceSlot) {
                        PartyUIState.safeSwapPartySlots(state.dragSourceSlot, i);
                    }
                    break;
                }
            }
        }
        state.dragSourceSlot = -1;
        state.selectedPartySlot = -1;
        cir.setReturnValue(true);
        cir.cancel();
    }

    // ==================== mouseDragged ====================
    @Inject(method = "mouseDragged(DDIDD)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void partyui_mouseDragged(double mouseX, double mouseY, int button,
                                      double dragX, double dragY,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().player == null) return;
        Object screen = this;
        if (!PartyUIState.isValidScreen(screen)) return;
        PartyUIState.State state = PartyUIState.getState(screen);
        if (state == null) return;

        int leftPos = this.leftPos;
        int topPos = this.topPos;

        if (state.dragSourceSlot >= 0 && !state.contextMenuOpen) {
            if (PartyUIState.isInPartyArea(state, leftPos, topPos, mouseX, mouseY)) {
                int slotSize = PartyUIState.SLOT_SIZE;
                int slotSpacing = PartyUIState.SLOT_SPACING;
                int partyX = PartyUIState.getPartyPanelX(leftPos);
                for (int i = 0; i < 6; i++) {
                    int slotX = partyX + 3;
                    int sY = topPos + PartyUIState.HEADER_HEIGHT + i * (slotSize + slotSpacing);
                    if (mouseX >= slotX && mouseX <= slotX + PartyUIState.PARTY_WIDTH - 6
                            && mouseY >= sY && mouseY <= sY + slotSize) {
                        state.selectedPartySlot = i;
                        cir.setReturnValue(true);
                        cir.cancel();
                        return;
                    }
                }
            }
            state.selectedPartySlot = -1;
        }
    }

    // ==================== keyPressed ====================
    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void partyui_keyPressed(int keyCode, int scanCode, int modifiers,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().player == null) return;
        Object screen = this;
        if (!PartyUIState.isValidScreen(screen)) return;
        PartyUIState.State state = PartyUIState.getState(screen);
        if (state == null) return;
    }
}