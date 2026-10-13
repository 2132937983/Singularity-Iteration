// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.core.api.suit.SuitBurstSpec;

/**
 * Default burst costs of the built-in nano units (0.1.8.5, phase 2 in 0.1.8.6). The common config
 * section {@code NanoSuit} can change each value ({@code nano.NanoConfig}).
 */
public final class NanoBurstDefaults {
    private NanoBurstDefaults() {}

    /** Kinetic Dash: 20 reserve, +15 heat, 2 s cooldown. */
    public static final SuitBurstSpec KINETIC_DASH = new SuitBurstSpec(20, 15, 40, false);
    /** EMP pulse: 35 reserve, +40 heat, 10 s cooldown. */
    public static final SuitBurstSpec EMP = new SuitBurstSpec(35, 40, 200, false);
    /** Optical camouflage: 6 reserve per second while it runs, +10 heat on activation, 3 s cooldown after it ends. */
    public static final SuitBurstSpec OPTICAL_CAMO = new SuitBurstSpec(6, 10, 60, true);
    /** Nano recon cloud: 15 reserve, +10 heat, 10 s cooldown (the glow lasts 10 s). */
    public static final SuitBurstSpec RECON_CLOUD = new SuitBurstSpec(15, 10, 200, false);
    /** Kinetic armor mesh: 25 reserve, +20 heat, 15 s cooldown (the mesh runs 6 s). */
    public static final SuitBurstSpec KINETIC_MESH = new SuitBurstSpec(25, 20, 300, false);
}
