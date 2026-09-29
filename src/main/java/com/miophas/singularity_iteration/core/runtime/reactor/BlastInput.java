// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.reactor;

/** Mathematical input limits for the existing blast algorithms; no gameplay radius cap. */
public final class BlastInput {
    private BlastInput() { }

    /** The ordinary task retains nextInt(edge squared) to preserve its existing random stream. */
    public static void nuke(double x, double y, double z, float power, int radius) {
        common(x, y, z, power, radius, radius);
        long edge = Math.max(1, radius / 4);
        if (edge * edge > Integer.MAX_VALUE)
            throw invalid("ordinary radiation random bound exceeds positive int range");
        spiralRadius(scaledRadiationRadius(radius));
        coordinates(x, y, z, radius, radius);
    }

    /** Ellipsoidal rays retain float coordinate rounding before the block-position floor. */
    public static void octree(double x, double y, double z, float power, int radius, int radiusY) {
        common(x, y, z, power, radius, radiusY);
        spiralRadius(radius);
        coordinates(x, y, z, radius, radius);
        int longestRay = Math.max(radius, radiusY);
        if (longestRay > 0) {
            long extent = (long) longestRay - 1;
            floatCoordinate(x, extent);
            floatCoordinate(y, extent);
            floatCoordinate(z, extent);
        }
    }

    /** Includes the wave's one-block horizontal width, min-two vertical range and proof band. */
    public static void wave(double x, double y, double z, float power, int radius, int radiusY) {
        common(x, y, z, power, radius, radiusY);
        int maximum = Math.max(radius, radiusY);
        int radiation = scaledRadiationRadius(maximum);
        spiralRadius(radiation);
        // currentWave < maximum, so its +/-1 footprint fits maximum. The radiation
        // sphere encloses the +/-15% vertical noise, except the min-two small wave.
        long vertical = maximum == 0 ? 0 : Math.max(2, radiation);
        coordinates(x, y, z, radiation, vertical);
        // Explicitly protect int range+1/proof-distance sentinels even if other
        // arithmetic is widened later. Current radiation/spiral checks are stronger.
        if (maximum == Integer.MAX_VALUE) throw invalid("wave sentinel exceeds int range");
    }

    private static void common(double x, double y, double z, float power, int radius, int radiusY) {
        if (!Float.isFinite(power)) throw invalid("power must be finite");
        center(x);
        center(y);
        center(z);
        if (radius < 0 || radiusY < 0) throw invalid("radius must be nonnegative");
    }

    private static void center(double value) {
        if (!Double.isFinite(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
            throw invalid("center must be finite and representable as an int coordinate");
    }

    private static int scaledRadiationRadius(int radius) {
        long scaled = (long) radius * 3 / 2;
        if (scaled > Integer.MAX_VALUE) throw invalid("radiation radius exceeds int range");
        return (int) scaled;
    }

    private static void spiralRadius(int radius) {
        long square = (long) radius * radius;
        if (square > Long.MAX_VALUE / 4) throw invalid("spiral column limit exceeds long range");
    }

    private static void coordinates(double x, double y, double z, long horizontal, long vertical) {
        integerCoordinate(x, horizontal);
        integerCoordinate(y, vertical);
        integerCoordinate(z, horizontal);
    }

    private static void integerCoordinate(double center, long extent) {
        // Match the task's truncation towards zero, then check addition before it occurs.
        long base = (long) center;
        if (base - extent < Integer.MIN_VALUE || base + extent > Integer.MAX_VALUE)
            throw invalid("blast coordinate addition exceeds int range");
    }

    private static void floatCoordinate(double center, long extent) {
        double minimum = (float) (center - extent);
        double maximum = (float) (center + extent);
        if (minimum < Integer.MIN_VALUE || maximum > Integer.MAX_VALUE)
            throw invalid("ray float rounding exceeds int coordinate range");
    }

    private static IllegalArgumentException invalid(String reason) {
        return new IllegalArgumentException("Invalid blast input: " + reason);
    }
}
