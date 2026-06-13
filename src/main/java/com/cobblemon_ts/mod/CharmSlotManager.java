package com.cobblemon_ts.mod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.EnumMap;
import java.util.Map;

public class CharmSlotManager {
    private final Map<CharmType, CharmState> charmStates = new EnumMap<>(CharmType.class);

    public CharmSlotManager() {
        for (CharmType type : CharmType.values()) {
            charmStates.put(type, CharmState.DISABLED);
        }
    }

    public Map<CharmType, CharmState> getCharmStates() {
        return charmStates;
    }
    
    public CharmState getCharmState(CharmType type) {
        return charmStates.getOrDefault(type, CharmState.AVAILABLE);
    }
    
    public void setCharmState(CharmType type, CharmState state) {
        charmStates.put(type, state);
    }
    
    public boolean isCharmActive(CharmType type) {
        return charmStates.getOrDefault(type, CharmState.AVAILABLE).isActive();
    }
    
    public boolean hasCharmInInventory(Player player, CharmType type) {
        if (player == null || type == null) {
            return false;
        }
        
        net.minecraft.world.item.Item targetItem = type.getItem();
        if (targetItem == null) {
            CobblemonTS.LOGGER.warn("护符类型 " + type + " 对应的物品为null");
            return false;
        }
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(targetItem)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean canActivateCharm(Player player, CharmType type) {
        if (player == null || type == null) {
            return false;
        }
        
        CharmState state = getCharmState(type);
        if (!state.isAvailable()) {
            return false;
        }
        
        return hasCharmInInventory(player, type);
    }
    
    public void activateCharm(Player player, CharmType type) {
        setCharmState(type, CharmState.ACTIVE);
        CobblemonTS.LOGGER.info("护符 " + type.getDisplayName() + " 已激活（消耗）");
    }
    
    public void toggleCharm(Player player, CharmType type) {
        CharmState currentState = getCharmState(type);
        if (currentState == CharmState.ACTIVE) {
            setCharmState(type, CharmState.AVAILABLE);
            CobblemonTS.LOGGER.info("护符 " + type.getDisplayName() + " 已关闭");
        } else {
            setCharmState(type, CharmState.ACTIVE);
            CobblemonTS.LOGGER.info("护符 " + type.getDisplayName() + " 已激活");
        }
    }
    
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag charmList = new ListTag();
        
        for (Map.Entry<CharmType, CharmState> entry : charmStates.entrySet()) {
            CompoundTag charmTag = new CompoundTag();
            charmTag.putString("type", entry.getKey().name());
            charmTag.putString("state", entry.getValue().name());
            charmList.add(charmTag);
        }
        
        tag.put("charms", charmList);
        return tag;
    }
    
    public void deserializeNBT(CompoundTag tag) {
        if (tag == null || !tag.contains("charms", Tag.TAG_LIST)) {
            return;
        }
        
        ListTag charmList = tag.getList("charms", Tag.TAG_COMPOUND);
        for (int i = 0; i < charmList.size(); i++) {
            CompoundTag charmTag = charmList.getCompound(i);
            try {
                CharmType type = CharmType.valueOf(charmTag.getString("type"));
                CharmState state = CharmState.valueOf(charmTag.getString("state"));
                charmStates.put(type, state);
            } catch (IllegalArgumentException e) {
                CobblemonTS.LOGGER.warn("Failed to deserialize charm state: " + e.getMessage());
            }
        }
    }
}