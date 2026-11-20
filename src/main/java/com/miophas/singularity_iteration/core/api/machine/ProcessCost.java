// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.machine;

/**
 * 0.1.11.107 (processing overhaul M0): what one operation of a processing machine costs before upgrades: the energy of
 * the operation (EU) and the power the machine draws (EU/t). The time follows: {@code ceil(energy / power)} ticks. A
 * machine with a fixed time and EU/t has the cost {@code energyPerTick × ticks} at {@code energyPerTick}; a machine that
 * models its material (a macerator by the Bond work index) changes the energy and keeps its power.
 *
 * @param energy EU per operation, at least 1
 * @param power  EU per tick, at least 1
 */
public record ProcessCost(long energy, long power) {
    public ProcessCost {
        if (energy < 1 || power < 1) throw new IllegalArgumentException("energy and power must be at least 1: " + energy + " EU, " + power + " EU/t");
    }

    /** A fixed time and draw: {@code ticks × power} EU at {@code power} EU/t. */
    public static ProcessCost fixed(int ticks, long power) {
        return new ProcessCost(Math.max(1, ticks) * Math.max(1L, power), Math.max(1L, power));
    }

    /** Ticks per operation before upgrades: {@code ceil(energy / power)}, at least 1, at most {@link Integer#MAX_VALUE}. */
    public int ticks() {
        long t = (energy + power - 1) / power;
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, t));
    }

    /** Implemented by machines that report their cost per operation (the screens and the tests read it). */
    public interface Source {
        /** The cost of the operation the machine would run now (its current input), before upgrades. */
        ProcessCost processCost();
    }
}
