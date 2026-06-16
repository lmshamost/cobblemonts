package com.cobblemon_ts.mod.mixin;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// [修复-M01] 用@Redirect拦截renderWidget中isBordered()调用，取消vanilla边框精灵图渲染
// renderWidget中isBordered()只出现一次（用于绘制边框），文字定位直接用bordered字段
// 保持bordered=true → 文字Y=getY()+(h-8)/2自动居中，自定义背景由drawInputFieldBackgrounds提供
@Mixin(EditBox.class)
public class EditBoxTextPositionMixin {
    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;isBordered()Z"))
    private boolean cancelBorderSprite(EditBox instance) {
        // 只在背包界面的输入框取消边框，避免影响其他界面的输入框
        // 检查当前屏幕是否是背包界面
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen) {
            return false;
        }
        // 其他界面的输入框保持原有边框
        return instance.isBordered();
    }
}