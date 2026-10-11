// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.core.api.heat;

/**
 * Heat source interface.
 * Use it for blocks or items that make heat.
 * 
 * Types of heat sources:
 * - Nuclear fuel rod: makes a large quantity of heat continuously.
 * - Combustion chamber: burns fuel to make heat.
 * - Geothermal: gets heat from the environment.
 * - Solar collector: uses sunlight to make heat.
 */
public interface IHeatSource {
    
    /**
     * Gets the current heat output (HU/tick).
     */
    int getHeatOutput();
    
    /**
     * Gets the maximum heat output.
     */
    int getMaxHeatOutput();
    
    /**
     * Tells if the source makes heat now.
     */
    boolean isProducingHeat();
    
    /**
     * Gets the temperature of the heat source.
     */
    int getSourceTemperature();
    
    /**
     * Gets the remaining work time (ticks).
     * 0 means that the time is unlimited or that the source is empty.
     */
    int getRemainingTime();
    
    /**
     * Gets the efficiency as a percentage (0-100).
     */
    default int getEfficiency() {
        return 100;
    }
    
    /**
     * Tells if the source is overheating.
     */
    default boolean isOverheating() {
        return getSourceTemperature() > 800;
    }
}


