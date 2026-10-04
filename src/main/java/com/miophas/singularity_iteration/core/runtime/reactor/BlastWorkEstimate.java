// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.reactor;

/**
 * Overflow-safe upper bounds for the resumable blast traversals.
 *
 * <p>This class deliberately does not change a cursor or cap a gameplay
 * radius.  It is an admission primitive for callers which need to reject or
 * defer a blast before allocating a task.  The bounds are intentionally
 * conservative: a false positive (a large estimate for a sparse world) is
 * preferable to admitting an operation whose traversal can run for an
 * unbounded amount of time.</p>
 */
public final class BlastWorkEstimate {
    /** A useful default for a caller-owned admission policy, not a hard radius cap. */
    public static final long DEFAULT_MAX_OPERATIONS = 100_000_000L;
    private static final int NUKE_RAY_CAP = 20_000;
    private static final int OCTREE_RAY_CAP = 50_000;
    private static final int WAVE_ANGLE_SEGMENTS = 144;

    /** Work units are bounded cursor/ray transitions, not nanoseconds. */
    public record Estimate(long rayOperations, long entityOperations,
                           long naturalOperations, long radiationOperations,
                           long totalOperations) {
        public Estimate {
            if (rayOperations < 0 || entityOperations < 0 || naturalOperations < 0
                    || radiationOperations < 0 || totalOperations < 0)
                throw new IllegalArgumentException("Negative blast work estimate");
            long expected = add(add(add(rayOperations, entityOperations), naturalOperations), radiationOperations);
            if (totalOperations != expected)
                throw new IllegalArgumentException("Blast estimate total does not match its components");
        }

        /** Returns whether this estimate fits a caller-provided operation budget. */
        public boolean within(long budget) {
            return budget >= 0 && totalOperations <= budget;
        }

        /** Adds a known entity phase without allowing long overflow. */
        public Estimate withEntities(long entities) {
            if (entities < 0) throw new IllegalArgumentException("Negative entity estimate");
            return create(rayOperations, add(entityOperations, entities), naturalOperations, radiationOperations);
        }
    }

    private BlastWorkEstimate() { }

    /** Upper bound for the pressure-front task, excluding its caller-owned entity snapshot. */
    public static Estimate nuke(int radius) {
        requireRadius(radius);
        int rayCount = boundedRayCount(4 * Math.PI * (double) radius * radius, NUKE_RAY_CAP, 500);
        long rays = add(multiply(radius, add(rayCount, 1)), 1);
        long surfaceRadius = add(multiply(radius, 3), 1) / 2;
        long diameter = add(multiply(surfaceRadius, 2), 1);
        long chunksPerAxis = add(diameter, 30) / 16;
        // A chunk prepares at most 256 section palettes; every column visits at most
        // the supported 4096-block build span plus cursor/section transitions.
        long perChunk=add(257,multiply(256,8198));
        long fallout=add(multiply(multiply(chunksPerAxis,chunksPerAxis),perChunk),1);
        return create(rays, 1, 0, fallout);
    }

    /** Includes one admission, at most radius LOS samples, and one damage step per entity. */
    public static Estimate nuke(int radius, long entityCount) {
        if (entityCount < 0) throw new IllegalArgumentException("Negative entity count");
        return nuke(radius).withEntities(multiply(entityCount, add(radius, 2)));
    }

    /** Upper bound for the ellipsoidal ray task. */
    public static Estimate octree(int radius, int radiusY) {
        requireRadius(radius);
        requireRadius(radiusY);
        double surfaceAreaHalf = 2.0 * Math.PI * (double) radius * radiusY;
        int rayCount = boundedRayCount(surfaceAreaHalf, OCTREE_RAY_CAP, 2_000);
        long maxRayLength = add(add(radius, radiusY), 1); // sqrt(r²+rY²) <= r+rY
        long rays = add(multiply(rayCount, add(maxRayLength, 2)), 1);
        long natural = spiralOperations(radius, radius);
        long radiation = spiralOperations(radius, radius);
        return create(rays, 0, natural, radiation);
    }

    /** Upper bound for wave propagation, shielding proofs and both spiral phases. */
    public static Estimate wave(int radius, int radiusY) {
        requireRadius(radius);
        requireRadius(radiusY);
        int maximum = Math.max(radius, radiusY);
        int radiationRadius = scaledRadius(maximum);
        long waves = waveOperations(maximum, radiusY);
        long natural = spiralOperations(radius, radius);
        long radiation = spiralOperations(radiationRadius, radiationRadius);
        return create(waves, 0, natural, radiation);
    }

    /**
     * Number of {@link SpiralBlastCursor} transitions, including one column
     * transition per candidate column and the terminal DONE transition.
     */
    public static long spiralOperations(int scanRadius, int effectRadius) {
        requireRadius(scanRadius);
        requireRadius(effectRadius);
        long columns = multiply(4L, multiply(scanRadius, scanRadius));
        // Every column can contain at most (2 * effectRadius + 1) positions.
        long positionsPerColumn = add(multiply(2L, effectRadius), 1);
        return add(1, multiply(columns, add(positionsPerColumn, 1)));
    }

    private static long waveOperations(int maximum, int radiusY) {
        if (maximum == 0) return 1;
        // A wave angle may open a 3x3 footprint.  Each opened column checks a
        // five-block proof ray before visiting its vertical range.  This is a
        // deliberately loose bound that remains valid for all integer inputs.
        long heights = add(multiply(2L, radiusY), 1);
        long perAngle = add(add(1, multiply(9, heights)), multiply(5L, maximum));
        long perWave = add(WAVE_ANGLE_SEGMENTS, multiply(WAVE_ANGLE_SEGMENTS, perAngle));
        return add(1, multiply(maximum, perWave));
    }

    private static int boundedRayCount(double raw, int cap, int minimum) {
        if (!(raw > 0.0)) return minimum;
        if (raw >= cap) return cap;
        return Math.max(minimum, (int) raw);
    }

    private static int scaledRadius(int radius) {
        long scaled = (long) radius * 3L / 2L;
        if (scaled > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Blast radiation radius exceeds int range");
        return (int) scaled;
    }

    private static void requireRadius(int radius) {
        if (radius < 0) throw new IllegalArgumentException("Negative blast radius");
    }

    private static Estimate create(long rays, long entities, long natural, long radiation) {
        return new Estimate(rays, entities, natural, radiation,
                add(add(add(rays, entities), natural), radiation));
    }

    private static long add(long left, long right) {
        if (left < 0 || right < 0 || left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    private static long multiply(long left, long right) {
        if (left < 0 || right < 0 || left != 0 && right > Long.MAX_VALUE / left)
            return Long.MAX_VALUE;
        return left * right;
    }
}
