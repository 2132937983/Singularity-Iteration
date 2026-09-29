// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.util;

import java.math.BigInteger;

/** Non-negative integer accounting shared by HU/KU adapters; no game-specific conversion policy. */
public final class BoundedUnits {
    private BoundedUnits() { }
    public static long clamp(long amount, long capacity) { return Math.max(0, Math.min(Math.max(0, capacity), amount)); }
    public static long receive(long amount, long capacity, long requested, long limit) {
        if (requested <= 0 || limit <= 0 || capacity <= 0 || amount < 0 || amount >= capacity) return 0;
        return Math.min(capacity - amount, Math.min(requested, limit));
    }
    public static long extract(long amount, long requested, long limit) {
        if (amount <= 0 || requested <= 0 || limit <= 0) return 0;
        return Math.min(amount, Math.min(requested, limit));
    }
    public static long multiplyDivide(long value, long multiplier, long divisor) {
        if (value < 0 || multiplier < 0 || divisor <= 0) throw new IllegalArgumentException("Non-negative ratio required");
        if (value == 0 || multiplier == 0) return 0;
        if (value <= Long.MAX_VALUE / multiplier) return value * multiplier / divisor;
        return BigInteger.valueOf(value).multiply(BigInteger.valueOf(multiplier))
            .divide(BigInteger.valueOf(divisor)).longValueExact();
    }
    /** Preserve the existing SI percentage quantization without overflow in amount * 100. */
    public static int gauge(long amount, long capacity, int minimum, int maximum) {
        if (capacity <= 0 || maximum <= minimum) return minimum;
        long percent = multiplyDivide(clamp(amount, capacity), 100, capacity);
        return (int) (minimum + percent * ((long) maximum - minimum) / 100);
    }
    public static float nonNegativeFactor(float factor) { return Float.isFinite(factor) ? Math.max(0, factor) : 0; }
    public static long friction(long amount, float factor) {
        if (amount <= 0) return 0;
        // SI's existing minimum decay of one unit is preserved, including a zero factor.
        return Math.min(amount, Math.max(1, (long) (amount * nonNegativeFactor(factor))));
    }
}
