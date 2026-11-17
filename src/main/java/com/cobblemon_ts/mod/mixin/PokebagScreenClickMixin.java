package com.cobblemon_ts.mod.mixin;

import com.cobblemon_ts.mod.gui.PokebagContainer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HandledScreen.class)
public abstract class PokebagScreenClickMixin {

    // [线性-B06] 原版onMouseClick只检测Shift不检测Alt，这里在发送点击前把shift+alt的mouseButton改写为EXTRACT_ALL_BUTTON，服务端据此执行全取。
    // 用@Redirect拦截clickSlot调用，检测到Alt时替换button参数。
    @Redirect(
        method = "onMouseClick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;clickSlot(IIILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V"
        )
    )
    private void cobblets$markExtractAll(ClientPlayerInteractionManager manager, int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player) {
        int actualButton = button;
        if (actionType == SlotActionType.QUICK_MOVE
            && Screen.hasAltDown()
            && ((HandledScreen<?>) (Object) this).getScreenHandler() instanceof PokebagContainer) {
            actualButton = PokebagContainer.EXTRACT_ALL_BUTTON;
        }
        manager.clickSlot(syncId, slotId, actualButton, actionType, player);
    }
}