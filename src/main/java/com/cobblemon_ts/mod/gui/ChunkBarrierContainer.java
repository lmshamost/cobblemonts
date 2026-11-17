package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.block.ChunkBarrierBlockEntity;
import com.cobblemon_ts.mod.block.ChunkBarrierConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public class ChunkBarrierContainer extends ScreenHandler {

    private final ChunkBarrierConfig config;
    private final BlockPos blockPos;
    private final World world;
    // [线性-R10] 客户端拿到 ownerId 用于只读模式判断
    private UUID ownerId = null;
    // [线性-hover] owner 名字（服务器同步，用于 tooltip 显示）
    private String ownerName = "";

    public ChunkBarrierContainer(int id, ChunkBarrierConfig config, BlockPos pos) {
        super(ModContainers.CHUNK_BARRIER, id);
        this.config = config;
        this.blockPos = pos;
        this.world = null;
    }

    // [线性-R10] 扩展屏幕机制：客户端从同步数据构建配置
    public ChunkBarrierContainer(int id, PlayerInventory inv, ChunkBarrierSyncData data) {
        super(ModContainers.CHUNK_BARRIER, id);
        this.blockPos = data.pos();
        this.world = inv.player.getWorld();
        this.ownerId = data.ownerId();
        this.ownerName = data.ownerName();
        ChunkBarrierConfig cfg = new ChunkBarrierConfig();
        cfg.chunkRules.putAll(data.rules());
        cfg.banLuckyBlock = data.banLuckyBlock();
        cfg.banShinyBlock = data.banShinyBlock();
        cfg.banLegendaryBlock = data.banLegendaryBlock();
        cfg.banUltraBlock = data.banUltraBlock();
        cfg.playerWhitelist.putAll(data.playerWhitelist());
        cfg.luckyPlayerWhitelist.putAll(data.luckyPlayerWhitelist());
        this.config = cfg;
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
        if (world != null && !world.isClient && world.getBlockEntity(blockPos) instanceof ChunkBarrierBlockEntity be) {
            be.setConfig(config);
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}