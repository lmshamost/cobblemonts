package com.cobblemon_ts.mod.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

/** [线性-BRIDGE] 桥接NeoForge PacketDistributor */
public class PacketDistributor {
    public static void sendToServer(CustomPayload payload) {
        ClientPlayNetworking.send(payload);
    }
    public static void sendToPlayer(ServerPlayerEntity player, CustomPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}