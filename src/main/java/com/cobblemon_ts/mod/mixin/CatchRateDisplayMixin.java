package com.cobblemon_ts.mod.mixin;

import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmType;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.DebugConfig;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(targets = "com.catchrate.SpeciesCatchRateCache", remap = false)
public class CatchRateDisplayMixin {

    @Inject(method = "getCatchRate", at = @At("RETURN"), cancellable = true)
    private void cobblemon_ts$applyCharmBoost(Species species, Set<String> aspects, CallbackInfoReturnable<Integer> cir) {
        boolean debug = DebugConfig.DEBUG_CHARM.get();
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                if (debug) CobblemonTS.LOGGER.info("[CatchRateDisplay] skip: player null");
                return;
            }

            boolean hasCharm = CharmPlayerData.get(mc.player)
                    .getCharmSlotManager()
                    .isCharmActive(CharmType.CATCHING);

            if (hasCharm) {
                int original = cir.getReturnValue();
                float boost = CharmConfig.CATCHING_CHARM_BOOST.get().floatValue();
                int boosted = Math.round(original * boost);

                if (debug) {
                    CobblemonTS.LOGGER.info("[CatchRateDisplay] species={}, aspects={}, catchRate: {} -> {} (x{})",
                            species.getName(), aspects, original, boosted, boost);
                }

                cir.setReturnValue(boosted);
            } else {
                if (debug) {
                    CobblemonTS.LOGGER.info("[CatchRateDisplay] species={}, aspects={}, catchRate={} (no charm)",
                            species.getName(), aspects, cir.getReturnValue());
                }
            }
        } catch (Exception e) {
            if (debug) {
                CobblemonTS.LOGGER.warn("[CatchRateDisplay] error: {}", e.toString());
            }
        }
    }
}