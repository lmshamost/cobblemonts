package com.cobblemon_ts.mod;

import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class CharmItem extends Item {
    private final CharmType charmType;

    public CharmItem(Item.Settings settings, CharmType charmType) {
        super(settings);
        this.charmType = charmType;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (!world.isClient) {
            CharmSlotManager manager = CharmPlayerData.get(player).getCharmSlotManager();
            CharmState currentState = manager.getCharmState(charmType);

            if (currentState == CharmState.DISABLED) {
                manager.setCharmState(charmType, CharmState.ACTIVE);

                ItemStack stack = player.getStackInHand(hand);
                stack.decrement(1);

                if (CharmConfig.SHOW_CHARM_MESSAGES.get()) {
                    player.sendMessage(Text.translatable("message.cobblemon_ts.charm_obtained", charmType.getDisplayName()));
                }
                CobblemonTS.LOGGER.info("护符 " + charmType.getDisplayName() + " 已获得并激活");

                return TypedActionResult.consume(stack);
            } else if (currentState == CharmState.AVAILABLE) {
                if (CharmConfig.SHOW_CHARM_MESSAGES.get()) {
                    player.sendMessage(Text.translatable("message.cobblemon_ts.charm_already_owned", charmType.getDisplayName()));
                }
                CobblemonTS.LOGGER.info("护符 " + charmType.getDisplayName() + " 已拥有，点击GUI中的护符槽激活");
            } else {
                if (CharmConfig.SHOW_CHARM_MESSAGES.get()) {
                    player.sendMessage(Text.translatable("message.cobblemon_ts.charm_already_active", charmType.getDisplayName()));
                }
                CobblemonTS.LOGGER.info("护符 " + charmType.getDisplayName() + " 已激活");
            }
        }

        return TypedActionResult.success(player.getStackInHand(hand));
    }

    public CharmType getCharmType() {
        return charmType;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, java.util.List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        String key = "tooltip.cobblemon_ts." + charmType.getItemId();
        tooltip.add(Text.translatable(key));
    }
}