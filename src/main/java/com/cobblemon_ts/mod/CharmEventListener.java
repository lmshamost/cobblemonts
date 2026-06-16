package com.cobblemon_ts.mod;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = CobblemonTS.MODID)
public class CharmEventListener {
    private static final Map<String, CharmPlayerData> playerDataMap = new HashMap<>();
    
    @SubscribeEvent
    public static void onPlayerLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        // [线性-B02] 始终加载持久化数据，不受START_ITEMS_ENABLED开关影响
        loadPlayerData(player);

        // 开局道具开关：默认关闭，需配置启用才发放初始物品
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
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.coblemon_ts.start_items_granted"));
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        savePlayerData(player);
        playerDataMap.remove(player.getStringUUID());
    }
    
    @SubscribeEvent
    public static void onPlayerDeath(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            Player original = event.getOriginal();
            Player player = event.getEntity();
            
            CharmPlayerData oldData = playerDataMap.get(original.getStringUUID());
            if (oldData != null) {
                CharmPlayerData newData = new CharmPlayerData();
                newData.deserializeNBT(oldData.serializeNBT(player.registryAccess()), player.registryAccess());
                // [线性-B02] 绑定新Player，死亡后数据继承并自动保存
                newData.setPlayer(player);
                playerDataMap.put(player.getStringUUID(), newData);
                // [线性-B02] 死亡后立即落盘，防止后续无状态变更导致数据未持久化
                CompoundTag tag = player.getPersistentData();
                tag.put("CharmPlayerData", newData.serializeNBT(player.registryAccess()));
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerSave(net.neoforged.neoforge.event.entity.player.PlayerEvent.SaveToFile event) {
        savePlayerData(event.getEntity());
    }
    
    public static CharmPlayerData getPlayerData(Player player) {
        return playerDataMap.computeIfAbsent(player.getStringUUID(), k -> {
            CharmPlayerData data = new CharmPlayerData();
            loadPlayerDataFromNBT(player, data);
            // [线性-B02] 绑定Player，开启自动保存
            data.setPlayer(player);
            return data;
        });
    }
    
    private static void loadPlayerData(Player player) {
        CharmPlayerData data = new CharmPlayerData();
        loadPlayerDataFromNBT(player, data);
        // [线性-B02] 绑定Player，开启自动保存
        data.setPlayer(player);
        playerDataMap.put(player.getStringUUID(), data);
    }
    
    private static void loadPlayerDataFromNBT(Player player, CharmPlayerData data) {
        CompoundTag tag = player.getPersistentData();
        if (tag.contains("CharmPlayerData")) {
            data.deserializeNBT(tag.getCompound("CharmPlayerData"), player.registryAccess());
        }
    }
    
    private static void savePlayerData(Player player) {
        CharmPlayerData data = playerDataMap.get(player.getStringUUID());
        if (data != null) {
            CompoundTag tag = player.getPersistentData();
            tag.put("CharmPlayerData", data.serializeNBT(player.registryAccess()));
        }
    }
}