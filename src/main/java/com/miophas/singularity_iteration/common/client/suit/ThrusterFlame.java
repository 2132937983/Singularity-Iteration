// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 0.1.10.31 (S-5): one thruster flame, in two passes so that no buffer of an immediate buffer source is used after another
 * render type was taken.
 * <ol>
 *   <li>{@link #glow}: the nozzle disc and the glow cone (additive {@code FIELD_GLOW}) in the colour of {@link FlameRules#colour}
 *       (white, blue, violet with the thrust); the camera-facing flame strip is recorded in {@link Strips}.</li>
 *   <li>{@link Strips#draw}: the strips with the animated flame texture in {@link RenderType#eyes} (vanilla shader, additive,
 *       full bright: shader packs treat it as emissive).</li>
 * </ol>
 * The pose is the camera-relative pose of the caller (an entity layer or the first-person hand): the camera sits at the
 * origin, so the strips are computed in that space and drawn without a matrix.
 */
public final class ThrusterFlame {
    private ThrusterFlame() {}

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
        "textures/effect/thruster_flame.png");
    private static final int SEGMENTS = 10;
    private static final double[] COS = new double[SEGMENTS + 1], SIN = new double[SEGMENTS + 1];
    static {
        for (int i = 0; i <= SEGMENTS; i++) {
            COS[i] = Math.cos(i * Math.PI * 2 / SEGMENTS);
            SIN[i] = Math.sin(i * Math.PI * 2 / SEGMENTS);
        }
    }

    /** Flame strips of one draw call (at most {@link #MAX} nozzles), reused. */
    public static final class Strips {
        static final int MAX = 8;
        private final double[] corners = new double[MAX * 12];
        private final float[] colour = new float[MAX * 3];
        private final int[] frame = new int[MAX];
        private final double[] scratch = new double[12];
        private int n;

        public void clear() { n = 0; }

        void add(Vector3f a, Vector3f b, float half, float r, float g, float bl, int fr) {
            if (n >= MAX || !FlameRules.strip(a.x, a.y, a.z, b.x, b.y, b.z, half, scratch)) return;
            System.arraycopy(scratch, 0, corners, n * 12, 12);
            colour[n * 3] = r;
            colour[n * 3 + 1] = g;
            colour[n * 3 + 2] = bl;
            frame[n] = fr;
            n++;
        }

        /** Pass 2: the textured strips (call after the glow pass of every nozzle). */
        public void draw(MultiBufferSource buffers) {
            if (n == 0) return;
            VertexConsumer vc = buffers.getBuffer(RenderType.eyes(TEXTURE));
            for (int i = 0; i < n; i++) {
                float u0 = frame[i] / (float) FlameRules.FRAMES, u1 = (frame[i] + 1) / (float) FlameRules.FRAMES;
                int r = (int) (255 * Math.min(1F, colour[i * 3])), g = (int) (255 * Math.min(1F, colour[i * 3 + 1]));
                int b = (int) (255 * Math.min(1F, colour[i * 3 + 2]));
                int o = i * 12;
                vertex(vc, corners, o, r, g, b, u0, 0);
                vertex(vc, corners, o + 3, r, g, b, u1, 0);
                vertex(vc, corners, o + 6, r, g, b, u1, 1);
                vertex(vc, corners, o + 9, r, g, b, u0, 1);
            }
            n = 0;
        }

        private static void vertex(VertexConsumer vc, double[] c, int o, int r, int g, int b, float u, float v) {
            vc.addVertex((float) c[o], (float) c[o + 1], (float) c[o + 2]).setColor(r, g, b, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    /**
     * Pass 1: disc and cone of one nozzle at the origin of {@code m}, the flame along +y. {@code power} sets the size and the
     * brightness (0..1), {@code throttle} the colour; {@code flicker} about 1; the strip goes into {@code strips} (may be null).
     */
    public static void glow(VertexConsumer vc, Matrix4f m, float radius, float length, float power, float throttle, float flicker,
                            float seconds, int seed, Strips strips) {
        int c = FlameRules.colour(throttle);
        float cr = ((c >> 16) & 255) / 255F, cg = ((c >> 8) & 255) / 255F, cb = (c & 255) / 255F;
        float p = power * flicker;
        // white-hot centre that takes a little of the flame colour
        float hr = 0.75F + 0.25F * cr, hg = 0.75F + 0.25F * cg, hb = 0.75F + 0.25F * cb;
        for (int s = 0; s < SEGMENTS; s++) {
            float x0 = (float) COS[s] * radius, z0 = (float) SIN[s] * radius;
            float x1 = (float) COS[s + 1] * radius, z1 = (float) SIN[s + 1] * radius;
            put(vc, m, 0, 0, 0, hr, hg, hb, 0.85F * p);
            put(vc, m, x0, 0, z0, cr, cg, cb, 0.35F * p);
            put(vc, m, x1, 0, z1, cr, cg, cb, 0.35F * p);
            put(vc, m, 0, 0, 0, hr, hg, hb, 0.85F * p);
            put(vc, m, x0 * 0.9F, 0.01F, z0 * 0.9F, cr, cg, cb, 0.55F * p);
            put(vc, m, x1 * 0.9F, 0.01F, z1 * 0.9F, cr, cg, cb, 0.55F * p);
            put(vc, m, x1 * 0.2F, length, z1 * 0.2F, cr * 0.5F, cg * 0.6F, cb, 0);
            put(vc, m, x0 * 0.2F, length, z0 * 0.2F, cr * 0.5F, cg * 0.6F, cb, 0);
        }
        if (strips != null && p > 0.03F) {
            Vector3f a = m.transformPosition(0, 0, 0, new Vector3f()), b = m.transformPosition(0, length * 1.15F, 0, new Vector3f());
            float k = Math.min(1F, 0.9F * p);
            strips.add(a, b, radius * 1.15F, cr * k, cg * k, cb * k, FlameRules.frame(seconds, seed));
        }
    }

    private static void put(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, float g, float b, float a) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, Math.max(0, Math.min(1, a)));
    }
}
