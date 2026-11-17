package com.cobblemon_ts.mod.block;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
        super(CobblemonTS.CHUNK_BARRIER_BE, pos, state);
    }

    // [线性-R09] owner getter/setter
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID id) { this.ownerId = id; this.markDirty(); }
    public String getOwnerNameLower() { return ownerNameLower; }
    public void setOwnerNameLower(String n) { this.ownerNameLower = n; this.markDirty(); }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        if (world instanceof ServerWorld sw && !registered) {
            registered = true;
            CobblemonTS.registerBarrierBE(this, sw);
        }
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        if (world instanceof ServerWorld sw) {
            CobblemonTS.unregisterBarrierBE(this, sw);
            releaseAllForceLoads(sw);
            registered = false;
        }
    }

    public ChunkBarrierConfig getConfig() {
        return config;
    }

    public void setConfig(ChunkBarrierConfig config) {
        this.config = config;
        this.markDirty();
        if (world instanceof ServerWorld sw) {
            CobblemonTS.rebuildLevelIndex(sw);
            applyAllForceLoads(sw);
        }
    }

    public void applyAllForceLoads(ServerWorld sw) {
        for (var e : config.chunkRules.entrySet()) {
            long k = e.getKey();
            int cx = (int) (k >> 32);
            int cz = (int) k;
            boolean shouldLoad = (e.getValue() & BIT_FORCELOAD) != 0;
            sw.setChunkForced(cx, cz, shouldLoad);
        }
    }

    private void releaseAllForceLoads(ServerWorld sw) {
        for (var e : config.chunkRules.entrySet()) {
            long k = e.getKey();
            int cx = (int) (k >> 32);
            int cz = (int) k;
            sw.setChunkForced(cx, cz, false);
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        config = ChunkBarrierConfig.load(nbt.getCompound("config"));
        // [线性-R09] 读 owner UUID
        if (nbt.containsUuid("owner")) {
            ownerId = nbt.getUuid("owner");
        }
        ownerNameLower = nbt.getString("ownerNameLower");
        if (world instanceof ServerWorld sw) {
            if (!registered) {
                registered = true;
                CobblemonTS.registerBarrierBE(this, sw);
            } else {
                CobblemonTS.rebuildLevelIndex(sw);
            }
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.put("config", config.save());
        // [线性-R09] 写 owner UUID
        if (ownerId != null) {
            nbt.putUuid("owner", ownerId);
        }
        if (ownerNameLower != null && !ownerNameLower.isEmpty()) {
            nbt.putString("ownerNameLower", ownerNameLower);
        }
    }
}