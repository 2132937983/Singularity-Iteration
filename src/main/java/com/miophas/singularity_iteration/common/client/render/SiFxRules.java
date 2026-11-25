// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

/**
 * Pure math rules for shader-aware FX brightness & alpha calibration.
 *
 * <p>Iris shader packs apply HDR bloom and filmic tonemapping to emissive buffers ({@code gbuffers_spidereyes}).
 * Slightly tempering additive alpha prevents stacked light-wings, shockwaves, and trails from clipping to flat white
 * while keeping crisp, vibrant neon edges.
 */
public final class SiFxRules {
    private SiFxRules() {}

    /** Gentle alpha multiplier for additive emissive quads under Iris HDR bloom. */
    public static final float SHADER_ADDITIVE_ALPHA_SCALE = 0.82F;
    /** Gentle alpha multiplier for translucent emissive quads under Iris. */
    public static final float SHADER_TRANSLUCENT_ALPHA_SCALE = 0.92F;

    /** Scales an alpha byte (0..255) for the active shader mode. */
    public static int calibrateAlphaByte(int alpha, boolean additive) {
        int clamped = Math.max(0, Math.min(255, alpha));
        float scale = additive ? SHADER_ADDITIVE_ALPHA_SCALE : SHADER_TRANSLUCENT_ALPHA_SCALE;
        return Math.max(0, Math.min(255, Math.round(clamped * scale)));
    }

    /** Scales a normalized alpha float (0..1) for the active shader mode. */
    public static float calibrateAlpha(float alpha, boolean additive) {
        float clamped = Math.max(0F, Math.min(1F, alpha));
        return clamped * (additive ? SHADER_ADDITIVE_ALPHA_SCALE : SHADER_TRANSLUCENT_ALPHA_SCALE);
    }
}
