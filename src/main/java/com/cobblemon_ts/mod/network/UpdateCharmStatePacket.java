// [线性-P01] TestBuild护符开关按钮：客户端→服务端同步护符状态
package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CharmEventListener;
import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmSlotManager;
import com.cobblemon_ts.mod.pokemoneditor.PacketCodecHelper;
import com.cobblemon_ts.mod.CharmState;
import com.cobblemon_ts.mod.CharmType;
import com.cobblemon_ts.mod.SecurityHelper;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;

public record UpdateCharmStatePacket(String charmId, boolean owned) implements CustomPayload {
    public static final Id<UpdateCharmStatePacket> TYPE = new Id<>(
            Identifier.of("cobblemon_ts", "update_charm_state"));

    @SuppressWarnings("unchecked")
    public static final PacketCodec<RegistryByteBuf, UpdateCharmStatePacket> STREAM_CODEC = new PacketCodec<>() {
        @Override
        public UpdateCharmStatePacket decode(RegistryByteBuf buf) {
            return new UpdateCharmStatePacket(
                    net.minecraft.network.codec.PacketCodecs.STRING.decode(buf),
                    net.minecraft.network.codec.PacketCodecs.BOOL.decode(buf)
            );
        }
        @Override
        public void encode(RegistryByteBuf buf, UpdateCharmStatePacket packet) {
            net.minecraft.network.codec.PacketCodecs.STRING.encode(buf, packet.charmId());
            net.minecraft.network.codec.PacketCodecs.BOOL.encode(buf, packet.owned());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    // [线性-P01a] 服务端处理：更新CharmSlotManager中的护符有无（owned=true→ACTIVE, false→DISABLED）
    public static void handleServerSide(UpdateCharmStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayerEntity serverPlayer) {
                // [线性-SEC] 权限校验：仅创造模式或OP
                if (!SecurityHelper.isTrusted(serverPlayer)) {
                    return;
                }
                CharmPlayerData playerData = CharmPlayerData.get(serverPlayer);
                if (playerData == null) return;
                CharmSlotManager manager = playerData.getCharmSlotManager();

                switch (packet.charmId) {
                    // [线性-P01b] 五种基础护符
                    case "shiny_charm" -> manager.setCharmState(CharmType.SHINY,
                            packet.owned ? CharmState.ACTIVE : CharmState.DISABLED);
                    case "oval_charm" -> manager.setCharmState(CharmType.OVAL,
                            packet.owned ? CharmState.ACTIVE : CharmState.DISABLED);
                    case "exp_charm" -> manager.setCharmState(CharmType.EXP,
                            packet.owned ? CharmState.ACTIVE : CharmState.DISABLED);
                    case "catching_charm" -> manager.setCharmState(CharmType.CATCHING,
                            packet.owned ? CharmState.ACTIVE : CharmState.DISABLED);
                    case "exp_all" -> manager.setCharmState(CharmType.EXP_ALL,
                            packet.owned ? CharmState.ACTIVE : CharmState.DISABLED);
                    // [线性-P01c] 道具袋
                    case "pokebag" -> manager.setPokebagOwned(packet.owned);
                }
            }
        });
    }
}