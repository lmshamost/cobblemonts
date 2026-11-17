package com.cobblemon_ts.mod.gui;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;

public class PartyHeldItemContainer extends ScreenHandler {
    public static final Text TITLE = Text.translatable("gui.cobblemon_ts.held_items");

    private final PlayerEntity player;
    private final SimpleInventory heldItems;

    // [线性-O04] 反射缓存：静态缓存避免每次打开容器时重复反射
    private static boolean reflectionReady;
    private static Method swapHeldItemMethod;
    private static Method heldItemMethod;
    private static Method removeHeldItemMethod;
    private static Object cobblemonInstance;
    private static Method cachedGetStorageMethod;
    private static Method cachedGetPartyMethod;
    private static Method cachedPartySizeMethod;
    private static Method cachedPartyGetMethod;

    public PartyHeldItemContainer(int containerId, PlayerInventory playerInventory) {
        super(ModContainers.PARTY_HELD_ITEM, containerId);
        this.player = playerInventory.player;
        this.heldItems = new SimpleInventory(6);

        initReflection();

        if (!player.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer) {
            loadFromParty(serverPlayer);
        }

        for (int i = 0; i < 6; i++) {
            addSlot(new PokemonHeldItemSlot(heldItems, i, 80 + i * 20, 20));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 51 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 109));
        }
    }

    private void initReflection() {
        if (reflectionReady) return;
        try {
            cobblemonInstance = Class.forName("com.cobblemon.mod.common.Cobblemon")
                    .getField("INSTANCE").get(null);
            cachedGetStorageMethod = cobblemonInstance.getClass().getMethod("getStorage");
            Class<?> pokemonClass = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
            swapHeldItemMethod = pokemonClass.getMethod("swapHeldItem",
                    net.minecraft.item.ItemStack.class, boolean.class, boolean.class);
            heldItemMethod = pokemonClass.getMethod("heldItem");
            removeHeldItemMethod = pokemonClass.getMethod("removeHeldItem");
            reflectionReady = true;
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("PartyHeldItemContainer reflection failed", e);
        }
    }

    private void loadFromParty(ServerPlayerEntity sp) {
        if (!reflectionReady) return;
        try {
            var storage = cachedGetStorageMethod.invoke(cobblemonInstance);
            // [线性-O04] 缓存party方法
            if (cachedGetPartyMethod == null) {
                cachedGetPartyMethod = storage.getClass().getMethod("getParty", ServerPlayerEntity.class);
            }
            var party = cachedGetPartyMethod.invoke(storage, sp);
            if (party == null) return;
            // [线性-O04] 缓存party的size/get方法
            if (cachedPartySizeMethod == null) {
                cachedPartySizeMethod = party.getClass().getMethod("size");
                cachedPartyGetMethod = party.getClass().getMethod("get", int.class);
            }
            int size = (Integer) cachedPartySizeMethod.invoke(party);
            for (int i = 0; i < Math.min(size, 6); i++) {
                var pokemon = cachedPartyGetMethod.invoke(party, i);
                if (pokemon != null) {
                    ItemStack held = (ItemStack) heldItemMethod.invoke(pokemon);
                    if (held != null && !held.isEmpty()) {
                        heldItems.setStack(i, held.copy());
                    }
                }
            }
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("Failed to load held items from party", e);
        }
    }

    private void syncToParty(int slotIndex) {
        if (player.getWorld().isClient) return;
        if (!reflectionReady || !(player instanceof ServerPlayerEntity serverPlayer)) return;
        try {
            var storage = cachedGetStorageMethod.invoke(cobblemonInstance);
            if (cachedGetPartyMethod == null) {
                cachedGetPartyMethod = storage.getClass().getMethod("getParty", ServerPlayerEntity.class);
            }
            var party = cachedGetPartyMethod.invoke(storage, serverPlayer);
            if (party == null) return;
            if (cachedPartyGetMethod == null) {
                cachedPartyGetMethod = party.getClass().getMethod("get", int.class);
            }
            var pokemon = cachedPartyGetMethod.invoke(party, slotIndex);
            if (pokemon == null) return;
            ItemStack newHeld = heldItems.getStack(slotIndex);
            if (newHeld.isEmpty()) {
                removeHeldItemMethod.invoke(pokemon);
            } else {
                swapHeldItemMethod.invoke(pokemon, newHeld.copyWithCount(1), true, false);
            }
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("Failed to sync held item to party", e);
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasStack()) {
            ItemStack slotStack = slot.getStack();
            result = slotStack.copy();
            if (index < 6) {
                if (!this.insertItem(slotStack, 6, 42, true)) {
                    return ItemStack.EMPTY;
                }
                syncToParty(index);
            } else if (!this.insertItem(slotStack, 0, 6, false)) {
                return ItemStack.EMPTY;
            }
            if (slotStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTakeItem(player, slotStack);
        }
        return result;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (!player.getWorld().isClient) {
            for (int i = 0; i < 6; i++) {
                syncToParty(i);
            }
        }
    }

    public static class PokemonHeldItemSlot extends Slot {
        public PokemonHeldItemSlot(Inventory container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public int getMaxItemCount() {
            return 1;
        }
    }
}