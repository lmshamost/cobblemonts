package com.cobblemon_ts.mod;

import com.cobblemon_ts.mod.network.PokebagClientCache;
import com.cobblemon_ts.mod.network.SyncCharmStatesS2CPacket;
import com.cobblemon_ts.mod.network.SyncRadarPermissionS2CPacket;

public class ClientEventHandler {
    public static void onClientDisconnect() {
        PokebagClientCache.clear();
        SyncCharmStatesS2CPacket.clearCache();
        SyncRadarPermissionS2CPacket.clearCache();
        CobblemonTS.LOGGER.info("[ClientCleanup] 缓存已清理");
    }
}