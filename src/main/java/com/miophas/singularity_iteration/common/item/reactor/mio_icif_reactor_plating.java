package com.miophas.singularity_iteration.common.item.reactor;

import com.miophas.singularity_iteration.core.api.reactor.ReactorComponentType;

import net.minecraft.world.item.ItemStack;

/**
 * Reactor plating (protective plate).
 *
 * Properties (based on IC2):
 * - Increases the maximum heat of the reactor.
 * - Decreases the reactor explosion radius.
 * - Does not store, absorb or vent heat.
 * - Is a passive protective component only.
 *
 * Function:
 * - Each plate adds a fixed amount to the maximum heat.
 * - Each plate decreases the explosion radius by a fixed percentage.
 * - The effects of multiple plates stack.
 *
 * Plating types:
 * - Reactor plating: +1000 maximum heat, -5% explosion radius.
 * - Containment reactor plating: +500 maximum heat, -10% explosion radius.
 * - Heat-capacity reactor plating: +1700 maximum heat, -1% explosion radius.
 */
@SuppressWarnings("null")
public class mio_icif_reactor_plating extends mio_icif_reactor {

    // Maximum heat that this plate adds.
private final int heatCapacityBonus;

    // Explosion radius decrease, in percent (0-100).
private final int explosionReductionPercent;

    /**
     * Creates a reactor plating item.
 * @param properties the item properties
 * @param heatCapacityBonus the maximum heat that the plate adds
 * @param explosionReductionPercent the explosion radius decrease, in percent
 */
    public mio_icif_reactor_plating(Properties properties, int heatCapacityBonus, int explosionReductionPercent) {
        super(properties.stacksTo(64), 1, ReactorComponentType.REACTOR_PLATING);
        this.heatCapacityBonus = heatCapacityBonus;
        this.explosionReductionPercent = explosionReductionPercent;
    }

    /**
     * Returns the maximum heat that the plate adds.
 * @param stack the item stack
 * @return the maximum heat bonus
     */
    public int getHeatCapacityBonus(ItemStack stack) {
        return heatCapacityBonus;
    }

    /**
     * Returns the explosion radius decrease.
 * @param stack the item stack
 * @return the explosion radius decrease, in percent (0-100)
 */
    public int getExplosionReductionPercent(ItemStack stack) {
        return explosionReductionPercent;
    }

    /**
     * Reactor plating does not show a durability bar.
 */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }
}

