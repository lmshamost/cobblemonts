package com.cobblemon_ts.mod.mixin.cobblemon;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 解决 Cobblemon 精灵栏 UI 不刷新的核心问题。
 *
 * Cobblemon 的 ClientStorageManager 维护两个数据结构：
 *  - partyStores (Map<UUID, PlayerPartyStore>) - 每个玩家的队伍数据
 *  - this.party (PlayerPartyStore) - 当前客户端"选中"的队伍
 *
 * 服务端通过 sendTo() 发送 InitializePartyPacket(false, ...) 时，
 * 客户端只更新 partyStores，但 this.party 仍指向旧对象。
 * Cobblemon 原生精灵栏 overlay 读的是 this.party，导致不刷新。
 *
 * 此 Mixin hook getParty()，让它从 partyStores 按当前玩家 UUID 取最新数据。
 */
@Mixin(targets = "com.cobblemon.mod.common.client.storage.ClientStorageManager", remap = false)
public abstract class ClientStorageManagerMixin {

    @Inject(method = "getParty", at = @At("HEAD"), remap = false, cancellable = true)
    private void cobblemon_ts$getPartyFromStores(CallbackInfoReturnable<Object> cir) {
        try {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            // 反射获取 partyStores 字段
            java.lang.reflect.Field psField = this.getClass().getDeclaredField("partyStores");
            psField.setAccessible(true);
            java.util.Map<?, ?> partyStores = (java.util.Map<?, ?>) psField.get(this);
            if (partyStores == null) return;

            Object fromStores = partyStores.get(player.getUUID());
            if (fromStores != null) {
                // 同步 this.party 字段
                try {
                    java.lang.reflect.Field pField = this.getClass().getDeclaredField("party");
                    pField.setAccessible(true);
                    pField.set(this, fromStores);
                } catch (Throwable ignored) {}
                cir.setReturnValue(fromStores);
                cir.cancel();
            }
        } catch (Throwable ignored) {}
    }
}