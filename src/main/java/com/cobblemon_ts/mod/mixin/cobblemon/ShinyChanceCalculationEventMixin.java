package com.cobblemon_ts.mod.mixin.cobblemon;

import com.cobblemon.mod.common.api.events.pokemon.ShinyChanceCalculationEvent;
import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmType;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.DebugConfig;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// [线性-D01] Mixin拦截ShinyChanceCalculationEvent.calculate(ServerPlayer)
// calculate返回的是闪光率分母（如4096），checkRate执行1.0f/rate判断
// rate越小闪光率越高，所以护符加成是 rate/boost 而非 rate*boost
@Mixin(value = ShinyChanceCalculationEvent.class, remap = false)
public class ShinyChanceCalculationEventMixin {

    @Inject(method = "calculate", at = @At("RETURN"), cancellable = true)
    private void cobblemon_ts$applyShinyCharm(ServerPlayer player, CallbackInfoReturnable<Float> cir) {
        try {
            if (player == null) return;

            boolean hasCharm = CharmPlayerData.get(player)
                    .getCharmSlotManager()
                    .isCharmActive(CharmType.SHINY);

            if (hasCharm) {
                float original = cir.getReturnValue();
                // [线性-D01] rate <= 0 时checkRate直接返回false，无需修改
                if (original <= 0.0f) return;

                float boost = CharmConfig.SHINY_CHARM_BOOST.get().floatValue();
                if (boost <= 0.0f) return;

                // [线性-D01] 闪光率 = 1/rate，所以要降低分母：rate / boost
                float boosted = original / boost;
                // [线性-D01] checkRate中rate<=0返回false(不闪光)，所以只需确保rate>0
                // 允许达到100%闪光（rate趋近0时1/rate→∞，nextFloat必定<∞）
                if (boosted <= 0.0f) boosted = 0.99f;

                if (DebugConfig.DEBUG_CHARM.get()) {
                    CobblemonTS.LOGGER.info("[Shiny] 闪耀护符加成: rate={} -> {} (÷{}) player={}",
                            original, boosted, boost, player.getName().getString());
                }

                cir.setReturnValue(boosted);
            }
        } catch (Exception e) {
            CobblemonTS.LOGGER.warn("[Shiny] 护符加成失败", e);
        }
    }
}