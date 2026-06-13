package com.cobblemon_ts.mod.mixin;

import com.cobblemon_ts.mod.ui.PartyUIState;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 界面关闭时清理 UI_STATES 缓存，防止内存泄漏
 * 注入 AbstractContainerScreen.removed() → 界面从屏幕移除时触发
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ScreenCloseCleanupMixin {

    @Inject(method = "removed()V", at = @At("HEAD"), require = 0)
    private void partyui_onRemoved(CallbackInfo ci) {
        Object screen = this;
        if (PartyUIState.isValidScreen(screen)) {
            // 重置状态后清理
            PartyUIState.State state = PartyUIState.getStateOrNull(screen);
            if (state != null) {
                PartyUIState.resetInteractionState(state);
            }
            PartyUIState.cleanupState(screen);
        }
    }
}