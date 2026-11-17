package com.cobblemon_ts.mod;

import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class PokebagItem extends Item {
    public PokebagItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (!world.isClient) {
            CharmPlayerData playerData = CharmPlayerData.get(player);
            if (playerData == null) {
                return TypedActionResult.fail(player.getStackInHand(hand));
            }

            CharmSlotManager manager = playerData.getCharmSlotManager();
            if (manager.isPokebagOwned()) {
                player.sendMessage(Text.translatable("message.cobblemon_ts.pokebag_already_owned"));
                return TypedActionResult.fail(player.getStackInHand(hand));
            }

            manager.setPokebagOwned(true);
            ItemStack stack = player.getStackInHand(hand);
            stack.decrement(1);
            player.sendMessage(Text.translatable("message.cobblemon_ts.pokebag_obtained"));
            return TypedActionResult.consume(stack);
        }
        return TypedActionResult.success(player.getStackInHand(hand));
    }
}