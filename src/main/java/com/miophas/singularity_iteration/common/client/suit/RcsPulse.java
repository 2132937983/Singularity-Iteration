// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.client.render.PhaseFx;
import com.miophas.singularity_iteration.common.suit.PhaseRules;
import com.miophas.singularity_iteration.common.suit.RcsPulseRules;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

/**
 * 0.1.11.127 (armour and flight overhaul E4): one electromagnetic attitude pulse ({@link RcsPulseRules}) at a firing port of
 * the quantum suit, in the port's frame {@code m} (+y out of the port, as {@link NozzleDraw#push} sets it): a point of light,
 * a ring snapping out across the axis, and micro arcs bulging out along it. Additive ({@code FIELD_GLOW}).
 */
final class RcsPulse {
    private RcsPulse() {}

    private static final float[] ARC = {0.78F, 0.72F, 1F};

    /** The last firing of each port: key (flyer, port) to {tick, power bits}. */
    private static final java.util.HashMap<Long, long[]> FIRED = new java.util.HashMap<>();

    /**
     * Notes the output of a port this tick and returns the age of its last pulse in ticks (NaN when it has none running)
     * and, through {@code power}, that pulse's strength.
     */
    static float age(int flyer, int port, int tick, float partial, float output, float[] power) {
        long key = ((long) flyer << 8) | port;
        long[] f = FIRED.get(key);
        if (output > 0.02F && (f == null || f[0] != tick)) {
            if (f == null) FIRED.put(key, f = new long[2]);
            f[0] = tick;
            f[1] = Float.floatToIntBits(output);
        }
        if (f == null) return Float.NaN;
        float age = tick - f[0] + partial;
        if (age < 0 || age >= RcsPulseRules.PULSE_TICKS) {
            if (age >= RcsPulseRules.PULSE_TICKS + 20 || age < 0) FIRED.remove(key);
            return Float.NaN;
        }
        power[0] = Float.intBitsToFloat((int) f[1]);
        if (FIRED.size() > 4096) FIRED.clear();
        return age;
    }

    /** A pulse of strength {@code power} {@code age} ticks old; {@code seed} picks the arcs (port and firing tick). */
    static void draw(VertexConsumer vc, Matrix4f m, float power, float age, long seed) {
        float fade = RcsPulseRules.fade(age) * Math.min(1F, power);
        if (fade <= 0.01F) return;
        NozzleDraw.flare(vc, m, RcsPulseRules.FLASH_RADIUS, RcsPulseRules.FLASH_ALPHA * fade);
        PhaseFx.ringAt(vc, m, 0.015F, RcsPulseRules.ringRadius(age), RcsPulseRules.RING_WIDTH, PhaseRules.CYAN,
            RcsPulseRules.RING_ALPHA * fade);
        float a = RcsPulseRules.ARC_ALPHA * (float) Math.pow(fade, 0.75);
        for (int arc = 0; arc < RcsPulseRules.ARCS; arc++) {
            float[][] pts = RcsPulseRules.arc(seed, arc);
            for (int k = 0; k + 1 < pts.length; k++) {
                float along = 1 - k / (float) (pts.length - 1);   // brighter at the port
                segment(vc, m, pts[k], pts[k + 1], RcsPulseRules.ARC_WIDTH, a * (0.5F + 0.5F * along));
            }
        }
    }

    /** A thin line of light from {@code p} to {@code q}: two crossed quads, so it reads from any side. */
    private static void segment(VertexConsumer vc, Matrix4f m, float[] p, float[] q, float w, float alpha) {
        float[] c = k(alpha);
        quad(vc, m, p, q, w, 0, c);
        quad(vc, m, p, q, 0, w, c);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float[] p, float[] q, float dx, float dz, float[] c) {
        put(vc, m, p[0] - dx, p[1], p[2] - dz, c);
        put(vc, m, p[0] + dx, p[1], p[2] + dz, c);
        put(vc, m, q[0] + dx, q[1], q[2] + dz, c);
        put(vc, m, q[0] - dx, q[1], q[2] - dz, c);
    }

    private static float[] k(float alpha) { return new float[]{ARC[0], ARC[1], ARC[2], Math.max(0, Math.min(1, alpha))}; }

    private static void put(VertexConsumer vc, Matrix4f m, float x, float y, float z, float[] c) {
        vc.addVertex(m, x, y, z).setColor(c[0], c[1], c[2], c[3]);
    }
}
