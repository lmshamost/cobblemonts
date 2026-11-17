// [线性-RDR1] 雷达权限同步包 S→C：服务端权威结果下发，客户端缓存布尔值，按键/渲染直接读缓存
package com.cobblemon_ts.mod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncRadarPermissionS2CPacket(boolean allowed) implements CustomPacketPayload {

    public static final Type<SyncRadarPermissionS2CPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("cobblemon_ts", "sync_radar_permission"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRadarPermissionS2CPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, SyncRadarPermissionS2CPacket::allowed,
                    SyncRadarPermissionS2CPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // [线性-RDR1a] 客户端缓存：默认允许，登录/命令变更时服务端推送覆盖
    private static volatile boolean clientAllowed = true;

    public static boolean isClientAllowed() {
        return clientAllowed;
    }

    public static void handleClientSide(SyncRadarPermissionS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> clientAllowed = packet.allowed());
    }

    // [线性-RDR1b] 断线/切服重置为默认允许
    public static void clearCache() {
        clientAllowed = true;
    }
}