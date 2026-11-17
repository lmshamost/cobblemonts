package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CharmEventListener;
import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmSlotManager;
import com.cobblemon_ts.mod.CharmState;
import com.cobblemon_ts.mod.CharmType;
import com.cobblemon_ts.mod.PokebagManager;
import com.cobblemon_ts.mod.SecurityHelper;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;

public record ClearAllCharmsPacket() implements CustomPayload {
    public static final Id<ClearAllCharmsPacket> TYPE = new Id<>(
            Identifier.of("cobblemon_ts", "clear_all_charms"));

    public static final PacketCodec<RegistryByteBuf, ClearAllCharmsPacket> STREAM_CODEC =
            PacketCodec.unit(new ClearAllCharmsPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    public static void handleServerSide(ClearAllCharmsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayerEntity serverPlayer) {
                if (!SecurityHelper.isTrusted(serverPlayer)) {
                    return;
                }
                CharmPlayerData playerData = CharmPlayerData.get(serverPlayer);
                if (playerData == null) return;

                CharmSlotManager manager = playerData.getCharmSlotManager();
                for (CharmType type : CharmType.values()) {
                    manager.setCharmState(type, CharmState.DISABLED);
                }
                manager.setPokebagOwned(false);

                serverPlayer.sendMessage(net.minecraft.text.Text.translatable("message.cobblemon_ts.all_charms_cleared"));
            }
        });
    }
}