package com.cobblemon_ts.mod.block;

import com.cobblemon_ts.mod.gui.ChunkBarrierContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ChunkBarrierBlock extends Block implements EntityBlock {

    public ChunkBarrierBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ChunkBarrierBlockEntity be) {
            // [线性-owner绑定] 首次打开 GUI 时自动绑定当前玩家为 owner + 自动加白名单
            if (be.getOwnerId() == null) {
                be.setOwnerId(player.getUUID());
                String ownerName = player.getName().getString().toLowerCase();
                be.setOwnerNameLower(ownerName);
                be.getConfig().playerWhitelist.putIfAbsent(ownerName, true);
                be.getConfig().luckyPlayerWhitelist.putIfAbsent(ownerName, true);
            }
            player.openMenu(new MenuProvider() {
                @Override
                public net.minecraft.network.chat.Component getDisplayName() {
                    return net.minecraft.network.chat.Component.translatable("container.cobblemon_ts.chunk_barrier");
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,
                                                                                      net.minecraft.world.entity.player.Inventory inv,
                                                                                      Player pPlayer) {
                    return new ChunkBarrierContainer(id, be.getConfig(), pos);
                }
            }, buf -> {
                buf.writeBlockPos(pos);
                buf.writeVarInt(be.getConfig().chunkRules.size());
                for (var e : be.getConfig().chunkRules.entrySet()) {
                    buf.writeLong(e.getKey());
                    buf.writeByte(e.getValue());
                }
                // [线性-R10] 同步 owner UUID + owner名字 到客户端
                buf.writeUUID(be.getOwnerId() != null ? be.getOwnerId() : new java.util.UUID(0, 0));
                String ownerName = "";
                if (be.getOwnerId() != null && level instanceof net.minecraft.server.level.ServerLevel sl) {
                    var sp = sl.getServer().getPlayerList().getPlayer(be.getOwnerId());
                    ownerName = sp != null ? sp.getName().getString() : be.getOwnerId().toString().substring(0, 8);
                }
                buf.writeUtf(ownerName);
                buf.writeBoolean(be.getConfig().banLuckyBlock);
                buf.writeBoolean(be.getConfig().banShinyBlock);
                buf.writeBoolean(be.getConfig().banLegendaryBlock);
                buf.writeBoolean(be.getConfig().banUltraBlock);
                buf.writeVarInt(be.getConfig().playerWhitelist.size());
                for (var e : be.getConfig().playerWhitelist.entrySet()) {
                    buf.writeUtf(e.getKey());
                    buf.writeBoolean(e.getValue());
                }
                buf.writeVarInt(be.getConfig().luckyPlayerWhitelist.size());
                for (var e : be.getConfig().luckyPlayerWhitelist.entrySet()) {
                    buf.writeUtf(e.getKey());
                    buf.writeBoolean(e.getValue());
                }
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public @Nullable net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChunkBarrierBlockEntity(pos, state);
    }
}