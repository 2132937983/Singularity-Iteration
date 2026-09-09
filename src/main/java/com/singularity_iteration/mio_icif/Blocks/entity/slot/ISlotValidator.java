package com.singularity_iteration.mio_icif.Blocks.entity.slot;

import net.minecraft.world.item.ItemStack;

public interface ISlotValidator {

    boolean isValidForSlot(int slot, ItemStack stack, SlotType type);
}

