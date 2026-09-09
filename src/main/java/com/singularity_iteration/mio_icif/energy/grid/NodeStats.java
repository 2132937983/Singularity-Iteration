package com.singularity_iteration.mio_icif.energy.grid;

/**
 * Statistics for a single Tile's energy flow.
 */
public class NodeStats {
    private final double energyIn;
    private final double energyOut;
    private final double voltage;

    public NodeStats(double energyIn, double energyOut, double voltage) {
        this.energyIn = energyIn;
        this.energyOut = energyOut;
        this.voltage = voltage;
    }

    public double getEnergyIn() { return energyIn; }
    public double getEnergyOut() { return energyOut; }
    public double getVoltage() { return voltage; }
}


