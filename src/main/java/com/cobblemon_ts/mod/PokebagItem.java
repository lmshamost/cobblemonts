package com.cobblemon_ts.mod;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PokebagItem extends Item {
    public PokebagItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            CharmPlayerData playerData = CharmPlayerData.get(player);
            if (playerData == null) {
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }

            CharmSlotManager manager = playerData.getCharmSlotManager();
            if (manager.isPokebagOwned()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.coblemon_ts.pokebag_already_owned"));
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }

            manager.setPokebagOwned(true);
            ItemStack stack = player.getItemInHand(hand);
            stack.shrink(1);
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.coblemon_ts.pokebag_obtained"));
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}