package com.cobblemon_ts.mod;

import com.cobblemon_ts.mod.gui.TestBuildScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * 测试用物品 - 用于快速确认当前运行的 mod 是否为最新构建版本。
 * 右击弹出功能选择GUI：
 *   1. 打开初始宝可梦选择界面（用于测试闪光和满 IV 功能）
 *   2. 切换精灵栏/护符栏亮暗主题
 *
 * 初始宝可梦选择会绕过 Cobblemon 的"已领过初始宝可梦"检查。
 */
public class TestBuildItem extends Item {

    public TestBuildItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("coblemon_ts.testbuild.tooltip.build_time", BuildInfo.BUILD_TIME));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("coblemon_ts.testbuild.tooltip.open_menu"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // [线性-L1] 客户端打开功能选择GUI
        if (level.isClientSide) {
            openTestScreen();
        }

        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }

    @OnlyIn(Dist.CLIENT)
    private static void openTestScreen() {
        Minecraft.getInstance().setScreen(new TestBuildScreen());
    }
}