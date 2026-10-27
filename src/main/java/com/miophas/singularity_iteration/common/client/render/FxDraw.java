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
 *
 * <p>0.1.10.30 (P-4): the same geometry with far fewer allocations: the primitives work on doubles instead of building a
 * {@link Vec3} for every corner (one small frame array per ring, disc or cone remains), and the rings, discs and cones take
 * their sines and cosines from cached tables per segment count (the offline harness checks the vertices against the
 * 0.1.10.29 code and measures the allocations).
 */
public final class FxDraw {
    private FxDraw() {}

    /** cos / sin of i * 2 pi / n for i = 0..n, per segment count (index n), built on first use. */
    private static final double[][] COS = new double[257][], SIN = new double[257][];

    private static double[] cos(int n) {
        double[] c = n < COS.length ? COS[n] : null;
        if (c != null) return c;
        double[] cs = new double[n + 1], sn = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            double a = i * Math.PI * 2 / n;
            cs[i] = Math.cos(a);
            sn[i] = Math.sin(a);
        }
        if (n < COS.length) {
            SIN[n] = sn;
            COS[n] = cs;
        }
        return cs;
    }

    private static double[] sin(int n) {
        if (n < SIN.length && SIN[n] != null) return SIN[n];
        cos(n);
        if (n < SIN.length) return SIN[n];
        double[] sn = new double[n + 1];
        for (int i = 0; i <= n; i++) sn[i] = Math.sin(i * Math.PI * 2 / n);
        return sn;
    }

    public static void vertex(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, Mth.clamp(a, 0, 1));
    }

    private static void v(VertexConsumer vc, Matrix4f m, double x, double y, double z, float r, float g, float b, float a) {
        vc.addVertex(m, (float) x, (float) y, (float) z).setColor(r, g, b, Mth.clamp(a, 0, 1));
    }

    /** Camera-facing ribbon from {@code a} to {@code b}; alpha fades from {@code alphaA} to {@code alphaB}. */
    public static void beam(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 cam, float width, float r, float g, float bl,
                            float alphaA, float alphaB) {
        double ax = b.x - a.x, ay = b.y - a.y, az = b.z - a.z;
        if (ax * ax + ay * ay + az * az < 1.0E-8) return;
        double mx = (a.x + b.x) * 0.5 - cam.x, my = (a.y + b.y) * 0.5 - cam.y, mz = (a.z + b.z) * 0.5 - cam.z;
        double sx = ay * mz - az * my, sy = az * mx - ax * mz, sz = ax * my - ay * mx;
        if (sx * sx + sy * sy + sz * sz < 1.0E-8) {
            double[] p = perp(ax, ay, az);
            sx = p[0]; sy = p[1]; sz = p[2];
        }
        double k = width * 0.5 / Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx *= k; sy *= k; sz *= k;
        // a bright core and a soft halo three times as wide
        v(vc, m, a.x + sx, a.y + sy, a.z + sz, r, g, bl, alphaA);
        v(vc, m, a.x - sx, a.y - sy, a.z - sz, r, g, bl, alphaA);
        v(vc, m, b.x - sx, b.y - sy, b.z - sz, r, g, bl, alphaB);
        v(vc, m, b.x + sx, b.y + sy, b.z + sz, r, g, bl, alphaB);
        double hx = sx * 3, hy = sy * 3, hz = sz * 3;
        v(vc, m, a.x + hx, a.y + hy, a.z + hz, r, g, bl, 0);
        v(vc, m, a.x, a.y, a.z, r, g, bl, alphaA * 0.35F);
        v(vc, m, b.x, b.y, b.z, r, g, bl, alphaB * 0.35F);
        v(vc, m, b.x + hx, b.y + hy, b.z + hz, r, g, bl, 0);
        v(vc, m, a.x - hx, a.y - hy, a.z - hz, r, g, bl, 0);
        v(vc, m, a.x, a.y, a.z, r, g, bl, alphaA * 0.35F);
        v(vc, m, b.x, b.y, b.z, r, g, bl, alphaB * 0.35F);
        v(vc, m, b.x - hx, b.y - hy, b.z - hz, r, g, bl, 0);
    }

    /** Thin line (a beam without the halo). */
    public static void line(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 cam, float width, float r, float g, float bl, float alpha) {
        line(vc, m, a.x, a.y, a.z, b.x, b.y, b.z, cam, width, r, g, bl, alpha);
    }

    /** {@link #line} on plain coordinates (0.1.10.30). */
    public static void line(VertexConsumer vc, Matrix4f m, double ax0, double ay0, double az0, double bx0, double by0, double bz0, Vec3 cam,
                            float width, float r, float g, float bl, float alpha) {
        double ax = bx0 - ax0, ay = by0 - ay0, az = bz0 - az0;
        if (ax * ax + ay * ay + az * az < 1.0E-8) return;
        double mx = (ax0 + bx0) * 0.5 - cam.x, my = (ay0 + by0) * 0.5 - cam.y, mz = (az0 + bz0) * 0.5 - cam.z;
        double sx = ay * mz - az * my, sy = az * mx - ax * mz, sz = ax * my - ay * mx;
        if (sx * sx + sy * sy + sz * sz < 1.0E-8) {
            double[] p = perp(ax, ay, az);
            sx = p[0]; sy = p[1]; sz = p[2];
        }
        double k = width * 0.5 / Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx *= k; sy *= k; sz *= k;
        v(vc, m, ax0 + sx, ay0 + sy, az0 + sz, r, g, bl, alpha);
        v(vc, m, ax0 - sx, ay0 - sy, az0 - sz, r, g, bl, alpha);
        v(vc, m, bx0 - sx, by0 - sy, bz0 - sz, r, g, bl, alpha);
        v(vc, m, bx0 + sx, by0 + sy, bz0 + sz, r, g, bl, alpha);
    }

    /** Flat annulus around {@code normal}; the inner edge carries {@code alpha}, the outer edge fades out. */
    public static void ring(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 normal, float radius, float width,
                            float r, float g, float b, float alpha, int segments) {
        double[] f = frame(normal);
        double[] cs = cos(segments), sn = sin(segments);
        double ro = radius + width;
        for (int i = 0; i < segments; i++) {
            double d0x = f[0] * cs[i] + f[3] * sn[i], d0y = f[1] * cs[i] + f[4] * sn[i], d0z = f[2] * cs[i] + f[5] * sn[i];
            double d1x = f[0] * cs[i + 1] + f[3] * sn[i + 1], d1y = f[1] * cs[i + 1] + f[4] * sn[i + 1], d1z = f[2] * cs[i + 1] + f[5] * sn[i + 1];
            v(vc, m, c.x + d0x * radius, c.y + d0y * radius, c.z + d0z * radius, r, g, b, alpha);
            v(vc, m, c.x + d1x * radius, c.y + d1y * radius, c.z + d1z * radius, r, g, b, alpha);
            v(vc, m, c.x + d1x * ro, c.y + d1y * ro, c.z + d1z * ro, r, g, b, 0);
            v(vc, m, c.x + d0x * ro, c.y + d0y * ro, c.z + d0z * ro, r, g, b, 0);
        }
    }

    /** Filled disc, bright centre, transparent edge. */
    public static void disc(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 normal, float radius, float r, float g, float b,
                            float alphaCentre, int segments) {
        disc(vc, m, c, frame(normal), radius, r, g, b, alphaCentre, segments);
    }

    private static void disc(VertexConsumer vc, Matrix4f m, Vec3 c, double[] f, float radius, float r, float g, float b,
                             float alphaCentre, int segments) {
        double[] cs = cos(segments), sn = sin(segments);
        for (int i = 0; i < segments; i++) {
            v(vc, m, c.x, c.y, c.z, r, g, b, alphaCentre);
            v(vc, m, c.x + f[0] * cs[i] * radius + f[3] * sn[i] * radius, c.y + f[1] * cs[i] * radius + f[4] * sn[i] * radius,
                c.z + f[2] * cs[i] * radius + f[5] * sn[i] * radius, r, g, b, 0);
            v(vc, m, c.x + f[0] * cs[i + 1] * radius + f[3] * sn[i + 1] * radius, c.y + f[1] * cs[i + 1] * radius + f[4] * sn[i + 1] * radius,
                c.z + f[2] * cs[i + 1] * radius + f[5] * sn[i + 1] * radius, r, g, b, 0);
            v(vc, m, c.x, c.y, c.z, r, g, b, alphaCentre);
        }
    }

    /** Lens star: four spikes and a glow, facing the camera ({@code left}, {@code up}: camera axes). */
    public static void star(VertexConsumer vc, Matrix4f m, Vec3 c, Vector3f left, Vector3f up, float size,
                            float r, float g, float b, float alpha) {
        double lx = left.x(), ly = left.y(), lz = left.z(), ux = up.x(), uy = up.y(), uz = up.z();
        for (int s = 0; s < 2; s++) {
            double alx = s == 0 ? lx : ux, aly = s == 0 ? ly : uy, alz = s == 0 ? lz : uz;
            double acx = s == 0 ? ux : lx, acy = s == 0 ? uy : ly, acz = s == 0 ? uz : lz;
            double al = size, ac = size * 0.07;
            v(vc, m, c.x + alx * al, c.y + aly * al, c.z + alz * al, r, g, b, 0);
            v(vc, m, c.x + acx * ac, c.y + acy * ac, c.z + acz * ac, r, g, b, alpha);
            v(vc, m, c.x - alx * al, c.y - aly * al, c.z - alz * al, r, g, b, 0);
            v(vc, m, c.x - acx * ac, c.y - acy * ac, c.z - acz * ac, r, g, b, alpha);
        }
        double[] f = frame(ly * uz - lz * uy, lz * ux - lx * uz, lx * uy - ly * ux);
        disc(vc, m, c, f, size * 0.35F, r, g, b, alpha * 0.9F, 12);
        disc(vc, m, c, f, size * 0.12F, 1, 1, 1, alpha, 8);
    }

    /** Cone from {@code apex} along {@code dir}: bright base ring at the apex, fading towards the tip. */
    public static void cone(VertexConsumer vc, Matrix4f m, Vec3 apex, Vec3 dir, float length, float radius,
                            float r, float g, float b, float alphaBase, int segments) {
        if (dir.lengthSqr() < 1.0E-8) return;
        double[] f = frame(dir);
        double len = Math.sqrt(dir.x * dir.x + dir.y * dir.y + dir.z * dir.z);
        double tx = apex.x + dir.x / len * length, ty = apex.y + dir.y / len * length, tz = apex.z + dir.z / len * length;
        double[] cs = cos(segments), sn = sin(segments);
        for (int i = 0; i < segments; i++) {
            v(vc, m, apex.x + f[0] * cs[i] * radius + f[3] * sn[i] * radius, apex.y + f[1] * cs[i] * radius + f[4] * sn[i] * radius,
                apex.z + f[2] * cs[i] * radius + f[5] * sn[i] * radius, r, g, b, alphaBase);
            v(vc, m, apex.x + f[0] * cs[i + 1] * radius + f[3] * sn[i + 1] * radius, apex.y + f[1] * cs[i + 1] * radius + f[4] * sn[i + 1] * radius,
                apex.z + f[2] * cs[i + 1] * radius + f[5] * sn[i + 1] * radius, r, g, b, alphaBase);
            v(vc, m, tx, ty, tz, r, g, b, 0);
            v(vc, m, tx, ty, tz, r, g, b, 0);
        }
    }

    /**
     * Tangent frame of a normal: {t, s} as six doubles (t = {@link #perp} of the normal, s = n x t normalised), the same
     * axes as 0.1.10.29 without the vectors.
     */
    private static double[] frame(Vec3 normal) {
        return frame(normal.x, normal.y, normal.z);
    }

    private static double[] frame(double nx0, double ny0, double nz0) {
        double nl = Math.sqrt(nx0 * nx0 + ny0 * ny0 + nz0 * nz0);
        double nx = nx0 / nl, ny = ny0 / nl, nz = nz0 / nl;
        double[] t = perp(nx, ny, nz);
        double sx = ny * t[2] - nz * t[1], sy = nz * t[0] - nx * t[2], sz = nx * t[1] - ny * t[0];
        double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
        return new double[]{t[0], t[1], t[2], sx / sl, sy / sl, sz / sl};
    }

    /** A unit vector perpendicular to {@code n}. */
    public static Vec3 perp(Vec3 n) {
        double[] p = perp(n.x, n.y, n.z);
        return new Vec3(p[0], p[1], p[2]);
    }

    /** {@link #perp} on plain coordinates: {x, y, z}. */
    static double[] perp(double nx, double ny, double nz) {
        double ax, ay, az;
        if (Math.abs(ny) < 0.9) { ax = 0; ay = 1; az = 0; } else { ax = 1; ay = 0; az = 0; }
        double cx = ny * az - nz * ay, cy = nz * ax - nx * az, cz = nx * ay - ny * ax;
        double l = cx * cx + cy * cy + cz * cz;
        if (l < 1.0E-8) return new double[]{1, 0, 0};
        l = Math.sqrt(l);
        return new double[]{cx / l, cy / l, cz / l};
    }

    /** RGB 0..1 of a 0xRRGGBB colour. */
    public static float[] rgb(int colour) {
        return new float[]{((colour >> 16) & 255) / 255F, ((colour >> 8) & 255) / 255F, (colour & 255) / 255F};
    }
}
