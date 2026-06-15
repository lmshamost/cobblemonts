package com.cobblemon_ts.mod.network;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.starter.StarterHandler;
import com.cobblemon.mod.common.api.storage.player.GeneralPlayerData;
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreManager;
import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// [线性-L1] 测试物品GUI→服务端：请求打开初始宝可梦选择界面
public record TestBuildOpenStarterPacket() implements CustomPacketPayload {
    public static final Type<TestBuildOpenStarterPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "test_build_open_starter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TestBuildOpenStarterPacket> STREAM_CODEC =
            StreamCodec.unit(new TestBuildOpenStarterPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServerSide(TestBuildOpenStarterPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                try {
                    // [线性-L1] 重置初始宝可梦检测标志
                    PlayerInstancedDataStoreManager dataManager = Cobblemon.INSTANCE.getPlayerDataManager();
                    if (dataManager != null) {
                        GeneralPlayerData playerData = dataManager.getGenericData(serverPlayer);
                        if (playerData != null) {
                            playerData.setStarterSelected(false);
                            playerData.setStarterPrompted(false);
                            playerData.setStarterLocked(false);
                        }
                    }
                    // [线性-L1] 打开初始选择界面
                    StarterHandler handler = Cobblemon.INSTANCE.getStarterHandler();
                    if (handler != null) {
                        handler.requestStarterChoice(serverPlayer);
                        serverPlayer.sendSystemMessage(Component.literal("已打开初始宝可梦选择界面"));
                    } else {
                        serverPlayer.sendSystemMessage(Component.literal("无法获取 StarterHandler"));
                    }
                } catch (Throwable t) {
                    serverPlayer.sendSystemMessage(Component.literal("打开初始宝可梦界面失败: " + t.getMessage()));
                    CobblemonTS.LOGGER.error("测试物品打开初始宝可梦界面失败", t);
                }
            }
        });
    }
}