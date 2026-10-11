package com.miophas.singularity_iteration.core.api.util;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Interface for item HUD information.
 * <p>
 * It is the equivalent of {@code IItemHudInfo} in IC2 1.12.2.
 * Items that implement this interface can show more information on the HUD.
 */
public interface IItemHudInfo {

    /**
     * Returns the HUD information lines of this item.
     *
     * @param stack    the item stack
     * @param extended true to show the extended information
     * @return the list of HUD information lines
     */
    List<String> getHudInfo(ItemStack stack, boolean extended);

    /**
     * Interface for HUD energy bar providers.
     * <p>
     * It is the equivalent of {@code IItemHudProvider.IItemHudBarProvider} in IC2 1.12.2.
     * Items that implement this interface can show an energy bar on the HUD.
     */
    interface IItemHudBarProvider {
        /**
         * Returns the energy bar percentage (0-100).
         *
         * @param stack the item stack
         * @return the energy bar percentage
         */
        int getBarPercent(ItemStack stack);
    }
}
