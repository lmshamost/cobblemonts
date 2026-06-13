package com.cobblemon_ts.mod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Method;
import java.util.UUID;

public record PartyToPCPacket(UUID pokemonUuid) implements CustomPacketPayload {

    public static final Type<PartyToPCPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "party_to_pc"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PartyToPCPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PartyToPCPacket decode(RegistryFriendlyByteBuf buf) {
            return new PartyToPCPacket(buf.readUUID());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, PartyToPCPacket packet) {
            buf.writeUUID(packet.pokemonUuid());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static boolean pcReflectionReady;
    private static Object cobblemonInstance;
    private static Method getStorageMethod;
    private static Method storageGetPartyMethod;
    private static Method storageGetPCMethod;
    private static Method partyGetMethod;
    private static Method pokemonGetUuidMethod;
    private static Method partyOccupiedMethod;
    private static Method getConfigMethod;
    private static Method getPreventCompletePartyDepositMethod;
    private static Method pcGetFirstAvailablePositionMethod;
    private static Method pcSetMethod;
    private static Class<?> pokemonClass;
    private static Object storageManagerRef;

    private static Class<?> partyStoreClass;
    private static Class<?> pcStoreClass;
    private static Class<?> pcPositionClass;
    private static Method partyRemoveMethod;
    private static Method partySendToMethod;

    private static void initPCReflection() {
        if (pcReflectionReady) return;
        try {
            Class<?> cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            cobblemonInstance = cobblemonClass.getField("INSTANCE").get(null);

            getStorageMethod = cobblemonClass.getMethod("getStorage");
            storageManagerRef = getStorageMethod.invoke(cobblemonInstance);
            com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] storageManager={}", storageManagerRef);

            storageGetPartyMethod = storageManagerRef.getClass().getMethod("getParty", ServerPlayer.class);
            com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] getParty method OK");

            storageGetPCMethod = storageManagerRef.getClass().getMethod("getPC", ServerPlayer.class);
            com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] getPC method OK");

            partyStoreClass = Class.forName("com.cobblemon.mod.common.api.storage.party.PartyStore");
            partyGetMethod = partyStoreClass.getMethod("get", int.class);
            partyOccupiedMethod = partyStoreClass.getMethod("occupied");
            com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] party methods OK");

            pokemonClass = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
            pokemonGetUuidMethod = pokemonClass.getMethod("getUuid");
            com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] pokemon methods OK");

            getConfigMethod = cobblemonClass.getMethod("getConfig");
            Object config = getConfigMethod.invoke(cobblemonInstance);
            getPreventCompletePartyDepositMethod = config.getClass().getMethod("getPreventCompletePartyDeposit");
            com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] config methods OK");

            pcStoreClass = Class.forName("com.cobblemon.mod.common.api.storage.pc.PCStore");
            pcPositionClass = Class.forName("com.cobblemon.mod.common.api.storage.pc.PCPosition");
            pcGetFirstAvailablePositionMethod = pcStoreClass.getMethod("getFirstAvailablePosition");
            pcSetMethod = pcStoreClass.getMethod("set", pcPositionClass, pokemonClass);
            com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] pcStore methods OK");

            // remove + sendTo：移除宝可梦 & 同步客户端
            partyRemoveMethod = partyStoreClass.getMethod("remove", pokemonClass);
            partySendToMethod = partyStoreClass.getMethod("sendTo", ServerPlayer.class);
            com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] remove/sendTo methods OK");

            pcReflectionReady = true;
            com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] all reflection ready");
        } catch (Exception e) {
            com.cobblemon_ts.mod.CobblemonTS.LOGGER.error("PartyToPCPacket reflection init failed: {}", e.toString());
        }
    }

    public static void handleServerSide(PartyToPCPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                initPCReflection();
                if (!pcReflectionReady) {
                    com.cobblemon_ts.mod.CobblemonTS.LOGGER.error("[PartyToPC] reflection not ready");
                    return;
                }

                ServerPlayer player = (ServerPlayer) context.player();
                com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] received packet for uuid={} player={}",
                        packet.pokemonUuid(), player.getName().getString());

                Object storageManager = getStorageMethod.invoke(cobblemonInstance);
                Object party = storageGetPartyMethod.invoke(storageManager, player);
                Object pc = storageGetPCMethod.invoke(storageManager, player);
                if (pc == null) {
                    com.cobblemon_ts.mod.CobblemonTS.LOGGER.warn("[PartyToPC] pc is null via storageManager.getPC");
                    return;
                }
                com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] got pc={}", pc);

                // 在队伍中找到这只宝可梦的槽位
                Object pokemon = null;
                int slotIndex = -1;
                for (int i = 0; i < 6; i++) {
                    Object p = partyGetMethod.invoke(party, i);
                    if (p != null && packet.pokemonUuid().equals(pokemonGetUuidMethod.invoke(p))) {
                        pokemon = p;
                        slotIndex = i;
                        break;
                    }
                }
                if (pokemon == null || slotIndex < 0) {
                    com.cobblemon_ts.mod.CobblemonTS.LOGGER.warn("[PartyToPC] pokemon not found in party");
                    return;
                }
                com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] found pokemon slot={}", slotIndex);

                boolean preventLast = (boolean) getPreventCompletePartyDepositMethod.invoke(
                        getConfigMethod.invoke(cobblemonInstance));
                int occupied = (int) partyOccupiedMethod.invoke(party);
                if (occupied <= 1 && preventLast) {
                    com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] blocked: last pokemon (occupied={})", occupied);
                    player.displayClientMessage(
                            Component.literal("不能放生最后一只宝可梦"), false);
                    return;
                }

                Object pcPos = pcGetFirstAvailablePositionMethod.invoke(pc);
                if (pcPos == null) {
                    com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] pc full");
                    player.displayClientMessage(
                            Component.literal("电脑已满"), false);
                    return;
                }
                com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] depositing slot={} to pc box", slotIndex);

                // 1. 放入 PC
                pcSetMethod.invoke(pc, pcPos, pokemon);
                com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] pc.set() OK");

                // 2. 用 setAtPosition(PartyPosition(slotIndex), null) 清空槽位
                // ★ 不能用 party.remove(pokemon)！会把后面宝可梦往前移，压缩队伍，导致精灵栏"凭空刷出一只"
                try {
                    Class<?> ppClass = Class.forName("com.cobblemon.mod.common.api.storage.party.PartyPosition");
                    java.lang.reflect.Constructor<?> ppCtor = ppClass.getConstructor(int.class);
                    Object position = ppCtor.newInstance(slotIndex);
                    Class<?> partyPosClass = Class.forName("com.cobblemon.mod.common.api.storage.party.PartyPosition");
                    Method setAtPosM = null;
                    Class<?> cls = party.getClass();
                    while (cls != null && setAtPosM == null) {
                        try { setAtPosM = cls.getDeclaredMethod("setAtPosition", partyPosClass, pokemonClass); }
                        catch (NoSuchMethodException ignored) {}
                        cls = cls.getSuperclass();
                    }
                    if (setAtPosM != null) {
                        setAtPosM.setAccessible(true);
                        setAtPosM.invoke(party, position, null);
                        com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] setAtPosition(slot={}, null) OK", slotIndex);
                    } else {
                        partyRemoveMethod.invoke(party, pokemon);
                        com.cobblemon_ts.mod.CobblemonTS.LOGGER.warn("[PartyToPC] setAtPosition not found, fallback to remove()");
                    }
                } catch (Throwable t) {
                    partyRemoveMethod.invoke(party, pokemon);
                    com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] party.remove(pokemon) OK (fallback)");
                }

                // 3. 同步队伍状态到客户端（sendTo 发送完整队伍数据）
                partySendToMethod.invoke(party, player);
                com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] party.sendTo(player) OK - synced to client");

                com.cobblemon_ts.mod.CobblemonTS.LOGGER.info("[PartyToPC] deposit success!");
                player.displayClientMessage(
                        Component.literal("已存入电脑"), false);
            } catch (Exception e) {
                com.cobblemon_ts.mod.CobblemonTS.LOGGER.error("PartyToPCPacket failed: {}", e.toString());
            }
        });
    }
}