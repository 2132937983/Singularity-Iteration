package com.miophas.singularity_iteration.core.api.util;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for toolbox storage.
 * <p>
 * It is the equivalent of {@code IBoxable} in IC2 1.12.2.
 * Items that implement this interface can be stored in a toolbox.
 */
public interface IBoxable {

    /**
     * Returns true if this item can be stored in a toolbox.
     *
     * @param stack the item stack
     * @return true if the item can be stored
     */
    boolean canBeStoredInToolbox(ItemStack stack);
}
