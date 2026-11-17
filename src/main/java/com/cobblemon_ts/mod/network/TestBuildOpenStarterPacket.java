package com.cobblemon_ts.mod.network;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.starter.StarterHandler;
import com.cobblemon.mod.common.api.storage.player.GeneralPlayerData;
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreManager;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.SecurityHelper;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.text.Text;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;

// [线性-L1] 测试物品GUI→服务端：请求打开初始宝可梦选择界面
public record TestBuildOpenStarterPacket() implements CustomPayload {
    public static final Id<TestBuildOpenStarterPacket> TYPE = new Id<>(
            Identifier.of("cobblemon_ts", "test_build_open_starter"));

    public static final PacketCodec<RegistryByteBuf, TestBuildOpenStarterPacket> STREAM_CODEC =
            PacketCodec.unit(new TestBuildOpenStarterPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    public static void handleServerSide(TestBuildOpenStarterPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayerEntity player) {
                // [线性-SEC] 权限校验：仅创造模式或OP
                if (!SecurityHelper.isTrusted(player)) {
                    CobblemonTS.LOGGER.warn("[Security] Unauthorized TestBuildOpenStarter from {}",
                            player.getName().getString());
                    return;
                }
                try {
                    // [线性-L1] 重置初始宝可梦检测标志
                    PlayerInstancedDataStoreManager dataManager = Cobblemon.INSTANCE.getPlayerDataManager();
                    if (dataManager != null) {
                        GeneralPlayerData playerData = dataManager.getGenericData(player);
                        if (playerData != null) {
                            playerData.setStarterSelected(false);
                            playerData.setStarterPrompted(false);
                            playerData.setStarterLocked(false);
                        }
                    }
                    // [线性-L1] 打开初始选择界面
                    StarterHandler handler = Cobblemon.INSTANCE.getStarterHandler();
                    if (handler != null) {
                        handler.requestStarterChoice(player);
                        player.sendMessage(Text.literal("已打开初始宝可梦选择界面"));
                    } else {
                        player.sendMessage(Text.literal("无法获取 StarterHandler"));
                    }
                } catch (Throwable t) {
                    player.sendMessage(Text.literal("打开初始宝可梦界面失败: " + t.getMessage()));
                    CobblemonTS.LOGGER.error("测试物品打开初始宝可梦界面失败", t);
                }
            }
        });
    }
}