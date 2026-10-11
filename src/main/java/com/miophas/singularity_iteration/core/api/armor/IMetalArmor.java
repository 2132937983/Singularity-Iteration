package com.miophas.singularity_iteration.core.api.armor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for metal armor.
 * <p>
 * It is the equivalent of {@code IMetalArmor} in IC2 1.12.2.
 * Items that implement this interface are conductive metal armor. They conduct damage from an electric shock.
 */
public interface IMetalArmor {

    /**
     * Returns true if this item is metal armor.
     * <p>
     * Metal armor conducts electric current. The wearer gets electric shock damage on contact with a live block.
     *
     * @param stack  the item stack
     * @param entity the entity that wears the item
     * @return true if the item is metal armor
     */
    boolean isMetalArmor(ItemStack stack, Entity entity);
}