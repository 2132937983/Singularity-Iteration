// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

/** Admission before consuming an item or an entire merged XP entity. */
public final class FuelAdmission {
    private FuelAdmission() { }
    /** Zero means leave the source untouched. Never saturate after consuming it. */
    public static int accepted(int stored, int value, int count) {
        if (stored < 0 || value <= 0 || count <= 0) return 0;
        long total = (long) value * count;
        return total <= Integer.MAX_VALUE - stored ? (int) total : 0;
    }
}
