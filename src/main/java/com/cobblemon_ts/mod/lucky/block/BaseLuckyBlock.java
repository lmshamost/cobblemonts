package com.cobblemon_ts.mod.lucky.block;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
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

    // 支持红石激活触发效果
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            boolean powered = level.hasNeighborSignal(pos);
            if (powered) {
                // 红石激活时触发效果并破坏方块
                triggerEffect((ServerLevel) level, pos, null);
                level.destroyBlock(pos, false); // 不掉落方块
            }
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        // 放置时检测红石信号
        if (!level.isClientSide) {
            boolean powered = level.hasNeighborSignal(pos);
            if (powered) {
                // 红石激活时触发效果并破坏方块
                triggerEffect((ServerLevel) level, pos, null);
                level.destroyBlock(pos, false); // 不掉落方块
            }
        }
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

    protected void dropItem(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        itemEntity.setDeltaMovement(level.random.nextGaussian() * 0.05, level.random.nextGaussian() * 0.05 + 0.2, level.random.nextGaussian() * 0.05);
        level.addFreshEntity(itemEntity);
    }

    // [线性-BASE-LUCK] 获取玩家幸运值，用于动态权重加成
    // 仿原版loot_tables的quality机制: effective_weight = base_weight + floor(quality * luck)
    // player为null时(红石触发)返回0，不加成
    protected double getPlayerLuck(Player player) {
        if (player == null) return 0;
        return player.getAttributeValue(Attributes.LUCK);
    }
}