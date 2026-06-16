package com.cobblemon_ts.mod.network;

import java.util.ArrayList;
import java.util.List;

// [线性-B05zf] Pokebag客户端缓存：存储服务端同步过来的Pokebag物品列表
// 由SyncPokebagContentsPacket.handleClientSide()更新
public class PokebagClientCache {

    private static final List<SyncPokebagContentsPacket.ItemEntry> cachedItems = new ArrayList<>();

    private PokebagClientCache() {}

    public static void update(List<SyncPokebagContentsPacket.ItemEntry> items) {
        synchronized (cachedItems) {
            cachedItems.clear();
            cachedItems.addAll(items);
        }
    }

    public static List<SyncPokebagContentsPacket.ItemEntry> getItems() {
        synchronized (cachedItems) {
            return new ArrayList<>(cachedItems);
        }
    }

    public static void clear() {
        synchronized (cachedItems) {
            cachedItems.clear();
        }
    }
}