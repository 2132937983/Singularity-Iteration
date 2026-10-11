package com.miophas.singularity_iteration.core.api.recipe;

/**
 * Recipe processing information interface.
 *
 * <p>All recipes that must expose their processing time and energy use must implement this interface.
 * With this interface, callers do not need reflection to read recipe properties.
 * This makes sure that the code works correctly in an obfuscated environment.
 *
 * <p>Implementation example:
 * <pre>{@code
 * public class MyCustomRecipe implements Recipe<MyInventory>, IRecipeProcessingInfo {
 *     private final int processingTime;
 *     private final int energyPerTick;
 *
 *     @Override
 *     public int getProcessingTime() {
 *         return processingTime;
 *     }
 *
 *     @Override
 *     public int getEnergyPerTick() {
 *         return energyPerTick;
 *     }
 * }
 * }</pre>
 */
public interface IRecipeProcessingInfo {

    /**
     * Gets the recipe processing time.
     *
     * @return the processing time (ticks)
     */
    int getProcessingTime();

    /**
     * Gets the recipe energy use per tick.
     *
     * @return the energy use per tick (EU/tick)
     */
    int getEnergyPerTick();

    /**
     * Gets the total recipe energy cost.
     *
     * <p>The default implementation returns getEnergyPerTick() * getProcessingTime().
     *
     * @return the total energy cost (EU)
     */
    default long getTotalEnergyCost() {
        return (long) getEnergyPerTick() * getProcessingTime();
    }

    /**
     * Gets the number of input items that the recipe consumes.
     *
     * <p>The default is 1. This means that each operation consumes 1 input item.
     * Some recipes, for example metal former recipes, can consume more than one input item.
     *
     * @return the number of consumed items
     */
    default int getIngredientCount() {
        return 1;
    }
}