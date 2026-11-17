package com.cobblemon_ts.mod.lucky.block;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemActionResult;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.util.hit.BlockHitResult;

import java.util.List;

public abstract class BaseLuckyBlock extends Block {

    private static final ThreadLocal<Boolean> SILK_TOUCH_FLAG = ThreadLocal.withInitial(() -> false);

    public BaseLuckyBlock(Settings settings) {
        super(settings);
    }

    // [线性-L01] Yarn1.21.1: useItemOn → onUseWithItem, 返回 ItemActionResult
    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world,
                                             BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hitResult) {
        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // [线性-L02] Yarn1.21.1: playerWillDestroy → onBreak
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        SILK_TOUCH_FLAG.set(false);
        if (!world.isClient) {
            ItemStack tool = player.getMainHandStack();
            // [线性-L03m] Yarn1.21.1: 通过RegistryKeys获取附魔注册表再查SILK_TOUCH
            var enchRegistry = world.getRegistryManager().get(
                    net.minecraft.registry.RegistryKeys.ENCHANTMENT);
            boolean hasSilkTouch = net.minecraft.enchantment.EnchantmentHelper.getLevel(
                    enchRegistry.getEntry(net.minecraft.enchantment.Enchantments.SILK_TOUCH).orElseThrow(), tool) > 0;

            if (hasSilkTouch) {
                CobblemonTS.LOGGER.info("[LuckyBlock] Silk touch - drop block, no effect trigger");
                SILK_TOUCH_FLAG.set(true);
                return state;
            }

            // [线性-BASE-LUCK] 创造模式只有拿镐子才触发；其他模式正常触发
            boolean isCreative = player.isInCreativeMode();
            boolean hasPickaxe = tool.getItem() instanceof net.minecraft.item.PickaxeItem;
            if (!isCreative || hasPickaxe) {
                triggerEffect((ServerWorld) world, pos, player);
            }
        }
        return state;
    }

    // [线性-L04] Yarn1.21.1: neighborChanged → neighborUpdate
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!world.isClient) {
            boolean powered = world.isReceivingRedstonePower(pos);
            if (powered) {
                triggerEffect((ServerWorld) world, pos, null);
                // [线性-L05] destroyBlock → breakBlock
                world.breakBlock(pos, false);
            }
        }
    }

    // [线性-L06] Yarn1.21.1: onPlace → onBlockAdded
    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onBlockAdded(state, world, pos, oldState, isMoving);
        if (!world.isClient) {
            boolean powered = world.isReceivingRedstonePower(pos);
            if (powered) {
                triggerEffect((ServerWorld) world, pos, null);
                // [线性-L07] destroyBlock → breakBlock
                world.breakBlock(pos, false);
            }
        }
    }

    // [线性-L08] Yarn1.21.1: getDrops → getDroppedStacks
    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        if (SILK_TOUCH_FLAG.get()) {
            SILK_TOUCH_FLAG.remove();
            return List.of(new ItemStack(this));
        }
        return List.of();
    }

    // [线性-L09] Yarn1.21.1: onRemove → onStateReplaced
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!world.isClient && movedByPiston) {
            triggerEffect((ServerWorld) world, pos, null);
        }
        super.onStateReplaced(state, world, pos, newState, movedByPiston);
    }

    protected abstract void triggerEffect(ServerWorld world, BlockPos pos, PlayerEntity player);

    protected void dropItem(ServerWorld world, BlockPos pos, ItemStack stack) {
        ItemEntity itemEntity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        itemEntity.setVelocity(world.random.nextGaussian() * 0.05,
                world.random.nextGaussian() * 0.05 + 0.2,
                world.random.nextGaussian() * 0.05);
        world.spawnEntity(itemEntity);
    }

    // [线性-L10] getPlayerLuck: EntityAttributes.GENERIC_LUCK
    protected double getPlayerLuck(PlayerEntity player) {
        if (player == null) return 0;
        return player.getAttributeValue(EntityAttributes.GENERIC_LUCK);
    }
}