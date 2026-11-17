package com.cobblemon_ts.mod;

import com.cobblemon_ts.mod.gui.TestBuildScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.world.World;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

public class TestBuildItem extends Item {

    public TestBuildItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType flagIn) {
        tooltip.add(Text.translatable("cobblemon_ts.testbuild.tooltip.build_time", FabricLoader.getInstance().getModContainer("cobblemon_ts").get().getMetadata().getVersion().getFriendlyString()));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("cobblemon_ts.testbuild.tooltip.open_menu"));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (world.isClient) {
            openTestScreen();
        }

        return TypedActionResult.success(stack);
    }

    @Environment(EnvType.CLIENT)
    private static void openTestScreen() {
        MinecraftClient.getInstance().setScreen(new TestBuildScreen());
    }
}