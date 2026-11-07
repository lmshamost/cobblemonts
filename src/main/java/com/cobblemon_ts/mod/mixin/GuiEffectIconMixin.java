package com.cobblemon_ts.mod.mixin;

// [GuiEffectIconMixin] 右上角HUD药水效果图标控制
//   show_effect_icons=false → 完全隐藏
//   hud_potion_hide_flash=true → 始终显示不闪烁
//   hud_potion_shrink=true → 按hud_potion_shrink_scale缩放+单列（与背包统一）
//   否则走原版渲染
import com.cobblemon_ts.mod.CobblemonTSConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(Gui.class)
public class GuiEffectIconMixin {

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void onRenderEffects(GuiGraphics g, DeltaTracker dt, CallbackInfo ci) {
        if (!CobblemonTSConfig.SHOW_EFFECT_ICONS.get()) {
            ci.cancel();
            return;
        }
        // [线性-P01] 隐藏闪烁或缩小模式→接管渲染
        if (CobblemonTSConfig.HUD_POTION_SHRINK.get()) {
            ci.cancel();
            renderShrunkHudEffects(g);
            return;
        }
        if (CobblemonTSConfig.HUD_POTION_HIDE_FLASH.get()) {
            ci.cancel();
            renderNoFlashEffects(g);  // [线性-P01] 原版大小但不闪烁
        }
    }

    @Unique
    private void renderShrunkHudEffects(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        Collection<MobEffectInstance> effects = player.getActiveEffects();
        if (effects.isEmpty()) return;

        float scale = CobblemonTSConfig.HUD_POTION_SHRINK_SCALE.get().floatValue();
        int iconSize = Math.max(4, (int)(18.0f * scale));
        int gap = 2;

        int sw = mc.getWindow().getGuiScaledWidth();
        int margin = 4;
        int x = sw - iconSize - margin;
        int y = 2;

        for (MobEffectInstance inst : effects) {
            if (!inst.isVisible() || !inst.showIcon()) continue;

            TextureAtlasSprite sprite = mc.getMobEffectTextures().get(inst.getEffect());
            if (sprite != null) {
                g.blit(x, y, 0, iconSize, iconSize, sprite);
            } else {
                int color = inst.getEffect().value().getColor();
                g.fill(x, y, x + iconSize, y + iconSize, 0xFF000000 | color);
            }

            y += iconSize + gap;
        }
    }

    // [线性-P01] 原版大小但不闪烁：始终绘制不跳帧
    @Unique
    private void renderNoFlashEffects(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        Collection<MobEffectInstance> effects = player.getActiveEffects();
        if (effects.isEmpty()) return;

        int iconSize = 18;
        int gap = 3;
        int sw = mc.getWindow().getGuiScaledWidth();
        int margin = 3;
        int x = sw - iconSize - margin;
        int y = 2;
        int col = 0;
        int colMax = 2;

        for (MobEffectInstance inst : effects) {
            if (!inst.isVisible() || !inst.showIcon()) continue;

            TextureAtlasSprite sprite = mc.getMobEffectTextures().get(inst.getEffect());
            if (sprite != null) {
                g.blit(x - col * (iconSize + gap), y, 0, iconSize, iconSize, sprite);
            }
            col++;
            if (col >= colMax) { col = 0; y += iconSize + gap; }
        }
    }
}