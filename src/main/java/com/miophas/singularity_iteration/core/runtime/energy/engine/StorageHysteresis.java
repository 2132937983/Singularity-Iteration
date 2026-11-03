// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

/**
 * 0.1.11.1: the role of a battery of another mod on the SI grid, with a dead band on its state of charge.
 *
 * <ul>
 *   <li>At {@value #DISCHARGE_PERCENT} % or more the battery becomes a provider: it supplies SI loads and takes nothing.</li>
 *   <li>At {@value #CHARGE_PERCENT} % or less it becomes an acceptor: it takes the surplus and supplies nothing.</li>
 *   <li>Between the two it keeps its role. A battery never charges and discharges in the same tick, and it does not
 *       switch roles while its charge moves inside the band, so two batteries cannot pass energy back and forth.</li>
 * </ul>
 * The first role of a battery: provider at half charge or more, else acceptor. A battery without capacity is an
 * acceptor. Pure functions on whole numbers; no world access.
 */
public final class StorageHysteresis {
    /** The state of charge (percent) at and above which a battery becomes a provider. */
    public static final int DISCHARGE_PERCENT = 90;
    /** The state of charge (percent) at and below which a battery becomes an acceptor. */
    public static final int CHARGE_PERCENT = 10;

    /** The role of a battery for one tick. */
    public enum Role { CHARGE, DISCHARGE }

    private StorageHysteresis() { }

    /** The first role of a battery that holds {@code stored} of {@code capacity}. */
    public static Role initial(long stored, long capacity) {
        if (capacity <= 0) return Role.CHARGE;
        return Math.max(0, stored) * 2 >= capacity ? Role.DISCHARGE : Role.CHARGE;
    }

    /**
     * The role after {@code current} for a battery that holds {@code stored} of {@code capacity}. A {@code null}
     * current role gives the first role ({@link #initial}).
     */
    public static Role next(Role current, long stored, long capacity) {
        if (current == null) return initial(stored, capacity);
        if (capacity <= 0) return Role.CHARGE;
        // stored * 100 can overflow a long only beyond 9.2e16; compare in double precision there
        long held = Math.max(0, Math.min(stored, capacity));
        boolean high, low;
        if (capacity < Long.MAX_VALUE / 100) {
            high = held * 100 >= capacity * DISCHARGE_PERCENT;
            low = held * 100 <= capacity * CHARGE_PERCENT;
        } else {
            double share = held / (double) capacity * 100.0;
            high = share >= DISCHARGE_PERCENT;
            low = share <= CHARGE_PERCENT;
        }
        if (high) return Role.DISCHARGE;
        if (low) return Role.CHARGE;
        return current;
    }
}
