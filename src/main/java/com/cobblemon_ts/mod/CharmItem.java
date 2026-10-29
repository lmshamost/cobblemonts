package com.cobblemon_ts.mod;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class CharmItem extends Item {
    private final CharmType charmType;
    
    public CharmItem(Properties properties, CharmType charmType) {
        super(properties);
        this.charmType = charmType;
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            CharmSlotManager manager = CharmPlayerData.get(player).getCharmSlotManager();
            CharmState currentState = manager.getCharmState(charmType);
            
            if (currentState == CharmState.DISABLED) {
                manager.setCharmState(charmType, CharmState.ACTIVE);
                
                ItemStack stack = player.getItemInHand(hand);
                stack.shrink(1);
                
                if (CharmConfig.SHOW_CHARM_MESSAGES.get()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.coblemon_ts.charm_obtained", charmType.getDisplayComponent()));
                }
                CobblemonTS.LOGGER.info("护符 " + charmType.getDisplayName() + " 已获得并激活");
                
                return InteractionResultHolder.consume(stack);
            } else if (currentState == CharmState.AVAILABLE) {
                if (CharmConfig.SHOW_CHARM_MESSAGES.get()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.coblemon_ts.charm_already_owned", charmType.getDisplayComponent()));
                }
                CobblemonTS.LOGGER.info("护符 " + charmType.getDisplayName() + " 已拥有，点击GUI中的护符槽激活");
            } else {
                if (CharmConfig.SHOW_CHARM_MESSAGES.get()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.coblemon_ts.charm_already_active", charmType.getDisplayComponent()));
                }
                CobblemonTS.LOGGER.info("护符 " + charmType.getDisplayName() + " 已激活");
            }
        }
        
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
    
    public CharmType getCharmType() {
        return charmType;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        String key = "tooltip.coblemon_ts." + charmType.getItemId();
        tooltip.add(Component.translatable(key));
    }
}