// [线性-P02] 客户端请求服务端护符状态快照 (C→S)
package com.cobblemon_ts.mod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashMap;
import java.util.Map;

import com.cobblemon_ts.mod.CharmEventListener;
import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmSlotManager;
import com.cobblemon_ts.mod.CharmState;
import com.cobblemon_ts.mod.CharmType;

public record RequestCharmStatesPacket() implements CustomPacketPayload {
    public static final Type<RequestCharmStatesPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "request_charm_states"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestCharmStatesPacket> STREAM_CODEC =
            StreamCodec.unit(new RequestCharmStatesPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // [线性-P02a] 服务端处理：读取护符是否拥有（isAvailable=有护符在栏位），不关心开关状态
    public static void handleServerSide(RequestCharmStatesPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                CharmPlayerData data = CharmPlayerData.get(sp);
                if (data == null) return;
                CharmSlotManager manager = data.getCharmSlotManager();
                // [线性-P02b] isAvailable()=有护符(DISABLED以外) → TestBuild绿/红只读有无
                Map<String, Boolean> states = new LinkedHashMap<>();
                for (CharmType type : CharmType.values()) {
                    states.put(type.getItemId(), manager.getCharmState(type).isAvailable());
                }
                states.put("pokebag", manager.isPokebagOwned());
                PacketDistributor.sendToPlayer(sp, new SyncCharmStatesS2CPacket(states));
            }
        });
    }
}