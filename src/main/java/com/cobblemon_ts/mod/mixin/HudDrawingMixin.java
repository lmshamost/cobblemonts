package com.cobblemon_ts.mod.mixin;

import com.cobblemon_ts.mod.CharmConfig;
import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmType;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.DebugConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.catchrate.client.HudDrawing", remap = false)
public class HudDrawingMixin {

    private static String cachedText;
    private static int cachedTextWidth;
    private static float lastBoost = -1f;

    @Inject(method = "drawStyledPanel", at = @At("RETURN"))
    private void cobblemon_ts$addCharmBonus(DrawContext DrawContext, int x, int y, int w, int h,
                                            double alpha, boolean reliable, CallbackInfo ci) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            boolean hasCharm = CharmPlayerData.get(mc.player)
                    .getCharmSlotManager()
                    .isCharmActive(CharmType.CATCHING);

            if (!hasCharm) return;

            float boost = (float) CharmConfig.CATCHING_CHARM_BOOST.get();

            if (cachedText == null || Math.abs(boost - lastBoost) > 0.001f) {
                cachedText = "护符加成: x" + String.format("%.1f", boost);
                cachedTextWidth = mc.textRenderer.getWidth(cachedText);
                lastBoost = boost;
            }

            int textX = x + (w - cachedTextWidth) / 2;
            int textY = y + h + 1;

            DrawContext.drawText(mc.textRenderer, Text.literal(cachedText), textX, textY, 0xFF55FF55, false);

            if (DebugConfig.DEBUG_CHARM.get()) {
                CobblemonTS.LOGGER.info("[HudDrawing] draw charm '{}' at ({}, {}), panel=({},{})({}x{})",
                        cachedText, textX, textY, x, y, w, h);
            }
        } catch (Exception e) {
            if (DebugConfig.DEBUG_CHARM.get()) {
                CobblemonTS.LOGGER.warn("[HudDrawing] error: {}", e.toString());
            }
        }
    }
}