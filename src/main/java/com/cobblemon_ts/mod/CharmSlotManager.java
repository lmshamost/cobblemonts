package com.cobblemon_ts.mod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import java.util.EnumMap;
import java.util.Map;

public class CharmSlotManager {
    private final Map<CharmType, CharmState> charmStates = new EnumMap<>(CharmType.class);
    private boolean pokebagOwned = false;
    // [线性-B02] 自动保存回调：数据一改即时写persistentData，解决重启丢护符问题
    private Runnable onDirty = () -> {};

    public void setOnDirty(Runnable callback) {
        this.onDirty = callback != null ? callback : () -> {};
    }

    public CharmSlotManager() {
        for (CharmType type : CharmType.values()) {
            charmStates.put(type, CharmState.DISABLED);
        }
    }

    public boolean isPokebagOwned() {
        return pokebagOwned;
    }

    public void setPokebagOwned(boolean owned) {
        this.pokebagOwned = owned;
        onDirty.run();
    }

    public Map<CharmType, CharmState> getCharmStates() {
        return charmStates;
    }
    
    public CharmState getCharmState(CharmType type) {
        return charmStates.getOrDefault(type, CharmState.AVAILABLE);
    }
    
    public void setCharmState(CharmType type, CharmState state) {
        charmStates.put(type, state);
        onDirty.run();
    }
    
    public boolean isCharmActive(CharmType type) {
        return charmStates.getOrDefault(type, CharmState.AVAILABLE).isActive();
    }
    
    public boolean hasCharmInInventory(PlayerEntity player, CharmType type) {
        if (player == null || type == null) {
            return false;
        }
        
        net.minecraft.item.Item targetItem = type.getItem();
        if (targetItem == null) {
            CobblemonTS.LOGGER.warn("护符类型 " + type + " 对应的物品为null");
            return false;
        }
        
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(targetItem)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean canActivateCharm(PlayerEntity player, CharmType type) {
        if (player == null || type == null) {
            return false;
        }
        
        CharmState state = getCharmState(type);
        if (!state.isAvailable()) {
            return false;
        }
        
        return hasCharmInInventory(player, type);
    }
    
    public void activateCharm(PlayerEntity player, CharmType type) {
        setCharmState(type, CharmState.ACTIVE);
        CobblemonTS.LOGGER.info("护符 " + type.getDisplayName() + " 已激活（消耗）");
    }
    
    public void toggleCharm(PlayerEntity player, CharmType type) {
        CharmState currentState = getCharmState(type);
        if (currentState == CharmState.ACTIVE) {
            setCharmState(type, CharmState.AVAILABLE);
            CobblemonTS.LOGGER.info("护符 " + type.getDisplayName() + " 已关闭");
        } else {
            setCharmState(type, CharmState.ACTIVE);
            CobblemonTS.LOGGER.info("护符 " + type.getDisplayName() + " 已激活");
        }
    }
    
    public NbtCompound serializeNBT() {
        NbtCompound nbt = new NbtCompound();
        NbtList charmList = new NbtList();
        
        for (Map.Entry<CharmType, CharmState> entry : charmStates.entrySet()) {
            NbtCompound charmTag = new NbtCompound();
            charmTag.putString("type", entry.getKey().name());
            charmTag.putString("state", entry.getValue().name());
            charmList.add(charmTag);
        }
        
        nbt.put("charms", charmList);
        nbt.putBoolean("pokebagOwned", pokebagOwned);
        return nbt;
    }
    
    public void deserializeNBT(NbtCompound nbt) {
        if (nbt == null || !nbt.contains("charms", net.minecraft.nbt.NbtElement.LIST_TYPE)) {
            return;
        }
        
        NbtList charmList = nbt.getList("charms", net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < charmList.size(); i++) {
            NbtCompound charmTag = charmList.getCompound(i);
            try {
                CharmType type = CharmType.valueOf(charmTag.getString("type"));
                CharmState state = CharmState.valueOf(charmTag.getString("state"));
                charmStates.put(type, state);
            } catch (IllegalArgumentException e) {
                CobblemonTS.LOGGER.warn("Failed to deserialize charm state: " + e.getMessage());
            }
        }
        pokebagOwned = nbt.getBoolean("pokebagOwned");
    }
}