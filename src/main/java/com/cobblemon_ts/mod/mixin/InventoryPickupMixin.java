package com.cobblemon_ts.mod.mixin;

import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmSlotManager;
import com.cobblemon_ts.mod.PokebagManager;
import com.cobblemon_ts.mod.gui.PokebagContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public class InventoryPickupMixin {

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void onAddItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isEmpty()) return;

        PlayerInventory inv = (PlayerInventory) (Object) this;
        PlayerEntity player = inv.player;
        if (player == null) return;

        if (player.getWorld().isClient) return;

        CharmPlayerData playerData = CharmPlayerData.get(player);
        if (playerData == null) return;

        CharmSlotManager slotManager = playerData.getCharmSlotManager();
        if (!slotManager.isPokebagOwned()) return;

        PokebagManager bagManager = playerData.getPokebagManager();
        if (!bagManager.isAutoCollect()) return;

        if (!bagManager.canItemEnterBag(stack)) return;

        int originalCount = stack.getCount();
        ItemStack remaining;
        if (player.currentScreenHandler instanceof PokebagContainer pokebagContainer) {
            remaining = pokebagContainer.getBagInventory().addStack(stack.copy());
        } else {
            remaining = bagManager.addItemToBag(stack.copy());
        }

        int eaten = originalCount - remaining.getCount();
        if (eaten > 0) {
            stack.decrement(eaten);
            if (remaining.isEmpty()) {
                cir.setReturnValue(true);
            }
        }
    }
}