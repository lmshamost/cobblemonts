package com.cobblemon_ts.mod.block;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class ChunkBarrierBlockEntity extends BlockEntity {

    public static final int BIT_POKEMON   = 1 << 0;
    public static final int BIT_NPC       = 1 << 1;
    public static final int BIT_HOSTILE   = 1 << 2;
    public static final int BIT_PASSIVE   = 1 << 3;
    public static final int BIT_FORCELOAD = 1 << 4;
    public static final int BIT_LUCKY     = 1 << 5;
    public static final int BIT_PLAYER    = 1 << 6;

    private ChunkBarrierConfig config = new ChunkBarrierConfig();
    private boolean registered = false;
    // [线性-R09] 放置者玩家UUID（用于区块上限统计）
    private UUID ownerId = null;
    // [线性-R10] owner名字（小写），用于isPlayerBanned排除判定
    private String ownerNameLower = "";

    public ChunkBarrierBlockEntity(BlockPos pos, BlockState state) {
        super(CobblemonTS.CHUNK_BARRIER_BE.get(), pos, state);
    }

    // [线性-R09] owner getter/setter
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID id) { this.ownerId = id; this.setChanged(); }
    public String getOwnerNameLower() { return ownerNameLower; }
    public void setOwnerNameLower(String n) { this.ownerNameLower = n; this.setChanged(); }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel sl && !registered) {
            registered = true;
            CobblemonTS.registerBarrierBE(this, sl);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel sl) {
            CobblemonTS.unregisterBarrierBE(this, sl);
            releaseAllForceLoads(sl);
            registered = false;
        }
    }

    public ChunkBarrierConfig getConfig() {
        return config;
    }

    public void setConfig(ChunkBarrierConfig config) {
        this.config = config;
        this.setChanged();
        if (level instanceof ServerLevel sl) {
            CobblemonTS.rebuildLevelIndex(sl);
            applyAllForceLoads(sl);
        }
    }

    public void applyAllForceLoads(ServerLevel sl) {
        for (var e : config.chunkRules.entrySet()) {
            long k = e.getKey();
            int cx = (int) (k >> 32);
            int cz = (int) k;
            boolean shouldLoad = (e.getValue() & BIT_FORCELOAD) != 0;
            sl.setChunkForced(cx, cz, shouldLoad);
        }
    }

    private void releaseAllForceLoads(ServerLevel sl) {
        for (var e : config.chunkRules.entrySet()) {
            long k = e.getKey();
            int cx = (int) (k >> 32);
            int cz = (int) k;
            sl.setChunkForced(cx, cz, false);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        config = ChunkBarrierConfig.load(tag.getCompound("config"));
        // [线性-R09] 读 owner UUID
        if (tag.hasUUID("owner")) {
            ownerId = tag.getUUID("owner");
        }
        ownerNameLower = tag.getString("ownerNameLower");
        if (level instanceof ServerLevel sl) {
            if (!registered) {
                registered = true;
                CobblemonTS.registerBarrierBE(this, sl);
            } else {
                CobblemonTS.rebuildLevelIndex(sl);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("config", config.save());
        // [线性-R09] 写 owner UUID
        if (ownerId != null) {
            tag.putUUID("owner", ownerId);
        }
        if (ownerNameLower != null && !ownerNameLower.isEmpty()) {
            tag.putString("ownerNameLower", ownerNameLower);
        }
    }
}