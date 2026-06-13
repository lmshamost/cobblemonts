package com.cobblemon_ts.mod;

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
        loadPlayerData(player);
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
                newData.deserializeNBT(oldData.serializeNBT());
                playerDataMap.put(player.getStringUUID(), newData);
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
            return data;
        });
    }
    
    private static void loadPlayerData(Player player) {
        CharmPlayerData data = new CharmPlayerData();
        loadPlayerDataFromNBT(player, data);
        playerDataMap.put(player.getStringUUID(), data);
    }
    
    private static void loadPlayerDataFromNBT(Player player, CharmPlayerData data) {
        CompoundTag tag = player.getPersistentData();
        if (tag.contains("CharmPlayerData")) {
            data.deserializeNBT(tag.getCompound("CharmPlayerData"));
        }
    }
    
    private static void savePlayerData(Player player) {
        CharmPlayerData data = playerDataMap.get(player.getStringUUID());
        if (data != null) {
            CompoundTag tag = player.getPersistentData();
            tag.put("CharmPlayerData", data.serializeNBT());
        }
    }
}