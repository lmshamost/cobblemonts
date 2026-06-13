package com.cobblemon_ts.mod.network;

import com.cobblemon_ts.mod.CobblemonTS;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record UseItemOnPokemonPacket(UUID pokemonUuid, String itemId, int count) implements CustomPacketPayload {

    public static final Type<UseItemOnPokemonPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("coblemon_ts", "use_item_on_pokemon"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UseItemOnPokemonPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public UseItemOnPokemonPacket decode(RegistryFriendlyByteBuf buf) {
            return new UseItemOnPokemonPacket(
                    buf.readUUID(),
                    buf.readUtf(),
                    buf.readVarInt()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, UseItemOnPokemonPacket packet) {
            buf.writeUUID(packet.pokemonUuid());
            buf.writeUtf(packet.itemId());
            buf.writeVarInt(packet.count());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServerSide(UseItemOnPokemonPacket packet, IPayloadContext context) {
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
                    CobblemonTS.LOGGER.warn("UseItemOnPokemonPacket: party is null for player {}",
                            serverPlayer.getName().getString());
                    return;
                }

                var pokemonClass = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
                var getUuidMethod = pokemonClass.getMethod("getUuid");
                var getCurrentHealthMethod = pokemonClass.getMethod("getCurrentHealth");
                var getMaxHealthMethod = pokemonClass.getMethod("getMaxHealth");
                var setCurrentHealthMethod = pokemonClass.getMethod("setCurrentHealth", int.class);
                var healMethod = pokemonClass.getMethod("heal");
                var isFaintedMethod = pokemonClass.getMethod("isFainted");
                var getStatusMethod = pokemonClass.getMethod("getStatus");
                var setStatusMethod = pokemonClass.getMethod("setStatus",
                        Class.forName("com.cobblemon.mod.common.pokemon.status.PersistentStatusContainer"));

                var sizeMethod = party.getClass().getMethod("size");
                var getMethod = party.getClass().getMethod("get", int.class);
                int size = (Integer) sizeMethod.invoke(party);

                for (int i = 0; i < size; i++) {
                    var pokemon = getMethod.invoke(party, i);
                    if (pokemon == null) continue;
                    var uuid = (UUID) getUuidMethod.invoke(pokemon);
                    if (uuid.equals(packet.pokemonUuid())) {
                        // 优先用 PokemonSelectingItem 原生逻辑（糖果/进化石等）
                        if (tryPokemonSelectingItem(serverPlayer, pokemon, packet)) {
                            // applyToPokemon 内部已处理物品消耗，不需要额外 consumeItem
                        } else if (applyItemEffect(pokemon, packet, getCurrentHealthMethod, getMaxHealthMethod,
                                setCurrentHealthMethod, healMethod, isFaintedMethod,
                                getStatusMethod, setStatusMethod)) {
                            // 只有治疗/复活等逻辑真正生效了才消耗物品
                            consumeItem(serverPlayer, packet);
                        }
                        // 否则什么都不做——物品既不是 PokemonSelectingItem 也不是治疗类，不消耗
                        break;
                    }
                }
            } catch (Exception e) {
                CobblemonTS.LOGGER.error("UseItemOnPokemonPacket: 处理失败", e);
            }
        });
    }

    private static boolean applyItemEffect(Object pokemon, UseItemOnPokemonPacket packet,
                                        java.lang.reflect.Method getCurrentHealthMethod,
                                        java.lang.reflect.Method getMaxHealthMethod,
                                        java.lang.reflect.Method setCurrentHealthMethod,
                                        java.lang.reflect.Method healMethod,
                                        java.lang.reflect.Method isFaintedMethod,
                                        java.lang.reflect.Method getStatusMethod,
                                        java.lang.reflect.Method setStatusMethod) throws Exception {
        String itemId = packet.itemId();
        int count = packet.count();

        boolean isHealing = switch (itemId) {
            case "cobblemon:potion", "cobblemon:super_potion",
                 "cobblemon:hyper_potion", "cobblemon:max_potion",
                 "cobblemon:full_restore" -> true;
            default -> false;
        };

        boolean isStatusCure = switch (itemId) {
            case "cobblemon:antidote", "cobblemon:awakening",
                 "cobblemon:burn_heal", "cobblemon:ice_heal",
                 "cobblemon:paralyze_heal", "cobblemon:full_heal",
                 "cobblemon:full_restore", "cobblemon:remedy",
                 "cobblemon:fine_remedy", "cobblemon:superb_remedy",
                 "cobblemon:heal_powder" -> true;
            default -> false;
        };

        boolean isRevive = switch (itemId) {
            case "cobblemon:revive", "cobblemon:max_revive" -> true;
            default -> false;
        };

        boolean anyEffect = false;

        if (isHealing) {
            int healAmount = switch (itemId) {
                case "cobblemon:potion" -> 20;
                case "cobblemon:super_potion" -> 60;
                case "cobblemon:hyper_potion" -> 120;
                case "cobblemon:max_potion", "cobblemon:full_restore" -> Integer.MAX_VALUE;
                default -> 0;
            };

            int currentHp = (Integer) getCurrentHealthMethod.invoke(pokemon);
            int maxHp = (Integer) getMaxHealthMethod.invoke(pokemon);
            if (currentHp < maxHp) {
                anyEffect = true;
                if (healAmount == Integer.MAX_VALUE) {
                    healMethod.invoke(pokemon);
                } else {
                    for (int c = 0; c < count; c++) {
                        currentHp = Math.min(maxHp, currentHp + healAmount);
                    }
                    setCurrentHealthMethod.invoke(pokemon, currentHp);
                }
            }
        }

        if (isStatusCure) {
            Object status = getStatusMethod.invoke(pokemon);
            if (status != null) {
                java.lang.reflect.Method sizeMethod = status.getClass().getMethod("size");
                int statusCount = (Integer) sizeMethod.invoke(status);
                if (statusCount > 0) {
                    anyEffect = true;
                    Class<?> containerClass = Class.forName(
                            "com.cobblemon.mod.common.pokemon.status.PersistentStatusContainer");
                    Object emptyContainer = containerClass.getDeclaredConstructor().newInstance();
                    setStatusMethod.invoke(pokemon, emptyContainer);
                }
            }
        }

        if (isRevive) {
            boolean fainted = (Boolean) isFaintedMethod.invoke(pokemon);
            if (fainted) {
                anyEffect = true;
                int maxHp = (Integer) getMaxHealthMethod.invoke(pokemon);
                int restoreHp = itemId.equals("cobblemon:max_revive") ? maxHp : maxHp / 2;
                setCurrentHealthMethod.invoke(pokemon, restoreHp);
                Class<?> containerClass = Class.forName(
                        "com.cobblemon.mod.common.pokemon.status.PersistentStatusContainer");
                Object emptyContainer = containerClass.getDeclaredConstructor().newInstance();
                setStatusMethod.invoke(pokemon, emptyContainer);
            }
        }

        return anyEffect;
    }

    private static boolean tryPokemonSelectingItem(ServerPlayer player, Object pokemon,
                                                   UseItemOnPokemonPacket packet) {
        try {
            ItemStack cursorStack = player.containerMenu.getCarried();
            if (cursorStack.isEmpty()) return false;

            if (!(cursorStack.getItem() instanceof com.cobblemon.mod.common.api.item.PokemonSelectingItem selectingItem)) {
                return false;
            }

            // 检查是否能对这只精灵使用
            if (!selectingItem.canUseOnPokemon(cursorStack, 
                    (com.cobblemon.mod.common.pokemon.Pokemon) pokemon)) {
                return false;
            }

            // 应用物品
            for (int i = 0; i < packet.count(); i++) {
                var result = selectingItem.applyToPokemon(player, cursorStack,
                        (com.cobblemon.mod.common.pokemon.Pokemon) pokemon);
                // applyToPokemon 返回 InteractionResultHolder，如果失败就停止
                if (result.getResult() != net.minecraft.world.InteractionResult.SUCCESS) {
                    return i > 0; // 如果已经用了至少1个，也算成功
                }
            }
            return true;
        } catch (Exception e) {
            CobblemonTS.LOGGER.warn("PokemonSelectingItem use failed: {}", e.getMessage());
            return false;
        }
    }

    private static void consumeItem(ServerPlayer player, UseItemOnPokemonPacket packet) {
        ItemStack cursorStack = player.containerMenu.getCarried();
        if (!cursorStack.isEmpty()) {
            int shrinkCount = Math.min(packet.count(), cursorStack.getCount());
            cursorStack.shrink(shrinkCount);
            if (cursorStack.isEmpty()) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }
        }
    }
}