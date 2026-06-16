package com.cobblemon_ts.mod.mixin.cobblemon;

import com.cobblemon_ts.mod.CobblemonTSConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

// [线性-B05r] 当替代捕捉模式开启时，把Cobblemon原版捕捉按钮文字"捕捉"改为"道具"
// 通过修改字符串常量 "ui.capture" 实现，精准定位，不受分支影响
@Mixin(targets = "com.cobblemon.mod.common.client.gui.battle.subscreen.BattleGeneralActionSelection", remap = false)
public abstract class BattleGeneralActionSelectionMixin {

    @ModifyConstant(method = "<init>", constant = @Constant(stringValue = "ui.capture"), remap = false)
    private String cobblemon_ts$changeCaptureText(String original) {
        if (CobblemonTSConfig.ALTERNATIVE_CAPTURE_MODE.get()) {
            // battleLang 会拼接成 "cobblemon.coblemon_ts.capture"，我们在lang文件中添加翻译为"道具"
            return "coblemon_ts.capture";
        }
        return original;
    }
}