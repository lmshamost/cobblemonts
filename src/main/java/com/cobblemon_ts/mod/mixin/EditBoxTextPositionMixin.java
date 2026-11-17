package com.cobblemon_ts.mod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import com.cobblemon_ts.mod.pokemoneditor.PokemonEditorScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextFieldWidget.class)
public class EditBoxTextPositionMixin {
    @Redirect(method = "renderWidget",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;drawsBackground()Z"))
    private boolean cancelBorderSprite(TextFieldWidget instance) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen) {
            return false;
        }
        return instance.drawsBackground();
    }

    private boolean shouldRemoveShadow() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.currentScreen instanceof PokemonEditorScreen;
    }

    @Redirect(method = "renderWidget",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"))
    private int redirectDrawOrderedText(DrawContext context, TextRenderer textRenderer, OrderedText text, int x, int y, int color) {
        if (shouldRemoveShadow()) {
            return context.drawText(textRenderer, text, x, y, color, false);
        }
        return context.drawTextWithShadow(textRenderer, text, x, y, color);
    }

    @Redirect(method = "renderWidget",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"))
    private int redirectDrawText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color) {
        if (shouldRemoveShadow()) {
            return context.drawText(textRenderer, text, x, y, color, false);
        }
        return context.drawTextWithShadow(textRenderer, text, x, y, color);
    }

    @Redirect(method = "renderWidget",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I"))
    private int redirectDrawString(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color) {
        if (shouldRemoveShadow()) {
            return context.drawText(textRenderer, text, x, y, color, false);
        }
        return context.drawTextWithShadow(textRenderer, text, x, y, color);
    }
}