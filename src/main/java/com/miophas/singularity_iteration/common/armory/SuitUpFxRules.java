// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

/**
 * Pure numbers and tier palettes for the Armory suit-up and drawn-blade arrival spectacles:
 * <ul>
 *   <li>Tech-tiered palettes ({@link Tier}) separating standard metal armor (classic atmospheric reentry) from
 *       Nano / Quantum / Living Nano Lining suits (electromagnetic cyan-violet phase reentry & emerald bio-core).</li>
 *   <li>Inward-contracting magnetic lock-in rings, outward purge decoupling rings, and 3-beat Core Ignition boot
 *       curves (chest starburst, dual-helix body scan, Quantum dorsal phase-wing self-test flex, first-person visor sweep).</li>
 *   <li>Drawn-blade 360-degree axial spin-to-grip angle ({@link #bladeSpinDeg}) and SlashBlade color unpacking.</li>
 * </ul>
 */
public final class SuitUpFxRules {
    private SuitUpFxRules() {}

    /** Visual technology tier of a suit piece or full suit. */
    public enum Tier { STANDARD, NANO, QUANTUM, QUANTUM_LIVING }

    /** Default quantum cyan RGB for drawn blades without a custom SlashBlade color code. */
    public static final int DEFAULT_BLADE_RGB = 0x3EE8FF;

    /** Duration (ticks) of per-piece lock-in rings, purge rings, and blade catch resonance. */
    public static final float LOCK_RING_TICKS = 7F;
    public static final float PURGE_RING_TICKS = 9F;
    public static final float BLADE_CATCH_TICKS = 11F;

    /** Total duration (seconds) of the 3-beat Core Ignition full-suit boot sequence. */
    public static final float BOOT_TOTAL_SEC = 0.90F;
    /** Duration (seconds) of the first-person optical visor boot telemetry sweep. */
    public static final float VISOR_SWEEP_SEC = 0.45F;

    public static Tier tierOf(boolean quantum, boolean nano, boolean livingLining) {
        if (quantum) return livingLining ? Tier.QUANTUM_LIVING : Tier.QUANTUM;
        if (nano) return Tier.NANO;
        return Tier.STANDARD;
    }

    /** True when the piece uses electromagnetic cyan-violet phase reentry instead of orange atmospheric plasma. */
    public static boolean isElectromagneticReentry(Tier tier) {
        return tier != Tier.STANDARD;
    }

    /** Primary accent RGB {r, g, b} 0..1 for {@code tier}. */
    public static float[] primaryColor(Tier tier) {
        return switch (tier == null ? Tier.STANDARD : tier) {
            case STANDARD -> new float[]{0.82F, 0.90F, 1.0F};
            case NANO -> new float[]{0.35F, 0.92F, 1.0F};
            case QUANTUM, QUANTUM_LIVING -> new float[]{0.24F, 0.91F, 1.0F};
        };
    }

    /** Secondary accent RGB {r, g, b} 0..1 (violet phase outer band for Quantum, crisp cyan-silver for others). */
    public static float[] secondaryColor(Tier tier) {
        return switch (tier == null ? Tier.STANDARD : tier) {
            case STANDARD -> new float[]{0.62F, 0.78F, 0.96F};
            case NANO -> new float[]{0.18F, 0.72F, 1.0F};
            case QUANTUM, QUANTUM_LIVING -> new float[]{0.66F, 0.42F, 1.0F};
        };
    }

    /** Core highlight RGB {r, g, b} 0..1 (emerald bio-core for Living Nano Lining, bright white-cyan otherwise). */
    public static float[] coreColor(Tier tier) {
        return switch (tier == null ? Tier.STANDARD : tier) {
            case QUANTUM_LIVING -> new float[]{0.42F, 1.0F, 0.72F};
            case QUANTUM -> new float[]{0.94F, 0.95F, 1.0F};
            case NANO -> new float[]{0.90F, 0.98F, 1.0F};
            case STANDARD -> new float[]{1.0F, 1.0F, 1.0F};
        };
    }

    /** Contracting lock-in ring radius (blocks): snaps inward from 0.68 to 0.20 as {@code prog} goes 0 -> 1. */
    public static float lockRingRadius(float prog) {
        float t = clamp(prog);
        float ease = 1F - (1F - t) * (1F - t) * (1F - t);
        return 0.68F - 0.48F * ease;
    }

    /** Contracting lock-in ring opacity (0..0.92). */
    public static float lockRingAlpha(float prog) {
        float t = clamp(prog);
        if (t <= 0F || t >= 1F) return 0F;
        float attack = Math.min(1F, t / 0.20F);
        float decay = (1F - t) * (1F - t);
        return 0.92F * attack * decay;
    }

    /** Outward decoupling purge ring radius (blocks): expands from 0.30 to 1.45. */
    public static float purgeRingRadius(float prog) {
        float t = clamp(prog);
        return 0.30F + 1.15F * (1F - (1F - t) * (1F - t));
    }

    /** Beat 2 dual-helix body scan progress (0..1) over 0.05 s -> 0.65 s; negative when inactive. */
    public static float bootScanProgress(float sec) {
        if (sec < 0.05F || sec > 0.65F) return -1F;
        return (sec - 0.05F) / 0.60F;
    }

    /** Beat 3 Quantum Suit exclusive dorsal phase-wing self-test flex (0..0.60) over 0.20 s -> 0.75 s. */
    public static float bootWingFlex(float sec) {
        if (sec <= 0.20F || sec >= 0.75F) return 0F;
        float u = (sec - 0.20F) / 0.55F;
        if (u < 0.32F) {
            float k = u / 0.32F;
            return 0.60F * (k * k * (3F - 2F * k));
        }
        float k = (u - 0.32F) / 0.68F;
        return 0.60F * (1F - k * k * (3F - 2F * k));
    }

    /** First-person optical visor telemetry sweep progress (0..1) over 0 -> 0.45 s; negative when finished. */
    public static float bootVisorScan(float sec) {
        if (sec < 0F || sec > VISOR_SWEEP_SEC) return -1F;
        return sec / VISOR_SWEEP_SEC;
    }

    /**
     * Axial spin-to-grip angle (degrees, 0 -> 360) for a drawn blade during its final hover-tail -> snap into the hand.
     * {@code hoverTicks}: ticks since flight ended (0..HOVER_TICKS+SNAP_TICKS).
     */
    public static float bladeSpinDeg(float hoverTicks) {
        float start = ArmoryFlight.HOVER_TICKS * 0.55F;
        float end = ArmoryFlight.HOVER_TICKS + ArmoryFlight.SNAP_TICKS;
        if (hoverTicks <= start) return 0F;
        if (hoverTicks >= end) return 360F;
        float u = (hoverTicks - start) / (end - start);
        // Smooth acceleration into a snappy wrist-lock finish
        float ease = u * u * (3F - 2F * u);
        return 360F * ease;
    }

    /** Unpacks 24-bit RGB into luminous 0..1 {r, g, b}, lifting very dark colors so blades always glow cleanly. */
    public static float[] unpackRgb(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255F;
        float g = ((rgb >> 8) & 0xFF) / 255F;
        float b = (rgb & 0xFF) / 255F;
        float max = Math.max(r, Math.max(g, b));
        if (max < 0.25F) return new float[]{0x3E / 255F, 0xE8 / 255F, 1.0F};
        if (max < 0.75F) {
            float lift = 0.85F / max;
            r = Math.min(1F, r * lift);
            g = Math.min(1F, g * lift);
            b = Math.min(1F, b * lift);
        }
        return new float[]{r, g, b};
    }

    private static float clamp(float x) { return Math.max(0F, Math.min(1F, x)); }
}
