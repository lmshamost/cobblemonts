package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity;
import com.cobblemon_ts.mod.block.ChunkBarrierConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ChunkBarrierContainer extends AbstractContainerMenu {

    private final ChunkBarrierConfig config;
    private final BlockPos blockPos;
    private final Level level;

    public ChunkBarrierContainer(int id, ChunkBarrierConfig config, BlockPos pos) {
        super(ModContainers.CHUNK_BARRIER.get(), id);
        this.config = config;
        this.blockPos = pos;
        this.level = null;
    }

    public ChunkBarrierContainer(int id, Inventory inv, FriendlyByteBuf buf) {
        super(ModContainers.CHUNK_BARRIER.get(), id);
        this.blockPos = buf.readBlockPos();
        this.level = inv.player.level();

        if (!level.isClientSide && level.getBlockEntity(blockPos) instanceof ChunkBarrierBlockEntity be) {
            this.config = be.getConfig();
        } else {
            this.config = new ChunkBarrierConfig();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) {
                long key = buf.readLong();
                byte flags = buf.readByte();
                int cx = (int) (key >> 32);
                int cz = (int) key;
                config.chunkRules.put(key, flags);
            }
        }
    }

    public ChunkBarrierConfig getConfig() {
        return config;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public void save() {
        if (level != null && !level.isClientSide && level.getBlockEntity(blockPos) instanceof ChunkBarrierBlockEntity be) {
            be.setConfig(config);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}