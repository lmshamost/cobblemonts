// [线性-RADAR-PERM-PKT] 服务端→客户端 同步雷达权限 (S→C)
// 设计：登录时下发一次 + 命令变更时推送一次，客户端缓存布尔值，按键时读缓存，不持续检测。
package com.cobblemon_ts.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncRadarPermissionS2CPacket(boolean allowed) implements CustomPayload {
    public static final Id<SyncRadarPermissionS2CPacket> TYPE = new Id<>(
            Identifier.of("cobblemon_ts", "sync_radar_permission"));

    public static final PacketCodec<RegistryByteBuf, SyncRadarPermissionS2CPacket> STREAM_CODEC = new PacketCodec<>() {
        @Override
        public SyncRadarPermissionS2CPacket decode(RegistryByteBuf buf) {
            return new SyncRadarPermissionS2CPacket(buf.readBoolean());
        }

        @Override
        public void encode(RegistryByteBuf buf, SyncRadarPermissionS2CPacket packet) {
            buf.writeBoolean(packet.allowed());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    // [线性-RADAR-PERM-PKT] 客户端缓存：默认 true（单机/未收到服务端包前允许），收到服务端包后覆盖
    private static volatile boolean clientAllowed = true;

    public static boolean isClientAllowed() {
        return clientAllowed;
    }

    public static void handleClientSide(SyncRadarPermissionS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            clientAllowed = packet.allowed();
        });
    }

    // [线性-RADAR-PERM-PKT] 客户端断线清理：恢复默认
    public static void clearCache() {
        clientAllowed = true;
    }
}