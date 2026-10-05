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
import net.minecraft.world.level.material.MapColor;

/**
 * Tactical 3D holomap (0.1.7.34): a box-shaped hologram that holds a voxel copy of the block
 * volume around the wearer. The box turns with the wearer's heading (forward = into the box) and
 * is seen from behind and above with a slight perspective.
 *
 * <p>The volume ({@link #RADIUS} x {@link #BELOW}..{@link #ABOVE}) is sampled every
 * {@link #RESAMPLE_TICKS} ticks from the client's chunk data, never per frame. Only block faces
 * that touch air (or the box wall, as a cut section) are kept. Each frame the kept faces that look
 * at the viewer are projected, sorted back to front and drawn as translucent quads, so no depth
 * buffer or shader is needed.
 */
public final class HoloMap {
    private HoloMap() {}

    public static final int RADIUS = 12;
    public static final int BELOW = 7, ABOVE = 6;
    public static final int RESAMPLE_TICKS = 10;
    public static final int CONTACT_RANGE = 24;
    public static final int MAX_FACES = 7000;
    private static final int SIDE = RADIUS * 2 + 1;
    private static final int TALL = BELOW + ABOVE + 1;
    private static final float TILT = (float) Math.toRadians(34);   // camera elevation

    /** Face directions: +x, -x, +y, -y, +z, -z. */
    private static final int[][] DIR = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    /** Packed face list: x, y, z (relative to the centre), dir, colour, flags (1 = cut section). */
    private static int[] faces = new int[0];
    private static int faceCount;
    private static int centreX, centreY, centreZ;
    private static long lastSample = -1000;   // not Long.MIN_VALUE: now - MIN overflows
    private static long sampledAtMs;

    public record Contact(double dx, double dy, double dz, int color, boolean self) { }
    private static List<Contact> contacts = List.of();

    public static int faceCount() { return faceCount; }

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

    private static boolean solid(ClientLevel level, BlockPos.MutableBlockPos m) {
        BlockState s = level.getBlockState(m);
        return !s.isAir() && s.getFluidState().isEmpty() && s.isSolidRender(level, m);
    }

    private static void sample(ClientLevel level, BlockPos at) {
        centreX = at.getX();
        centreY = at.getY();
        centreZ = at.getZ();
        // occupancy grid with a one-cell border (border = outside the box)
        int sx = SIDE + 2, sy = TALL + 2;
        byte[] occ = new byte[sx * sy * sx];
        int[] col = new int[sx * sy * sx];
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int ix = 1; ix <= SIDE; ix++) {
            for (int iz = 1; iz <= SIDE; iz++) {
                int x = centreX + ix - 1 - RADIUS, z = centreZ + iz - 1 - RADIUS;
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                for (int iy = 1; iy <= TALL; iy++) {
                    int y = centreY + iy - 1 - BELOW;
                    m.set(x, y, z);
                    BlockState s = level.getBlockState(m);
                    if (s.isAir()) continue;
                    int idx = (iy * sx + iz) * sx + ix;
                    boolean water = !s.getFluidState().isEmpty();
                    occ[idx] = (byte) (water ? 2 : solid(level, m) ? 1 : 3);
                    MapColor mc = s.getMapColor(level, m);
                    col[idx] = mc == MapColor.NONE ? 0x6A7680 : mc.col;
                }
            }
        }
        int[] out = new int[MAX_FACES * 6];
        int n = 0;
        outer:
        for (int iy = 1; iy <= TALL; iy++) {
            for (int iz = 1; iz <= SIDE; iz++) {
                for (int ix = 1; ix <= SIDE; ix++) {
                    int idx = (iy * sx + iz) * sx + ix;
                    int o = occ[idx];
                    if (o == 0) continue;
                    for (int d = 0; d < 6; d++) {
                        int nx = ix + DIR[d][0], ny = iy + DIR[d][1], nz = iz + DIR[d][2];
                        boolean border = nx == 0 || ny == 0 || nz == 0 || nx == SIDE + 1 || ny == TALL + 1 || nz == SIDE + 1;
                        int no = occ[(ny * sx + nz) * sx + nx];
                        boolean exposed;
                        if (border) exposed = o == 1 && d != 2;   // cut section of solid ground (not the box top)
                        else if (o == 1) exposed = no != 1;
                        else exposed = no == 0 && (o != 2 || d == 2);   // water: surface only; plants etc: all open faces
                        if (!exposed) continue;
                        int k = n * 6;
                        out[k] = ix - 1 - RADIUS;
                        out[k + 1] = iy - 1 - BELOW;
                        out[k + 2] = iz - 1 - RADIUS;
                        out[k + 3] = d;
                        out[k + 4] = col[idx];
                        out[k + 5] = (border ? 1 : 0) | (o == 2 ? 2 : 0) | (o == 3 ? 4 : 0);
                        if (++n >= MAX_FACES) break outer;
                    }
                }
            }
        }
        faces = out;
        faceCount = n;
        sampledAtMs = System.currentTimeMillis();
    }

    // ------------------------------------------------------------------ projection

    private static float rightX, rightZ, fwdX, fwdZ, cosT, sinT, focal, dist, cx0, cy0;

    /** World offset (blocks, relative to the box centre) -> {screen x, screen y, depth}. */
    private static void project(float x, float y, float z, float[] out) {
        float u = x * rightX + z * rightZ;
        float w = x * fwdX + z * fwdZ;
        float up = w * sinT + y * cosT;
        float depth = dist + w * cosT - y * sinT;
        float s = focal / Math.max(1F, depth);
        out[0] = cx0 + u * s;
        out[1] = cy0 - up * s;
        out[2] = depth;
    }

    /** Draws the map with its centre at (cx, cy); {@code size} is the frame width in GUI px. */
    public static void render(GuiGraphics g, float cx, float cy, float size, float yawDeg, int primary, float partial) {
        long now = System.currentTimeMillis();
        float yaw = (float) Math.toRadians(yawDeg);
        // look direction (-sin, cos); right of it (-cos, -sin)
        fwdX = -Mth.sin(yaw); fwdZ = Mth.cos(yaw);
        rightX = -Mth.cos(yaw); rightZ = -Mth.sin(yaw);
        cosT = Mth.cos(TILT); sinT = Mth.sin(TILT);
        dist = RADIUS * 5.5F;
        float half = size / 2;
        float panelH = size * 0.92F;
        focal = dist * (size * 0.66F) / (SIDE * 1.18F);
        cx0 = cx;
        cy0 = cy + panelH * 0.06F;

        // panel
        FcsDraw.panel(g, (int) (cx - half), (int) (cy - panelH / 2), (int) size, (int) panelH,
            String.format(java.util.Locale.ROOT, "HOLO  %dx%dx%d", SIDE, TALL, SIDE), primary);

        float[] a = new float[3], b = new float[3], c = new float[3], d = new float[3];
        float x0 = -RADIUS, x1 = RADIUS + 1, y0 = -BELOW, y1 = ABOVE + 1, z0 = -RADIUS, z1 = RADIUS + 1;
        // floor grid of the box
        int grid = FcsDraw.argb(primary, 0.13F);
        for (int k = 0; k <= SIDE; k += 5) {
            project(x0 + k, y0, z0, a); project(x0 + k, y0, z1, b);
            FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.5F, grid);
            project(x0, y0, z0 + k, a); project(x1, y0, z0 + k, b);
            FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.5F, grid);
        }
        // back edges of the box (behind the voxels)
        boxEdges(g, primary, false);

        // voxel faces: collect visible ones, sort back to front
        int n = faceCount;
        int[] f = faces;
        float[] depth = new float[n];
        int[] vis = new int[n];
        int nv = 0;
        for (int i = 0; i < n; i++) {
            int k = i * 6;
            int dir = f[k + 3];
            float fx = f[k] + 0.5F + DIR[dir][0] * 0.5F, fy = f[k + 1] + 0.5F + DIR[dir][1] * 0.5F, fz = f[k + 2] + 0.5F + DIR[dir][2] * 0.5F;
            // camera-space position and normal; visible when the normal points at the camera
            float u = fx * rightX + fz * rightZ, w = fx * fwdX + fz * fwdZ;
            float px = u, py = w * sinT + fy * cosT, pz = dist + w * cosT - fy * sinT;
            float nu = DIR[dir][0] * rightX + DIR[dir][2] * rightZ, nw = DIR[dir][0] * fwdX + DIR[dir][2] * fwdZ;
            float qy = nw * sinT + DIR[dir][1] * cosT, qz = nw * cosT - DIR[dir][1] * sinT;
            // camera looks along +z from the origin; the screen y axis is up here
            if (nu * px + qy * py + qz * pz >= 0) continue;
            depth[nv] = pz;
            vis[nv++] = i;
        }
        Integer[] order = new Integer[nv];
        for (int i = 0; i < nv; i++) order[i] = i;
        final float[] dd = depth;
        java.util.Arrays.sort(order, (p, q) -> Float.compare(dd[q], dd[p]));
        float flicker = 0.92F + 0.08F * (float) Math.sin(now / 47.0) * (float) Math.sin(now / 131.0);
        float scan = (now % 2600) / 2600F;   // scan band position, bottom -> top
        for (int oi = 0; oi < nv; oi++) {
            int i = vis[order[oi]];
            int k = i * 6;
            int x = f[k], y = f[k + 1], z = f[k + 2], dir = f[k + 3], rgb = f[k + 4], flags = f[k + 5];
            corners(x, y, z, dir, a, b, c, d);
            float shade = switch (dir) { case 2 -> 1.0F; case 3 -> 0.45F; case 0, 1 -> 0.72F; default -> 0.82F; };
            float hRel = (y + BELOW) / (float) TALL;
            float band = Math.max(0, 1 - Math.abs(hRel - scan) * 9);
            boolean cut = (flags & 1) != 0, water = (flags & 2) != 0, plant = (flags & 4) != 0;
            int base = mix(rgb, primary, water ? 0.25F : cut ? 0.45F : 0.28F);
            base = scale(base, (cut ? 0.62F : 1F) * shade + band * 0.5F);
            float alpha = (cut ? 0.26F : water ? 0.40F : plant ? 0.45F : 0.58F) * flicker;
            // player layer: a touch brighter so the wearer's floor reads at a glance
            if (y == -1 && dir == 2) alpha = Math.min(0.85F, alpha + 0.12F);
            FcsDraw.quad(g, a[0], a[1], b[0], b[1], c[0], c[1], d[0], d[1], FcsDraw.argb(base, alpha));
            if (!cut && dir != 2 && dir != 3) {
                // vertical steps (walls, cliffs, block edges) get a bright outline: the structure reads at a glance
                int edge = FcsDraw.argb(mix(primary, 0xFFFFFF, 0.35F), 0.42F * flicker);
                FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.5F, edge);
                FcsDraw.line(g, b[0], b[1], c[0], c[1], 0.5F, edge);
                FcsDraw.line(g, c[0], c[1], d[0], d[1], 0.5F, edge);
                FcsDraw.line(g, d[0], d[1], a[0], a[1], 0.5F, edge);
            } else if (!cut && dir == 2 && (x + z & 3) == 0) {
                // sparse wire on top faces for the hologram texture
                FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.4F, FcsDraw.argb(primary, 0.22F));
            }
        }
        g.flush();

        // slice plane at the wearer's feet
        project(x0, 0, z0, a); project(x1, 0, z0, b); project(x1, 0, z1, c); project(x0, 0, z1, d);
        int slice = FcsDraw.argb(primary, 0.35F);
        FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.6F, slice);
        FcsDraw.line(g, b[0], b[1], c[0], c[1], 0.6F, slice);
        FcsDraw.line(g, c[0], c[1], d[0], d[1], 0.6F, slice);
        FcsDraw.line(g, d[0], d[1], a[0], a[1], 0.6F, slice);

        // contacts: stalk down to the floor of the box, diamond head
        for (Contact ct : contacts) {
            float ux = (float) ct.dx, uy = (float) Mth.clamp(ct.dy, -BELOW, ABOVE + 1), uz = (float) ct.dz;
            boolean edge = Math.abs(ux) > RADIUS + 0.5F || Math.abs(uz) > RADIUS + 0.5F;
            ux = Mth.clamp(ux, -RADIUS - 0.5F, RADIUS + 1.5F);
            uz = Mth.clamp(uz, -RADIUS - 0.5F, RADIUS + 1.5F);
            project(ux + 0.5F, uy, uz + 0.5F, a);
            project(ux + 0.5F, y0, uz + 0.5F, b);
            FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.6F, FcsDraw.argb(ct.color, 0.45F));
            FcsDraw.disc(g, b[0], b[1], 0.9F, FcsDraw.argb(ct.color, 0.6F));
            float r = edge ? 1.6F : 2.4F;
            FcsDraw.quad(g, a[0], a[1] - r - 2, a[0] + r, a[1] - 2, a[0], a[1] + r - 2, a[0] - r, a[1] - 2, FcsDraw.argb(ct.color, edge ? 0.7F : 1F));
        }
        // self: chevron at the centre, pointing forward (into the box)
        project(0.5F, 0, 0.5F, a);
        project(0.5F + fwdX * 1.6F, 0, 0.5F + fwdZ * 1.6F, b);
        project(0.5F - fwdX * 0.9F + rightX * 1.1F, 0, 0.5F - fwdZ * 0.9F + rightZ * 1.1F, c);
        project(0.5F - fwdX * 0.9F - rightX * 1.1F, 0, 0.5F - fwdZ * 0.9F - rightZ * 1.1F, d);
        FcsDraw.quad(g, b[0], b[1], c[0], c[1], a[0], a[1], d[0], d[1], FcsDraw.argb(FcsDraw.WHITE, 0.95F));
        project(0.5F, 0, 0.5F, a);
        project(0.5F, 1.8F, 0.5F, b);
        FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.8F, FcsDraw.argb(FcsDraw.WHITE, 0.7F));

        // front edges of the box, corner marks and a rising scan plane
        boxEdges(g, primary, true);
        float sy = y0 + (y1 - y0) * scan;
        project(x0, sy, z0, a); project(x1, sy, z0, b); project(x1, sy, z1, c); project(x0, sy, z1, d);
        int sc = FcsDraw.argb(primary, 0.22F);
        FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.6F, sc);
        FcsDraw.line(g, b[0], b[1], c[0], c[1], 0.6F, sc);
        FcsDraw.line(g, c[0], c[1], d[0], d[1], 0.6F, sc);
        FcsDraw.line(g, d[0], d[1], a[0], a[1], 0.6F, sc);

        // north label on the floor edge
        project(0.5F, y0, z0 - 1.5F, a);
        FcsDraw.textCentered(g, "N", a[0], a[1] - 3, FcsDraw.argb(primary, 1F), 0.6F);
        // read-outs
        float ty = cy + panelH / 2 - 8;
        FcsDraw.text(g, String.format(java.util.Locale.ROOT, "Y%+d", centreY), cx - half + 5, ty, FcsDraw.argb(primary, 0.9F), 0.5F);
        FcsDraw.textRight(g, String.format(java.util.Locale.ROOT, "%d CT", contacts.size()), cx + half - 4, ty, FcsDraw.argb(primary, 0.9F), 0.5F);
        long age = now - sampledAtMs;
        if (age < 250) FcsDraw.textRight(g, "SCAN", cx + half - 4, cy - panelH / 2 + 2, FcsDraw.argb(0x050A0C, 0.9F), 0.5F);
    }

    /** The 4 corners of a face, ordered around the face. */
    private static void corners(int x, int y, int z, int dir, float[] a, float[] b, float[] c, float[] d) {
        switch (dir) {
            case 0 -> { project(x + 1, y, z, a); project(x + 1, y + 1, z, b); project(x + 1, y + 1, z + 1, c); project(x + 1, y, z + 1, d); }
            case 1 -> { project(x, y, z, a); project(x, y, z + 1, b); project(x, y + 1, z + 1, c); project(x, y + 1, z, d); }
            case 2 -> { project(x, y + 1, z, a); project(x, y + 1, z + 1, b); project(x + 1, y + 1, z + 1, c); project(x + 1, y + 1, z, d); }
            case 3 -> { project(x, y, z, a); project(x + 1, y, z, b); project(x + 1, y, z + 1, c); project(x, y, z + 1, d); }
            case 4 -> { project(x, y, z + 1, a); project(x + 1, y, z + 1, b); project(x + 1, y + 1, z + 1, c); project(x, y + 1, z + 1, d); }
            default -> { project(x, y, z, a); project(x, y + 1, z, b); project(x + 1, y + 1, z, c); project(x + 1, y, z, d); }
        }
    }

    /**
     * The 12 edges of the hologram box. {@code front} selects the edges that touch the corner
     * nearest the camera (drawn last, over the voxels); the others are drawn first.
     */
    private static void boxEdges(GuiGraphics g, int primary, boolean front) {
        float x0 = -RADIUS, x1 = RADIUS + 1, y0 = -BELOW, y1 = ABOVE + 1, z0 = -RADIUS, z1 = RADIUS + 1;
        float[][] p = new float[8][3];
        float[] xs = {x0, x1}, ys = {y0, y1}, zs = {z0, z1};
        int nearest = 0;
        float best = Float.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            project(xs[i & 1], ys[(i >> 1) & 1], zs[(i >> 2) & 1], p[i]);
            if (p[i][2] < best) { best = p[i][2]; nearest = i; }
        }
        int col = FcsDraw.argb(primary, front ? 0.85F : 0.35F);
        for (int i = 0; i < 8; i++) {
            for (int bit = 1; bit < 8; bit <<= 1) {
                int j = i | bit;
                if (j == i) continue;
                // an edge is "front" when it contains the corner nearest the camera, or lies on the top face
                boolean isFront = i == nearest || j == nearest || (((i >> 1) & 1) == 1 && ((j >> 1) & 1) == 1);
                if (isFront != front) continue;
                FcsDraw.line(g, p[i][0], p[i][1], p[j][0], p[j][1], front ? 1.1F : 0.7F, col);
            }
        }
        if (front) {
            for (int i = 0; i < 8; i++) FcsDraw.disc(g, p[i][0], p[i][1], 1.1F, FcsDraw.argb(primary, 0.95F));
        }
    }

    private static int scale(int rgb, float f) {
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
}
