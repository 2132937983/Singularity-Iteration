package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Metal armor interface.
 * <p>
 * Equivalent to {@code IMetalArmor} in IC2 1.12.2.
 * The mod identifies an item that implements this interface as metal armor.
 * The mod uses this to calculate the protection of metal armor and the charge efficiency of electric armor.
 */
public interface IMetalArmor {

    /**
     * Checks if this item is metal armor.
     *
     * @param stack the item stack
     * @param player the player who wears this armor
     * @return true if the item is metal armor
     */
    boolean isMetalArmor(ItemStack stack, Player player);
}
