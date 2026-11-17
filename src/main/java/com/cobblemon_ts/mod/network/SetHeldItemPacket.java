package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.SecurityHelper;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.UUID;

// [线性-H02] 新增 newHeld 字段：创造模式下服务端 getCarried() 返回 EMPTY，
// 导致手持/携带栏操作被服务端错误逆转。通过把客户端操作的物品直接塞进包体，
// 服务端不再依赖 cursor，创造/生存模式统一使用包内数据。
public record SetHeldItemPacket(UUID pokemonUuid, int slotIndex, net.minecraft.item.ItemStack newHeld) implements CustomPayload {

    public static final Id<SetHeldItemPacket> TYPE = new Id<>(
            Identifier.of("cobblemon_ts", "set_held_item"));

    public static final PacketCodec<RegistryByteBuf, SetHeldItemPacket> STREAM_CODEC = new PacketCodec<>() {
        @Override
        public SetHeldItemPacket decode(RegistryByteBuf buf) {
            return new SetHeldItemPacket(
                    buf.readUuid(),
                    buf.readVarInt(),
                    ItemStack.PACKET_CODEC.decode(buf)  // [线性-H02b] 包内携带 newHeld
            );
        }

        @Override
        public void encode(RegistryByteBuf buf, SetHeldItemPacket packet) {
            buf.writeUuid(packet.pokemonUuid());
            buf.writeVarInt(packet.slotIndex());
            ItemStack.PACKET_CODEC.encode(buf, packet.newHeld());  // [线性-H02b]
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    public static void handleServerSide(SetHeldItemPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayerEntity player)) return;
            // [线性-SEC] 权限校验：仅创造模式或OP
            if (!SecurityHelper.isTrusted(player)) {
                CobblemonTS.LOGGER.warn("[Security] Unauthorized SetHeldItem from {}",
                        player.getName().getString());
                return;
            }
            try {
                var cobblemonInstance = Class.forName("com.cobblemon.mod.common.Cobblemon")
                        .getDeclaredField("INSTANCE").get(null);
                var getStorageMethod = cobblemonInstance.getClass().getMethod("getStorage");
                var storage = getStorageMethod.invoke(cobblemonInstance);

                var getPartyMethod = storage.getClass().getMethod("getParty",
                        Class.forName("net.minecraft.server.network.ServerPlayerEntity"));
                var party = getPartyMethod.invoke(storage, player);

                if (party == null) {
                    CobblemonTS.LOGGER.warn("SetHeldItemPacket: party is null for PlayerEntity {}", player.getName().getString());
                    return;
                }

                var pokemonClass = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
                var getUuidMethod = pokemonClass.getMethod("getUuid");
                var swapHeldItemMethod = pokemonClass.getMethod("swapHeldItem",
                        net.minecraft.item.ItemStack.class, boolean.class, boolean.class);
                var removeHeldItemMethod = pokemonClass.getMethod("removeHeldItem");

                var sizeMethod = party.getClass().getMethod("size");
                var getMethod = party.getClass().getMethod("get", int.class);
                int size = (Integer) sizeMethod.invoke(party);

                for (int i = 0; i < size; i++) {
                    var pokemon = getMethod.invoke(party, i);
                    if (pokemon == null) continue;
                    var uuid = (UUID) getUuidMethod.invoke(pokemon);
                    if (uuid.equals(packet.pokemonUuid())) {
                        // [线性-H02c] 创造模式：服务端 cursor 不可信，用包内 newHeld 决定操作
                        boolean isCreative = player.isInCreativeMode();
                        ItemStack pktItem = packet.newHeld();
                        boolean clientPutting = !pktItem.isEmpty();
                        boolean serverHasCursor;

                        if (!isCreative) {
                            // 生存模式：cursor 可信，按旧逻辑消费物品
                            ItemStack serverCursor = player.currentScreenHandler.getCursorStack().copy();
                            serverHasCursor = !serverCursor.isEmpty();
                            if (serverHasCursor) {
                                ItemStack putStack = serverCursor.copy();
                                putStack.setCount(1);
                                // [线性-H05] swapHeldItem 内部触发 HeldItemUpdatePacket+返回旧物品，单次调用避免双发包
                                ItemStack oldHeld = (ItemStack) swapHeldItemMethod.invoke(pokemon, putStack.copy(), true, false);
                                serverCursor.decrement(1);
                                player.currentScreenHandler.setCursorStack(serverCursor.isEmpty() ? ItemStack.EMPTY : serverCursor);
                                if (oldHeld != null && !oldHeld.isEmpty()) {
                                    if (!player.getInventory().insertStack(oldHeld)) {
                                        player.dropItem(oldHeld, false);
                                    }
                                }
                            } else {
                                ItemStack oldHeld = (ItemStack) removeHeldItemMethod.invoke(pokemon);
                                if (oldHeld != null && !oldHeld.isEmpty()) {
                                    player.currentScreenHandler.setCursorStack(oldHeld);
                                }
                            }
                        } else {
                            // [线性-H02c] 创造模式：不碰 cursor（cursor 是客户端态），直接用包内数据
                            // [线性-H05] swapHeldItem 单次调用完成替换+返回旧物品，避免 removeHeldItem 双发包
                            if (clientPutting) {
                                ItemStack oldHeld = (ItemStack) swapHeldItemMethod.invoke(pokemon, pktItem.copy(), true, false);
                                if (oldHeld != null && !oldHeld.isEmpty()) {
                                    if (!player.getInventory().insertStack(oldHeld)) {
                                        player.dropItem(oldHeld, false);
                                    }
                                }
                            } else {
                                // [线性-H06] 创造模式空光标=直接清空携带槽，物品消失（不进入背包/光标）
                                removeHeldItemMethod.invoke(pokemon);
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