package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CharmEventListener;
import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmSlotManager;
import com.cobblemon_ts.mod.CharmState;
import com.cobblemon_ts.mod.CharmType;
import com.cobblemon_ts.mod.PokebagManager;
import com.cobblemon_ts.mod.SecurityHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClearAllCharmsPacket() implements CustomPacketPayload {
    public static final Type<ClearAllCharmsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "clear_all_charms"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClearAllCharmsPacket> STREAM_CODEC =
            StreamCodec.unit(new ClearAllCharmsPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServerSide(ClearAllCharmsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                // [线性-SEC] 权限校验：仅创造模式或OP
                if (!SecurityHelper.isTrusted(sp)) {
                    return;
                }
                CharmPlayerData playerData = CharmPlayerData.get(sp);
                if (playerData == null) return;

                CharmSlotManager manager = playerData.getCharmSlotManager();
                for (CharmType type : CharmType.values()) {
                    manager.setCharmState(type, CharmState.DISABLED);
                }
                manager.setPokebagOwned(false);

                sp.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.coblemon_ts.all_charms_cleared"));
            }
        });
    }
}