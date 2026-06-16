package com.cobblemon_ts.mod;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class CharmPlayerData {
    private final CharmSlotManager charmSlotManager = new CharmSlotManager();
    private final PokebagManager pokebagManager = new PokebagManager();
    // [线性-B02] 缓存Player引用，用于即时写persistentData
    private Player currentPlayer;

    public CharmSlotManager getCharmSlotManager() {
        return charmSlotManager;
    }

    public PokebagManager getPokebagManager() {
        return pokebagManager;
    }

    public static CharmPlayerData get(Player player) {
        return CharmEventListener.getPlayerData(player);
    }

    // [线性-B02] 绑定Player并设置自动保存：CharmSlotManager/PokebagManager任意数据变更→即时写persistentData
    public void setPlayer(Player player) {
        this.currentPlayer = player;
        Runnable saveTask = () -> {
            if (currentPlayer != null) {
                CompoundTag tag = currentPlayer.getPersistentData();
                tag.put("CharmPlayerData", this.serializeNBT(currentPlayer.registryAccess()));
            }
        };
        this.charmSlotManager.setOnDirty(saveTask);
        this.pokebagManager.setOnDirty(saveTask);
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.put("CharmSlotManager", charmSlotManager.serializeNBT());
        tag.put("PokebagManager", pokebagManager.serializeNBT(provider));
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        if (nbt.contains("CharmSlotManager")) {
            charmSlotManager.deserializeNBT(nbt.getCompound("CharmSlotManager"));
        }
        if (nbt.contains("PokebagManager")) {
            pokebagManager.deserializeNBT(nbt.getCompound("PokebagManager"), provider);
        }
    }
}