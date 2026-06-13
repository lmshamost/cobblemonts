package com.cobblemon_ts.mod.mixin.cobblemon;

import com.cobblemon.mod.common.pokemon.EVs;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 三层拦截，确保 EV 总和 510 的检查被完全绕过：
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ 问题根源：EVs 类中有多处检查 "EV 总和 <= 510"，包括：                 │
 * │   1. public static final int MAX_TOTAL_VALUE = 510              │
 * │   2. EVs.CODEC 解码时检查 total() <= MAX_TOTAL_VALUE             │
 * │   3. EVs.STREAM_CODEC 解码时检查 total() <= MAX_TOTAL_VALUE      │
 * │   4. EVs.add(int)/canSet(int) 中比较 total + new <= 510          │
 * │                                                                 │
 * │ Java 编译器会将 static final int 的值**内联**到使用它的地方（变成   │
 * │ 字面量 510 写入字节码），所以仅拦截 GETSTATIC 可能失效。             │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * 三层防御：
 *   ① @ModifyConstant(intValue = 510) — 拦截字节码中所有硬编码的 510
 *   ② @Redirect(GETSTATIC) — 拦截对 MAX_TOTAL_VALUE 字段的读取
 *   ③ EVsCanSetMixin — 拦截 canSet 方法（已单独存在）
 *
 * 当 EV_EFFORT_CAP_252X6=true 时，上述拦截均返回 1512（252×6）
 * 否则返回原始值 510，保持 vanilla 行为。
 */
@Mixin(EVs.class)
public abstract class EVsTotalCapMixin {

    /**
     * ① 拦截 EVs 类中所有值为 510 的 int 常量。
     *
     * 这是最关键的一层：当 Kotlin 编译器将 MAX_TOTAL_VALUE 内联到
     * CODEC lambda / STREAM_CODEC / add() 方法等位置时，它们在字节码中
     * 变成了字面量 `BIPUSH 510` 或 `LDC 510`，不再是字段读取。
     *
     * @ModifyConstant 直接扫描字节码中的常量值，在运行时动态替换。
     */
    @ModifyConstant(method = "*", constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = 510), remap = false, require = 0)
    private static int cobblemon_ts$modifyMaxTotalConstant(int originalValue) {
        if (Boolean.TRUE.equals(CobblemonTSConfig.EV_EFFORT_CAP_252X6.get())) {
            return 252 * 6;  // 1512
        }
        return originalValue;  // 510, vanilla 行为
    }

    /**
     * ② 兜底：拦截所有对 EVs.MAX_TOTAL_VALUE 的 GETSTATIC 读取。
     *
     * 虽然 static final 常量常被内联，但有些路径（如通过反射、
     * 或其他类读取）仍然使用 GETSTATIC 指令。这里兜底拦截。
     *
     * opcode = 178 = GETSTATIC
     */
    @Redirect(
            method = "*",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/cobblemon/mod/common/pokemon/EVs;MAX_TOTAL_VALUE:I",
                    opcode = 178  // GETSTATIC
            ),
            remap = false,
            require = 0
    )
    private static int cobblemon_ts$getDynamicMaxTotalValue() {
        if (Boolean.TRUE.equals(CobblemonTSConfig.EV_EFFORT_CAP_252X6.get())) {
            return 252 * 6;  // 1512
        }
        return 510;
    }
}