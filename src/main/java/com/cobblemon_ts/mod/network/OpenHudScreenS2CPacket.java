// [线性-CMD-HUD-PKT] /cts hud 服务端→客户端打开HUD设置屏 S→C 空payload
// [线性-CMD-HUD-PKT-NOTE] 客户端命令 cts 与服务端命令 cts 同名会导致Fabric命令树合并丢失子节点，
//                       故 hud 改走服务端命令+网络包通知客户端打开GUI，规避命令树冲突。
package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.gui.HudSettingsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenHudScreenS2CPacket() implements CustomPayload {

    public static final Id<OpenHudScreenS2CPacket> TYPE = new Id<>(
            Identifier.of("cobblemon_ts", "open_hud_screen"));

    public static final PacketCodec<RegistryByteBuf, OpenHudScreenS2CPacket> STREAM_CODEC = new PacketCodec<>() {
        @Override
        public OpenHudScreenS2CPacket decode(RegistryByteBuf buf) {
            return new OpenHudScreenS2CPacket();
        }

        @Override
        public void encode(RegistryByteBuf buf, OpenHudScreenS2CPacket packet) {
            // 空payload：无字段
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    // [线性-CMD-HUD-PKT] 客户端收到后打开HUD设置屏
    public static void handleClientSide(OpenHudScreenS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            MinecraftClient.getInstance().setScreen(new HudSettingsScreen());
        });
    }
}