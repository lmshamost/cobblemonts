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

import java.util.UUID;

public class ChunkBarrierContainer extends AbstractContainerMenu {

    private final ChunkBarrierConfig config;
    private final BlockPos blockPos;
    private final Level level;
    // [线性-R10] 客户端拿到 ownerId 用于只读模式判断
    private UUID ownerId = null;
    // [线性-hover] owner 名字（服务器同步，用于 tooltip 显示）
    private String ownerName = "";

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
            // [线性-R10] 客户端解 owner UUID + owner名字 + lucky_bans + players
            UUID uid = buf.readUUID();
            if (uid.getMostSignificantBits() != 0 || uid.getLeastSignificantBits() != 0) {
                this.ownerId = uid;
            }
            this.ownerName = buf.readUtf();
            config.banLuckyBlock = buf.readBoolean();
            config.banShinyBlock = buf.readBoolean();
            config.banLegendaryBlock = buf.readBoolean();
            config.banUltraBlock = buf.readBoolean();
            int ps = buf.readVarInt();
            for (int i = 0; i < ps; i++) {
                config.playerWhitelist.put(buf.readUtf(), buf.readBoolean());
            }
            int lps = buf.readVarInt();
            for (int i = 0; i < lps; i++) {
                config.luckyPlayerWhitelist.put(buf.readUtf(), buf.readBoolean());
            }
        }
    }

    public ChunkBarrierConfig getConfig() {
        return config;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    // [线性-R10] Screen 用来判断只读模式
    public UUID getOwnerId() { return ownerId; }
    // [线性-hover] Screen tooltip 显示 owner 名字
    public String getOwnerName() { return ownerName; }

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