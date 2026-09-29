// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.math.BigInteger;

/**
 * Nonnegative EU with the complete signed-long whole range and 52 binary
 * fractional places. Integer callers never scale their capacity into a long.
 * Arithmetic and transaction conservation are exact in these units; converting
 * a finer double input rounds once, by at most half a unit (2^-53 EU).
 * This is an independent numeric policy, not a reference-mod implementation.
 */
public record EnergyAmount(long whole, long fraction) implements Comparable<EnergyAmount> {
    public static final int FRACTION_BITS = 52;
    public static final long UNITS = 1L << FRACTION_BITS;
    public static final EnergyAmount ZERO = new EnergyAmount(0, 0);
    private static final BigInteger MASK = BigInteger.valueOf(UNITS - 1);

    public EnergyAmount {
        if (whole < 0 || fraction < 0 || fraction >= UNITS)
            throw new IllegalArgumentException("Invalid EU balance");
    }
    public static EnergyAmount of(long whole) { return new EnergyAmount(whole, 0); }
    public static EnergyAmount fromDouble(double amount) {
        if (!Double.isFinite(amount) || amount < 0 || amount >= 0x1p63)
            throw new IllegalArgumentException("Double EU outside supported range");
        long whole = (long) amount;
        long fraction = Math.round((amount - whole) * UNITS);
        if (fraction == UNITS) return of(Math.incrementExact(whole));
        return new EnergyAmount(whole, fraction);
    }
    public double toDouble() { return whole + Math.scalb((double) fraction, -FRACTION_BITS); }
    public boolean isZero() { return whole == 0 && fraction == 0; }
    @Override public int compareTo(EnergyAmount other) {
        int integral = Long.compare(whole, other.whole);
        return integral == 0 ? Long.compare(fraction, other.fraction) : integral;
    }
    public EnergyAmount min(EnergyAmount other) { return compareTo(other) <= 0 ? this : other; }
    public EnergyAmount add(EnergyAmount other) {
        long nextWhole = Math.addExact(whole, other.whole);
        long nextFraction = fraction + other.fraction;
        if (nextFraction >= UNITS) { nextWhole = Math.incrementExact(nextWhole); nextFraction -= UNITS; }
        return new EnergyAmount(nextWhole, nextFraction);
    }
    public EnergyAmount subtract(EnergyAmount other) {
        if (compareTo(other) < 0) throw new IllegalArgumentException("Negative EU result");
        long nextWhole = whole - other.whole, nextFraction = fraction - other.fraction;
        if (nextFraction < 0) { nextWhole--; nextFraction += UNITS; }
        return new EnergyAmount(nextWhole, nextFraction);
    }
    public EnergyAmount roomBelow(long capacity) {
        EnergyAmount ceiling = of(capacity);
        return compareTo(ceiling) >= 0 ? ZERO : ceiling.subtract(this);
    }
    /** Wide aggregate arithmetic is confined to planning and validation. */
    public BigInteger units() {
        return BigInteger.valueOf(whole).shiftLeft(FRACTION_BITS).add(BigInteger.valueOf(fraction));
    }
    public static EnergyAmount fromUnits(BigInteger units) {
        if (units.signum() < 0) throw new IllegalArgumentException("Negative EU units");
        return new EnergyAmount(units.shiftRight(FRACTION_BITS).longValueExact(), units.and(MASK).longValueExact());
    }
}
