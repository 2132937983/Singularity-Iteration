package com.miophas.singularity_iteration.core.api.machine;

import net.neoforged.neoforge.items.IItemHandler;

/**
 * Interface for energy storage blocks.
 * All blocks that store energy or charge and discharge items must implement this interface.
 *
 * <p>This interface extends {@link IEnergyBlock}. An energy storage block is also an energy block.
 * Thus it gives access to the energy storage (from {@link IEnergyBlock}) and also manages the battery slots.
 */
public interface IEnergyContainerBlock extends IEnergyBlock {
    
    /**
     * Gets the item handler.
     * @return the item handler
     */
    IItemHandler getItemHandler();
    
    /**
     * Gets the number of battery slots.
     * @return the number of battery slots
     */
    int getBatterySlotCount();
    
    /**
     * Gets the charge rate.
     * @return the charge rate (EU/tick)
     */
    long getChargeRate();
    
    /**
     * Gets the discharge rate.
     * @return the discharge rate (EU/tick)
     */
    long getDischargeRate();
    
    /**
     * Gets the total charged energy.
     * @return the total charged energy (EU)
     */
    long getTotalCharged();
    
    /**
     * Gets the total discharged energy.
     * @return the total discharged energy (EU)
     */
    long getTotalDischarged();
    
    /**
     * Gets the running time.
     * @return the running time (ticks)
     */
    long getRunningTime();
    
    /**
     * Gets the running state.
     * @return plain English text ("Working", "Full" or "Idle" for energy containers), never null
     */
    String getRunningState();
    
    /**
     * Gets the redstone mode.
     * @return the redstone mode (0 to 7)
     */
    byte getRedstoneMode();
    
    /**
     * Sets the redstone mode.
     * @param mode the redstone mode (0 to 7)
     */
    void setRedstoneMode(byte mode);
    
    /**
     * Checks if the block must emit a redstone signal.
     * @return true if the block must emit a signal
     */
    boolean shouldEmitRedstone();
    
    /**
     * Gets the redstone signal strength.
     * @return the signal strength (0 to 15)
     */
    int getRedstoneSignalStrength();
    
    /**
     * Checks if the block has a redstone input.
     * @return true if the block has a redstone input
     */
    boolean hasRedstoneInput();
    
    /**
     * Checks if energy output is enabled.
     * @return true if energy output is enabled
     */
    boolean shouldEmitEnergy();
}