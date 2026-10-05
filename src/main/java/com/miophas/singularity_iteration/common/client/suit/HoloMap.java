// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.suit.SuitSensorData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Tactical 3D holomap: a tilted terrain relief of the area around the wearer that turns with
 * the wearer's heading (forward = up), with the wearer and nearby contacts on it.
 *
 * <p>The terrain grid is sampled every {@link #RESAMPLE_TICKS} ticks from the client's chunk
 * data (heightmap + map colour), never per frame. Drawing is a painter's-order list of flat
 * quads, so it needs no depth buffer and no shader.
 */
public final class HoloMap {
    private HoloMap() {}

    public static final int RADIUS = 20;
    public static final int RESAMPLE_TICKS = 10;
    public static final int CONTACT_RANGE = 32;
    private static final int SIDE = RADIUS * 2 + 1;
    private static final float TILT = (float) Math.toRadians(58);

    private static final int[] HEIGHT = new int[SIDE * SIDE];
    private static final int[] COLOR = new int[SIDE * SIDE];
    private static int centreX, centreY, centreZ;
    private static long lastSample = -1000;   // not Long.MIN_VALUE: now - MIN overflows

    public record Contact(double dx, double dy, double dz, int color, boolean self) { }
    private static List<Contact> contacts = List.of();

    public static void tick(ClientLevel level, LocalPlayer player) {
        long now = level.getGameTime();
        if (now - lastSample >= RESAMPLE_TICKS || now < lastSample) {
            lastSample = now;
            sample(level, player.blockPosition());
        }
        List<Contact> list = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(CONTACT_RANGE),
                e -> e != player && e.isAlive() && !e.isInvisible())) {
            int threat = SuitSensorData.threatLevel(e.getId());
            int color = threat == 2 ? FcsDraw.RED : threat == 1 ? FcsDraw.YELLOW
                : e instanceof Enemy ? 0xFF6A5C : e instanceof Player ? FcsDraw.CYAN : e instanceof NeutralMob ? 0xFFE08A : 0x9FE8B0;
            list.add(new Contact(e.getX() - player.getX(), e.getY() - player.getY(), e.getZ() - player.getZ(), color, false));
            if (list.size() >= 48) break;
        }
        contacts = list;
    }

    private static void sample(ClientLevel level, BlockPos at) {
        centreX = at.getX();
        centreY = at.getY();
        centreZ = at.getZ();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int i = 0; i < SIDE; i++) {
            for (int j = 0; j < SIDE; j++) {
                int x = centreX + i - RADIUS, z = centreZ + j - RADIUS;
                int idx = j * SIDE + i;
                if (!level.hasChunk(x >> 4, z >> 4)) { HEIGHT[idx] = Integer.MIN_VALUE; continue; }
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                // underground: show the cave floor/roof band around the wearer instead of the surface
                if (top > centreY + 10) top = caveSurface(level, m, x, z, centreY);
                HEIGHT[idx] = top;
                BlockState state = level.getBlockState(m.set(x, top, z));
                MapColor mc = state.getMapColor(level, m);
                COLOR[idx] = mc == MapColor.NONE ? 0x404850 : mc.col;
            }
        }
    }

    private static int caveSurface(ClientLevel level, BlockPos.MutableBlockPos m, int x, int z, int y) {
        for (int dy = 0; dy <= 12; dy++) {
            int yy = y - dy;
            if (!level.getBlockState(m.set(x, yy, z)).isAir() && level.getBlockState(m.set(x, yy + 1, z)).isAir()) return yy;
        }
        return y - 1;
    }

    /** Draws the map with its centre at (cx, cy); {@code size} is the frame width in GUI px. */
    public static void render(GuiGraphics g, float cx, float cy, float size, float yawDeg, int primary, float partial) {
        float cell = size / (SIDE * 1.15F);
        float yaw = (float) Math.toRadians(yawDeg);
        float cos = Mth.cos(yaw), sin = Mth.sin(yaw);
        float cosT = Mth.cos(TILT), sinT = Mth.sin(TILT);
        float hScale = cell * 0.55F;

        // frame
        float half = size / 2;
        g.fill((int) (cx - half), (int) (cy - half * 0.72F), (int) (cx + half), (int) (cy + half * 0.78F), 0x5A050A0C);
        FcsDraw.brackets(g, cx - half, cy - half * 0.72F, size, half * 1.5F, 8, 1.2F, FcsDraw.argb(primary, 0.95F));
        // range rings on the ground plane
        for (int ring = 1; ring <= 2; ring++) {
            float r = RADIUS * ring / 2F * cell;
            ellipse(g, cx, cy, r, r * cosT, FcsDraw.argb(primary, 0.28F));
        }

        // terrain: painter's order (far rows first). Screen depth grows with rotated v.
        int n = SIDE * SIDE;
        float[] depth = new float[n];
        Integer[] order = new Integer[n];
        for (int idx = 0; idx < n; idx++) {
            int i = idx % SIDE - RADIUS, j = idx / SIDE - RADIUS;
            depth[idx] = rotV(i + 0.5F, j + 0.5F, cos, sin);
            order[idx] = idx;
        }
        java.util.Arrays.sort(order, (a, b) -> Float.compare(depth[a], depth[b]));
        for (int idx : order) {
            int h = HEIGHT[idx];
            if (h == Integer.MIN_VALUE) continue;
            int i = idx % SIDE - RADIUS, j = idx / SIDE - RADIUS;
            if (i * i + j * j > RADIUS * RADIUS) continue;
            float rel = Mth.clamp(h - centreY, -12, 12);
            float lift = rel * hScale * sinT;
            float[] p00 = proj(i, j, cos, sin, cell, cosT), p10 = proj(i + 1, j, cos, sin, cell, cosT);
            float[] p11 = proj(i + 1, j + 1, cos, sin, cell, cosT), p01 = proj(i, j + 1, cos, sin, cell, cosT);
            int base = shade(COLOR[idx], rel);
            float fade = 1F - (float) Math.sqrt(i * i + j * j) / (RADIUS + 1F) * 0.55F;
            int top = FcsDraw.argb(mix(base, primary, 0.35F), 0.80F * fade);
            FcsDraw.quad(g, cx + p00[0], cy + p00[1] - lift, cx + p10[0], cy + p10[1] - lift,
                cx + p11[0], cy + p11[1] - lift, cx + p01[0], cy + p01[1] - lift, top);
            // skirt towards the viewer where the neighbour in front is lower
            int front = frontNeighbour(idx, cos, sin);
            if (front >= 0 && HEIGHT[front] != Integer.MIN_VALUE) {
                float relF = Mth.clamp(HEIGHT[front] - centreY, -12, 12);
                if (relF < rel) {
                    float drop = (rel - relF) * hScale * sinT;
                    float[][] edge = frontEdge(p00, p10, p11, p01);
                    int side = FcsDraw.argb(mix(shade(COLOR[idx], rel - 4), primary, 0.45F), 0.75F * fade);
                    FcsDraw.quad(g, cx + edge[0][0], cy + edge[0][1] - lift, cx + edge[1][0], cy + edge[1][1] - lift,
                        cx + edge[1][0], cy + edge[1][1] - lift + drop, cx + edge[0][0], cy + edge[0][1] - lift + drop, side);
                }
            }
        }
        g.flush();

        // contacts: stalk from the ground plane up to the contact height, diamond head
        for (Contact c : contacts) {
            float u = (float) (c.dx * cos + c.dz * sin) * -1F;
            float v = rotV((float) c.dx, (float) c.dz, cos, sin);
            float dist = (float) Math.sqrt(u * u + v * v);
            boolean edge = dist > RADIUS;
            if (edge) { u = u / dist * RADIUS * 1.06F; v = v / dist * RADIUS * 1.06F; }
            float sx = cx + u * cell, sy = cy + v * cell * cosT;
            float lift = edge ? 0 : (float) Mth.clamp(c.dy, -12, 12) * hScale * sinT;
            if (!edge) FcsDraw.line(g, sx, sy, sx, sy - lift - 2, 0.8F, FcsDraw.argb(c.color, 0.6F));
            diamond(g, sx, sy - lift - 2, edge ? 1.5F : 2.2F, FcsDraw.argb(c.color, edge ? 0.7F : 1F));
        }
        // self: heading chevron at the centre
        FcsDraw.triangle(g, cx, cy - 4.5F, cx - 3.2F, cy + 3, cx + 3.2F, cy + 3, FcsDraw.argb(FcsDraw.WHITE, 0.95F));
        // north marker on the outer ring
        float u = (float) (0 * cos + (-1) * sin) * -1F;
        float v = rotV(0, -1, cos, sin);
        float len = (float) Math.sqrt(u * u + v * v);
        float r = RADIUS * cell * 1.02F;
        float mx = cx + u / len * r, my = cy + v / len * r * cosT;
        FcsDraw.textCentered(g, "N", mx, my - 3, FcsDraw.argb(primary, 1F), 0.6F);
        FcsDraw.text(g, "HOLO R" + RADIUS, cx - half + 3, cy - half * 0.72F + 2, FcsDraw.argb(primary, 0.9F), 0.55F);
        FcsDraw.textRight(g, String.format(java.util.Locale.ROOT, "Y%d", centreY), cx + half - 3, cy - half * 0.72F + 2,
            FcsDraw.argb(primary, 0.9F), 0.55F);
    }

    /** Rotated "depth" (screen down) of a ground point: forward (look direction) maps to up. */
    private static float rotV(float x, float z, float cos, float sin) {
        // look direction in world: (-sin(yaw), cos(yaw)); points ahead get negative v (up on screen)
        return -(-x * sin + z * cos);
    }

    private static float[] proj(float i, float j, float cos, float sin, float cell, float cosT) {
        float u = -(i * cos + j * sin);
        float v = rotV(i, j, cos, sin);
        return new float[]{u * cell, v * cell * cosT};
    }

    /** Neighbour cell index that lies closest to the viewer (largest v). */
    private static int frontNeighbour(int idx, float cos, float sin) {
        int i = idx % SIDE, j = idx / SIDE;
        int bestI = i, bestJ = j;
        float best = -Float.MAX_VALUE;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            float v = rotV(d[0], d[1], cos, sin);
            if (v > best) { best = v; bestI = i + d[0]; bestJ = j + d[1]; }
        }
        if (bestI < 0 || bestJ < 0 || bestI >= SIDE || bestJ >= SIDE) return -1;
        return bestJ * SIDE + bestI;
    }

    /** The two corners of the cell edge that faces the viewer. */
    private static float[][] frontEdge(float[] a, float[] b, float[] c, float[] d) {
        float[][] pts = {a, b, c, d};
        int i1 = 0, i2 = 1;
        for (int i = 0; i < 4; i++) if (pts[i][1] > pts[i1][1]) i1 = i;
        i2 = i1 == 0 ? 1 : 0;
        for (int i = 0; i < 4; i++) if (i != i1 && pts[i][1] > pts[i2][1]) i2 = i;
        return new float[][]{pts[i1], pts[i2]};
    }

    private static int shade(int rgb, float rel) {
        float f = Mth.clamp(0.75F + rel / 24F, 0.4F, 1.15F);
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * f)), g = Math.min(255, (int) (((rgb >> 8) & 255) * f)),
            b = Math.min(255, (int) ((rgb & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    private static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static void ellipse(GuiGraphics g, float cx, float cy, float rx, float ry, int argb) {
        float px = 0, py = 0;
        for (int k = 0; k <= 48; k++) {
            float a = (float) (k * Math.PI * 2 / 48);
            float x = cx + Mth.sin(a) * rx, y = cy - Mth.cos(a) * ry;
            if (k > 0) FcsDraw.line(g, px, py, x, y, 0.7F, argb);
            px = x;
            py = y;
        }
    }

    private static void diamond(GuiGraphics g, float x, float y, float r, int argb) {
        FcsDraw.quad(g, x, y - r, x + r, y, x, y + r, x - r, y, argb);
    }
}
