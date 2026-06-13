package com.cobblemon_ts.mod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class CharmPlayerData {
    private final CharmSlotManager charmSlotManager = new CharmSlotManager();
    
    public CharmSlotManager getCharmSlotManager() {
        return charmSlotManager;
    }
    
    public static CharmPlayerData get(Player player) {
        return CharmEventListener.getPlayerData(player);
    }
    
    public CompoundTag serializeNBT() {
        return charmSlotManager.serializeNBT();
    }
    
    public void deserializeNBT(CompoundTag nbt) {
        charmSlotManager.deserializeNBT(nbt);
    }
}