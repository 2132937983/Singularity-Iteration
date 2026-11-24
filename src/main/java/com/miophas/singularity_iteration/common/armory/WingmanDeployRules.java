// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

/**
 * Pure numbers and palette rules for Wingman Armory deployment & recall spectacles:
 * <ul>
 *   <li>V-wedge staggered aerial drop formation offsets ({@link #vFormationOffset})</li>
 *   <li>Rapid-deploy stagger delay and harmonic audio pitch ramp ({@link #staggerDelayTicks}, {@link #staggerPitch})</li>
 *   <li>Large quantum transfer gate open/shut curves ({@link #gateOpen}) and Armory silo hatch pulse ({@link #hatchOpen})</li>
 *   <li>Tactical mode arrival palettes ({@link #modeAccentColor}) and Living Nano Lining emerald core</li>
 * </ul>
 */
public final class WingmanDeployRules {
    private WingmanDeployRules() {}

    /** Large quantum transfer gate radius (blocks) for Wingman aerial drop / remote recall. */
    public static final float GATE_RADIUS = 1.45F;
    public static final float GATE_OPEN_TICKS = 7F, GATE_HOLD_TICKS = 11F, GATE_SHUT_TICKS = 10F;
    /** Total lifetime of a Wingman transfer gate in ticks. */
    public static final float GATE_TOTAL_TICKS = GATE_OPEN_TICKS + GATE_HOLD_TICKS + GATE_SHUT_TICKS;

    /** Armory silo iris hatch open/hold/shut timing when launching a Wingman. */
    public static final float HATCH_OPEN_TICKS = 4F, HATCH_HOLD_TICKS = 16F, HATCH_SHUT_TICKS = 10F;
    public static final float HATCH_TOTAL_TICKS = HATCH_OPEN_TICKS + HATCH_HOLD_TICKS + HATCH_SHUT_TICKS;

    /** Duration (ticks) of the body phase-materialization boot scan and orbital air-brake ring. */
    public static final float SCAN_TICKS = 14F, SHOCK_TICKS = 12F;

    /** Window (ticks) during which rapid consecutive deployments count as a staggered salvo, and ticks per step. */
    public static final int STAGGER_WINDOW_TICKS = 40;
    public static final int STAGGER_STEP_TICKS = 3;

    /** Electric violet quantum portal inner core. */
    public static final float[] VIOLET_INNER = {0.66F, 0.42F, 1.0F};
    /** Emerald-jade core tint for Wingmen wearing awakened Living Nano Lining. */
    public static final float[] LIVING_CORE = {0.38F, 1.0F, 0.70F};

    /**
     * V-wedge aerial drop offset {@code {dx, dy, dz}} for squad {@code slot} (0..3) relative to the player's facing yaw
     * ({@code yawRad}, Minecraft Y-rot in radians). {@code tight} pulls the wedge inward for indoor/cave spaces.
     */
    public static double[] vFormationOffset(int slot, float yawRad, boolean tight) {
        double fwdX = -Math.sin(yawRad), fwdZ = Math.cos(yawRad);
        double rightX = -Math.cos(yawRad), rightZ = -Math.sin(yawRad);
        double lateral, ahead, dy;
        switch (Math.floorMod(slot, 4)) {
            case 0 -> { lateral = 0.0; ahead = tight ? 0.9 : 1.6; dy = 0.0; }
            case 1 -> { lateral = tight ? 1.5 : 3.2; ahead = tight ? -0.6 : -1.4; dy = tight ? 0.5 : 1.5; }
            case 2 -> { lateral = tight ? -1.5 : -3.2; ahead = tight ? -0.6 : -1.4; dy = tight ? 0.5 : 1.5; }
            default -> { lateral = 0.0; ahead = tight ? -1.5 : -3.0; dy = tight ? 1.0 : 2.5; }
        }
        return new double[]{
            fwdX * ahead + rightX * lateral,
            dy,
            fwdZ * ahead + rightZ * lateral
        };
    }

    /** Visual stagger delay in ticks for the {@code index}-th rapid consecutive deployment (0, 3, 6, 9). */
    public static int staggerDelayTicks(int index) {
        return Math.min(3, Math.max(0, index)) * STAGGER_STEP_TICKS;
    }

    /** Harmonic ascending audio pitch for the {@code index}-th rapid consecutive deployment (0.90 -> 1.14). */
    public static float staggerPitch(int index) {
        return 0.90F + 0.08F * Math.min(3, Math.max(0, index));
    }

    /** Smooth 0..1 opening of a Wingman quantum transfer gate at {@code age} ticks since gate creation. */
    public static float gateOpen(float age) {
        if (age <= 0F || age >= GATE_TOTAL_TICKS) return 0F;
        if (age < GATE_OPEN_TICKS) return smooth(age / GATE_OPEN_TICKS);
        if (age < GATE_OPEN_TICKS + GATE_HOLD_TICKS) return 1F;
        return 1F - smooth((age - GATE_OPEN_TICKS - GATE_HOLD_TICKS) / GATE_SHUT_TICKS);
    }

    /** Bright starburst flash (0..1) right as the Wingman bursts through the transfer gate. */
    public static float gateFlash(float age) {
        float peak = GATE_OPEN_TICKS * 0.65F;
        float dt = age - peak;
        if (dt < 0F || dt > 5F) return 0F;
        return 1F - dt / 5F;
    }

    /** Smooth 0..1 Armory silo iris hatch opening driven by a Wingman launch pulse. */
    public static float hatchOpen(float age) {
        if (age <= 0F || age >= HATCH_TOTAL_TICKS) return 0F;
        if (age < HATCH_OPEN_TICKS) return smooth(age / HATCH_OPEN_TICKS);
        if (age < HATCH_OPEN_TICKS + HATCH_HOLD_TICKS) return 1F;
        return 1F - smooth((age - HATCH_OPEN_TICKS - HATCH_HOLD_TICKS) / HATCH_SHUT_TICKS);
    }

    /** Progress (0..1) of the body phase-materialization scan line; negative if not active. */
    public static float scanProgress(float age) {
        if (age < 0F || age > SCAN_TICKS) return -1F;
        return age / SCAN_TICKS;
    }

    /** Tactical mode accent RGB {r, g, b} 0..1 for the arrival gate, orbital air-brake shockwave, and boot scan. */
    public static float[] modeAccentColor(WingmanMode mode) {
        return switch (mode == null ? WingmanMode.ESCORT : mode) {
            case OFFENSIVE -> new float[]{1.0F, 0.24F, 0.22F};
            case DISPERSAL -> new float[]{1.0F, 0.72F, 0.18F};
            case ESCORT -> new float[]{0.22F, 0.90F, 1.0F};
        };
    }

    private static float smooth(float x) {
        float t = Math.max(0F, Math.min(1F, x));
        return t * t * (3F - 2F * t);
    }
}
