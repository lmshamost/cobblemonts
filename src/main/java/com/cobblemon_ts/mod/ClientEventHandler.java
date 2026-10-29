// [线性-P03] 客户端事件：断线/世界切换时清理缓存
package com.cobblemon_ts.mod;

import com.cobblemon_ts.mod.network.PokebagClientCache;
import com.cobblemon_ts.mod.network.SyncCharmStatesS2CPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = CobblemonTS.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    // [线性-P03a] 客户端断开连接 → 清理缓存
    @SubscribeEvent
    public static void onClientDisconnect(
            net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        PokebagClientCache.clear();
        SyncCharmStatesS2CPacket.clearCache();
        CobblemonTS.LOGGER.debug("[ClientCleanup] 缓存已清理");
    }
}