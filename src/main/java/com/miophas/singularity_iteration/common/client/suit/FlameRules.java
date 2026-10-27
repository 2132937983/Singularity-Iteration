// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

/**
 * 0.1.10.31 (S-5): the look of a thruster flame. Pure (java.* only); {@code ThrusterFlame} draws with it.
 * <ul>
 *   <li><b>Colour:</b> the nozzle glow and the flame go from a warm white at low thrust through blue to violet at full
 *       thrust ({@link #colour}), without steps.</li>
 *   <li><b>Texture:</b> a camera-facing strip of the animated flame texture ({@code textures/effect/thruster_flame.png}, four
 *       frames side by side, {@link #FPS} frames a second, each nozzle starts on its own frame) over the glow cone.</li>
 *   <li><b>Sparks:</b> above {@link #SPARK_THROTTLE} each nozzle throws up to {@link #MAX_SPARKS} sparks a tick.</li>
 * </ul>
 */
public final class FlameRules {
    private FlameRules() {}

    public static final int WHITE = 0xFFF2E0, BLUE = 0x5AA8FF, VIOLET = 0xB878FF;
    /** Thrust below which the flame is white, and where it is fully blue. */
    public static final float WHITE_UNTIL = 0.3F, BLUE_AT = 0.65F;
    public static final int FRAMES = 4;
    public static final float FPS = 15F;
    public static final float SPARK_THROTTLE = 0.85F;
    public static final float MAX_SPARKS = 1.5F;

    /** Flame colour at {@code throttle} (0..1): white, then blue, then violet. */
    public static int colour(float throttle) {
        float t = Math.max(0F, Math.min(1F, throttle));
        if (t <= WHITE_UNTIL) return WHITE;
        if (t <= BLUE_AT) return mix(WHITE, BLUE, (t - WHITE_UNTIL) / (BLUE_AT - WHITE_UNTIL));
        return mix(BLUE, VIOLET, (t - BLUE_AT) / (1F - BLUE_AT));
    }

    /** Linear mix of two RGB colours. */
    public static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }

    /** Frame (0..3) of the flame texture at {@code seconds} for the nozzle {@code seed}. */
    public static int frame(float seconds, int seed) {
        return Math.floorMod((int) Math.floor(seconds * FPS) + seed * 3, FRAMES);
    }

    /** Sparks a nozzle throws per tick at {@code throttle} (a fraction is a chance). */
    public static float sparks(float throttle) {
        if (throttle <= SPARK_THROTTLE) return 0F;
        return MAX_SPARKS * Math.min(1F, (throttle - SPARK_THROTTLE) / (1F - SPARK_THROTTLE));
    }

    /**
     * Corners of a camera-facing strip from {@code a} (nozzle) to {@code b} (flame tip), {@code half} wide on each side, in
     * the space where the camera sits at the origin. Order a-s, a+s, b+s, b-s: counter-clockwise seen from the camera (the
     * front face of a culled render type). Returns false (and leaves {@code out}) when the strip points at the camera.
     */
    public static boolean strip(double ax, double ay, double az, double bx, double by, double bz, double half, double[] out) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        // side = axis x (camera - a), the camera at the origin
        double sx = dy * -az - dz * -ay, sy = dz * -ax - dx * -az, sz = dx * -ay - dy * -ax;
        double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (len < 1.0E-9) return false;
        double k = half / len;
        sx *= k; sy *= k; sz *= k;
        out[0] = ax - sx; out[1] = ay - sy; out[2] = az - sz;
        out[3] = ax + sx; out[4] = ay + sy; out[5] = az + sz;
        out[6] = bx + sx; out[7] = by + sy; out[8] = bz + sz;
        out[9] = bx - sx; out[10] = by - sy; out[11] = bz - sz;
        return true;
    }
}
