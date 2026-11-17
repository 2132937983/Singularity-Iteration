// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 0.1.11.62 (hit feel and constructivist FX phase Q1, decisions three B / four B): the constructivist effect vocabulary.
 * Few shapes, flat colour, hard edges, strong diagonals: the thrust <b>wedge</b> (a long triangle out of a nozzle, a black
 * edge, a red body, a white core), the speed <b>bar</b> (a long straight strip along the flight) and the shock <b>ring</b>
 * (a flat annulus that grows and is cut). The palette is red, white and black. Shapes are drawn in world space (the pose
 * is translated by minus the camera) into {@link SiRenderTypes#FLAT}; the wedges and bars face the camera about their axis.
 * Time steps (a two-state beat) instead of noise: the thrust jumps between two lengths.
 */
public final class ConstructFx {
    private ConstructFx() {}

    public static final float[] RED = {0.84F, 0.15F, 0.12F}, WHITE = {0.97F, 0.96F, 0.93F}, BLACK = {0.07F, 0.07F, 0.08F};

    /** One camera-facing triangle: the base across {@code base} ({@code width} wide), the tip {@code length} along {@code dir}. */
    public static void wedge(VertexConsumer vc, Matrix4f m, Vec3 base, Vec3 dir, float length, float width, Vec3 cam, float[] rgb, float alpha) {
        if (length <= 0 || width <= 0 || alpha <= 0 || dir.lengthSqr() < 1.0E-8) return;
        Vec3 d = dir.normalize();
        Vec3 side = d.cross(cam.subtract(base));
        if (side.lengthSqr() < 1.0E-8) side = FxDraw.perp(d);
        side = side.normalize().scale(width / 2);
        Vec3 tip = base.add(d.scale(length));
        FxDraw.vertex(vc, m, base.add(side), rgb[0], rgb[1], rgb[2], alpha);
        FxDraw.vertex(vc, m, base.subtract(side), rgb[0], rgb[1], rgb[2], alpha);
        FxDraw.vertex(vc, m, tip, rgb[0], rgb[1], rgb[2], alpha);
        FxDraw.vertex(vc, m, tip, rgb[0], rgb[1], rgb[2], alpha);
    }

    /** A thrust wedge: a black edge, the red body, a white core (shorter and narrower). */
    public static void thrust(VertexConsumer vc, Matrix4f m, Vec3 base, Vec3 dir, float length, float width, Vec3 cam, float alpha) {
        wedge(vc, m, base, dir, length * 1.05F + 0.03F, width * 1.16F, cam, BLACK, alpha);   // a thin hard edge, not a black shape
        wedge(vc, m, base, dir, length, width, cam, RED, alpha);
        wedge(vc, m, base, dir, length * 0.5F, width * 0.45F, cam, WHITE, alpha);
    }

    /** A camera-facing straight strip from {@code a} to {@code b}. */
    public static void bar(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, float width, Vec3 cam, float[] rgb, float alpha) {
        Vec3 d = b.subtract(a);
        if (d.lengthSqr() < 1.0E-8 || alpha <= 0) return;
        Vec3 side = d.cross(cam.subtract(a));
        if (side.lengthSqr() < 1.0E-8) side = FxDraw.perp(d.normalize());
        side = side.normalize().scale(width / 2);
        FxDraw.vertex(vc, m, a.add(side), rgb[0], rgb[1], rgb[2], alpha);
        FxDraw.vertex(vc, m, a.subtract(side), rgb[0], rgb[1], rgb[2], alpha);
        FxDraw.vertex(vc, m, b.subtract(side), rgb[0], rgb[1], rgb[2], alpha);
        FxDraw.vertex(vc, m, b.add(side), rgb[0], rgb[1], rgb[2], alpha);
    }

    /** A flat annulus about {@code centre} in the plane normal to {@code normal}. */
    public static void ring(VertexConsumer vc, Matrix4f m, Vec3 centre, Vec3 normal, float radius, float width, float[] rgb, float alpha, int segments) {
        if (radius <= 0 || alpha <= 0) return;
        Vec3 n = normal.lengthSqr() < 1.0E-8 ? new Vec3(0, 1, 0) : normal.normalize();
        Vec3 u = FxDraw.perp(n).normalize(), v = n.cross(u).normalize();
        float r0 = Math.max(0, radius - width / 2), r1 = radius + width / 2;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments, a1 = Math.PI * 2 * (i + 1) / segments;
            Vec3 d0 = u.scale(Math.cos(a0)).add(v.scale(Math.sin(a0))), d1 = u.scale(Math.cos(a1)).add(v.scale(Math.sin(a1)));
            FxDraw.vertex(vc, m, centre.add(d0.scale(r0)), rgb[0], rgb[1], rgb[2], alpha);
            FxDraw.vertex(vc, m, centre.add(d0.scale(r1)), rgb[0], rgb[1], rgb[2], alpha);
            FxDraw.vertex(vc, m, centre.add(d1.scale(r1)), rgb[0], rgb[1], rgb[2], alpha);
            FxDraw.vertex(vc, m, centre.add(d1.scale(r0)), rgb[0], rgb[1], rgb[2], alpha);
        }
    }
}
