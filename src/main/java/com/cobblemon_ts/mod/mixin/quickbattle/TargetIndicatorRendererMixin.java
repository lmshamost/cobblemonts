package com.cobblemon_ts.mod.mixin.quickbattle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.cobblemon_ts.mod.QuickBattleKeys;

@Pseudo
@Mixin(targets = "quick.battle.client.TargetIndicatorRenderer", remap = false)
public abstract class TargetIndicatorRendererMixin {

    @ModifyArg(
            method = "renderHUDInternal",
            at = @At(
                    value = "INVOKE",
                    target = "Lquick/battle/config/QuickBattleConfigScreen;getKeyDisplayName(I)Ljava/lang/String;",
                    remap = false
            ),
            index = 0
    )
    private static int cobblemon_ts$redirectKeyDisplay(int originalKeyCode) {
        return QuickBattleKeys.QUICK_BATTLE.getKey().getValue();
    }
}