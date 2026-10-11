package com.miophas.singularity_iteration.core.api.machine;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Interface for generators.
 * All machines that generate power must implement this interface.
 *
 * <p>This interface extends {@link IBurnControl}. A generator supports burn time queries,
 * and it must also support control of the burn progress through the API (forced start and stop).
 */
public interface IGeneratorBlock extends IBurnControl {
    
    /**
     * Returns the cable tier.
     *
     * @return the cable tier
     */
    ICableTier getCableTier();
    
    /**
     * Returns true if the generator burns fuel now.
     *
     * @return true if it burns fuel
     */
    boolean isBurning();
    
    /**
     * Returns the remaining burn time.
     *
     * @return the remaining burn time (ticks)
     */
    int getBurnTime();
    
    /**
     * Returns the maximum burn time.
     *
     * @return the maximum burn time (ticks)
     */
    int getMaxBurnTime();
    
    /**
     * Returns the power output (EU/t).
     *
     * @return the power output
     */
    long getPowerOutput();
    
    /**
     * Returns the item in the fuel slot.
     *
     * @return the fuel slot item
     */
    ItemStack getFuelSlotItem();
    
    /**
     * Returns the item in the charge slot.
     *
     * @return the charge slot item
     */
    ItemStack getChargeSlotItem();
    
    /**
     * Returns the item handler.
     *
     * @return the item handler
     */
    IItemHandler getItemHandler();

    /**
     * Returns true if the generator works now (produces energy).
     *
     * <p>The default calls {@link #isBurning()}. Subclasses can override it to make "burning" and "generating" different.
     *
     * @return true if the generator produces energy
     */
    default boolean isGenerating() {
        return isBurning();
    }

    /**
     * Returns the current generation rate (EU/t).
     *
     * <p>The default calls {@link #getPowerOutput()}.
     *
     * @return the energy generated each tick
     */
    default long getGenerationRate() {
        return getPowerOutput();
    }

    /**
     * Returns the generator type ID.
     *
     * <p>The default returns an empty string. Subclasses can override it to return a custom type ID.
     *
     * @return the type ID
     */
    default String getGeneratorTypeId() {
        return "";
    }

    /**
     * Returns the total energy that the generator produced (EU).
     *
     * <p>The default returns 0. Subclasses can override it to track the total.
     *
     * @return the total generated energy
     */
    default long getTotalGenerated() {
        return 0;
    }
}