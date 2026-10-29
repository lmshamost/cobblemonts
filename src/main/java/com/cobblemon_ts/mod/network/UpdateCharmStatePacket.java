// [线性-P01] TestBuild护符开关按钮：客户端→服务端同步护符状态
package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CharmEventListener;
import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmSlotManager;
import com.cobblemon_ts.mod.CharmState;
import com.cobblemon_ts.mod.CharmType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateCharmStatePacket(String charmId, boolean owned) implements CustomPacketPayload {
    public static final Type<UpdateCharmStatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "update_charm_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateCharmStatePacket> STREAM_CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8,
                    UpdateCharmStatePacket::charmId,
                    net.minecraft.network.codec.ByteBufCodecs.BOOL,
                    UpdateCharmStatePacket::owned,
                    UpdateCharmStatePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // [线性-P01a] 服务端处理：更新CharmSlotManager中的护符状态
    public static void handleServerSide(UpdateCharmStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                CharmPlayerData playerData = CharmPlayerData.get(sp);
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