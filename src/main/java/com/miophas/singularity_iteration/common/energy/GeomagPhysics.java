// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * 0.1.11.20: the geomagnetic field's disturbance. The geomagnetic generator works on the field's change, not on the
 * field; its old constant output (30,720 EU/t at depth 1) is the output at a disturbance of 1.
 * <ul>
 *   <li><b>Daily variation (Sq):</b> the sunlit ionosphere's currents, {@code 0.08 + 0.47 max(0, cos(2 pi (d - 0.25)))}
 *   for the time of day {@code d} (0.55 at noon, 0.08 all night; 0.23 on average).</li>
 *   <li><b>Storms:</b> each day (by the world's seed, so every generator in a world sees the same storm) has a
 *   {@link #STORM_CHANCE} chance of a storm starting at a random time, Kp 6 to 9, lasting half a day to a day and a
 *   half and fading as {@code exp(-3 t / length)}; it adds {@code 1.2 Kp / 9}.</li>
 *   <li>The disturbance is at most {@link #MAX}; Kp is shown as {@code max(1 + 1.5 Sq, storm)}. The long-run mean is
 *   about 0.27.</li>
 * </ul>
 */
public final class GeomagPhysics {
    private GeomagPhysics() {}

    public static final double SQ_NIGHT = 0.08, SQ_DAY = 0.47, STORM_CHANCE = 0.12, STORM_GAIN = 1.2, MAX = 1.5;
    public static final int DAY = 24000;

    public static double sq(long dayTime) {
        double d = Math.floorMod(dayTime, DAY) / (double) DAY;
        return SQ_NIGHT + SQ_DAY * Math.max(0, Math.cos(2 * Math.PI * (d - 0.25)));
    }

    /** A storm of a day: start tick, length, peak Kp; or null. */
    public record Storm(long start, int length, double kp) {}

    public static Storm storm(long seed, long day) {
        var rng = new java.util.Random(seed ^ (day * 0x9E3779B97F4A7C15L) ^ 0x5DEECE66DL);
        if (rng.nextDouble() >= STORM_CHANCE) return null;
        return new Storm(day * DAY + rng.nextInt(DAY), 12000 + rng.nextInt(24000), 6 + 3 * rng.nextDouble());
    }

    /** The storm's Kp at a time (0 when quiet). */
    public static double stormKp(long seed, long t) {
        long day = Math.floorDiv(t, DAY);
        double kp = 0;
        for (long d = day - 2; d <= day; d++) {
            Storm s = storm(seed, d);
            if (s == null) continue;
            long dt = t - s.start();
            if (dt >= 0 && dt < s.length()) kp = Math.max(kp, s.kp() * Math.exp(-3.0 * dt / s.length()));
        }
        return kp;
    }

    public static double disturbance(long seed, long t) {
        return Math.min(MAX, sq(t) + STORM_GAIN * stormKp(seed, t) / 9);
    }

    public static double kp(long seed, long t) {
        return Math.max(1 + 1.5 * sq(t), stormKp(seed, t));
    }

    public static long seed(Level level) {
        return level instanceof net.minecraft.server.level.ServerLevel server ? server.getSeed() : 0;
    }

    /**
     * The site's field ratio: below sea level the depth from the bottom of the world over the height to sea level, times
     * the share of solid blocks in the 20 below (the generator's rule since 0.1.11.11; the detector now uses it too).
     */
    public static float source(Level level, BlockPos pos) {
        float source = 1.0f;
        int bottom = level.getMinBuildHeight();
        if (pos.getY() < level.getSeaLevel()) source = (float) (pos.getY() - bottom) / (float) (level.getSeaLevel() + 1 - bottom);
        int baseDelta = Math.min(20, pos.getY() - bottom);
        int air = 0;
        for (int i = 0; i < baseDelta; i++) {
            var state = level.getBlockState(pos.below(i));
            if (state.isAir() || state.getBlock() == Blocks.WATER) air++;
        }
        return baseDelta != 0 ? source * (baseDelta - air) / (float) baseDelta : 0;
    }
}
