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
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public @Nullable net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChunkBarrierBlockEntity(pos, state);
    }
}