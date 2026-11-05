package com.cobblemon_ts.mod.mixin;

// [GuiEffectIconMixin] 右上角HUD药水效果图标控制
//   show_effect_icons=false → 完全隐藏
//   hud_potion_shrink=true → 缩小0.6倍(14x14)+暗色边框+单列
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

    @Unique
    private static final int SHRINK_ICON_SIZE = 14;
    @Unique
    private static final int SHRINK_GAP = 2;
    @Unique
    private static final int FRAME_COLOR = 0xAA000000;

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void onRenderEffects(GuiGraphics g, DeltaTracker dt, CallbackInfo ci) {
        if (!CobblemonTSConfig.SHOW_EFFECT_ICONS.get()) {
            ci.cancel();
            return;
        }
        if (CobblemonTSConfig.HUD_POTION_SHRINK.get()) {
            ci.cancel();
            renderShrunkHudEffects(g);
        }
    }

    @Unique
    private void renderShrunkHudEffects(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        Collection<MobEffectInstance> effects = player.getActiveEffects();
        if (effects.isEmpty()) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int margin = 4;
        int x = sw - SHRINK_ICON_SIZE - margin;
        int y = 2;

        for (MobEffectInstance inst : effects) {
            if (!inst.isVisible() || !inst.showIcon()) continue;

            g.fill(x - 1, y - 1, x + SHRINK_ICON_SIZE + 1, y + SHRINK_ICON_SIZE + 1, FRAME_COLOR);

            TextureAtlasSprite sprite = mc.getMobEffectTextures().get(inst.getEffect());
            if (sprite != null) {
                g.blit(x, y, 0, SHRINK_ICON_SIZE, SHRINK_ICON_SIZE, sprite);
            } else {
                int color = inst.getEffect().value().getColor();
                g.fill(x, y, x + SHRINK_ICON_SIZE, y + SHRINK_ICON_SIZE, 0xFF000000 | color);
            }

            y += SHRINK_ICON_SIZE + SHRINK_GAP;
        }
    }
}