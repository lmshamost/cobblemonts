package com.cobblemon_ts.mod.mixin;

// [GuiEffectIconMixin] 右上角HUD药水效果图标控制
//   show_effect_icons=false → 完全隐藏
//   hud_potion_hide_flash=true → 始终显示不闪烁
//   hud_potion_shrink=true → 按hud_potion_shrink_scale缩放+单列（与背包统一）
//   否则走原版渲染
import com.cobblemon_ts.mod.CobblemonTSConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(InGameHud.class)
public class GuiEffectIconMixin {

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!CobblemonTSConfig.SHOW_EFFECT_ICONS.get()) {
            ci.cancel();
            return;
        }
        // [线性-P01] 隐藏闪烁或缩小模式→接管渲染
        if (CobblemonTSConfig.HUD_POTION_SHRINK.get()) {
            ci.cancel();
            renderShrunkHudEffects(context);
            return;
        }
        if (CobblemonTSConfig.HUD_POTION_HIDE_FLASH.get()) {
            ci.cancel();
            renderNoFlashEffects(context);  // [线性-P01] 原版大小但不闪烁
        }
    }

    @Unique
    private void renderShrunkHudEffects(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null) return;
        Collection<StatusEffectInstance> effects = player.getStatusEffects();
        if (effects.isEmpty()) return;

        float scale = (float) CobblemonTSConfig.HUD_POTION_SHRINK_SCALE.get();
        int iconSize = Math.max(4, (int) (18.0f * scale));
        int gap = 2;

        int sw = mc.getWindow().getScaledWidth();
        int margin = 4;
        int x = sw - iconSize - margin;
        int y = 2;

        for (StatusEffectInstance inst : effects) {
            if (!inst.shouldShowParticles() || !inst.shouldShowIcon()) continue;

            Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(inst.getEffectType());
            if (sprite != null) {
                context.drawSprite(x, y, 0, iconSize, iconSize, sprite);
            } else {
                int color = inst.getEffectType().value().getColor();
                context.fill(x, y, x + iconSize, y + iconSize, 0xFF000000 | color);
            }

            y += iconSize + gap;
        }
    }

    // [线性-P01] 原版大小但不闪烁：始终绘制不跳帧
    @Unique
    private void renderNoFlashEffects(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null) return;
        Collection<StatusEffectInstance> effects = player.getStatusEffects();
        if (effects.isEmpty()) return;

        int iconSize = 18;
        int gap = 3;
        int sw = mc.getWindow().getScaledWidth();
        int margin = 3;
        int x = sw - iconSize - margin;
        int y = 2;
        int col = 0;
        int colMax = 2;

        for (StatusEffectInstance inst : effects) {
            if (!inst.shouldShowParticles() || !inst.shouldShowIcon()) continue;

            Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(inst.getEffectType());
            if (sprite != null) {
                context.drawSprite(x - col * (iconSize + gap), y, 0, iconSize, iconSize, sprite);
            }
            col++;
            if (col >= colMax) { col = 0; y += iconSize + gap; }
        }
    }
}