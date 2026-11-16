// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.machine;

/**
 * 0.1.11.13: a block entity whose block gives a comparator signal (0..15). Generators implement it; the signal is sent
 * to the neighbours when it changes (see {@code ComparatorSignals}).
 */
public interface IComparatorSource {
    /** The comparator signal now, 0..15. */
    int comparatorSignal();

    /** 0 when empty, else 1..15 by the share filled (like a container). */
    static int fill(long stored, long capacity) {
        if (stored <= 0 || capacity <= 0) return 0;
        return 1 + (int) Math.min(14, Math.floor(14.0 * stored / capacity));
    }
}
