package com.cobblemon_ts.mod.mixin;

import com.cobblemon_ts.mod.CharmPlayerData;
import com.cobblemon_ts.mod.CharmSlotManager;
import com.cobblemon_ts.mod.PokebagManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public class InventoryPickupMixin {

    @Inject(method = "insertStack(ILnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void onAddItem(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isEmpty()) return;
        // [线性-B07] slot==-1 才是掉落物自动拾取；slot>=0 是 placeItemBackInInventory 等"指定槽位"路径，
        // 若不拦截，关闭容器时鼠标上的物品会被误判为拾取而重新吸入道具袋，导致"左键拿起→关闭→物品消失"。
        if (slot != -1) return;

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
        // [线性-B07] 统一走 bagManager.addItemToBag：它操作的真实容器与当前打开菜单引用的是同一实例，
        // 且内部会正确设置脏标记落盘；之前的 if 分支直接调容器 addItem 会跳过脏标记导致数据不持久化。
        ItemStack remaining = bagManager.addItemToBag(stack.copy());

        int eaten = originalCount - remaining.getCount();
        if (eaten > 0) {
            stack.decrement(eaten);
            if (remaining.isEmpty()) {
                cir.setReturnValue(true);
            }
        }
    }
}