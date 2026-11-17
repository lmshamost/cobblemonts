// [线性-P02] 客户端请求服务端护符状态快照 (C→S)
package com.cobblemon_ts.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.LinkedHashMap;
import java.util.Map;

import com.cobblemon_ts.mod.CharmEventListener;
import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmSlotManager;
import com.cobblemon_ts.mod.CharmState;
import com.cobblemon_ts.mod.CharmType;

public record RequestCharmStatesPacket() implements CustomPayload {
    public static final Id<RequestCharmStatesPacket> TYPE = new Id<>(
            Identifier.of("cobblemon_ts", "request_charm_states"));

    public static final PacketCodec<RegistryByteBuf, RequestCharmStatesPacket> STREAM_CODEC =
            PacketCodec.unit(new RequestCharmStatesPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    // [线性-P02a] 服务端处理：读取护符是否拥有（isAvailable=有护符在栏位），不关心开关状态
    public static void handleServerSide(RequestCharmStatesPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayerEntity serverPlayer) {
                CharmPlayerData data = CharmPlayerData.get(serverPlayer);
                if (data == null) return;
                CharmSlotManager manager = data.getCharmSlotManager();
                // [线性-P02b] isAvailable()=有护符(DISABLED以外) → TestBuild绿/红只读有无
                Map<String, Boolean> states = new LinkedHashMap<>();
                for (CharmType type : CharmType.values()) {
                    states.put(type.getItemId(), manager.getCharmState(type).isAvailable());
                }
                states.put("pokebag", manager.isPokebagOwned());
                PacketDistributor.sendToPlayer(serverPlayer, new SyncCharmStatesS2CPacket(states));
            }
        });
    }
}