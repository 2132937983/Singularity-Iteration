// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

/**
 * 0.1.10.31 (A-6): the flight effects of an Armory piece. Pure (java.* only); {@code PieceFx} draws with it.
 * <ul>
 *   <li><b>Ion trail:</b> the last {@link #TRAIL_POINTS} tick positions of a flying piece make a ribbon that narrows and
 *       fades towards its end; points older than {@link #TRAIL_TICKS} ticks or slower than {@link #TRAIL_SPEED} are dropped.</li>
 *   <li><b>Re-entry glow:</b> a piece that comes down fast from high above its wearer (the far summon starts in the sky)
 *       glows white-orange: the product of a speed term, a descent term (how steeply it falls) and a height term (blocks
 *       above the dock point), each a smooth step.</li>
 *   <li><b>Seam light:</b> when a segment locks, a line of light runs along the edges of its quads from top to bottom in
 *       {@link #SEAM_TICKS} ticks.</li>
 * </ul>
 */
public final class PieceFxRules {
    private PieceFxRules() {}

    public static final int TRAIL_POINTS = 14;
    public static final int TRAIL_TICKS = 16;
    public static final float TRAIL_SPEED = 0.25F;
    public static final float TRAIL_WIDTH = 0.16F;
    /** Re-entry: speed (blocks a tick) where the glow starts and the span to full; the same for the descent and the height. */
    public static final float GLOW_SPEED = 0.7F, GLOW_SPEED_SPAN = 0.45F;
    public static final float GLOW_DESCENT = 0.3F, GLOW_DESCENT_SPAN = 0.35F;
    public static final float GLOW_HEIGHT = 9F, GLOW_HEIGHT_SPAN = 6F;
    public static final float SEAM_TICKS = 3F;
    /** Width of the seam sweep front, as a fraction of the segment height. */
    public static final float SEAM_FRONT = 0.18F;

    static float smooth(double x) {
        double t = Math.max(0, Math.min(1, x));
        return (float) (t * t * (3 - 2 * t));
    }

    /**
     * Re-entry glow 0..1 of a piece moving {@code speed} blocks a tick with the vertical speed {@code vy} (negative: down),
     * {@code height} blocks above its dock point.
     */
    public static float reentry(double speed, double vy, double height) {
        if (speed < 1.0E-6) return 0F;
        return smooth((speed - GLOW_SPEED) / GLOW_SPEED_SPAN) * smooth((-vy / speed - GLOW_DESCENT) / GLOW_DESCENT_SPAN)
            * smooth((height - GLOW_HEIGHT) / GLOW_HEIGHT_SPAN);
    }

    /** Alpha of the trail at point {@code i} (0 = the piece) of {@code n} points. */
    public static float trailAlpha(int i, int n) {
        if (n < 2) return 0F;
        float k = i / (float) (n - 1);
        return (1 - k) * (1 - k);
    }

    /** Half width of the trail at point {@code i} of {@code n}. */
    public static float trailHalfWidth(int i, int n) {
        if (n < 2) return 0F;
        return TRAIL_WIDTH * 0.5F * (1 - 0.7F * i / (float) (n - 1));
    }

    /**
     * Seam light at height {@code y01} of a segment (0 top, 1 bottom), {@code age} ticks after it locked: a bright front that
     * runs down in {@link #SEAM_TICKS} ticks and a dimmer line behind it; 0 before and after.
     */
    public static float seam(float y01, float age) {
        if (age < 0 || age >= SEAM_TICKS) return 0F;
        float front = -SEAM_FRONT + (1 + 2 * SEAM_FRONT) * age / SEAM_TICKS;
        float d = (y01 - front) / SEAM_FRONT;
        float head = (float) Math.exp(-d * d);
        float behind = y01 < front ? 0.35F * (1 - age / SEAM_TICKS) : 0F;
        return Math.min(1F, head + behind);
    }
}
