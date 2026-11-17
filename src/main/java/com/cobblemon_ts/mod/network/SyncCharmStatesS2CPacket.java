// [线性-P03] 服务端→客户端同步护符状态快照 (S→C)
// [线性-P03-NEW] 无缓存设计：每次屏幕打开发送请求，响应到达后一次性消费，不保留静态缓存
package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.pokemoneditor.PacketCodecHelper;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public record SyncCharmStatesS2CPacket(Map<String, Boolean> states) implements CustomPayload {
    public static final Id<SyncCharmStatesS2CPacket> TYPE = new Id<>(
            Identifier.of("cobblemon_ts", "sync_charm_states"));

    public static final PacketCodec<RegistryByteBuf, SyncCharmStatesS2CPacket> STREAM_CODEC =
            PacketCodecHelper.composite(
                    PacketCodecs.map(
                            LinkedHashMap::new,
                            PacketCodecs.STRING,
                            PacketCodecs.BOOL
                    ),
                    SyncCharmStatesS2CPacket::states,
                    SyncCharmStatesS2CPacket::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    // [线性-P03a] volatile 单次消费引用：屏幕 init 时 resetForNewRequest() 置null，
    //            服务端响应到达后写入，屏幕 tick() 读取一次后不再保留
    private static volatile Map<String, Boolean> latestStates = null;

    // [线性-P03b] 屏幕调用：发起新请求前清空旧数据
    public static void resetForNewRequest() {
        latestStates = null;
    }

    // [线性-P03c] 屏幕 tick() 轮询：非阻塞读取，null = 还没响应
    public static Map<String, Boolean> pollLatestStates() {
        return latestStates;
    }

    public static void handleClientSide(SyncCharmStatesS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            latestStates = packet.states;
        });
    }

    // [线性-P03d] 客户端断线清理：清空volatile引用
    public static void clearCache() {
        latestStates = null;
    }
}