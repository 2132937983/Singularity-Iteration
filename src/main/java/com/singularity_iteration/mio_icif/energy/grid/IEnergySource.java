package com.singularity_iteration.mio_icif.energy.grid;

/**
 * A tile that provides energy into the grid.
 */
public interface IEnergySource extends IEnergyEmitter {
    /**
     * @return The amount of energy currently offered (available to draw this tick).
     */
    double getOfferedEnergy();

    /**
     * Called after energy has been drawn from this source.
     * @param amount The amount actually drawn. Always non-negative.
     */
    void drawEnergy(double amount);

    /**
     * @return The source tier (determines packet size: 8 * 4^tier).
     */
    int getSourceTier();

    /**
     * Number of energy packets this source emits per tick.
     * IC2 original: generators = 1, transformers in step-down = 4.
     * Each packet has size = getPowerFromTier(getSourceTier()).
     * @return packet count per tick (default 1)
     */
    default int getPacketCount() { return 1; }
}