package com.cobblemon_ts.mod.mixin;

// [S14-final] Summary强制guiScale=4：HEAD设scale（OPEN+CLOSE），RETURN补resizeDisplay
//   仅在窗口≥1920px+guiScale==2时开Summary才触发；其他界面零影响
import com.cobblemon.mod.common.client.gui.summary.Summary;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftSetScreenScaleMixin {

    @Unique
    private int cts$savedGuiScale = -1;
    @Unique
    private boolean cts$needsResize = false;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void cts$setScreenHead(Screen newScreen, CallbackInfo ci) {
        try {
            if (!CobblemonTSConfig.HUD_FORCE_SUMMARY_GUISCALE.get()) return;
        } catch (IllegalStateException e) {
            return;
        }
        Minecraft self = (Minecraft) (Object) this;

        boolean oldIsSummary = self.screen instanceof Summary;
        boolean newIsSummary = newScreen instanceof Summary;

        if (newIsSummary && !oldIsSummary) {
            int currentScale = self.options.guiScale().get();
            if (currentScale != 2) return;
            if (self.getWindow().getWidth() < 1920) return;
            cts$savedGuiScale = 2;
            self.options.guiScale().set(4);
            self.getWindow().setGuiScale(4.0);
        } else if (!newIsSummary && oldIsSummary) {
            if (cts$savedGuiScale == -1) return;
            self.options.guiScale().set(cts$savedGuiScale);
            self.getWindow().setGuiScale((double) cts$savedGuiScale);
            cts$savedGuiScale = -1;
            cts$needsResize = true;
        }
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    private void cts$setScreenReturn(Screen newScreen, CallbackInfo ci) {
        if (cts$needsResize) {
            cts$needsResize = false;
            Minecraft self = (Minecraft) (Object) this;
            ((MinecraftResizeDisplayAccessor) self).invokeResizeDisplay();
        }
    }
}