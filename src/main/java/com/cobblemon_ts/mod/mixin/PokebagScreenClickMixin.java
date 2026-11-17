package com.cobblemon_ts.mod.mixin;

import com.cobblemon_ts.mod.gui.PokebagContainer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AbstractContainerScreen.class)
public abstract class PokebagScreenClickMixin {

    // [线性-B06] 原版slotClicked只检测Shift不检测Alt，这里在发送点击前把shift+alt的mouseButton改写为EXTRACT_ALL_BUTTON，服务端据此执行全取
    // 注意：@ModifyArg多参数模式下handler参数必须与目标方法(handleInventoryMouseClick)参数完全一致，返回值=被改写的第3个参数(mouseButton)
    @ModifyArg(
        method = "slotClicked",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleInventoryMouseClick(IIILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)Z"
        ),
        index = 2
    )
    private int cobblets$markExtractAll(int containerId, int slotId, int mouseButton, ClickType type, Player player) {
        if (type == ClickType.QUICK_MOVE
            && Screen.hasAltDown()
            && ((AbstractContainerScreen<?>) (Object) this).getMenu() instanceof PokebagContainer) {
            return PokebagContainer.EXTRACT_ALL_BUTTON;
        }
        return mouseButton;
    }
}