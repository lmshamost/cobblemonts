package com.cobblemon_ts.mod.event;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.network.UseItemOnPokemonPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * 金色王冠右键对Pokemon实体使用
 * 拦截 PlayerInteractEvent.EntityInteract，发送 UseItemOnPokemonPacket 到服务端
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class PokemonEntityInteractHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (Minecraft.getInstance().player == null) return;

        var player = event.getEntity();
        if (player != Minecraft.getInstance().player) return;

        // ★ 检查手持物品是否为金色王冠
        ItemStack held = player.getItemInHand(event.getHand());
        if (held.isEmpty()) return;
        String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        if (!"coblemon_ts:gold_crown".equals(itemId)) return;

        // ★ 检查目标实体是否为cobblemon的PokemonEntity
        var target = event.getTarget();
        String className = target.getClass().getName();
        if (!className.equals("com.cobblemon.mod.common.entity.pokemon.PokemonEntity")) return;

        // ★ 获取Pokemon实体上的Pokemon对象的UUID
        try {
            var getPokemonMethod = target.getClass().getMethod("getPokemon");
            Object pokemon = getPokemonMethod.invoke(target);
            if (pokemon == null) return;

            var getUuidMethod = pokemon.getClass().getMethod("getUuid");
            UUID uuid = (UUID) getUuidMethod.invoke(pokemon);

            // ★ 发送到服务端处理
            UseItemOnPokemonPacket packet = new UseItemOnPokemonPacket(uuid, itemId, 1, false);
            PacketDistributor.sendToServer(packet);

            CobblemonTS.LOGGER.info("EntityInteract: sent gold_crown for {}", uuid);
        } catch (Exception e) {
            CobblemonTS.LOGGER.error("EntityInteract: 金色王冠实体交互失败 - {}", e.getMessage());
            return;
        }

        // ★ 取消事件，阻止打开Summary界面
        event.setCanceled(true);
    }
}