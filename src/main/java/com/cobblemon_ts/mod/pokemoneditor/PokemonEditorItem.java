package com.cobblemon_ts.mod.pokemoneditor;

import com.cobblemon_ts.mod.CharmConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class PokemonEditorItem extends Item {

    public PokemonEditorItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        PokemonEditorDebug.log("use() called - world.isClientSide=" + world.isClientSide
                + ", isCreative=" + player.isCreative() + ", hand=" + hand);

        if (!world.isClientSide && hand == InteractionHand.MAIN_HAND) {
            PokemonEditorDebug.log("use() - conditions passed, checking permission...");
            if (checkPermission((ServerPlayer) player)) {
                PokemonEditorDebug.log("use() - sending open editor packet for self: " + player.getName().getString());
                PokemonEditorNetwork.sendOpenEditorPacket((ServerPlayer) player, player.getUUID(), player.getName().getString());
            }
        }
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, net.minecraft.world.entity.Entity entity) {
        PokemonEditorDebug.log("onLeftClickEntity() called - isServerSide=" + (player instanceof ServerPlayer)
                + ", isCreative=" + player.isCreative() + ", entity=" + entity.getClass().getSimpleName());

        if (player instanceof ServerPlayer serverPlayer) {
            if (checkPermission(serverPlayer)) {
                if (entity instanceof ServerPlayer otherPlayer) {
                    PokemonEditorDebug.log("onLeftClickEntity() - editing player: " + otherPlayer.getName().getString());
                    PokemonEditorNetwork.sendOpenEditorPacket(serverPlayer, otherPlayer.getUUID(), otherPlayer.getName().getString());
                    return true;
                } else {
                    PokemonEditorDebug.log("onLeftClickEntity() - entity is not a player, ignoring");
                }
            }
        }
        return false;
    }

    public static boolean checkPermission(ServerPlayer player) {
        // 从配置读取：是否要求必须是创造模式
        boolean requireCreative;
        try {
            requireCreative = CharmConfig.REQUIRE_CREATIVE.get();
        } catch (Exception e) {
            requireCreative = true; // 配置未加载时默认 true
        }
        PokemonEditorDebug.log("checkPermission() - require_creative=" + requireCreative
                + " isCreative=" + player.isCreative() + " hasOpLevel2=" + player.hasPermissions(2));

        // 必须是 OP（权限等级 2 以上）才能使用
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("你没有使用宝可梦编辑器的权限！"));
            PokemonEditorDebug.log("checkPermission() - no OP permission, rejected");
            return false;
        }

        // 如果配置要求必须创造模式，则额外检查是否是创造模式
        if (requireCreative && !player.isCreative()) {
            player.sendSystemMessage(Component.literal("必须在创造模式下才能使用宝可梦编辑器！"));
            PokemonEditorDebug.log("checkPermission() - not creative mode, rejected");
            return false;
        }

        PokemonEditorDebug.log("checkPermission() - permission granted");
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext world, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, world, tooltip, flagIn);
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.translatable("item.coblemon_ts.pokemon_editor.tooltip.one"));
        tooltip.add(Component.translatable("item.coblemon_ts.pokemon_editor.tooltip.two"));
    }
}