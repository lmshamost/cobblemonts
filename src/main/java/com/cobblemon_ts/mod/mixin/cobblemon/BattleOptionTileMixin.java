package com.cobblemon_ts.mod.mixin.cobblemon;

import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleActionSelection;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import com.cobblemon_ts.mod.gui.battle.BattleChangeMenuActionSelection;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// [线性-B05] 拦截"道具"按钮点击→使用Cobblemon原生changeActionSelection切换到三选一菜单
// BattleOptionTile 是 final class，用 targets 语法 + abstract 绕过
@Mixin(targets = "com.cobblemon.mod.common.client.gui.battle.widgets.BattleOptionTile", remap = false)
public abstract class BattleOptionTileMixin {

    @Inject(method = "mousePrimaryClicked", at = @At("HEAD"), remap = false, cancellable = true)
    private void cobblemon_ts$interceptCaptureClick(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (!CobblemonTSConfig.ALTERNATIVE_CAPTURE_MODE.get()) return;

        try {
            // [线性-B05a] 获取 tile 的文本，判断是否为"道具"按钮
            Object textObj = this.getClass().getMethod("getText").invoke(this);
            if (textObj == null) return;
            String text = textObj.toString().toLowerCase();

            // [线性-B05r] 匹配"道具"（ALTERNATIVE_CAPTURE_MODE开启时捕捉文字被改为"道具"）
            boolean isCapture = text.contains("capture") || text.contains("捕捉")
                    || text.contains("捕获") || text.contains("catch")
                    || text.contains("道具");
            if (!isCapture) return;

            // [线性-B05b] 检查点击是否在 tile 范围内（90x26）
            int x = (int) this.getClass().getMethod("getX").invoke(this);
            int y = (int) this.getClass().getMethod("getY").invoke(this);
            if (mouseX < x || mouseX > x + 90 || mouseY < y || mouseY > y + 26) return;

            cir.setReturnValue(false);
            cir.cancel();

            // [线性-B05y] 使用Cobblemon原生changeActionSelection切换
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.screen instanceof BattleGUI battleGUI) {
                BattleActionSelection current = battleGUI.getCurrentActionSelection();
                if (current != null) {
                    battleGUI.changeActionSelection(
                            new BattleChangeMenuActionSelection(battleGUI, current.getRequest()));
                }
            }
        } catch (Exception ignored) {
        }
    }
}