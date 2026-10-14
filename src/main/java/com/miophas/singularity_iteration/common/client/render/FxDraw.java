package com.miophas.singularity_iteration.common.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Additive effect primitives in world space (0.1.9.1), for {@link SiRenderTypes#FIELD_GLOW} and
 * {@link SiRenderTypes#ENERGY_BEAM}: the pose has been translated by minus the camera position,
 * so the points are plain world coordinates. Colours are 0..1; alpha scales the additive light.
 */
public final class FxDraw {
    private FxDraw() {}

    public static void vertex(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, Mth.clamp(a, 0, 1));
    }

    /** Camera-facing ribbon from {@code a} to {@code b}; alpha fades from {@code alphaA} to {@code alphaB}. */
    public static void beam(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 cam, float width, float r, float g, float bl,
                            float alphaA, float alphaB) {
        Vec3 axis = b.subtract(a);
        if (axis.lengthSqr() < 1.0E-8) return;
        Vec3 mid = a.add(b).scale(0.5);
        Vec3 side = axis.cross(mid.subtract(cam));
        if (side.lengthSqr() < 1.0E-8) side = perp(axis);
        side = side.normalize().scale(width * 0.5);
        // a bright core and a soft halo three times as wide
        vertex(vc, m, a.add(side), r, g, bl, alphaA);
        vertex(vc, m, a.subtract(side), r, g, bl, alphaA);
        vertex(vc, m, b.subtract(side), r, g, bl, alphaB);
        vertex(vc, m, b.add(side), r, g, bl, alphaB);
        Vec3 halo = side.scale(3);
        vertex(vc, m, a.add(halo), r, g, bl, 0);
        vertex(vc, m, a, r, g, bl, alphaA * 0.35F);
        vertex(vc, m, b, r, g, bl, alphaB * 0.35F);
        vertex(vc, m, b.add(halo), r, g, bl, 0);
        vertex(vc, m, a.subtract(halo), r, g, bl, 0);
        vertex(vc, m, a, r, g, bl, alphaA * 0.35F);
        vertex(vc, m, b, r, g, bl, alphaB * 0.35F);
        vertex(vc, m, b.subtract(halo), r, g, bl, 0);
    }

    /** Thin line (a beam without the halo). */
    public static void line(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 cam, float width, float r, float g, float bl, float alpha) {
        Vec3 axis = b.subtract(a);
        if (axis.lengthSqr() < 1.0E-8) return;
        Vec3 side = axis.cross(a.add(b).scale(0.5).subtract(cam));
        if (side.lengthSqr() < 1.0E-8) side = perp(axis);
        side = side.normalize().scale(width * 0.5);
        vertex(vc, m, a.add(side), r, g, bl, alpha);
        vertex(vc, m, a.subtract(side), r, g, bl, alpha);
        vertex(vc, m, b.subtract(side), r, g, bl, alpha);
        vertex(vc, m, b.add(side), r, g, bl, alpha);
    }

    /** Flat annulus around {@code normal}; the inner edge carries {@code alpha}, the outer edge fades out. */
    public static void ring(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 normal, float radius, float width,
                            float r, float g, float b, float alpha, int segments) {
        Vec3 n = normal.normalize(), t = perp(n), s = n.cross(t).normalize();
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2 / segments, a1 = (i + 1) * Math.PI * 2 / segments;
            Vec3 d0 = t.scale(Math.cos(a0)).add(s.scale(Math.sin(a0))), d1 = t.scale(Math.cos(a1)).add(s.scale(Math.sin(a1)));
            vertex(vc, m, c.add(d0.scale(radius)), r, g, b, alpha);
            vertex(vc, m, c.add(d1.scale(radius)), r, g, b, alpha);
            vertex(vc, m, c.add(d1.scale(radius + width)), r, g, b, 0);
            vertex(vc, m, c.add(d0.scale(radius + width)), r, g, b, 0);
        }
    }

    /** Filled disc, bright centre, transparent edge. */
    public static void disc(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 normal, float radius, float r, float g, float b,
                            float alphaCentre, int segments) {
        Vec3 n = normal.normalize(), t = perp(n), s = n.cross(t).normalize();
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2 / segments, a1 = (i + 1) * Math.PI * 2 / segments;
            vertex(vc, m, c, r, g, b, alphaCentre);
            vertex(vc, m, c.add(t.scale(Math.cos(a0) * radius)).add(s.scale(Math.sin(a0) * radius)), r, g, b, 0);
            vertex(vc, m, c.add(t.scale(Math.cos(a1) * radius)).add(s.scale(Math.sin(a1) * radius)), r, g, b, 0);
            vertex(vc, m, c, r, g, b, alphaCentre);
        }
    }

    /** Lens star: four spikes and a glow, facing the camera ({@code left}, {@code up}: camera axes). */
    public static void star(VertexConsumer vc, Matrix4f m, Vec3 c, Vector3f left, Vector3f up, float size,
                            float r, float g, float b, float alpha) {
        Vec3 l = new Vec3(left.x(), left.y(), left.z()), u = new Vec3(up.x(), up.y(), up.z());
        Vec3[][] spikes = {{l, u}, {u, l}};
        for (Vec3[] sp : spikes) {
            Vec3 along = sp[0].scale(size), across = sp[1].scale(size * 0.07);
            vertex(vc, m, c.add(along), r, g, b, 0);
            vertex(vc, m, c.add(across), r, g, b, alpha);
            vertex(vc, m, c.subtract(along), r, g, b, 0);
            vertex(vc, m, c.subtract(across), r, g, b, alpha);
        }
        Vec3 n = l.cross(u);
        disc(vc, m, c, n, size * 0.35F, r, g, b, alpha * 0.9F, 12);
        disc(vc, m, c, n, size * 0.12F, 1, 1, 1, alpha, 8);
    }

    /** Cone from {@code apex} along {@code dir}: bright base ring at the apex, fading towards the tip. */
    public static void cone(VertexConsumer vc, Matrix4f m, Vec3 apex, Vec3 dir, float length, float radius,
                            float r, float g, float b, float alphaBase, int segments) {
        if (dir.lengthSqr() < 1.0E-8) return;
        Vec3 n = dir.normalize(), t = perp(n), s = n.cross(t).normalize();
        Vec3 tip = apex.add(n.scale(length));
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2 / segments, a1 = (i + 1) * Math.PI * 2 / segments;
            Vec3 p0 = apex.add(t.scale(Math.cos(a0) * radius)).add(s.scale(Math.sin(a0) * radius));
            Vec3 p1 = apex.add(t.scale(Math.cos(a1) * radius)).add(s.scale(Math.sin(a1) * radius));
            vertex(vc, m, p0, r, g, b, alphaBase);
            vertex(vc, m, p1, r, g, b, alphaBase);
            vertex(vc, m, tip, r, g, b, 0);
            vertex(vc, m, tip, r, g, b, 0);
        }
    }

    /** A unit vector perpendicular to {@code n}. */
    public static Vec3 perp(Vec3 n) {
        Vec3 a = Math.abs(n.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 c = n.cross(a);
        return c.lengthSqr() < 1.0E-8 ? new Vec3(1, 0, 0) : c.normalize();
    }

    /** RGB 0..1 of a 0xRRGGBB colour. */
    public static float[] rgb(int colour) {
        return new float[]{((colour >> 16) & 255) / 255F, ((colour >> 8) & 255) / 255F, (colour & 255) / 255F};
    }
}
