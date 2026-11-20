// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.1.11.128 (armour and flight overhaul E5): the ionised sheath of the quantum suit at high speed, pure numbers for the
 * client to draw. Past the barrier the air round the flyer ionises: a net of short flickering arcs crawls over a box
 * {@link #MARGIN} blocks out from the body. Every {@link #REGEN_TICKS} ticks (2 and 3 in turn) {@link #MIN_ARCS} to
 * {@link #MAX_ARCS} new arcs are drawn between two points of the box surface, jagged and pressed to the surface; their
 * colours switch between white, pale yellow and violet every tick. It grows with the speed like the light trail: nothing
 * below Mach {@link #RAMP_START_MACH}, full from Mach {@link #RAMP_FULL_MACH}.
 *
 * <p>Coordinates: the frame of the body part of the model, in blocks, +y down the body (towards the feet), +z the back.
 */
public final class ArcNetRules {
    private ArcNetRules() {}

    /** The box round the body (head to feet, arms included), before the margin. */
    public static final float BOX_X = 0.45F, BOX_TOP = -0.55F, BOX_BOTTOM = 1.55F, BOX_FRONT = -0.22F, BOX_BACK = 0.3F;
    /** How far out from the body the net lies. */
    public static final float MARGIN = 0.18F;
    /** Arcs per generation, ticks per generation (on average: 2 and 3 in turn). */
    public static final int MIN_ARCS = 4, MAX_ARCS = 6;
    public static final float REGEN_TICKS = 2.5F;
    /** Points per arc, how jagged, how long an arc may be, its line width and alpha. */
    public static final int ARC_POINTS = 8;
    public static final float JITTER = 0.05F, MIN_SPAN = 0.35F, MAX_SPAN = 1.0F, WIDTH = 0.009F, ALPHA = 0.85F;
    /** The arc colours: white, pale yellow, violet. */
    public static final float[][] COLOURS = {{1F, 1F, 1F}, {1F, 0.92F, 0.45F}, {0.72F, 0.45F, 1F}};
    /** The net comes in from this Mach number and is full from {@link #RAMP_FULL_MACH}. */
    public static final double RAMP_START_MACH = 0.95, RAMP_FULL_MACH = 1.3;

    /** The strength of the net (0..1) at a Mach number. */
    public static float level(double mach) {
        double k = Math.max(0, Math.min(1, (mach - RAMP_START_MACH) / (RAMP_FULL_MACH - RAMP_START_MACH)));
        return (float) (k * k * (3 - 2 * k));
    }

    /** The generation of the net at {@code tick}: a new one every 2 and 3 ticks in turn. */
    public static long generation(long tick) {
        return (long) Math.floor(tick / REGEN_TICKS);
    }

    /** The colour of arc {@code arc} at {@code tick} (switching every tick). */
    public static float[] colour(int arc, long tick) {
        return COLOURS[(int) Math.floorMod(arc + tick, (long) COLOURS.length)];
    }

    /** The flicker of arc {@code arc} at {@code tick}: 0.55 to 1. */
    public static float flicker(long seed, int arc, long tick) {
        return 0.55F + 0.45F * unit(seed + tick * 7919L, arc, 3);
    }

    /** The arcs of one generation ({@code seed}: the flyer and the generation), each {@link #ARC_POINTS} points {x, y, z}. */
    public static List<float[][]> arcs(long seed) {
        float[] lo = {-BOX_X - MARGIN, BOX_TOP - MARGIN, BOX_FRONT - MARGIN}, hi = {BOX_X + MARGIN, BOX_BOTTOM + MARGIN, BOX_BACK + MARGIN};
        int n = MIN_ARCS + (int) (unit(seed, 0, 0) * (MAX_ARCS - MIN_ARCS + 1));
        List<float[][]> out = new ArrayList<>();
        int slot = 1;
        for (int a = 0; a < n; a++) {
            float[] p = surface(seed, a, slot++, lo, hi), q = null;
            for (int tries = 0; tries < 10; tries++) {
                float[] c = surface(seed, a, slot++, lo, hi);
                double d = Math.sqrt(sq(c[0] - p[0]) + sq(c[1] - p[1]) + sq(c[2] - p[2]));
                if (d >= MIN_SPAN && d <= MAX_SPAN) { q = c; break; }
            }
            if (q == null) q = toward(p, lo, hi);
            float[][] arc = new float[ARC_POINTS][];
            for (int k = 0; k < ARC_POINTS; k++) {
                float s = k / (float) (ARC_POINTS - 1);
                float[] v = {p[0] + (q[0] - p[0]) * s, p[1] + (q[1] - p[1]) * s, p[2] + (q[2] - p[2]) * s};
                v = onSurface(v, lo, hi);
                if (k > 0 && k < ARC_POINTS - 1)
                    for (int c = 0; c < 3; c++) v[c] += JITTER * (2 * unit(seed, a * 31 + k, 100 + c) - 1);
                arc[k] = v;
            }
            out.add(arc);
        }
        return out;
    }

    /** A point on a face of the box (faces weighed by their area). */
    private static float[] surface(long seed, int arc, int slot, float[] lo, float[] hi) {
        float dx = hi[0] - lo[0], dy = hi[1] - lo[1], dz = hi[2] - lo[2];
        float ax = dy * dz, ay = dx * dz, az = dx * dy, pick = unit(seed, arc, slot * 4) * (ax + ay + az);
        int axis = pick < ax ? 0 : pick < ax + ay ? 1 : 2;
        float[] v = {lo[0] + dx * unit(seed, arc, slot * 4 + 1), lo[1] + dy * unit(seed, arc, slot * 4 + 2), lo[2] + dz * unit(seed, arc, slot * 4 + 3)};
        v[axis] = unit(seed, arc, slot * 4 + 2 + axis) < 0.5F ? lo[axis] : hi[axis];
        return v;
    }

    /** A point half a span along the longest side from {@code p}, on the surface. */
    private static float[] toward(float[] p, float[] lo, float[] hi) {
        float[] q = p.clone();
        q[1] = Math.min(hi[1], Math.max(lo[1], p[1] + (p[1] > (lo[1] + hi[1]) / 2 ? -0.6F : 0.6F)));
        return onSurface(q, lo, hi);
    }

    /** Pushes a point inside the box out to its nearest face. */
    static float[] onSurface(float[] v, float[] lo, float[] hi) {
        float[] out = v.clone();
        int axis = 0;
        float best = Float.MAX_VALUE;
        boolean toHi = false;
        for (int c = 0; c < 3; c++) {
            float dLo = Math.abs(v[c] - lo[c]), dHi = Math.abs(hi[c] - v[c]);
            if (dLo < best) { best = dLo; axis = c; toHi = false; }
            if (dHi < best) { best = dHi; axis = c; toHi = true; }
        }
        out[axis] = toHi ? hi[axis] : lo[axis];
        return out;
    }

    private static double sq(float x) { return x * x; }

    /** A fixed number in [0, 1) for a seed, an arc and a slot. */
    static float unit(long seed, int arc, int slot) {
        long h = seed * 0x9E3779B97F4A7C15L + arc * 0xC2B2AE3D27D4EB4FL + slot * 0x165667B19E3779F9L;
        h ^= h >>> 33; h *= 0xff51afd7ed558ccdL; h ^= h >>> 33; h *= 0xc4ceb9fe1a85ec53L; h ^= h >>> 33;
        return (h >>> 40) / (float) (1L << 24);
    }
}
