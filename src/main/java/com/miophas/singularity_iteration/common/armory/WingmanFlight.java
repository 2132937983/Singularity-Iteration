// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.suit.ManeuverMode;
import net.minecraft.world.phys.Vec3;

/**
 * 0.1.11.50 (escort and open Armory phase F1, decision three B): the escort flight law of a Wingman, pure math so that
 * tests can fly it offline.
 *
 * <p>Before, a Wingman steered to its formation place with {@code min(cruise, distance x 0.3)} towards it: at 0.9 blocks
 * per tick at most, and to hold a speed v it had to stay v / 0.3 blocks behind. A player faster than 0.9 (creative flight
 * sprinting 1.09, elytra with rockets about 1.7, maneuver flight up to 4.0) left it behind until the 64-block jump, again
 * and again. Now the Wingman flies the owner's velocity plus the correction to its place ({@link #want}); its speed limit
 * and acceleration grow with the owner's speed ({@link #cap}, {@link #accel}), and the speed above cruise costs EU like the
 * maneuver flight ({@link #speedCost}). At rest (owner still) the law is the old one.
 */
public final class WingmanFlight {
    private WingmanFlight() {}

    /** Fastest escort speed in blocks per tick (1.5 x the maneuver flight's top speed). */
    public static final double MAX_ESCORT = 6.0;
    /** Fraction of the distance to its place that a Wingman closes per tick (the old law). */
    public static final double GAIN = 0.3;
    /** Highest acceleration in blocks per tick per tick. */
    public static final double MAX_ACCEL = 0.9;
    /** A move of the owner longer than this in one tick is a teleport (not a velocity). */
    public static final double TELEPORT = 12.0;
    /** Smoothing of the owner velocity measured from its positions (the server gets them in uneven steps). */
    public static final double OWNER_SMOOTH = 0.5;

    /** Speed limit of an escorting Wingman whose owner flies at {@code ownerSpeed}: never below {@code base}. */
    public static double cap(double ownerSpeed, double base) {
        return Math.max(base, Math.min(MAX_ESCORT, ownerSpeed * 1.25 + 0.5));
    }

    /** Acceleration limit: {@code base} at rest, more with the owner's speed (to follow its turns). */
    public static double accel(double ownerSpeed, double base) {
        return Math.min(MAX_ACCEL, base + Math.max(0, ownerSpeed) * 0.15);
    }

    /**
     * Wanted velocity: the owner's velocity plus {@link #GAIN} of the way to the place ({@code to}), the correction and the
     * sum within {@code cap}.
     */
    public static Vec3 want(Vec3 to, Vec3 ownerVelocity, double cap) {
        Vec3 fix = to.scale(GAIN);
        double f = fix.length();
        if (f > cap) fix = fix.scale(cap / f);
        Vec3 want = ownerVelocity.add(fix);
        double w = want.length();
        return w > cap ? want.scale(cap / w) : want;
    }

    /** The velocity moved towards {@code want} by at most {@code accel}. */
    public static Vec3 accelerate(Vec3 velocity, Vec3 want, double accel) {
        Vec3 dv = want.subtract(velocity);
        if (dv.lengthSqr() > accel * accel) dv = dv.normalize().scale(accel);
        return velocity.add(dv);
    }

    /** Next smoothed owner velocity from its last value and the owner's move of this tick (a teleport resets it). */
    public static Vec3 ownerVelocity(Vec3 last, Vec3 move) {
        if (move.lengthSqr() > TELEPORT * TELEPORT) return Vec3.ZERO;
        return last.add(move.subtract(last).scale(OWNER_SMOOTH));
    }

    /**
     * EU of one tick at {@code speed} above {@code cruise}: the maneuver flight's {@code SPEED_COST x speed^2}, less the
     * part below cruise (which the upkeep pays), so the cost starts at zero. Zero at or below cruise.
     */
    public static double speedCost(double speed, double cruise) {
        if (speed <= cruise) return 0;
        return ManeuverMode.SPEED_COST * (speed * speed - cruise * cruise);
    }

    // ---- the formation (0.1.11.51, escort and open Armory phase F2, decision four A)

    /** Places of the four slots (right, up, forward; blocks) at rest: behind and beside the owner at shoulder height. */
    static final double[][] SLOW = {{-1.6, 1.2, -1.8}, {1.6, 1.2, -1.8}, {-2.9, 1.7, -0.6}, {2.9, 1.7, -0.6}};
    /** Places at full speed ({@link #SPREAD_FULL}): a V opened behind the owner, about twice as wide. */
    static final double[][] FAST = {{-2.2, 1.0, -2.6}, {2.2, 1.0, -2.6}, {-4.4, 1.4, -5.2}, {4.4, 1.4, -5.2}};
    /** Owner speeds (blocks per tick) where the V starts to open and where it is fully open. */
    public static final double SPREAD_START = 0.9, SPREAD_FULL = 4.0;
    /** Owner speeds where the formation starts to follow the flight direction instead of the view, and follows it fully. */
    public static final double HEADING_START = 0.4, HEADING_FULL = 0.9;

    /** 0 at rest .. 1 at {@link #SPREAD_FULL}: how far the V is open. */
    public static double spread(double horizontalSpeed) {
        return Math.max(0, Math.min(1, (horizontalSpeed - SPREAD_START) / (SPREAD_FULL - SPREAD_START)));
    }

    /**
     * The place of {@code slot} around an owner at {@code owner} looking at {@code ownerYaw} (degrees) and flying at
     * {@code ownerVelocity}; {@code tighten} scales the sideways and forward offsets (0.6 when the owner is hurt). At rest
     * the formation is the one of 0.1.8.4 and faces the way the owner looks; with speed it turns to the flight direction
     * (so a look back does not swing the squad across the owner's path) and opens into a V.
     */
    public static Vec3 place(Vec3 owner, float ownerYaw,
                                 Vec3 ownerVelocity, int slot, double tighten) {
        double speed = ownerVelocity.horizontalDistance();
        int i = Math.floorMod(slot, SLOW.length);
        double s = spread(speed);
        double ox = (SLOW[i][0] + (FAST[i][0] - SLOW[i][0]) * s) * tighten;
        double oy = SLOW[i][1] + (FAST[i][1] - SLOW[i][1]) * s;
        double oz = (SLOW[i][2] + (FAST[i][2] - SLOW[i][2]) * s) * tighten;
        double yaw = Math.toRadians(ownerYaw);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);   // forward of the view
        double w = Math.max(0, Math.min(1, (speed - HEADING_START) / (HEADING_FULL - HEADING_START)));
        if (w > 0) {
            double vx = ownerVelocity.x / speed, vz = ownerVelocity.z / speed;
            double bx = fx + (vx - fx) * w, bz = fz + (vz - fz) * w, b = Math.sqrt(bx * bx + bz * bz);
            if (b < 1.0E-3) { fx = vx; fz = vz; } else { fx = bx / b; fz = bz / b; }
        }
        double rx = -fz, rz = fx;                          // right
        return owner.add(rx * ox + fx * oz, oy, rz * ox + fz * oz);
    }
}
