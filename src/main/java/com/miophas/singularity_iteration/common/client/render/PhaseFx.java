// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.suit.PhaseRules;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 0.1.11.70 (phase warp style phase W0): the light shapes of the phase warp flight, drawn additively into
 * {@link SiRenderTypes#FIELD_GLOW} (the dark day bed into {@link SiRenderTypes#FLAT}). Shapes and colours come from
 * {@link PhaseRules}.
 * <ul>
 *   <li>{@link #cone}: in a nozzle frame along +y: an outer shell of light, a white needle inside and the rings of
 *       compressed space sliding out.</li>
 *   <li>{@link #blob}: a round glow facing the camera (the nozzle flare, the blink of an attitude jet).</li>
 *   <li>{@link #ring}: a thin glowing ring in a plane.</li>
 *   <li>{@link #underlay}: the soft dark bed under the light by day.</li>
 * </ul>
 * Entity layers and world passes alike draw with camera-relative coordinates (no camera rotation in the pose), so a
 * shape that faces the camera faces the origin.
 */
public final class PhaseFx {
    private PhaseFx() {}

    private static final int SEG = 10, SLICES = 6;
    private static final float[] COS = new float[SEG + 1], SIN = new float[SEG + 1];

    static {
        for (int i = 0; i <= SEG; i++) { COS[i] = (float) Math.cos(i * Math.PI * 2 / SEG); SIN[i] = (float) Math.sin(i * Math.PI * 2 / SEG); }
    }

    /**
     * A phase cone in the frame {@code m} along +y from the origin: {@code length} long, {@code width} across at the
     * nozzle, the rings sliding at {@code rate} per second, {@code hot} (0..1) turning the middle to violet, all times
     * {@code alpha}.
     */
    public static void cone(VertexConsumer vc, Matrix4f m, float length, float width, float seconds, float rate, float hot, float alpha) {
        if (length <= 0 || width <= 0 || alpha <= 0.01F) return;
        float half = width / 2;
        shell(vc, m, length, half, hot, alpha * 0.8F);
        shell(vc, m, length * 0.4F, half * 0.24F, 0F, alpha * 0.6F);   // the white needle
        for (int k = 0; k < PhaseRules.RINGS; k++) {
            float ph = PhaseRules.ring(seconds, rate, k), y = length * (0.08F + 0.8F * ph);
            float[] shape = PhaseRules.ringShape(ph), c = PhaseRules.colour(0.18F + 0.5F * ph, hot);
            float r = half * PhaseRules.radius(0.08F + 0.8F * ph) * shape[0] * 1.15F;
            // faint: seen down the exhaust a stack of bright rings read as a face
            ringAt(vc, m, y, r, Math.max(0.006F, width * 0.05F), c, shape[1] * alpha * 0.45F);
        }
    }

    private static void shell(VertexConsumer vc, Matrix4f m, float length, float half, float hot, float alpha) {
        for (int j = 0; j < SLICES; j++) {
            float f0 = (float) j / SLICES, f1 = (float) (j + 1) / SLICES;
            float y0 = f0 * length, y1 = f1 * length, r0 = half * PhaseRules.radius(f0), r1 = half * PhaseRules.radius(f1);
            float[] c0 = PhaseRules.colour(f0, hot), c1 = PhaseRules.colour(f1, hot);
            for (int s = 0; s < SEG; s++) {
                put(vc, m, COS[s] * r0, y0, SIN[s] * r0, c0, alpha);
                put(vc, m, COS[s + 1] * r0, y0, SIN[s + 1] * r0, c0, alpha);
                put(vc, m, COS[s + 1] * r1, y1, SIN[s + 1] * r1, c1, alpha);
                put(vc, m, COS[s] * r1, y1, SIN[s] * r1, c1, alpha);
            }
        }
    }

    /** A ring across +y at {@code y} in the frame {@code m}. */
    public static void ringAt(VertexConsumer vc, Matrix4f m, float y, float radius, float width, float[] rgb, float alpha) {
        if (radius <= 0 || alpha <= 0.01F) return;
        float r0 = Math.max(0, radius - width / 2), r1 = radius + width / 2;
        float[] core = PhaseRules.mix(rgb, PhaseRules.WHITE, 0.45F);
        for (int s = 0; s < SEG * 2; s++) {
            float a0 = (float) (s * Math.PI / SEG), a1 = (float) ((s + 1) * Math.PI / SEG);
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            put(vc, m, c0 * r0, y, s0 * r0, core, alpha);
            put(vc, m, c0 * r1, y, s0 * r1, core, alpha);
            put(vc, m, c1 * r1, y, s1 * r1, core, alpha);
            put(vc, m, c1 * r0, y, s1 * r0, core, alpha);
        }
    }

    /** A glowing ring about {@code centre} (camera-relative) in the plane normal to {@code normal}. */
    public static void ring(VertexConsumer vc, Vec3 centre, Vec3 normal, float radius, float width, float[] rgb, float alpha) {
        if (radius <= 0 || alpha <= 0.01F) return;
        Vec3 n = normal.lengthSqr() < 1.0E-8 ? new Vec3(0, 1, 0) : normal.normalize();
        Vec3 u = FxDraw.perp(n).normalize(), v = n.cross(u).normalize();
        Matrix4f m = new Matrix4f().translate((float) centre.x, (float) centre.y, (float) centre.z)
            .mul(new Matrix4f().set(new float[]{(float) u.x, (float) u.y, (float) u.z, 0, (float) n.x, (float) n.y, (float) n.z, 0,
                (float) v.x, (float) v.y, (float) v.z, 0, 0, 0, 0, 1}));
        // a soft halo and the bright ring
        ringAt(vc, m, 0, radius, width * 3, rgb, alpha * 0.25F);
        ringAt(vc, m, 0, radius, width, rgb, alpha);
    }

    /** A round glow at {@code centre} (camera-relative), facing the camera: white in the middle, {@code rgb} out to nothing. */
    public static void blob(VertexConsumer vc, Vec3 centre, float radius, float[] rgb, float alpha) {
        if (radius <= 0 || alpha <= 0.01F) return;
        Vec3 view = centre.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : centre.normalize();
        Vec3 u = FxDraw.perp(view).normalize().scale(radius), v = view.cross(u).normalize().scale(radius);
        Matrix4f id = new Matrix4f();
        float[] core = PhaseRules.WHITE;
        for (int s = 0; s < SEG; s++) {
            Vec3 d0 = u.scale(COS[s]).add(v.scale(SIN[s])), d1 = u.scale(COS[s + 1]).add(v.scale(SIN[s + 1]));
            // the white heart to a quarter, then the colour out to the rim
            Vec3 i0 = centre.add(d0.scale(0.25)), i1 = centre.add(d1.scale(0.25));
            FxDraw.vertex(vc, id, centre, core[0], core[1], core[2], alpha);
            FxDraw.vertex(vc, id, i0, rgb[0], rgb[1], rgb[2], alpha * 0.8F);
            FxDraw.vertex(vc, id, i1, rgb[0], rgb[1], rgb[2], alpha * 0.8F);
            FxDraw.vertex(vc, id, centre, core[0], core[1], core[2], alpha);
            FxDraw.vertex(vc, id, i0, rgb[0], rgb[1], rgb[2], alpha * 0.8F);
            FxDraw.vertex(vc, id, centre.add(d0), rgb[0], rgb[1], rgb[2], 0F);
            FxDraw.vertex(vc, id, centre.add(d1), rgb[0], rgb[1], rgb[2], 0F);
            FxDraw.vertex(vc, id, i1, rgb[0], rgb[1], rgb[2], alpha * 0.8F);
        }
    }

    /** The dark bed under the light by day at {@code centre} (camera-relative), into a translucent buffer; none at night. */
    public static void underlay(VertexConsumer flat, Vec3 centre, float radius, float strength) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        float a = PhaseRules.underlay(level.getSkyDarken(1F)) * Math.max(0F, Math.min(1F, strength));
        if (a <= 0.01F) return;
        Vec3 view = centre.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : centre.normalize();
        Vec3 u = FxDraw.perp(view).normalize().scale(radius), v = view.cross(u).normalize().scale(radius);
        Matrix4f id = new Matrix4f();
        for (int s = 0; s < SEG; s++) {
            Vec3 d0 = u.scale(COS[s]).add(v.scale(SIN[s])), d1 = u.scale(COS[s + 1]).add(v.scale(SIN[s + 1]));
            FxDraw.vertex(flat, id, centre, 0.09F, 0.05F, 0.24F, a);
            FxDraw.vertex(flat, id, centre.add(d0), 0.09F, 0.05F, 0.24F, 0F);
            FxDraw.vertex(flat, id, centre.add(d1), 0.09F, 0.05F, 0.24F, 0F);
            FxDraw.vertex(flat, id, centre, 0.09F, 0.05F, 0.24F, a);
        }
    }

    private static void put(VertexConsumer vc, Matrix4f m, float x, float y, float z, float[] c, float alpha) {
        vc.addVertex(m, x, y, z).setColor(c[0], c[1], c[2], Math.max(0F, Math.min(1F, (c.length > 3 ? c[3] : 1F) * alpha)));
    }
}
