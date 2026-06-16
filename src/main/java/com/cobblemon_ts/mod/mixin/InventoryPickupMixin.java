package com.cobblemon_ts.mod.mixin;

import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmSlotManager;
import com.cobblemon_ts.mod.PokebagManager;
import com.cobblemon_ts.mod.gui.PokebagContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public class InventoryPickupMixin {

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void onAddItem(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isEmpty()) return;

        Inventory inv = (Inventory) (Object) this;
        Player player = inv.player;
        if (player == null) return;

        if (player.level().isClientSide()) return;

        CharmPlayerData playerData = CharmPlayerData.get(player);
        if (playerData == null) return;

        CharmSlotManager slotManager = playerData.getCharmSlotManager();
        if (!slotManager.isPokebagOwned()) return;

        PokebagManager bagManager = playerData.getPokebagManager();
        if (!bagManager.isAutoCollect()) return;

        if (!bagManager.canItemEnterBag(stack)) return;

        int originalCount = stack.getCount();
        ItemStack remaining;
        if (player.containerMenu instanceof PokebagContainer pokebagContainer) {
            remaining = pokebagContainer.getBagInventory().addItem(stack.copy());
        } else {
            remaining = bagManager.addItemToBag(stack.copy());
        }

        int eaten = originalCount - remaining.getCount();
        if (eaten > 0) {
            stack.shrink(eaten);
            if (remaining.isEmpty()) {
                cir.setReturnValue(true);
            }
        }
    }
}