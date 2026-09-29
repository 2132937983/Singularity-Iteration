package com.miophas.singularity_iteration.core.prefab.inventory;

import net.minecraft.world.item.ItemStack;

public interface ISlotValidator {

    boolean isValidForSlot(int slot, ItemStack stack, SlotType type);
}

