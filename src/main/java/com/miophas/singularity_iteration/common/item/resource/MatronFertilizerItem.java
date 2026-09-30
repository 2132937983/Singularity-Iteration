package com.miophas.singularity_iteration.common.item.resource;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

@SuppressWarnings("null")
public class MatronFertilizerItem extends Item {
    public static final int FERTILIZER_VALUE = 100;

    public MatronFertilizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.mio_icif.fertilizer.tooltip"));
        tooltipComponents.add(Component.translatable("item.mio_icif.fertilizer.tooltip.matron"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    public int getFertilizerValue() {
        return FERTILIZER_VALUE;
    }

    public static boolean isFertilizer(ItemStack stack) {
        return stack.getItem() instanceof MatronFertilizerItem;
    }
}


