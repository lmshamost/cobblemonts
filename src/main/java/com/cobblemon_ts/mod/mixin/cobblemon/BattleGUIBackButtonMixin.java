package com.cobblemon_ts.mod.mixin.cobblemon;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.battle.ClientBattle;
import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleActionSelection;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleBackButton;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// [线性-B05t] 当打开道具背包（精灵球选择/道具选择）时，渲染返回按键
// [线性-B05zb] 修复：原版Cobblemon的specBackButton仅在getSpectating()==true时渲染
// 这里补上currentActionSelection==null且spectating时的返回键（与BattleGUI.render()中逻辑一致）
// 非spectating模式（正常对战）不应渲染specBackButton，由各ActionSelection自行管理返回键
@Mixin(value = BattleGUI.class, remap = false)
public abstract class BattleGUIBackButtonMixin {

    @Shadow
    public abstract BattleActionSelection getCurrentActionSelection();

    @Shadow
    public abstract BattleBackButton getSpecBackButton();

    // [线性-B05zb] render末尾：模仿原版BattleGUI.render()，仅spectating模式且currentActionSelection==null时渲染
    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void cobblemon_ts$renderBackButtonOnBag(GuiGraphics context, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!CobblemonTSConfig.ALTERNATIVE_CAPTURE_MODE.get()) return;
        if (getCurrentActionSelection() != null) return;

        ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
        if (battle == null) return;

        // [线性-B05zb] 模仿原版：仅spectating模式渲染specBackButton
        if (!battle.getSpectating()) return;

        BattleBackButton backButton = getSpecBackButton();
        if (backButton == null) return;

        backButton.render(context, mouseX, mouseY, partialTick);
    }

    // [线性-B05zb] mouseClicked头部：拦截spectating模式返回键点击
    @Inject(method = "mouseClicked", at = @At("HEAD"), remap = false, cancellable = true)
    private void cobblemon_ts$handleBackButtonClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!CobblemonTSConfig.ALTERNATIVE_CAPTURE_MODE.get()) return;
        if (button != 0) return;
        if (getCurrentActionSelection() != null) return;

        ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
        if (battle == null) return;

        // [线性-B05zb] 仅spectating模式处理返回键点击
        if (!battle.getSpectating()) return;

        BattleBackButton backButton = getSpecBackButton();
        if (backButton == null) return;

        if (!backButton.isHovered(mouseX, mouseY)) return;

        // [线性-B05t] 取消上一个已应答的请求，让服务器重新派发行动选择
        battle.cancelLastAnsweredRequest();
        cir.setReturnValue(true);
    }
}