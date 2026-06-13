package com.cobblemon_ts.mod.mixin.cobblemon;

import com.cobblemon.mod.common.pokemon.EVs;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 核心：拦截 EVs.canSet 方法，当 EV_EFFORT_CAP_252X6 开启时，
 * 跳过 "总和 <= 510" 的检查，允许每单项 252。
 *
 * 调用链：Pokemon.setEV(Stat, int) → EVs.set(Stat, int)
 *         → EVs.canSet(Stat, int) [检查 单项 in 0-252 AND 总和 <= 510]
 *         如果 canSet 返回 false → 值不会写入 stats map
 */
@Mixin(EVs.class)
public abstract class EVsCanSetMixin {

    @Inject(method = "canSet", at = @At("HEAD"), remap = false, cancellable = true)
    private void cobblemon_ts$canSetEV(com.cobblemon.mod.common.api.pokemon.stats.Stat stat, int value, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(CobblemonTSConfig.EV_EFFORT_CAP_252X6.get())) return;

        // 252x6 模式：只检查单项是否在 0-252 范围内，不检查总和
        int clamped = Math.max(0, Math.min(252, value));
        cir.setReturnValue(clamped == value);
        cir.cancel();
    }
}