package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record SetHeldItemPacket(UUID pokemonUuid, int slotIndex) implements CustomPacketPayload {

    public static final Type<SetHeldItemPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "set_held_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetHeldItemPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SetHeldItemPacket decode(RegistryFriendlyByteBuf buf) {
            return new SetHeldItemPacket(
                    buf.readUUID(),
                    buf.readVarInt()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SetHeldItemPacket packet) {
            buf.writeUUID(packet.pokemonUuid());
            buf.writeVarInt(packet.slotIndex());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServerSide(SetHeldItemPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
            try {
                var cobblemonInstance = Class.forName("com.cobblemon.mod.common.Cobblemon")
                        .getDeclaredField("INSTANCE").get(null);
                var getStorageMethod = cobblemonInstance.getClass().getMethod("getStorage");
                var storage = getStorageMethod.invoke(cobblemonInstance);

                var getPartyMethod = storage.getClass().getMethod("getParty",
                        Class.forName("net.minecraft.server.level.ServerPlayer"));
                var party = getPartyMethod.invoke(storage, serverPlayer);

                if (party == null) {
                    CobblemonTS.LOGGER.warn("SetHeldItemPacket: party is null for player {}", serverPlayer.getName().getString());
                    return;
                }

                var pokemonClass = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
                var getUuidMethod = pokemonClass.getMethod("getUuid");
                var swapHeldItemMethod = pokemonClass.getMethod("swapHeldItem",
                        net.minecraft.world.item.ItemStack.class, boolean.class, boolean.class);
                var removeHeldItemMethod = pokemonClass.getMethod("removeHeldItem");

                var sizeMethod = party.getClass().getMethod("size");
                var getMethod = party.getClass().getMethod("get", int.class);
                int size = (Integer) sizeMethod.invoke(party);

                for (int i = 0; i < size; i++) {
                    var pokemon = getMethod.invoke(party, i);
                    if (pokemon == null) continue;
                    var uuid = (UUID) getUuidMethod.invoke(pokemon);
                    if (uuid.equals(packet.pokemonUuid())) {
                        ItemStack serverCursor = serverPlayer.containerMenu.getCarried().copy();
                        boolean serverHasCursor = !serverCursor.isEmpty();

                        if (serverHasCursor) {
                            // 鼠标有物品 → 放1个到携带栏
                            ItemStack putStack = serverCursor.copy();
                            putStack.setCount(1);

                            // 先取出旧携带物（如果有）
                            ItemStack oldHeld = (ItemStack) removeHeldItemMethod.invoke(pokemon);

                            // 放入新物品
                            swapHeldItemMethod.invoke(pokemon, putStack.copy(), true, false);

                            // 缩小服务端鼠标上的物品数量
                            serverCursor.shrink(1);
                            serverPlayer.containerMenu.setCarried(serverCursor.isEmpty() ? ItemStack.EMPTY : serverCursor);

                            // 旧携带物退回玩家物品栏
                            if (oldHeld != null && !oldHeld.isEmpty()) {
                                if (!serverPlayer.getInventory().add(oldHeld)) {
                                    serverPlayer.drop(oldHeld, false);
                                }
                            }
                        } else {
                            // 鼠标空 → 取出携带物到鼠标
                            ItemStack oldHeld = (ItemStack) removeHeldItemMethod.invoke(pokemon);
                            if (oldHeld != null && !oldHeld.isEmpty()) {
                                serverPlayer.containerMenu.setCarried(oldHeld);
                            }
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                CobblemonTS.LOGGER.error("SetHeldItemPacket: 处理失败", e);
            }
        });
    }
}