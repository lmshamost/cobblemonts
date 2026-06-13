package com.cobblemon_ts.mod.mixin;

import com.cobblemon.mod.common.api.events.pokeball.PokemonCatchRateEvent;
import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmType;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.DebugConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PokemonCatchRateEvent.class, remap = false)
public class PokemonCatchRateEventMixin {

    @Inject(method = "getCatchRate", at = @At("RETURN"), cancellable = true)
    private void cobblemon_ts$applyCharmBoost(CallbackInfoReturnable<Float> cir) {
        try {
            PokemonCatchRateEvent self = (PokemonCatchRateEvent) (Object) this;
            LivingEntity thrower = self.getThrower();
            if (thrower instanceof ServerPlayer player) {
                boolean hasCharm = CharmPlayerData.get(player)
                        .getCharmSlotManager()
                        .isCharmActive(CharmType.CATCHING);
                if (hasCharm) {
                    float original = cir.getReturnValue();
                    float boost = CharmConfig.CATCHING_CHARM_BOOST.get().floatValue();
                    float boosted = original * boost;

                    if (DebugConfig.DEBUG_CHARM.get()) {
                        CobblemonTS.LOGGER.info("[CatchRate] 防晃护符加成: {} -> {} (x{}) player={}",
                                original, boosted, boost, player.getName().getString());
                    }

                    cir.setReturnValue(boosted);
                }
            }
        } catch (Exception e) {
            CobblemonTS.LOGGER.warn("[CatchRate] 护符加成失败", e);
        }
    }
}