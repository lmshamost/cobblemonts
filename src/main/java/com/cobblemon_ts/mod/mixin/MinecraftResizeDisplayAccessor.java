package com.cobblemon_ts.mod.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftResizeDisplayAccessor {
    @Invoker("resizeDisplay")
    void invokeResizeDisplay();
}