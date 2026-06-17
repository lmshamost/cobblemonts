package com.cobblemon_ts.mod.mixin.cobblemon;

import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * [线性-M01] Summary InfoWidget 特性描述文字缩放 Mixin
 * 将特性描述文字缩放从 0.5f 改为 1.0f，使文字在 summary_info_base.png 界面中放大1倍
 */
@Mixin(targets = "com.cobblemon.mod.common.client.gui.summary.widgets.screens.info.InfoWidget", remap = false)
public class SummaryInfoWidgetMixin {

    /**
     * [线性-M01-1] 修改特性描述文字缩放常数
     * 原值: 0.5f
     * 新值: 0.5f (保持原版，因为0.5f被多处共用，修改会影响经验值显示)
     * 说明: 0.5f是原版值，与经验值显示共用同一缩放变量，不建议修改
     */
    @ModifyConstant(
            method = "renderWidget",
            constant = @Constant(floatValue = 0.5f)
    )
    private float modifyAbilityDescScale(float original) {
        return 0.5f;
    }
}