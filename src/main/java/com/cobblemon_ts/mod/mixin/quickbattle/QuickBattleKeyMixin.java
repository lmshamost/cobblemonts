package com.cobblemon_ts.mod.mixin.quickbattle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.cobblemon_ts.mod.QuickBattleKeys;

@Pseudo
@Mixin(targets = "quick.battle.neoforge.client.QuickBattleNeoForgeClient$GameEvents", remap = false)
public abstract class QuickBattleKeyMixin {

    @ModifyArg(
            method = "onClientTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/glfw/GLFW;glfwGetKey(JI)I",
                    remap = false
            ),
            index = 1
    )
    private static int cobblemon_ts$redirectQuickBattleKey(int originalKeyCode) {
        return QuickBattleKeys.QUICK_BATTLE.getKey().getValue();
    }
}