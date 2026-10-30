package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.SecurityHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

// [线性-H02] 新增 newHeld 字段：创造模式下服务端 getCarried() 返回 EMPTY，
// 导致手持/携带栏操作被服务端错误逆转。通过把客户端操作的物品直接塞进包体，
// 服务端不再依赖 cursor，创造/生存模式统一使用包内数据。
public record SetHeldItemPacket(UUID pokemonUuid, int slotIndex, net.minecraft.world.item.ItemStack newHeld) implements CustomPacketPayload {

    public static final Type<SetHeldItemPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "set_held_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetHeldItemPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SetHeldItemPacket decode(RegistryFriendlyByteBuf buf) {
            return new SetHeldItemPacket(
                    buf.readUUID(),
                    buf.readVarInt(),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)  // [线性-H02b] 包内携带 newHeld
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SetHeldItemPacket packet) {
            buf.writeUUID(packet.pokemonUuid());
            buf.writeVarInt(packet.slotIndex());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, packet.newHeld());  // [线性-H02b]
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServerSide(SetHeldItemPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
            // [线性-SEC] 权限校验：仅创造模式或OP
            if (!SecurityHelper.isTrusted(serverPlayer)) {
                CobblemonTS.LOGGER.warn("[Security] Unauthorized SetHeldItem from {}",
                        serverPlayer.getName().getString());
                return;
            }
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
                        // [线性-H02c] 创造模式：服务端 cursor 不可信，用包内 newHeld 决定操作
                        boolean isCreative = serverPlayer.isCreative();
                        ItemStack pktItem = packet.newHeld();
                        boolean clientPutting = !pktItem.isEmpty();
                        boolean serverHasCursor;

                        if (!isCreative) {
                            // 生存模式：cursor 可信，按旧逻辑消费物品
                            ItemStack serverCursor = serverPlayer.containerMenu.getCarried().copy();
                            serverHasCursor = !serverCursor.isEmpty();
                            if (serverHasCursor) {
                                ItemStack putStack = serverCursor.copy();
                                putStack.setCount(1);
                                // [线性-H05] swapHeldItem 内部触发 HeldItemUpdatePacket+返回旧物品，单次调用避免双发包
                                ItemStack oldHeld = (ItemStack) swapHeldItemMethod.invoke(pokemon, putStack.copy(), true, false);
                                serverCursor.shrink(1);
                                serverPlayer.containerMenu.setCarried(serverCursor.isEmpty() ? ItemStack.EMPTY : serverCursor);
                                if (oldHeld != null && !oldHeld.isEmpty()) {
                                    if (!serverPlayer.getInventory().add(oldHeld)) {
                                        serverPlayer.drop(oldHeld, false);
                                    }
                                }
                            } else {
                                ItemStack oldHeld = (ItemStack) removeHeldItemMethod.invoke(pokemon);
                                if (oldHeld != null && !oldHeld.isEmpty()) {
                                    serverPlayer.containerMenu.setCarried(oldHeld);
                                }
                            }
                        } else {
                            // [线性-H02c] 创造模式：不碰 cursor（cursor 是客户端态），直接用包内数据
                            // [线性-H05] swapHeldItem 单次调用完成替换+返回旧物品，避免 removeHeldItem 双发包
                            if (clientPutting) {
                                ItemStack oldHeld = (ItemStack) swapHeldItemMethod.invoke(pokemon, pktItem.copy(), true, false);
                                if (oldHeld != null && !oldHeld.isEmpty()) {
                                    if (!serverPlayer.getInventory().add(oldHeld)) {
                                        serverPlayer.drop(oldHeld, false);
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