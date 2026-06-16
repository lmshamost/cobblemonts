package com.cobblemon_ts.mod.mixin.cobblemon;

import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleActionSelection;
import com.cobblemon_ts.mod.CobblemonTSConfig;
import com.cobblemon_ts.mod.gui.battle.BattleChangeMenuActionSelection;
import com.cobblemon_ts.mod.gui.battle.PkBagActionSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// [线性-B05] 拦截"道具"按钮点击→使用Cobblemon原生changeActionSelection切换到三选一菜单
// BattleOptionTile 是 final class，用 targets 语法 + abstract 绕过
// [线性-B05zk] 在"道具"tile下方渲染auto_botton.png(18x42垂直strip: ON=y0~17, gap=y18~19, OFF=y20~41)
// auto=ON时点击"道具"直接自动使用上次pkbag使用的物品，不打开三选一菜单
@Mixin(targets = "com.cobblemon.mod.common.client.gui.battle.widgets.BattleOptionTile", remap = false)
public abstract class BattleOptionTileMixin {

    private static final ResourceLocation AUTO_BOTTON = ResourceLocation.parse("coblemon_ts:textures/gui/auto_botton.png");
    private static final int AUTO_TEX_W = 18;
    private static final int AUTO_TEX_H = 42;
    private static final int AUTO_ON_H = 18;    // ON状态纹理高(0~17)
    private static final int AUTO_OFF_V = 20;   // OFF状态纹理v起点(20~41)
    private static final int AUTO_OFF_H = 22;   // OFF状态纹理高(22px)
    private static final int AUTO_SCREEN_W = 18; // 屏幕渲染宽
    private static final int AUTO_SCREEN_H = 18; // 屏幕渲染高（ON/OFF统一18px）

    // [线性-B05zk] 渲染auto_botton在"道具"tile正下方（右对齐tile）
    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void cobblemon_ts$renderAutoButton(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!CobblemonTSConfig.ALTERNATIVE_CAPTURE_MODE.get()) return;

        try {
            // [线性-B05zk] 检查是否为"道具"tile
            Object textObj = this.getClass().getMethod("getText").invoke(this);
            if (textObj == null) return;
            String text = textObj.toString().toLowerCase();
            boolean isItems = text.contains("capture") || text.contains("捕捉")
                    || text.contains("捕获") || text.contains("catch")
                    || text.contains("道具");
            if (!isItems) return;

            int x = (int) this.getClass().getMethod("getX").invoke(this);
            int y = (int) this.getClass().getMethod("getY").invoke(this);
            int autoX = x + 90 - AUTO_SCREEN_W;  // 右对齐tile(90w)
            int autoY = y + 26;                    // tile正下方(tile高26)

            // [线性-B05zk] 垂直strip：ON=上半(v=0)，OFF=下半(v=20)
            boolean isAutoOn = PkBagActionSelection.autoUseEnabled;
            int vOffset = isAutoOn ? 0 : AUTO_OFF_V;
            int vHeight = isAutoOn ? AUTO_ON_H : AUTO_OFF_H;
            graphics.blit(AUTO_BOTTON, autoX, autoY,
                    AUTO_SCREEN_W, AUTO_SCREEN_H,
                    0, vOffset,
                    AUTO_TEX_W, vHeight,
                    AUTO_TEX_W, AUTO_TEX_H);
        } catch (Exception ignored) {
        }
    }

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

            int x = (int) this.getClass().getMethod("getX").invoke(this);
            int y = (int) this.getClass().getMethod("getY").invoke(this);

            // [线性-B05zk] 检测auto按钮点击
            int autoX = x + 90 - AUTO_SCREEN_W;
            int autoY = y + 26;
            if (mouseX >= autoX && mouseX <= autoX + AUTO_SCREEN_W
                    && mouseY >= autoY && mouseY <= autoY + AUTO_SCREEN_H) {
                PkBagActionSelection.autoUseEnabled = !PkBagActionSelection.autoUseEnabled;
                cir.setReturnValue(false);
                cir.cancel();
                return;
            }

            // [线性-B05b] 检查点击是否在 tile 范围内（90x26）
            if (mouseX < x || mouseX > x + 90 || mouseY < y || mouseY > y + 26) return;

            cir.setReturnValue(false);
            cir.cancel();

            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.screen instanceof BattleGUI battleGUI) {
                BattleActionSelection current = battleGUI.getCurrentActionSelection();
                if (current != null) {
                    // [线性-B05zk] auto=ON → 直接使用上次物品，不打开三选一菜单
                    if (PkBagActionSelection.autoUseEnabled) {
                        PkBagActionSelection.autoUseLastItem(battleGUI, current.getRequest());
                    } else {
                        // [线性-B05y] 使用Cobblemon原生changeActionSelection切换
                        battleGUI.changeActionSelection(
                                new BattleChangeMenuActionSelection(battleGUI, current.getRequest()));
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}