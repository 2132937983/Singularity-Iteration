package com.miophas.singularity_iteration.core.api.heat;

import net.minecraft.core.Direction;

/**
 * Heat conductor interface.
 * Blocks that conduct heat implement it, for example heat pipes and heat exchangers.
 *
 * Heat conduction rules:
 * - The conduction efficiency depends on the material.
 * - Longer distances cause more heat loss.
 * - Conduction needs a temperature difference.
 */
public interface IHeatConductor {
    
    /**
     * Gets the conduction efficiency (0.0 - 1.0).
     * 1.0 means 100% efficiency with no loss.
     */
    float getConductionEfficiency();
    
    /**
     * Gets the maximum conduction rate, in HU/tick.
     */
    int getMaxConductionRate();
    
    /**
     * Conducts heat from the specified direction.
     * @param fromDirection the direction of the heat source
     * @param amount the heat to conduct
     * @param simulate true to simulate the transfer without changing state
     * @return the heat actually conducted
     */
    int conductHeat(Direction fromDirection, int amount, boolean simulate);
    
    /**
     * Returns true if this conductor can receive heat from the given direction.
     */
    boolean canReceiveFrom(Direction direction);
    
    /**
     * Returns true if this conductor can output heat to the given direction.
     */
    boolean canOutputTo(Direction direction);
    
    /**
     * Gets the stored heat in the buffer.
     */
    int getBufferedHeat();
    
    /**
     * Gets the maximum buffered heat.
     */
    int getMaxBufferedHeat();
    
    /**
     * Gets the thermal resistance. It controls the conduction speed.
     * A lower value gives faster conduction. The default is 1.0 minus the conduction efficiency.
     */
    default float getThermalResistance() {
        return 1.0f - getConductionEfficiency();
    }
    
    /**
     * Returns true if conduction needs a temperature difference.
     */
    default boolean requiresTemperatureDifference() {
        return true;
    }
    
    /**
     * Gets the minimum working temperature.
     */
    default int getMinimumWorkingTemperature() {
        return 20;
    }
}


