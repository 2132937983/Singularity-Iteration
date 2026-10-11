// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.world;

import com.miophas.singularity_iteration.core.runtime.CoreConfig;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Global wind simulation. It copies the IC2 WindSim mechanism with an independent implementation.
 *
 * <p>Each dimension keeps one wind state:
 * <ul>
 *   <li>The base wind strength does a random walk every 128 ticks. A high wind rises less easily
 *       (above 30, the chance to rise decreases). A low wind falls less easily
 *       (below 12, the chance to fall decreases). Thus the wind stays near the range 12 to 30.</li>
 *   <li>Every 128 ticks, the wind direction can turn 18° left or right.</li>
 *   <li>The wind changes with height by a cubic polynomial. The factor has its peak of 1 at the midpoint
 *       between sea level and the build limit. It falls to 0 at 1.125 times the build limit.
 *       The derivative is 0 at the peak. These three constraints give the coefficients.
 *       Example for the overworld (build limit 320, sea level 63): y=64 is about 0.54, y=140 is about 0.93,
 *       y=191 is the peak 1.0, y=320 is about 0.44.</li>
 *   <li>Thunder multiplies the wind by 1.5. Rain multiplies it by 1.25. Then the global wind multiplier
 *       converts it to MCU. Typical wind at middle and high altitude in clear weather is about 45 to 110 MCU.
 *       Storm peaks can go above 150 MCU. Carbon rotors then overload, as in the IC2 risk and reward design.</li>
 * </ul>
 *
 * <p>You can change the global wind multiplier with
 * <code>[wind] globalMultiplier</code> in <code>assets/mio_icif/config/core.ini</code> (default 4.0).
 * It matches the IC2 setting balance/energyfix/kineticgenerator/wind.
 *
 * <p>The state is saved with the world. The random walk follows the absolute game time.
 * After a world load or a server restart, the simulation calculates the missed steps.
 * A step limit prevents lag after a long time offline.
 *
 * <p>Server side only. Call on the server thread.
 */
public final class WindSim extends SavedData {
    private static final String DATA_ID = "scex_wind_sim";
    /** The IC2 wind update interval (ticks). */
    public static final int UPDATE_INTERVAL = 128;
    /** The default global wind multiplier. It is higher than the IC2 value of 2.4, so typical wind at middle and high altitude is in the useful range of carbon rotors. */
    public static final double DEFAULT_GLOBAL_MULTIPLIER = 4.0;
    /** The maximum number of missed steps to calculate in one access. Above this limit, the time jumps to the current time. */
    private static final int MAX_CATCH_UP_STEPS = 4096;

    private ServerLevel level;
    /** The cubic polynomial coefficients: p(y) = c1*y + c2*y^2 + c3*y^3. */
    private double[] coefficients;
    private int windStrength = -1;
    private int windDirection;
    private long lastProcessedTick = Long.MIN_VALUE;

    public WindSim() {}

    public static WindSim get(ServerLevel level) {
        WindSim sim = level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(WindSim::new, WindSim::load), DATA_ID);
        sim.attach(level);
        return sim;
    }

    /** DataStorage keeps one instance per dimension. On first access, this binds the dimension and sets the local parameters. */
    private void attach(ServerLevel level) {
        if (this.level == level && coefficients != null) return;
        this.level = level;
        this.coefficients = calculateCoefficients(level.getMaxBuildHeight(), level.getSeaLevel());
        if (windStrength < 0) windStrength = 10 + level.random.nextInt(20);
        if (lastProcessedTick == Long.MIN_VALUE) lastProcessedTick = level.getGameTime();
    }

    public static WindSim load(CompoundTag tag, HolderLookup.Provider registries) {
        WindSim sim = new WindSim();
        sim.windStrength = tag.getInt("WindStrength");
        sim.windDirection = tag.getInt("WindDirection");
        sim.lastProcessedTick = tag.contains("LastProcessedTick") ? tag.getLong("LastProcessedTick") : Long.MIN_VALUE;
        return sim;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("WindStrength", windStrength);
        tag.putInt("WindDirection", windDirection);
        tag.putLong("LastProcessedTick", lastProcessedTick);
        return tag;
    }

    /** Moves the wind simulation forward to the current game time (one step per 128 ticks). */
    private void advance() {
        if (level == null) return;
        long now = level.getGameTime();
        if (lastProcessedTick == Long.MIN_VALUE) { lastProcessedTick = now; return; }
        int steps = 0;
        while (lastProcessedTick + UPDATE_INTERVAL <= now && steps < MAX_CATCH_UP_STEPS) {
            step();
            lastProcessedTick += UPDATE_INTERVAL;
            steps++;
        }
        if (lastProcessedTick + UPDATE_INTERVAL <= now) lastProcessedTick = now;
        if (steps > 0) setDirty();
    }

    private void step() {
        RandomSource random = level.random;
        int upChance = 10;
        int downChance = 10;
        // Balance range 12 to 30: above 30 the chance to rise decreases; below 12 the chance to fall decreases
        if (windStrength > 30) {
            upChance -= windStrength - 30;
        } else if (windStrength < 12) {
            downChance -= 12 - windStrength;
        }
        if (random.nextInt(100) < upChance) {
            windStrength++;
        } else if (random.nextInt(100) < downChance) {
            windStrength--;
        }
        switch (random.nextInt(3)) {
            case 0 -> windDirection = turn(windDirection, -18);
            case 2 -> windDirection = turn(windDirection, 18);
        }
    }

    private static int turn(int direction, int amount) {
        int next = direction + amount;
        if (next < 0) return 359 + next + 1;
        if (next > 359) return next - 360;
        return next;
    }

    /**
     * Solves the height coefficients with Cramer's rule. The constraints are:
     * p(sh)=1 (peak at the midpoint), p(fh)=0 (at 1.125 times the build limit), and p'(sh)=0.
     */
    private static double[] calculateCoefficients(int height, int seaLevel) {
        double h = Math.max(1, height);
        double sea = Math.max(0, seaLevel);
        double baseHeight = sea < h ? sea : h * 0.5;
        double sh = baseHeight + (h - baseHeight) / 2.0;
        double fh = h * 1.125;
        double det = det3(sh, sh * sh, sh * sh * sh,
                          fh, fh * fh, fh * fh * fh,
                          1.0, 2.0 * sh, 3.0 * sh * sh);
        if (det == 0.0) return new double[]{0, 0, 0};
        double c1 = det3(1.0, sh * sh, sh * sh * sh,
                         0.0, fh * fh, fh * fh * fh,
                         0.0, 2.0 * sh, 3.0 * sh * sh) / det;
        double c2 = det3(sh, 1.0, sh * sh * sh,
                         fh, 0.0, fh * fh * fh,
                         1.0, 0.0, 3.0 * sh * sh) / det;
        double c3 = det3(sh, sh * sh, 1.0,
                         fh, fh * fh, 0.0,
                         1.0, 2.0 * sh, 0.0) / det;
        return new double[]{c1, c2, c3};
    }

    private static double det3(double a00, double a01, double a02,
                               double a10, double a11, double a12,
                               double a20, double a21, double a22) {
        return a00 * (a11 * a22 - a12 * a21)
             - a01 * (a10 * a22 - a12 * a20)
             + a02 * (a10 * a21 - a11 * a20);
    }

    /** Gets the global wind multiplier from [wind] globalMultiplier in core.ini (default 4.0). A negative value counts as 0. */
    public static double globalMultiplier() {
        return Math.max(0.0, CoreConfig.getDouble("wind", "globalMultiplier", DEFAULT_GLOBAL_MULTIPLIER));
    }

    /** Gets the current wind at a height (MCU). The value is never less than 0. */
    public double getWindAt(double height) {
        advance();
        double multiplier = 0.0;
        if (coefficients != null) {
            multiplier = Math.max(0.0,
                coefficients[0] * height + coefficients[1] * height * height + coefficients[2] * height * height * height);
        }
        double ret = Math.max(0, windStrength) * multiplier;
        if (level != null && level.isThundering()) {
            ret *= 1.5;
        } else if (level != null && level.isRaining()) {
            ret *= 1.25;
        }
        return ret * globalMultiplier();
    }

    /** Gets the current base wind strength (without height and weather correction). */
    public int getWindStrength() {
        advance();
        return Math.max(0, windStrength);
    }

    /** Gets the current wind direction (0 to 359 degrees). */
    public int getWindDirection() {
        return Math.floorMod(windDirection, 360);
    }
}
