package com.cobblemon_ts.mod.mixin;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * [线性-A1] EditBox Accessor —— 暴露私有字段 highlightPos
 * NeoForge 1.21 只有 setHighlightPos(int) 没有 getter，Mixin注入读访问
 */
@Mixin(EditBox.class)
public interface EditBoxAccessor {

    @Accessor("highlightPos")
    int getHighlightPos();
}