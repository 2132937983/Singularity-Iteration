// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

/**
 * 0.1.10.32 (plan C): the timing and shape rules of the armour hit ripple (S-6), the full suit boot (A-5), the visor
 * light-up and the moving parts (S-8). Pure (java.* only).
 * <ul>
 *   <li><b>Hit ripple:</b> a hexagonal ring of light runs over the armour from the hit point at {@link #RIPPLE_SPEED} blocks
 *       a tick for {@link #RIPPLE_TICKS} ticks, with a bright spot at the hit that fades in the first half
 *       ({@link #ripple}).</li>
 *   <li><b>Visor:</b> the visor lights from its centre line to both sides at {@link #VISOR_SPEED} pixels a second, a bright
 *       front ahead of the lit part ({@link #visor}).</li>
 *   <li><b>Boot</b> (all pieces locked by the Armory): the suit light sweeps from the chest core (the power-on sweep), a
 *       ripple runs from the core at once, the visor lights after {@link #BOOT_VISOR_DELAY} seconds.</li>
 *   <li><b>Moving parts:</b> vents and shoulder plates ease open and shut ({@link #ease}): open in {@link #OPEN_TICKS},
 *       shut in {@link #SHUT_TICKS} ticks.</li>
 * </ul>
 */
public final class SuitFxRules {
    private SuitFxRules() {}

    public static final int RIPPLE_TICKS = 12;
    public static final float RIPPLE_SPEED = 0.15F;
    public static final float RIPPLE_WIDTH = 0.11F;
    /** Least ticks between two ripples of one wearer (a burst of hits draws one ripple). */
    public static final int RIPPLE_GAP = 4;
    public static final float VISOR_SPEED = 14F;
    /** Half width of the visor, pixels: past it the sweep is done. */
    public static final float VISOR_HALF = 5F;
    public static final float BOOT_VISOR_DELAY = 0.25F;
    public static final float OPEN_TICKS = 6F, SHUT_TICKS = 12F;
    /** Vent slide (pixels) and shoulder slide (pixels up, back) at full opening. */
    public static final float VENT_SLIDE = 0.9F, SHOULDER_UP = 0.4F, SHOULDER_BACK = 0.5F;

    /**
     * Light of the ripple at {@code distance} blocks from the hit, {@code age} ticks after it, for a hit of {@code strength}
     * (0..1): the ring at {@code RIPPLE_SPEED * age} and the fading spot; 0 after {@link #RIPPLE_TICKS}.
     */
    public static float ripple(double distance, float age, float strength) {
        if (age < 0 || age >= RIPPLE_TICKS) return 0F;
        double radius = 0.04 + RIPPLE_SPEED * age;
        double d = (distance - radius) / RIPPLE_WIDTH;
        double ring = Math.exp(-d * d);
        double spot = age < RIPPLE_TICKS / 2F ? Math.exp(-(distance / 0.2) * (distance / 0.2)) * (1 - age / (RIPPLE_TICKS / 2F)) : 0;
        double fade = 1 - Math.pow(age / RIPPLE_TICKS, 3);
        return (float) Math.min(1.5, Math.max(0, strength) * (ring + 0.7 * spot) * fade);
    }

    /**
     * The parts of {@link #ripple} that depend only on the age, prepared once a frame for the thousands of vertices of a pass:
     * {@code out[o]} ring radius, {@code out[o + 1]} strength times fade (0 when the ripple is over), {@code out[o + 2]} spot
     * factor.
     */
    public static void prepare(float age, float strength, float[] out, int o) {
        boolean live = age >= 0 && age < RIPPLE_TICKS;
        out[o] = (float) (0.04 + RIPPLE_SPEED * age);
        out[o + 1] = live ? (float) (Math.max(0, strength) * (1 - Math.pow(age / RIPPLE_TICKS, 3))) : 0F;
        out[o + 2] = live && age < RIPPLE_TICKS / 2F ? 1 - age / (RIPPLE_TICKS / 2F) : 0F;
    }

    /**
     * {@link #ripple} from a prepared ripple at squared distance {@code d2}: the ring is cut off beyond 3 widths and the spot
     * beyond 3 of its radii (both below 0.00013 of their peak there), so most vertices of a pass need no square root and no
     * exponential. The result is at most 0.0002 below {@link #ripple}.
     */
    public static float rippleAt(double d2, float[] prep, int o) {
        float scale = prep[o + 1];
        if (scale <= 0F) return 0F;
        float radius = prep[o], spot = prep[o + 2];
        double ringFar = radius + 3 * RIPPLE_WIDTH, ringNear = Math.max(0, radius - 3 * RIPPLE_WIDTH);
        boolean inSpot = spot > 0 && d2 < 0.36;
        boolean inRing = d2 < ringFar * ringFar && d2 >= ringNear * ringNear;   // 0.1.10.42: >=, the hit point itself is in a new ring
        if (!inSpot && !inRing) return 0F;
        double d = Math.sqrt(d2);
        double v = 0;
        if (inRing) {
            double k = (d - radius) / RIPPLE_WIDTH;
            v += Math.exp(-k * k);
        }
        if (inSpot) v += 0.7 * Math.exp(-(d / 0.2) * (d / 0.2)) * spot;
        return (float) Math.min(1.5, scale * v);
    }

    /** Strength (0.35..1) of a ripple from the absorbed share of a hit (0..1). */
    public static float rippleStrength(float absorbedShare) {
        return Math.max(0.35F, Math.min(1F, 0.35F + 0.65F * absorbedShare));
    }

    /**
     * Visor light multiplier at {@code absX} pixels from the centre line, {@code since} seconds after the light-up started
     * (negative: no light-up running, 1): dark ahead of the front, a bright front, lit behind it.
     */
    public static float visor(float absX, float since) {
        if (since < 0) return 1F;
        float front = since * VISOR_SPEED;
        if (front > VISOR_HALF + 2F) return 1F;
        float d = absX - front;
        if (d > 0.5F) return 0.12F;
        return 1F + 0.9F * (float) Math.exp(-((d + 0.25F) / 0.6F) * ((d + 0.25F) / 0.6F));
    }

    /** One tick of a moving part towards {@code target} (0 shut, 1 open). */
    public static float ease(float current, boolean open) {
        return open ? Math.min(1F, current + 1F / OPEN_TICKS + 1.0E-4F) : Math.max(0F, current - 1F / SHUT_TICKS - 1.0E-4F);
    }

    /** Smooth step of a moving part for drawing (0..1). */
    public static float smooth(float v) {
        float t = Math.max(0F, Math.min(1F, v));
        return t * t * (3 - 2 * t);
    }
}
