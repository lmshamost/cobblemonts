package com.cobblemon_ts.mod.lucky.block;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public abstract class BaseLuckyBlock extends Block {

    private static final ThreadLocal<Boolean> SILK_TOUCH_FLAG = ThreadLocal.withInitial(() -> false);

    public BaseLuckyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // 只破坏时触发效果，右键不触发
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        SILK_TOUCH_FLAG.set(false);
        if (!level.isClientSide) {
            ItemStack tool = player.getMainHandItem();
            boolean hasSilkTouch = tool.getEnchantmentLevel(level.registryAccess()
                    .holderOrThrow(Enchantments.SILK_TOUCH)) > 0;

            if (hasSilkTouch) {
                CobblemonTS.LOGGER.info("[LuckyBlock] 精准采集挖掘，掉落方块本体，不触发效果");
                SILK_TOUCH_FLAG.set(true);
                return state;
            }

            if (!player.isCreative()) {
                triggerEffect((ServerLevel) level, pos, player);
            }
        }
        return state;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (SILK_TOUCH_FLAG.get()) {
            SILK_TOUCH_FLAG.remove();
            return List.of(new ItemStack(this));
        }
        return List.of();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && movedByPiston) {
            triggerEffect((ServerLevel) level, pos, null);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    protected abstract void triggerEffect(ServerLevel level, BlockPos pos, Player player);
}