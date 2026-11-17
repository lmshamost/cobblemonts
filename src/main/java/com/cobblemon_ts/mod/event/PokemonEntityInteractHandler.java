package com.cobblemon_ts.mod.event;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.network.UseItemOnPokemonPacket;
import com.cobblemon_ts.mod.network.PacketDistributor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import java.util.UUID;

public class PokemonEntityInteractHandler {

    public static boolean onEntityInteract(net.minecraft.entity.player.PlayerEntity player, net.minecraft.util.Hand hand, net.minecraft.entity.Entity target) {
        if (MinecraftClient.getInstance().player == null) return false;
        if (player != MinecraftClient.getInstance().player) return false;

        ItemStack held = player.getStackInHand(hand);
        if (held.isEmpty()) return false;
        String itemId = Registries.ITEM.getId(held.getItem()).toString();
        if (!"cobblemon_ts:gold_crown".equals(itemId)) return false;

        String className = target.getClass().getName();
        if (!className.equals("com.cobblemon.mod.common.entity.pokemon.PokemonEntity")) return false;

        try {
            var getPokemonMethod = target.getClass().getMethod("getPokemon");
            Object pokemon = getPokemonMethod.invoke(target);
            if (pokemon == null) return false;

            var getUuidMethod = pokemon.getClass().getMethod("getUuid");
            UUID uuid = (UUID) getUuidMethod.invoke(pokemon);

            UseItemOnPokemonPacket packet = new UseItemOnPokemonPacket(uuid, itemId, 1, false, false);
            ClientPlayNetworking.send(packet);

            CobblemonTS.LOGGER.info("EntityInteract: sent gold_crown for {}", uuid);
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("EntityInteract: 金色王冠实体交互失败 - {}", e.getMessage());
            return false;
        }

        return true;
    }
}