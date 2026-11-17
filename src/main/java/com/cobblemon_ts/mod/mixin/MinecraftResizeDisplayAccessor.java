package com.cobblemon_ts.mod.mixin;

// [线性-S14-ACCESSOR] 暴露 MinecraftClient.onResolutionChanged() 供缩放切换后刷新
//   1.21.1 Yarn: resizeDisplay -> onResolutionChanged (method_15993)
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftResizeDisplayAccessor {
    @Invoker("onResolutionChanged")
    void invokeResizeDisplay();
}