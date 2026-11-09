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
    public static final int BIT_LUCKY     = 32;
    public static final int BIT_PLAYER    = 64;

    public final Map<Long, Byte> chunkRules = new HashMap<>();

    // [线性-R10] 幸运方块禁止：默认 false=不禁止；true=禁止开启后宝可梦生成
    public boolean banLuckyBlock = false;
    public boolean banShinyBlock = false;
    public boolean banLegendaryBlock = false;
    public boolean banUltraBlock = false;

    // [线性-R10] 玩家禁止开关：默认 true=按名单禁止；false=不检查玩家名单
    public boolean banPlayer = true;

    // [线性-R10] 玩家黑白名单：key=玩家名(小写), value=true=白名单(允许), false=黑名单(禁止扔)
    public final Map<String, Boolean> playerWhitelist = new HashMap<>();

    // [线性-R10] 幸运方块黑白名单：和 playerWhitelist 完全独立
    public final Map<String, Boolean> luckyPlayerWhitelist = new HashMap<>();

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

        // [线性-R10] 玩家名单数据保留持久化（服务端 isPlayerBanned 要用到）
        ListTag playerList = new ListTag();
        for (Map.Entry<String, Boolean> e : playerWhitelist.entrySet()) {
            CompoundTag pe = new CompoundTag();
            pe.putString("name", e.getKey());
            pe.putBoolean("whitelist", e.getValue());
            playerList.add(pe);
        }
        tag.put("players", playerList);

        ListTag luckyPlayerList = new ListTag();
        for (Map.Entry<String, Boolean> e : luckyPlayerWhitelist.entrySet()) {
            CompoundTag pe = new CompoundTag();
            pe.putString("name", e.getKey());
            pe.putBoolean("whitelist", e.getValue());
            luckyPlayerList.add(pe);
        }
        tag.put("luckyPlayers", luckyPlayerList);

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

        // [线性-持久化] 补读 playerWhitelist（之前漏了）
        ListTag playerList = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < playerList.size(); i++) {
            CompoundTag pe = playerList.getCompound(i);
            c.playerWhitelist.put(pe.getString("name"), pe.getBoolean("whitelist"));
        }

        ListTag luckyPlayerList = tag.getList("luckyPlayers", Tag.TAG_COMPOUND);
        for (int i = 0; i < luckyPlayerList.size(); i++) {
            CompoundTag pe = luckyPlayerList.getCompound(i);
            c.luckyPlayerWhitelist.put(pe.getString("name"), pe.getBoolean("whitelist"));
        }

        return c;
    }

    // [线性-R10] 玩家检查：返回 true=禁止(不允许扔), false=允许
    // excludeNameLower=owner名字，其白名单条目不参与判定统计
    public boolean isPlayerBanned(String playerNameLower, String excludeNameLower) {
        if (!banPlayer) return false;
        if (playerWhitelist.isEmpty()) return false;

        boolean hasWhite = false, hasBlack = false;
        int effectiveCount = 0;
        for (var e : playerWhitelist.entrySet()) {
            if (e.getKey().equals(excludeNameLower) && e.getValue()) continue;
            effectiveCount++;
            if (e.getValue()) hasWhite = true;
            else hasBlack = true;
        }

        if (effectiveCount == 0) return false;

        if (playerNameLower.equals(excludeNameLower)) {
            Boolean ownerMode = playerWhitelist.get(playerNameLower);
            if (ownerMode != null && !ownerMode) return true;
            return false;
        }

        Boolean mode = playerWhitelist.get(playerNameLower);
        if (mode != null) {
            return !mode;
        }

        return hasWhite && !hasBlack;
    }

    // [线性-R10] 幸运方块玩家检查：逻辑同 isPlayerBanned，用 luckyPlayerWhitelist
    public boolean isLuckyPlayerBanned(String playerNameLower, String excludeNameLower) {
        if (!banLuckyBlock) return false;
        if (luckyPlayerWhitelist.isEmpty()) return false;

        boolean hasWhite = false, hasBlack = false;
        int effectiveCount = 0;
        for (var e : luckyPlayerWhitelist.entrySet()) {
            if (e.getKey().equals(excludeNameLower) && e.getValue()) continue;
            effectiveCount++;
            if (e.getValue()) hasWhite = true;
            else hasBlack = true;
        }

        if (effectiveCount == 0) return false;

        if (playerNameLower.equals(excludeNameLower)) {
            Boolean ownerMode = luckyPlayerWhitelist.get(playerNameLower);
            if (ownerMode != null && !ownerMode) return true;
            return false;
        }

        Boolean mode = luckyPlayerWhitelist.get(playerNameLower);
        if (mode != null) return !mode;

        return hasWhite && !hasBlack;
    }
}