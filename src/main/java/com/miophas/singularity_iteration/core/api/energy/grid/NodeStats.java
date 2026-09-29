// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy.grid;

/** A measured tick sample. Voltage is the maximum native EU packet in the sample;
 * FE-only intake has no packet voltage and reports zero. Missing node samples remain unavailable. */
public record NodeStats(double energyIn, double energyOut, double voltage) {
    public NodeStats {
        requireMeasurement(energyIn);
        requireMeasurement(energyOut);
        requireMeasurement(voltage);
    }

    private static void requireMeasurement(double value) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException("Node measurements must be finite and nonnegative");
        }
    }

    public double getEnergyIn() { return energyIn; }
    public double getEnergyOut() { return energyOut; }
    public double getVoltage() { return voltage; }
}
