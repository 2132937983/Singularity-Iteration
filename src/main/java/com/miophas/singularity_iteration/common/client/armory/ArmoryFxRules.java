// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

/**
 * 0.1.10.32 (A-1, A-4): the rules of the Armory gantry and the transfer gate. Pure (java.* only).
 * <ul>
 *   <li><b>Iris hatch:</b> six blades tile a hexagon of radius {@link #IRIS_RADIUS} when shut (each blade a triangle from the
 *       centre to one hexagon edge); opening moves each blade tip out along a spiral to {@link #TIP_OPEN} of the radius, so a
 *       twisting hole opens ({@link #blade}). The hatch opens {@link #HATCH_LEAD} ticks before a launch and while a returning
 *       piece comes in ({@link #hatch}).</li>
 *   <li><b>Transfer gate:</b> a summon from far away or from another dimension starts in the sky; there a gate of
 *       {@link #GATE_RADIUS} blocks opens while the piece pressurises and shuts after it left; a piece that flies home to the
 *       sky goes into a gate that opens in front of it ({@link #gate}).</li>
 *   <li><b>Icons:</b> the stored suits circle above the Armory ({@link #iconAngle}).</li>
 * </ul>
 */
public final class ArmoryFxRules {
    private ArmoryFxRules() {}

    public static final float IRIS_RADIUS = 0.34F;
    public static final float TIP_OPEN = 0.92F;
    /** Twist of a blade tip at full opening, radians. */
    public static final float TWIST = 0.7F;
    public static final float HATCH_LEAD = 10F, HATCH_HOLD = 4F, HATCH_SHUT = 8F;
    public static final float GATE_RADIUS = 0.85F;
    public static final float GATE_OPEN = 8F, GATE_HOLD = 6F, GATE_SHUT = 8F;
    public static final float ICON_RADIUS = 0.62F, ICON_HEIGHT = 1.5F;
    /** Icons show within this many blocks of the camera. */
    public static final double ICON_RANGE = 8;

    static float smooth(float x) {
        float t = Math.max(0F, Math.min(1F, x));
        return t * t * (3 - 2 * t);
    }

    /**
     * Blade {@code i} (0..5) at opening {@code open} (0..1): three corners {tip x, tip z, rim0 x, rim0 z, rim1 x, rim1 z} in
     * blocks around the hatch centre.
     */
    public static void blade(int i, float open, float[] out) {
        double a0 = i * Math.PI / 3, a1 = (i + 1) * Math.PI / 3, mid = (a0 + a1) / 2 + TWIST * open;
        double tip = IRIS_RADIUS * TIP_OPEN * open;
        out[0] = (float) (Math.cos(mid) * tip);
        out[1] = (float) (Math.sin(mid) * tip);
        out[2] = (float) (Math.cos(a0) * IRIS_RADIUS);
        out[3] = (float) (Math.sin(a0) * IRIS_RADIUS);
        out[4] = (float) (Math.cos(a1) * IRIS_RADIUS);
        out[5] = (float) (Math.sin(a1) * IRIS_RADIUS);
    }

    /**
     * Opening of the hatch for one piece: a delivery at {@code age} ticks (negative in the hatch), or a return piece at
     * {@code age} of {@code duration} ticks; 0..1.
     */
    public static float hatch(float age, int duration, boolean returning) {
        if (returning) {
            float t = age - duration;                          // 0 = the piece arrives
            if (t < -HATCH_LEAD) return 0F;
            if (t < -HATCH_HOLD) return smooth((t + HATCH_LEAD) / (HATCH_LEAD - HATCH_HOLD));
            if (t < HATCH_HOLD) return 1F;
            return 1F - smooth((t - HATCH_HOLD) / HATCH_SHUT);
        }
        if (age < -HATCH_LEAD) return 0F;
        if (age < -HATCH_HOLD) return smooth((age + HATCH_LEAD) / (HATCH_LEAD - HATCH_HOLD));
        if (age < HATCH_HOLD) return 1F;
        return 1F - smooth((age - HATCH_HOLD) / HATCH_SHUT);
    }

    /**
     * Opening of a transfer gate, 0..1, {@code t} ticks after the piece passes it (a delivery leaves the gate at its launch,
     * a return reaches it at the end of its flight): a delivery gate opens in the {@link #GATE_OPEN} ticks before, stays
     * {@link #GATE_HOLD} ticks and shuts in {@link #GATE_SHUT}; a return gate is open {@link #GATE_HOLD} ticks before the piece
     * arrives (it shrinks into it, {@link #swallow}) and shuts from then.
     */
    public static float gate(float t, boolean returning) {
        if (returning) t += GATE_HOLD;
        if (t < -GATE_OPEN) return 0F;
        if (t < 0) return smooth((t + GATE_OPEN) / GATE_OPEN);
        if (t < GATE_HOLD) return 1F;
        return 1F - smooth((t - GATE_HOLD) / GATE_SHUT);
    }

    /** Ticks a gate lives from its first opening tick to shut. */
    public static float gateLife() {
        return GATE_OPEN + GATE_HOLD + GATE_SHUT;
    }

    /** Flash of a gate at a launch, 0..1 (the first 3 ticks after the piece leaves). */
    public static float flash(float age) {
        return age < 0 || age > 3 ? 0F : 1F - age / 3F;
    }

    /** Angle (radians) of icon {@code i} of {@code n} at {@code time} ticks. */
    public static double iconAngle(int i, int n, float time) {
        return time * 0.02 + i * Math.PI * 2 / Math.max(1, n);
    }

    /** Scale of a returning piece that flies into a gate: shrinks in its last 6 ticks. */
    public static float swallow(float age, int duration) {
        float left = duration - age;
        return left >= 6 ? 1F : Math.max(0F, left / 6F);
    }
}
