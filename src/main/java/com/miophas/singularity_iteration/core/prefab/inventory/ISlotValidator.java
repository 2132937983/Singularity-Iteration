package com.miophas.singularity_iteration.core.prefab.inventory;

import net.minecraft.world.item.ItemStack;

public interface ISlotValidator {

    boolean isValidForSlot(int slot, ItemStack stack, SlotType type);

    /**
     * Narrows which upgrades an upgrade slot takes. The handler has already checked that
     * the stack is an upgrade; machines that only support some upgrade kinds override this.
     */
    default boolean acceptsUpgrade(int slot, ItemStack stack) {
        return true;
    }
}
