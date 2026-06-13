package com.cobblemon_ts.mod.mixin.cobblemon;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 双保险方案：
 * 1. EVsCanSetMixin 已经拦截了 canSet 方法，跳过总和检查
 * 2. 本 mixin 作为兜底：直接写入底层 stats Map，100% 保证值被写入
 *
 * 直接访问的字段链：
 *   Pokemon.evs -> EVs (extends PokemonStats) -> stats: Map<Stat, Integer> (private, in PokemonStats)
 *
 * 我们用反射直接向这个 Map 写入，绕过所有的 canSet 检查。
 */
@Mixin(Pokemon.class)
public abstract class PokemonEVMixin {

    private static Field evsStatsField = null;
    private static boolean evsStatsFieldResolved = false;

    @Inject(method = "setEV", at = @At("RETURN"), remap = false)
    private void cobblemon_ts$afterSetEV(Stat stat, int value, CallbackInfo ci) {
        if (!Boolean.TRUE.equals(CobblemonTSConfig.EV_EFFORT_CAP_252X6.get())) return;
        forceWriteEV(stat, Math.max(0, Math.min(252, value)));
    }

    @Inject(method = "addEVs", at = @At("RETURN"), remap = false)
    private void cobblemon_ts$afterAddEVs(Stat stat, int amount, CallbackInfoReturnable<Integer> cir) {
        if (!Boolean.TRUE.equals(CobblemonTSConfig.EV_EFFORT_CAP_252X6.get())) return;
        // addEVs 返回的是实际增加的量，如果总和超过510可能被截断
        // 我们直接强制写入 252（或累加后的值）
        int current = 0;
        Object evsObj = getEvsField();
        if (evsObj != null) {
            Object currentVal = readStatsMap(evsObj, stat);
            if (currentVal instanceof Integer iv) current = iv;
        }
        int target = Math.max(0, Math.min(252, current + amount));
        forceWriteEV(stat, target);
    }

    /** 兜底：直接写入底层 Map，绕过所有 canSet 检查 */
    private void forceWriteEV(Stat stat, int value) {
        try {
            Object evs = getEvsField();
            if (evs == null) return;
            Map<Object, Object> statsMap = getStatsMapFromEvs(evs);
            if (statsMap == null) return;
            statsMap.put(stat, value);
        } catch (Throwable ignored) {}
    }

    private Object getEvsField() {
        try {
            Pokemon self = (Pokemon) (Object) this;
            return self.getEvs();
        } catch (Throwable ignored) { return null; }
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> getStatsMapFromEvs(Object evs) {
        if (!evsStatsFieldResolved) {
            try {
                // EVs extends PokemonStats，stats 字段在 PokemonStats 中
                Class<?> current = evs.getClass();
                while (current != null && current != Object.class) {
                    try {
                        Field f = current.getDeclaredField("stats");
                        f.setAccessible(true);
                        evsStatsField = f;
                        break;
                    } catch (NoSuchFieldException ignored) {
                        current = current.getSuperclass();
                    }
                }
            } catch (Throwable ignored) {}
            evsStatsFieldResolved = true;
        }
        if (evsStatsField != null) {
            try {
                return (Map<Object, Object>) evsStatsField.get(evs);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Object readStatsMap(Object evs, Stat stat) {
        try {
            Map<Object, Object> map = getStatsMapFromEvs(evs);
            if (map == null) return null;
            return map.get(stat);
        } catch (Throwable ignored) { return null; }
    }
}