package com.cobblemon_ts.mod.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// [线性-B05zf] Pokebag客户端缓存：存储服务端同步过来的Pokebag物品列表
// 由SyncPokebagContentsPacket.handleClientSide()更新
public class PokebagClientCache {

    // [线性-O02] 使用不可变空列表避免GC，缓存引用避免每帧创建新ArrayList
    private static final List<SyncPokebagContentsPacket.ItemEntry> EMPTY_LIST = Collections.emptyList();
    private static List<SyncPokebagContentsPacket.ItemEntry> cachedItems = EMPTY_LIST;

    private PokebagClientCache() {}

    public static void update(List<SyncPokebagContentsPacket.ItemEntry> items) {
        // [线性-O02] 直接替换引用，不clear/addAll，减少同步开销
        if (items == null || items.isEmpty()) {
            cachedItems = EMPTY_LIST;
        } else {
            cachedItems = List.copyOf(items); // 不可变副本，线程安全
        }
    }

    // [线性-O02] 直接返回缓存引用，不创建新ArrayList
    // 调用者只读不修改，避免每帧render时的GC压力
    public static List<SyncPokebagContentsPacket.ItemEntry> getItems() {
        return cachedItems;
    }

    public static void clear() {
        cachedItems = EMPTY_LIST;
    }
}