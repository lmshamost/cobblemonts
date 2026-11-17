package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.registry.Registries;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.inventory.Inventory;

// [线性-C02] 道具袋库存扣除包：客户端→服务端
//   UseItemOnPokemonPacket(fromPokebag=true) 负责完整物品使用逻辑（不扣玩家物品栏）
//   本包只负责从道具袋库存中扣除物品并同步客户端
public record UsePokebagItemPacket(int partySlot, String itemId, int count) implements CustomPayload {

    public static final Id<UsePokebagItemPacket> TYPE = new Id<>(
            Identifier.of("cobblemon_ts", "use_pokebag_item"));

    public static final PacketCodec<RegistryByteBuf, UsePokebagItemPacket> STREAM_CODEC = new PacketCodec<>() {
        @Override
        public UsePokebagItemPacket decode(RegistryByteBuf buf) {
            return new UsePokebagItemPacket(buf.readVarInt(), buf.readString(), buf.readVarInt());
        }

        @Override
        public void encode(RegistryByteBuf buf, UsePokebagItemPacket packet) {
            buf.writeVarInt(packet.partySlot());
            buf.writeString(packet.itemId());
            buf.writeVarInt(packet.count());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    // [线性-FIX-baguse-03] 纯库存扣除——所有物品使用逻辑由UseItemOnPokemonPacket(fromPokebag=true)处理
    //   道具袋消耗自扣自有库存，无越权/刷物品风险，不需要SecurityHelper.isTrusted
    public static void handleServerSide(UsePokebagItemPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayerEntity player)) return;

            try {
                var playerData = com.cobblemon_ts.mod.CharmPlayerData.get(player);
                var bagManager = playerData.getPokebagManager();
                if (bagManager == null) return;

                var inventory = bagManager.getBagInventory();
                int remainingToConsume = packet.count();
                for (int i = 0; i < inventory.size() && remainingToConsume > 0; i++) {
                    ItemStack stack = inventory.getStack(i);
                    if (stack.isEmpty()) continue;
                    String stackId = Registries.ITEM.getId(stack.getItem()).toString();
                    if (stackId.equals(packet.itemId())) {
                        int consume = Math.min(remainingToConsume, stack.getCount());
                        stack.decrement(consume);
                        remainingToConsume -= consume;
                        if (stack.isEmpty()) {
                            inventory.setStack(i, ItemStack.EMPTY);
                        }
                    }
                }

                syncPokebagToClient(player, bagManager);

                if (remainingToConsume > 0) {
                    CobblemonTS.LOGGER.warn("[UsePokebagItem] item insufficient: need {} got {}",
                            packet.count(), packet.count() - remainingToConsume);
                }
            } catch (Exception e) {
                CobblemonTS.LOGGER.error("[UsePokebagItem] deduct failed", e);
            }
        });
    }

    private static void syncPokebagToClient(ServerPlayerEntity player, Object bagManager) {
        try {
            var inventory = bagManager.getClass().getMethod("getBagInventory").invoke(bagManager);
            if (inventory == null) return;

            var sizeMethod = inventory.getClass().getMethod("getContainerSize");
            int size = (int) sizeMethod.invoke(inventory);

            List<SyncPokebagContentsPacket.ItemEntry> entries = new ArrayList<>();
            var getItemMethod = inventory.getClass().getMethod("getItem", int.class);
            for (int i = 0; i < size; i++) {
                var stack = (ItemStack) getItemMethod.invoke(inventory, i);
                if (!stack.isEmpty()) {
                    String id = Registries.ITEM.getId(stack.getItem()).toString();
                    entries.add(new SyncPokebagContentsPacket.ItemEntry(id, stack.getCount()));
                }
            }

            PacketDistributor.sendToPlayer(player,
                    new SyncPokebagContentsPacket(entries));
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("[UsePokebagItem] sync failed", e);
        }
    }
}