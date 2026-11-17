package com.cobblemon_ts.mod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.HashMap;
import java.util.Map;

/** [线性-EVT] 玩家数据事件处理 — Fabric版本 */
public class CharmEventListener {
    private static final Map<String, CharmPlayerData> playerDataMap = new HashMap<>();

    /** [线性-E01] 玩家加入时加载数据 */
    public static void onPlayerLogin(PlayerEntity player) {
        loadPlayerData(player);

        if (!CharmConfig.START_ITEMS_ENABLED.get()) return;

        CharmPlayerData data = getPlayerData(player);
        if (data == null) return;

        CharmSlotManager manager = data.getCharmSlotManager();
        boolean changed = false;

        for (String itemId : CharmConfig.START_WITH_ITEMS.get()) {
            if ("pokebag".equals(itemId)) {
                if (!manager.isPokebagOwned()) {
                    manager.setPokebagOwned(true);
                    changed = true;
                }
            } else {
                for (CharmType type : CharmType.values()) {
                    if (type.getItemId().equals(itemId)) {
                        if (manager.getCharmState(type) == CharmState.DISABLED) {
                            manager.setCharmState(type, CharmState.AVAILABLE);
                            changed = true;
                        }
                        break;
                    }
                }
            }
        }

        if (changed) {
            player.sendMessage(net.minecraft.text.Text.translatable("message.cobblemon_ts.start_items_granted"));
        }
    }

    /** [线性-E02] 玩家登出时落盘 */
    public static void onPlayerLogout(PlayerEntity player) {
        CharmPlayerData data = playerDataMap.get(player.getUuidAsString());
        if (data != null) {
            data.getPokebagManager().forceFlush();
        }
        savePlayerData(player);
        playerDataMap.remove(player.getUuidAsString());
    }

    /** [线性-E03] 玩家死亡时继承数据 */
    public static void onPlayerClone(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        CharmPlayerData oldData = playerDataMap.get(oldPlayer.getUuidAsString());
        if (oldData != null) {
            CharmPlayerData newData = new CharmPlayerData();
            newData.deserializeNBT(oldData.serializeNBT(newPlayer.getRegistryManager()), newPlayer.getRegistryManager());
            newData.setPlayer(newPlayer);
            playerDataMap.put(newPlayer.getUuidAsString(), newData);
            NbtCompound persistent = PersistentDataHelper.getPersistentData(newPlayer);
            persistent.put("CharmPlayerData", newData.serializeNBT(newPlayer.getRegistryManager()));
        }
    }

    /** [线性-E04] 强制保存 */
    public static void savePlayerDataExplicit(PlayerEntity player) {
        savePlayerData(player);
    }

    /** [线性-E05] 服务端停止时清理 */
    public static void onServerStopping() {
        playerDataMap.clear();
        CobblemonTS.LOGGER.debug("[Cleanup] 服务端停止，已清空playerDataMap");
    }

    public static CharmPlayerData getPlayerData(PlayerEntity player) {
        return playerDataMap.computeIfAbsent(player.getUuidAsString(), k -> {
            CharmPlayerData data = new CharmPlayerData();
            loadPlayerDataFromNBT(player, data);
            data.setPlayer(player);
            return data;
        });
    }

    private static void loadPlayerData(PlayerEntity player) {
        CharmPlayerData data = new CharmPlayerData();
        loadPlayerDataFromNBT(player, data);
        data.setPlayer(player);
        playerDataMap.put(player.getUuidAsString(), data);
    }

    private static void loadPlayerDataFromNBT(PlayerEntity player, CharmPlayerData data) {
        NbtCompound persistent = PersistentDataHelper.getPersistentData(player);
        if (persistent.contains("CharmPlayerData")) {
            data.deserializeNBT(persistent.getCompound("CharmPlayerData"), player.getRegistryManager());
        }
    }

    private static void savePlayerData(PlayerEntity player) {
        CharmPlayerData data = playerDataMap.get(player.getUuidAsString());
        if (data != null) {
            NbtCompound persistent = PersistentDataHelper.getPersistentData(player);
            persistent.put("CharmPlayerData", data.serializeNBT(player.getRegistryManager()));
        }
    }

    /** [线性-E06] 每tick检查在线玩家的道具袋脏标记 */
    public static void onServerTick(Iterable<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            CharmPlayerData data = playerDataMap.get(player.getUuidAsString());
            if (data != null) {
                data.getPokebagManager().tryFlush();
            }
        }
    }
}