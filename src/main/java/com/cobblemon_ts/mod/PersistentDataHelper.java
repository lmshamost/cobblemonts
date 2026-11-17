package com.cobblemon_ts.mod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** [线性-PDATA] Fabric替代NeoForge PlayerEntity.getPersistentData() */
public class PersistentDataHelper {
    private static final Map<UUID, NbtCompound> persistentDataMap = new ConcurrentHashMap<>();

    public static NbtCompound getPersistentData(PlayerEntity player) {
        return persistentDataMap.computeIfAbsent(player.getUuid(), k -> new NbtCompound());
    }

    /** 玩家退出时调用，将数据写入玩家NBT */
    public static void saveToPlayer(PlayerEntity player) {
        NbtCompound data = persistentDataMap.get(player.getUuid());
        if (data != null && !data.isEmpty()) {
            NbtCompound playerNbt = player.writeNbt(new NbtCompound());
            playerNbt.put("CobblemonTS_Persistent", data);
            // 注意: Fabric需要mixin来实际持久化此数据
            // 临时方案：保存在内存中，待后续添加PlayerDataMixin
        }
    }

    /** 玩家加入时调用，从NBT恢复数据 */
    public static void loadFromPlayer(PlayerEntity player) {
        // 临时方案：从内存读取
        // 待PlayerDataMixin添加后改为从NBT读取
    }

    /** 服务端停止时清理 */
    public static void clear() {
        persistentDataMap.clear();
    }
}