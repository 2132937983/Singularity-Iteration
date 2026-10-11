package com.miophas.singularity_iteration.core.api.heat;

/**
 * Heat storage interface for HU (Heat Units).
 * This is an IC2-style heat system. It is fully independent of FE energy.
 * 
 * Properties of heat:
 * - Only specific heat sources make heat (for example nuclear reactors, geothermal, combustion).
 * - Only specific machines use heat (for example heat exchangers, steam generators).
 * - Heat cannot move over long distances like electricity. It needs a special heat conduction mechanism.
 * - Heat can be lost.
 */
public interface IHeatStorage {
    
    /**
     * Adds heat to the storage. Returns the heat that was added.
     * 
     * @param toReceive the heat to receive, in HU
     * @param simulate  if true, only simulate; do not change the storage
     * @return the heat that was received, in HU
     */
    long receiveHeat(long toReceive, boolean simulate);
    
    /**
     * Extracts heat from the storage. Returns the heat that was extracted.
     * 
     * @param toExtract the heat to extract, in HU
     * @param simulate  if true, only simulate; do not change the storage
     * @return the heat that was extracted, in HU
     */
    long extractHeat(long toExtract, boolean simulate);
    
    /**
     * Gets the heat that is stored now, in HU.
     */
    long getHeatStored();
    
    /**
     * Gets the maximum heat capacity, in HU.
     */
    long getMaxHeatStored();
    
    /**
     * Returns true if heat can be extracted.
     * If this returns false, extractHeat always returns 0.
     */
    boolean canExtractHeat();
    
    /**
     * Returns true if heat can be received.
     * If this returns false, receiveHeat always returns 0.
     */
    boolean canReceiveHeat();
    
    /**
     * Gets the heat temperature in degrees Celsius.
     * The default maps the fill level linearly to the range 20 to 1000 (in steps of 1%).
     * Use it for display and for some special machines.
     */
    default int getTemperature() {
        return com.miophas.singularity_iteration.core.api.util.BoundedUnits.gauge(getHeatStored(), getMaxHeatStored(), 20, 1000);
    }
    
    /**
     * Returns true if the storage is overheated (temperature 800 °C or more by default).
     */
    default boolean isOverheated() {
        return getTemperature() >= 800;
    }
    
    /**
     * Gets the heat loss rate, in HU per tick.
     * A heat system usually loses some heat.
     * By default there is no loss at 100 °C or less.
     */
    default long getHeatLossPerTick() {
        int temp = getTemperature();
        if (temp <= 100) return 0;
        return (temp - 100) / 100;
    }
}
