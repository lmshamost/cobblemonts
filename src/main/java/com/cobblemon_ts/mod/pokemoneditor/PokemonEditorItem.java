package com.cobblemon_ts.mod.pokemoneditor;

import com.cobblemon_ts.mod.CharmConfig;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.world.World;

import java.util.List;

public class PokemonEditorItem extends Item {

    public PokemonEditorItem() {
        super(new Item.Settings().maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        PokemonEditorDebug.log("use() called - world.isClient=" + world.isClient
                + ", isCreative=" + user.isInCreativeMode() + ", hand=" + hand);

        if (!world.isClient && hand == Hand.MAIN_HAND) {
            PokemonEditorDebug.log("use() - conditions passed, checking permission...");
            ServerPlayerEntity serverUser = (ServerPlayerEntity) user;
            if (checkPermission(serverUser)) {
                PokemonEditorDebug.log("use() - sending open editor packet for self: " + user.getName().getString());
                PokemonEditorNetwork.sendOpenEditorPacket(serverUser, user.getUuid(), user.getName().getString());
            }
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    public boolean onLeftClickEntity(ItemStack stack, PlayerEntity user, net.minecraft.entity.Entity entity) {
        PokemonEditorDebug.log("onLeftClickEntity() called - isServerSide=" + (user instanceof ServerPlayerEntity)
                + ", isCreative=" + user.isInCreativeMode() + ", entity=" + entity.getClass().getSimpleName());

        if (user instanceof ServerPlayerEntity serverUser) {
            if (checkPermission(serverUser)) {
                if (entity instanceof ServerPlayerEntity otherPlayer) {
                    PokemonEditorDebug.log("onLeftClickEntity() - editing player: " + otherPlayer.getName().getString());
                    PokemonEditorNetwork.sendOpenEditorPacket(serverUser, otherPlayer.getUuid(), otherPlayer.getName().getString());
                    return true;
                } else {
                    PokemonEditorDebug.log("onLeftClickEntity() - entity is not a PlayerEntity, ignoring");
                }
            }
        }
        return false;
    }

    public static boolean checkPermission(ServerPlayerEntity player) {
        boolean requireCreative;
        try {
            requireCreative = CharmConfig.REQUIRE_CREATIVE.get();
        } catch (Exception e) {
            requireCreative = true;
        }
        PokemonEditorDebug.log("checkPermission() - require_creative=" + requireCreative
                + " isCreative=" + player.isInCreativeMode() + " hasOpLevel2=" + player.hasPermissionLevel(2));

        if (!player.hasPermissionLevel(2)) {
            player.sendMessage(Text.literal("你没有使用宝可梦编辑器的权限！"));
            PokemonEditorDebug.log("checkPermission() - no OP permission, rejected");
            return false;
        }

        if (requireCreative && !player.isInCreativeMode()) {
            player.sendMessage(Text.literal("必须在创造模式下才能使用宝可梦编辑器！"));
            PokemonEditorDebug.log("checkPermission() - not creative mode, rejected");
            return false;
        }

        PokemonEditorDebug.log("checkPermission() - permission granted");
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.literal(" "));
        tooltip.add(Text.translatable("item.cobblemon_ts.pokemon_editor.tooltip.one"));
        tooltip.add(Text.translatable("item.cobblemon_ts.pokemon_editor.tooltip.two"));
    }
}