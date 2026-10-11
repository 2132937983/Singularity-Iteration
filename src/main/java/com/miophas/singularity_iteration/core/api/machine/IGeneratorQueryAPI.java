package com.miophas.singularity_iteration.core.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Generator query API.
 *
 * <p>Gives queries for generator fuel, burn state and energy generation.
 * It was split from IMachineAPI to follow the single responsibility principle.
 */
public interface IGeneratorQueryAPI {

    /**
     * Checks if the generator is burning fuel.
     */
    boolean isGeneratorBurning(Level world, BlockPos pos);

    /**
     * Gets the remaining burn time of the current fuel.
     *
     * @return the remaining burn time (ticks)
     */
    int getGeneratorBurnTime(Level world, BlockPos pos);

    /**
     * Gets the total burn time of the current fuel.
     *
     * @return the total burn time (ticks)
     */
    int getGeneratorBurnDuration(Level world, BlockPos pos);

    /**
     * Gets the energy generation rate of the generator.
     *
     * @return the generation rate (EU/tick)
     */
    long getGeneratorEnergyRate(Level world, BlockPos pos);

    /**
     * Gets the item in the fuel slot of the generator.
     */
    ItemStack getGeneratorFuelSlot(Level world, BlockPos pos);

    /**
     * Sets the item in the fuel slot of the generator.
     *
     * @return true if the item was set
     */
    boolean setGeneratorFuelSlot(Level world, BlockPos pos, ItemStack stack);
}
