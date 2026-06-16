package com.cobblemon_ts.mod.mixin.cobblemon;

import com.cobblemon.mod.common.item.PokeBallItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// [线性-B05ze] 暴露PokeBallItem.throwPokeBall()私有方法，供网络包直接调用投掷精灵球
@Mixin(PokeBallItem.class)
public interface PokeBallItemInvoker {

    @Invoker("throwPokeBall")
    void invokeThrowPokeBall(Level world, ServerPlayer player);
}