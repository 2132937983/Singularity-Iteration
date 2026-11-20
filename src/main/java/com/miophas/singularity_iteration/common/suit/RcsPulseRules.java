// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

/**
 * 0.1.11.127 (armour and flight overhaul E4): the electromagnetic attitude control of the quantum suit, pure numbers for the
 * client to draw. The attitude ports throw no gas: a port that {@link RcsAllocator} fires in a tick (its four-tick
 * pulse-width frames are unchanged) shows one sharp electromagnetic pulse at its locator bone ({@code rcs_locator_*}):
 * <ul>
 *   <li>a white-cyan point of light, brightest as the port fires and gone {@link #PULSE_TICKS} ticks later;</li>
 *   <li>a small ring across the port's axis that snaps out and fades with it;</li>
 *   <li>{@link #ARCS} micro arcs: field lines bulging out of the port along its axis (the way the field is bent), jagged,
 *       new every tick.</li>
 * </ul>
 * A port firing several ticks in a row (a long pulse, or a hard brake) crackles once a tick, each pulse starting afresh. Positions are in the port's frame:
 * +y along its axis (out of the port), in blocks.
 */
public final class RcsPulseRules {
    private RcsPulseRules() {}

    /** Ticks a pulse lasts from the tick its port fired. */
    public static final float PULSE_TICKS = 2F;
    /** The point of light: radius and alpha as the port fires. */
    public static final float FLASH_RADIUS = 0.07F, FLASH_ALPHA = 1F;
    /** The ring: from, to radius over the tick, line width, alpha at the start. */
    public static final float RING_FROM = 0.03F, RING_TO = 0.15F, RING_WIDTH = 0.012F, RING_ALPHA = 0.85F;
    /** Micro arcs per pulse, segments per arc, how far out along the axis, how far they bulge, how jagged, line width, alpha. */
    public static final int ARCS = 2, ARC_SEGMENTS = 5;
    public static final float ARC_LENGTH = 0.18F, ARC_BULGE = 0.05F, ARC_JITTER = 0.02F, ARC_WIDTH = 0.007F, ARC_ALPHA = 0.9F;

    /** The fade of a pulse {@code age} ticks after its port fired: 1 to 0 over {@link #PULSE_TICKS}. */
    public static float fade(float age) {
        float k = 1 - Math.max(0, Math.min(1, age / PULSE_TICKS));
        return k * k;
    }

    public static float ringRadius(float age) {
        float k = Math.max(0, Math.min(1, age / PULSE_TICKS));
        return RING_FROM + (RING_TO - RING_FROM) * (1 - (1 - k) * (1 - k));
    }

    /**
     * The points of micro arc {@code arc} of a pulse, {@code ARC_SEGMENTS + 1} of them as {x, y, z} in the port's frame: from
     * the port out along +y, bulging to one side (each arc another way) and jagged; the same for the same {@code seed}
     * (port and tick), new the next tick.
     */
    public static float[][] arc(long seed, int arc) {
        float[][] out = new float[ARC_SEGMENTS + 1][];
        double a = 2 * Math.PI * (arc + 0.25 * unit(seed, arc, 99)) / ARCS;
        float cx = (float) Math.cos(a), cz = (float) Math.sin(a);
        for (int k = 0; k <= ARC_SEGMENTS; k++) {
            float s = k / (float) ARC_SEGMENTS, bulge = ARC_BULGE * (float) Math.sin(Math.PI * s);
            float jx = k == 0 || k == ARC_SEGMENTS ? 0 : ARC_JITTER * (2 * unit(seed, arc, k) - 1);
            float jz = k == 0 || k == ARC_SEGMENTS ? 0 : ARC_JITTER * (2 * unit(seed, arc, k + 50) - 1);
            out[k] = new float[]{cx * bulge + jx, s * ARC_LENGTH, cz * bulge + jz};
        }
        return out;
    }

    /** A fixed number in [0, 1) for a seed, an arc and a slot. */
    static float unit(long seed, int arc, int slot) {
        long h = seed * 0x9E3779B97F4A7C15L + arc * 0xC2B2AE3D27D4EB4FL + slot * 0x165667B19E3779F9L;
        h ^= h >>> 33; h *= 0xff51afd7ed558ccdL; h ^= h >>> 33; h *= 0xc4ceb9fe1a85ec53L; h ^= h >>> 33;
        return (h >>> 40) / (float) (1L << 24);
    }
}
