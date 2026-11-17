package com.cobblemon_ts.mod;

import net.minecraft.text.Text;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;

public class GoldCrownItem extends Item {
    public GoldCrownItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, java.util.List<Text> tooltip, TooltipType flag) {
        super.appendTooltip(stack, context, tooltip, flag);
        tooltip.add(Text.translatable("tooltip.cobblemon_ts.gold_crown"));
    }
}