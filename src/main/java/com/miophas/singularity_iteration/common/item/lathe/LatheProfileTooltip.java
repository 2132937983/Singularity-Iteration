package com.miophas.singularity_iteration.common.item.lathe;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/** Tooltip image of a lathe workpiece: its profile and the thickness of every position. */
public record LatheProfileTooltip(ItemStack stack) implements TooltipComponent {
}
