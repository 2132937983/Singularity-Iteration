package com.miophas.singularity_iteration.common.item.normal;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

@SuppressWarnings("null")
public class MatronHerbicideItem extends Item {
    public static final int HERBICIDE_VALUE = 50;

    public static final int DURABILITY = 64;

    public MatronHerbicideItem(Properties properties) {
        super(properties.durability(DURABILITY));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.mio_icif.herbicide.tooltip"));
        tooltipComponents.add(Component.translatable("item.mio_icif.herbicide.tooltip.matron"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    public int getHerbicideValue() {
        return HERBICIDE_VALUE;
    }

    public static boolean isHerbicide(ItemStack stack) {
        return stack.getItem() instanceof MatronHerbicideItem;
    }
}