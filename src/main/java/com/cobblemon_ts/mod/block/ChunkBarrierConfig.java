package com.cobblemon_ts.mod.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

public class ChunkBarrierConfig {

    public static final int BIT_POKEMON   = 1;
    public static final int BIT_NPC       = 2;
    public static final int BIT_HOSTILE   = 4;
    public static final int BIT_PASSIVE   = 8;
    public static final int BIT_FORCELOAD = 16;

    public final Map<Long, Byte> chunkRules = new HashMap<>();

    public static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public int getFlags(int chunkX, int chunkZ) {
        return chunkRules.getOrDefault(chunkKey(chunkX, chunkZ), (byte) 0) & 0xFF;
    }

    public void setFlags(int chunkX, int chunkZ, int flags) {
        long key = chunkKey(chunkX, chunkZ);
        if (flags == 0) {
            chunkRules.remove(key);
        } else {
            chunkRules.put(key, (byte) (flags & 0xFF));
        }
    }

    public boolean hasAnyBan(int chunkX, int chunkZ) {
        int f = getFlags(chunkX, chunkZ);
        return (f & (BIT_POKEMON | BIT_NPC | BIT_HOSTILE | BIT_PASSIVE)) != 0;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<Long, Byte> e : chunkRules.entrySet()) {
            CompoundTag entry = new CompoundTag();
            long k = e.getKey();
            entry.putInt("cx", (int) (k >> 32));
            entry.putInt("cz", (int) k);
            entry.putByte("flags", e.getValue());
            list.add(entry);
        }
        tag.put("rules", list);
        return tag;
    }

    public static ChunkBarrierConfig load(CompoundTag tag) {
        ChunkBarrierConfig c = new ChunkBarrierConfig();
        if (tag == null) return c;
        ListTag list = tag.getList("rules", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int cx = entry.getInt("cx");
            int cz = entry.getInt("cz");
            byte flags = entry.getByte("flags");
            c.chunkRules.put(chunkKey(cx, cz), flags);
        }
        return c;
    }
}