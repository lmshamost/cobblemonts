package com.cobblemon_ts.mod;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.starter.StarterHandler;
import com.cobblemon.mod.common.api.storage.player.GeneralPlayerData;
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 测试用物品 - 用于快速确认当前运行的 mod 是否为最新构建版本。
 * 右击可以打开初始宝可梦选择界面，用于测试初始精灵的闪光和满 IV 功能。
 *
 * 关键：绕过 Cobblemon 的"已领过初始宝可梦"检查。
 * Cobblemon 通过 GeneralPlayerData 的三个字段检测：
 *   - starterSelected: 是否已选择初始宝可梦
 *   - starterPrompted: 是否已提示过
 *   - starterLocked: 是否已锁定
 * 在调用 requestStarterChoice 之前，我们将这些字段重置为 false。
 */
public class TestBuildItem extends Item {

    public TestBuildItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.literal("构建时间: " + BuildInfo.BUILD_TIME));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("右击打开初始宝可梦选择界面"));
        tooltip.add(Component.literal("(用于测试闪光和满 IV 功能)"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            if (player instanceof ServerPlayer serverPlayer) {
                try {
                    // 1. 重置玩家的初始宝可梦检测标志（让 Cobblemon 认为玩家还没领过）
                    //    不需要 saveSingle — Cobblemon 自己会同步数据
                    PlayerInstancedDataStoreManager dataManager = Cobblemon.INSTANCE.getPlayerDataManager();
                    if (dataManager != null) {
                        GeneralPlayerData playerData = dataManager.getGenericData(serverPlayer);
                        if (playerData != null) {
                            playerData.setStarterSelected(false);
                            playerData.setStarterPrompted(false);
                            playerData.setStarterLocked(false);
                        }
                    }

                    // 2. 通过 Cobblemon 的 StarterHandler 打开初始选择界面
                    StarterHandler handler = Cobblemon.INSTANCE.getStarterHandler();
                    if (handler != null) {
                        handler.requestStarterChoice(serverPlayer);
                        player.sendSystemMessage(Component.literal("已打开初始宝可梦选择界面"));
                    } else {
                        player.sendSystemMessage(Component.literal("无法获取 StarterHandler"));
                    }
                } catch (Throwable t) {
                    player.sendSystemMessage(Component.literal("打开初始宝可梦界面失败: " + t.getMessage()));
                    CobblemonTS.LOGGER.error("测试物品打开初始宝可梦界面失败", t);
                }
            }
        }

        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }
}