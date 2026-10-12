// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.area;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * Hologram grid of a work area (0.1.8.0): block grid lines on the six faces and a scan ring
 * that moves up the box. The grid step grows with the box size, so a box draws at most about
 * {@link #MAX_LINES} lines (a 3 x 3 chunk loader area gets a 16-block grid).
 */
@OnlyIn(Dist.CLIENT)
final class HoloGrid {
    private HoloGrid() {}

    static final int MAX_LINES = 1500;

    /**
     * @param t animation phase 0..1 (repeats)
     */
    static void draw(PoseStack pose, VertexConsumer vc, AABB b, float r, float g, float bl, float alpha, float t) {
        double sx = b.getXsize(), sy = b.getYsize(), sz = b.getZsize();
        int step = 1;
        while (step < 64 && lineCount(sx, sy, sz, step) > MAX_LINES) step *= 2;
        Matrix4f m = pose.last().pose();
        int a = (int) (Math.max(0, Math.min(1, alpha)) * 255);
        int ri = (int) (r * 255), gi = (int) (g * 255), bi = (int) (bl * 255);
        // vertical faces: horizontal rings every step blocks, vertical lines every step blocks
        for (double y = b.minY + step; y < b.maxY - 1.0E-3; y += step) ring(vc, pose, m, b, y, ri, gi, bi, a);
        for (double x = b.minX + step; x < b.maxX - 1.0E-3; x += step) {
            line(vc, pose, m, x, b.minY, b.minZ, x, b.maxY, b.minZ, ri, gi, bi, a);
            line(vc, pose, m, x, b.minY, b.maxZ, x, b.maxY, b.maxZ, ri, gi, bi, a);
            line(vc, pose, m, x, b.minY, b.minZ, x, b.minY, b.maxZ, ri, gi, bi, a);
            line(vc, pose, m, x, b.maxY, b.minZ, x, b.maxY, b.maxZ, ri, gi, bi, a);
        }
        for (double z = b.minZ + step; z < b.maxZ - 1.0E-3; z += step) {
            line(vc, pose, m, b.minX, b.minY, z, b.minX, b.maxY, z, ri, gi, bi, a);
            line(vc, pose, m, b.maxX, b.minY, z, b.maxX, b.maxY, z, ri, gi, bi, a);
            line(vc, pose, m, b.minX, b.minY, z, b.maxX, b.minY, z, ri, gi, bi, a);
            line(vc, pose, m, b.minX, b.maxY, z, b.maxX, b.maxY, z, ri, gi, bi, a);
        }
        // scan ring: bright, moves from the bottom to the top in one animation period
        double scanY = b.minY + sy * t;
        ring(vc, pose, m, b, scanY, Math.min(255, ri + 60), Math.min(255, gi + 60), Math.min(255, bi + 60), Math.min(255, a * 3 + 40));
    }

    private static long lineCount(double sx, double sy, double sz, int step) {
        return (long) (sy / step) * 4 + (long) (sx / step) * 4 + (long) (sz / step) * 4;
    }

    private static void ring(VertexConsumer vc, PoseStack pose, Matrix4f m, AABB b, double y, int r, int g, int bl, int a) {
        line(vc, pose, m, b.minX, y, b.minZ, b.maxX, y, b.minZ, r, g, bl, a);
        line(vc, pose, m, b.maxX, y, b.minZ, b.maxX, y, b.maxZ, r, g, bl, a);
        line(vc, pose, m, b.maxX, y, b.maxZ, b.minX, y, b.maxZ, r, g, bl, a);
        line(vc, pose, m, b.minX, y, b.maxZ, b.minX, y, b.minZ, r, g, bl, a);
    }

    private static void line(VertexConsumer vc, PoseStack pose, Matrix4f m, double x0, double y0, double z0, double x1, double y1, double z1,
                             int r, int g, int b, int a) {
        float nx = (float) (x1 - x0), ny = (float) (y1 - y0), nz = (float) (z1 - z0);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-4F) return;
        nx /= len; ny /= len; nz /= len;
        vc.addVertex(m, (float) x0, (float) y0, (float) z0).setColor(r, g, b, a).setNormal(pose.last(), nx, ny, nz);
        vc.addVertex(m, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a).setNormal(pose.last(), nx, ny, nz);
    }
}
