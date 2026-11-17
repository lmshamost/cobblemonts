package com.cobblemon_ts.mod;

import net.minecraft.registry.RegistryWrapper;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.player.PlayerEntity;

public class CharmPlayerData {
    private final CharmSlotManager charmSlotManager = new CharmSlotManager();
    private final PokebagManager pokebagManager = new PokebagManager();
    // [线性-B02] 缓存Player引用，用于即时写persistentData
    private PlayerEntity currentPlayer;

    public CharmSlotManager getCharmSlotManager() {
        return charmSlotManager;
    }

    public PokebagManager getPokebagManager() {
        return pokebagManager;
    }

    public static CharmPlayerData get(PlayerEntity player) {
        return CharmEventListener.getPlayerData(player);
    }

    // [线性-B02] 绑定Player并设置自动保存：CharmSlotManager/PokebagManager任意数据变更→即时写persistentData
    public void setPlayer(PlayerEntity player) {
        this.currentPlayer = player;
        Runnable saveTask = () -> {
            if (currentPlayer != null) {
                NbtCompound NbtElement = PersistentDataHelper.getPersistentData(currentPlayer);
                NbtElement.put("CharmPlayerData", this.serializeNBT(currentPlayer.getRegistryManager()));
            }
        };
        this.charmSlotManager.setOnDirty(saveTask);
        this.pokebagManager.setOnDirty(saveTask);
    }

    public NbtCompound serializeNBT(RegistryWrapper.WrapperLookup provider) {
        NbtCompound NbtElement = new NbtCompound();
        NbtElement.put("CharmSlotManager", charmSlotManager.serializeNBT());
        NbtElement.put("PokebagManager", pokebagManager.serializeNBT(provider));
        return NbtElement;
    }

    public void deserializeNBT(NbtCompound nbt, RegistryWrapper.WrapperLookup provider) {
        if (nbt.contains("CharmSlotManager")) {
            charmSlotManager.deserializeNBT(nbt.getCompound("CharmSlotManager"));
        }
        if (nbt.contains("PokebagManager")) {
            pokebagManager.deserializeNBT(nbt.getCompound("PokebagManager"), provider);
        }
    }
}