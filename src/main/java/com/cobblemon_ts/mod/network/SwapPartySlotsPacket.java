package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.SecurityHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public record SwapPartySlotsPacket(int slot1, int slot2) implements CustomPacketPayload {

    public static final Type<SwapPartySlotsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "swap_party_slots"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwapPartySlotsPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SwapPartySlotsPacket decode(RegistryFriendlyByteBuf buf) {
            return new SwapPartySlotsPacket(buf.readInt(), buf.readInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SwapPartySlotsPacket packet) {
            buf.writeInt(packet.slot1());
            buf.writeInt(packet.slot2());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // ===== 反射：服务端一次性准备好，后续复用 =====
    private static boolean reflectionReady = false;
    private static Object cobblemonInstance;
    private static Method getStorageMethod;
    private static Method storageGetPartyMethod;
    private static Class<?> pokemonClass;
    private static Class<?> partyPositionClass;
    private static Constructor<?> partyPositionCtor;

    private static Method partyGetMethod;     // PartyStore.get(int) -> Pokemon
    private static Method partySendToMethod;  // PartyStore.sendTo(ServerPlayer)

    private static void initReflection(ServerPlayer player) {
        if (reflectionReady) return;
        try {
            CobblemonTS.LOGGER.info("[SwapSlots] init reflection start");

            Class<?> cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            cobblemonInstance = cobblemonClass.getField("INSTANCE").get(null);

            getStorageMethod = cobblemonClass.getMethod("getStorage");
            Object storage = getStorageMethod.invoke(cobblemonInstance);
            if (storage == null) {
                CobblemonTS.LOGGER.error("[SwapSlots] storage is null");
                return;
            }

            storageGetPartyMethod = storage.getClass().getMethod("getParty", ServerPlayer.class);
            Object party = storageGetPartyMethod.invoke(storage, player);
            if (party == null) {
                CobblemonTS.LOGGER.error("[SwapSlots] party is null");
                return;
            }
            CobblemonTS.LOGGER.info("[SwapSlots] party class: {}", party.getClass().getName());

            pokemonClass = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");

            partyPositionClass = Class.forName("com.cobblemon.mod.common.api.storage.party.PartyPosition");
            partyPositionCtor = partyPositionClass.getConstructor(int.class);

            // PartyStore.get(int) - 从实际 party 类查找
            Class<?> partyCls = party.getClass();
            try {
                partyGetMethod = partyCls.getMethod("get", int.class);
            } catch (NoSuchMethodException e) {
                partyGetMethod = partyCls.getSuperclass().getMethod("get", int.class);
            }
            CobblemonTS.LOGGER.info("[SwapSlots] party.get(int) found");

            // PartyStore.sendTo(ServerPlayer) - 多签名尝试
            try {
                partySendToMethod = partyCls.getMethod("sendTo", ServerPlayer.class);
            } catch (NoSuchMethodException e1) {
                try {
                    partySendToMethod = partyCls.getMethod("sendTo",
                            Class.forName("net.minecraft.world.entity.player.Player"));
                } catch (NoSuchMethodException e2) {
                    // 向上遍历所有父类
                    Class<?> c = partyCls.getSuperclass();
                    while (c != null && partySendToMethod == null) {
                        for (Method m : c.getMethods()) {
                            if (m.getName().equals("sendTo") && m.getParameterCount() == 1) {
                                partySendToMethod = m;
                                break;
                            }
                        }
                        c = c.getSuperclass();
                    }
                }
            }
            if (partySendToMethod != null) {
                CobblemonTS.LOGGER.info("[SwapSlots] party.sendTo found: param={}",
                        partySendToMethod.getParameterTypes()[0].getSimpleName());
            } else {
                CobblemonTS.LOGGER.warn("[SwapSlots] party.sendTo NOT FOUND");
            }

            reflectionReady = true;
            CobblemonTS.LOGGER.info("[SwapSlots] reflection ready");
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("[SwapSlots] init reflection FAILED: {}", e.toString());
        }
    }

    /** 动态查找 setAtPosition 方法（protected，必须从对象实际类向上遍历） */
    private static Method findSetAtPosition(Object party) {
        Class<?> cls = party.getClass();
        while (cls != null) {
            try {
                Method m = cls.getDeclaredMethod("setAtPosition", partyPositionClass, pokemonClass);
                m.setAccessible(true);
                CobblemonTS.LOGGER.info("[SwapSlots] setAtPosition found in {}", cls.getSimpleName());
                return m;
            } catch (NoSuchMethodException ignored) {}
            cls = cls.getSuperclass();
        }
        CobblemonTS.LOGGER.warn("[SwapSlots] setAtPosition NOT FOUND in party hierarchy");
        return null;
    }

    public static void handleServerSide(SwapPartySlotsPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                ServerPlayer player = (ServerPlayer) ctx.player();
                int s1 = pkt.slot1();
                int s2 = pkt.slot2();

                if (s1 < 0 || s1 >= 6 || s2 < 0 || s2 >= 6 || s1 == s2) {
                    CobblemonTS.LOGGER.warn("[SwapSlots] invalid slots: {} <-> {}", s1, s2);
                    return;
                }

                initReflection(player);
                if (!reflectionReady) {
                    CobblemonTS.LOGGER.error("[SwapSlots] reflection not ready");
                    return;
                }

                Object storage = getStorageMethod.invoke(cobblemonInstance);
                Object party = storageGetPartyMethod.invoke(storage, player);
                if (party == null) {
                    CobblemonTS.LOGGER.error("[SwapSlots] party is null");
                    return;
                }

                // 读取两个槽位的 Pokemon
                Object pk1 = partyGetMethod.invoke(party, s1);
                Object pk2 = partyGetMethod.invoke(party, s2);
                CobblemonTS.LOGGER.info("[SwapSlots] swapping slot {}({}) <-> slot {}({})",
                        s1, pk1 != null ? "filled" : "empty",
                        s2, pk2 != null ? "filled" : "empty");

                // 用 setAtPosition 交换
                Method setAtPos = findSetAtPosition(party);
                if (setAtPos == null) {
                    CobblemonTS.LOGGER.error("[SwapSlots] setAtPosition method not available");
                    return;
                }

                Object pos1 = partyPositionCtor.newInstance(s1);
                Object pos2 = partyPositionCtor.newInstance(s2);

                // setAtPosition(slot1, pk2), setAtPosition(slot2, pk1)
                setAtPos.invoke(party, pos1, pk2);
                setAtPos.invoke(party, pos2, pk1);
                CobblemonTS.LOGGER.info("[SwapSlots] swap completed");

                // 同步到客户端（通过 onPokemonChanged + sendTo 同步，不做文字提示）
                try {
                    Method onChangedM = party.getClass().getMethod("onPokemonChanged", pokemonClass);
                    if (pk1 != null) onChangedM.invoke(party, pk1);
                    if (pk2 != null) onChangedM.invoke(party, pk2);
                } catch (NoSuchMethodException ignored) {}
                if (partySendToMethod != null) {
                    partySendToMethod.invoke(party, player);
                }
            } catch (Exception e) {
                CobblemonTS.LOGGER.error("[SwapSlots] FAILED: {}", e.toString());
                e.printStackTrace();
            }
        });
    }
}