package com.miophas.singularity_iteration.core.api.kinetic;

/**
 * KU (Kinetic Units) storage interface.
 * Kinetic system in IndustrialCraft 2 style. It is fully independent of FE energy and HU heat.
 * 
 * Properties of kinetic energy:
 * - Only specific power sources make it (for example steam turbines, water mills, wind generators).
 * - Only specific machines use it (for example macerators, compressors, centrifuges).
 * - It cannot move over long distances as electricity does. It needs a mechanical drive (for example shafts, gearboxes).
 * - Some kinetic energy can be lost during transmission.
 * - It needs rotating parts for transmission.
 */
public interface IKineticStorage {
    
    /**
     * Adds kinetic energy to the storage. Returns the kinetic energy that was really added.
     * 
     * @param toReceive the kinetic energy to receive
     * @param simulate  if true, only simulate the operation and do not change the storage.
     * @return the kinetic energy that was really received
     */
    long receiveKinetic(long toReceive, boolean simulate);
    
    /**
     * Removes kinetic energy from the storage. Returns the kinetic energy that was really removed.
     * 
     * @param toExtract the kinetic energy to extract
     * @param simulate  if true, only simulate the operation and do not change the storage.
     * @return the kinetic energy that was really extracted
     */
    long extractKinetic(long toExtract, boolean simulate);
    
    /**
     * Gets the kinetic energy stored now.
     */
    long getKineticStored();
    
    /**
     * Gets the maximum kinetic energy capacity.
     */
    long getMaxKineticStored();
    
    /**
     * Checks if kinetic energy can be extracted.
     * If this returns false, extractKinetic always returns 0.
     */
    boolean canExtractKinetic();
    
    /**
     * Checks if kinetic energy can be received.
     * If this returns false, receiveKinetic always returns 0.
     */
    boolean canReceiveKinetic();
    
    /**
     * Gets the current rotation speed (RPM).
     * Used for display and by some special machines.
     */
    default int getRPM() {
        return com.miophas.singularity_iteration.core.api.util.BoundedUnits.gauge(getKineticStored(), getMaxKineticStored(), 0, 10000);
    }
    
    /**
     * Checks if the storage is in overspeed state.
     */
    default boolean isOverspeed() {
        return getRPM() >= 8000;
    }
    
    /**
     * Gets the kinetic loss rate (kinetic energy lost per tick).
     * The kinetic system usually has friction loss.
     */
    default long getKineticLossPerTick() {
        int rpm = getRPM();
        if (rpm < 1000) return 0;
        if (rpm < 3000) return 1;
        if (rpm < 6000) return 2;
        return 3;
    }
    
    /**
     * Gets the maximum receive rate.
     */
    long getMaxReceive();
    
    /**
     * Gets the maximum extract rate.
     */
    long getMaxExtract();
}
