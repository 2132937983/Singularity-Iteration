package com.miophas.singularity_iteration.core.api.machine;

/**
 * API-level slot type interface.
 * It keeps addon developers independent of the internal SlotType implementation.
 */
public interface ISlotType {
    
    /**
     * Gets the name of the slot type.
     * @return the type name
     */
    String getName();
    
    /**
     * Checks if this is an input slot.
     * @return true if this is an input slot
     */
    boolean isInput();
    
    /**
     * Checks if this is an output slot.
     * @return true if this is an output slot
     */
    boolean isOutput();
    
    /**
     * Checks if this is a battery slot.
     * @return true if this is a battery slot
     */
    boolean isBattery();
    
    /**
     * Checks if this is an upgrade slot.
     * @return true if this is an upgrade slot
     */
    boolean isUpgrade();
    
    /**
     * Checks if this is an extra slot.
     * @return true if this is an extra slot
     */
    boolean isExtra();
}