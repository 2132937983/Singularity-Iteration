// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.fcs;

/**
 * 0.1.10.23 (FCS P7): warning of an incoming projectile (an arrow, a trident, a thrown item, a fireball) for the Threat
 * Sensor: where and when it passes closest to the wearer, whether it hits, and which way to step aside. Pure
 * arithmetic; units are blocks and ticks.
 *
 * <p>The projectile follows its vanilla rules per tick (move, then drag, then gravity; a fireball adds its push along
 * its heading before the drag); the wearer keeps its velocity. Each tick is checked in four steps against the
 * wearer's box grown by the margin of the projectile kind ({@link Ballistics#margin}).
 */
public final class ProjectileWarning {
    private ProjectileWarning() {}

    /** Ticks looked ahead (2 s). */
    public static final int HORIZON = 40;

    /**
     * The rules of one kind of projectile: gravity and drag per tick, push per tick along its heading (fireballs), and
     * the growth of the wearer's box for its hit test (the vanilla arrow test grows the box by 0.3; a large fireball is
     * 1 block wide).
     */
    public record Ballistics(double gravity, double drag, double push, double margin) { }

    public static final Ballistics ARROW = new Ballistics(0.05, 0.99, 0, 0.3);
    public static final Ballistics THROWN = new Ballistics(0.03, 0.99, 0, 0.3);
    public static final Ballistics SPIT = new Ballistics(0.06, 0.99, 0, 0.3);
    public static final Ballistics FIREBALL = new Ballistics(0, 0.95, 0.1, 0.5);

    /**
     * The closest approach.
     *
     * @param ticks  ticks until the closest approach (fractional)
     * @param miss   distance from the grown box at that moment (0 = it hits)
     * @param hit    it enters the grown box within the horizon
     * @param evadeX x of the horizontal unit direction to step (away from the side it passes, across its path)
     * @param evadeZ z of that direction
     */
    public record Warning(double ticks, double miss, boolean hit, double evadeX, double evadeZ) { }

    /**
     * The closest approach of a projectile at {@code pos} with velocity {@code vel} to a wearer whose feet are at
     * {@code feet}, moving with {@code wearerVel}, within {@link #HORIZON} ticks; null when it moves away from the
     * start (it never comes closer than it is now).
     */
    public static Warning closest(double[] pos, double[] vel, Ballistics b, double[] feet, double[] wearerVel, double halfWidth, double height) {
        double px = pos[0], py = pos[1], pz = pos[2], vx = vel[0], vy = vel[1], vz = vel[2];
        double best = Double.MAX_VALUE, bestT = 0, bx = px, bz = pz, hvx = vx, hvz = vz;
        double first = distance(px, py, pz, feet, wearerVel, 0, halfWidth, height, b.margin());
        for (int t = 0; t < HORIZON; t++) {
            for (int s = 1; s <= 4; s++) {
                double f = s / 4.0, time = t + f;
                double x = px + vx * f, y = py + vy * f, z = pz + vz * f;
                double d = distance(x, y, z, feet, wearerVel, time, halfWidth, height, b.margin());
                if (d < best) { best = d; bestT = time; bx = x; bz = z; hvx = vx; hvz = vz; }
                if (d <= 0) break;
            }
            if (best <= 0) break;
            px += vx; py += vy; pz += vz;
            if (b.push() > 0) {
                double n = Math.sqrt(vx * vx + vy * vy + vz * vz);
                if (n > 1.0E-9) { vx += vx / n * b.push(); vy += vy / n * b.push(); vz += vz / n * b.push(); }
            }
            vx *= b.drag(); vy *= b.drag(); vz *= b.drag();
            vy -= b.gravity();
        }
        if (best >= first - 1.0E-6 && first > 0) return null;
        // step across the path, away from the side it passes (or to the left of its path when it comes dead centre)
        double cx = feet[0] + wearerVel[0] * bestT - bx, cz = feet[2] + wearerVel[2] * bestT - bz;
        double hn = Math.hypot(hvx, hvz);
        double ex, ez;
        if (hn < 1.0E-6) { ex = cx; ez = cz; }
        else {
            double ux = hvx / hn, uz = hvz / hn;
            double side = cx * -uz + cz * ux;                    // offset of the wearer to the left of the path
            if (Math.abs(side) < 0.05) side = 1;
            ex = -uz * Math.signum(side); ez = ux * Math.signum(side);
        }
        double en = Math.hypot(ex, ez);
        if (en < 1.0E-9) { ex = 1; ez = 0; en = 1; }
        return new Warning(bestT, Math.max(0, best), best <= 0, ex / en, ez / en);
    }

    /** Distance from (x, y, z) to the grown box of the wearer at {@code time} ticks (0 inside). */
    private static double distance(double x, double y, double z, double[] feet, double[] wv, double time, double halfWidth, double height, double margin) {
        double cx = feet[0] + wv[0] * time, cy = feet[1] + wv[1] * time, cz = feet[2] + wv[2] * time;
        double r = halfWidth + margin;
        double dx = Math.max(0, Math.abs(x - cx) - r);
        double dz = Math.max(0, Math.abs(z - cz) - r);
        double dy = Math.max(0, Math.max(cy - margin - y, y - (cy + height + margin)));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
