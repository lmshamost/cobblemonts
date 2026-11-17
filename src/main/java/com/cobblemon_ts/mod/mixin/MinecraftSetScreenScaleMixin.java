package com.cobblemon_ts.mod.mixin;

// [线性-S14] Summary强制guiScale=4：HEAD设scale（OPEN+CLOSE），RETURN补onResolutionChanged刷新
//   仅在窗口>=1920px且guiScale==2时打开Summary才触发；其他界面零影响
//   1.21.1 Yarn映射: screen->currentScreen, guiScale()->getGuiScale(), setGuiScale->setScaleFactor
import com.cobblemon.mod.common.client.gui.summary.Summary;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftSetScreenScaleMixin {

    @Unique
    private int cts$savedGuiScale = -1;
    @Unique
    private boolean cts$needsResize = false;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void cts$setScreenHead(Screen newScreen, CallbackInfo ci) {
        if (!CobblemonTSConfig.HUD_FORCE_SUMMARY_GUISCALE.get()) return;
        MinecraftClient self = (MinecraftClient) (Object) this;

        boolean oldIsSummary = self.currentScreen instanceof Summary;
        boolean newIsSummary = newScreen instanceof Summary;

        if (newIsSummary && !oldIsSummary) {
            int currentScale = self.options.getGuiScale().getValue();
            if (currentScale != 2) return;
            if (self.getWindow().getWidth() < 1920) return;
            cts$savedGuiScale = 2;
            self.options.getGuiScale().setValue(4);
            self.getWindow().setScaleFactor(4.0);
        } else if (!newIsSummary && oldIsSummary) {
            if (cts$savedGuiScale == -1) return;
            self.options.getGuiScale().setValue(cts$savedGuiScale);
            self.getWindow().setScaleFactor((double) cts$savedGuiScale);
            cts$savedGuiScale = -1;
            cts$needsResize = true;
        }
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    private void cts$setScreenReturn(Screen newScreen, CallbackInfo ci) {
        if (cts$needsResize) {
            cts$needsResize = false;
            MinecraftClient self = (MinecraftClient) (Object) this;
            ((MinecraftResizeDisplayAccessor) self).invokeResizeDisplay();
        }
    }
}