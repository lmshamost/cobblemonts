package com.cobblemon_ts.mod.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * [线性-BRIDGE] NeoForge IPayloadContext→Fabric桥接
 * 包装ServerPlayerEntity(服务端)或MinecraftClient(客户端)统一提供player()/enqueueWork()/client()
 */
public class IPayloadContext {
    private final ServerPlayerEntity player;
    private final MinecraftClient client;

    public IPayloadContext(ServerPlayerEntity player) {
        this.player = player;
        this.client = null;
    }

    public IPayloadContext(MinecraftClient client) {
        this.player = null;
        this.client = client;
    }

    public PlayerEntity player() {
        return player;
    }

    public MinecraftClient client() {
        return client;
    }

    public void enqueueWork(Runnable r) {
        if (player != null && player.getServer() != null) {
            player.getServer().execute(r);
        } else if (client != null) {
            client.execute(r);
        }
    }
}