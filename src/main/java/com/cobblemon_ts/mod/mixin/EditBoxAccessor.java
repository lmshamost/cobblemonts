package com.cobblemon_ts.mod.mixin;

import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * [线性-A1] TextFieldWidget Accessor —— 暴露私有字段
 * Yarn 1.21.1: highlightPos→selectionEnd, cursor→selectionStart
 */
@Mixin(TextFieldWidget.class)
public interface EditBoxAccessor {

    @Accessor("selectionEnd")
    int getHighlightPos();

    @Accessor("selectionStart")
    void setCursorPosition(int cursor);
}