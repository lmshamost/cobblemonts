package com.cobblemon_ts.mod.block;

import com.cobblemon_ts.mod.gui.ChunkBarrierContainer;
import com.cobblemon_ts.mod.gui.ChunkBarrierSyncData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class ChunkBarrierBlock extends Block implements BlockEntityProvider {

    public ChunkBarrierBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof ChunkBarrierBlockEntity be) {
            // [线性-owner绑定] 首次打开 GUI 时自动绑定当前玩家为 owner + 自动加白名单
            if (be.getOwnerId() == null) {
                be.setOwnerId(player.getUuid());
                String ownerName = player.getName().getString().toLowerCase();
                be.setOwnerNameLower(ownerName);
                be.getConfig().playerWhitelist.putIfAbsent(ownerName, true);
                be.getConfig().luckyPlayerWhitelist.putIfAbsent(ownerName, true);
            }
            player.openHandledScreen(new ExtendedScreenHandlerFactory<ChunkBarrierSyncData>() {
                @Override
                public ChunkBarrierSyncData getScreenOpeningData(ServerPlayerEntity sp) {
                    String ownerName = "";
                    if (be.getOwnerId() != null && world instanceof ServerWorld sw) {
                        var owner = sw.getServer().getPlayerManager().getPlayer(be.getOwnerId());
                        ownerName = owner != null ? owner.getName().getString() : be.getOwnerId().toString().substring(0, 8);
                    }
                    return new ChunkBarrierSyncData(
                            pos,
                            new HashMap<>(be.getConfig().chunkRules),
                            be.getOwnerId(),
                            ownerName,
                            be.getConfig().banLuckyBlock,
                            be.getConfig().banShinyBlock,
                            be.getConfig().banLegendaryBlock,
                            be.getConfig().banUltraBlock,
                            new HashMap<>(be.getConfig().playerWhitelist),
                            new HashMap<>(be.getConfig().luckyPlayerWhitelist)
                    );
                }

                @Override
                public Text getDisplayName() {
                    return Text.translatable("container.cobblemon_ts.chunk_barrier");
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity pPlayer) {
                    return new ChunkBarrierContainer(syncId, be.getConfig(), pos);
                }
            });
        }
        return ActionResult.success(world.isClient);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ChunkBarrierBlockEntity(pos, state);
    }
}