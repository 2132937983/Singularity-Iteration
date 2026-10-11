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
 * Tactical 3D holomap (0.1.7.34). It is a box-shaped hologram with a voxel copy of the blocks
 * around the wearer. The box turns with the heading of the wearer (forward = into the box). The
 * viewer sees it from behind and above, with a small perspective.
 *
 * <p><b>Sampling.</b> The volume is {@link #SIDE} x {@link #TALL} x {@link #SIDE} blocks
 * (25 x 14 x 25). It goes from {@code -RADIUS} to {@code +RADIUS} in x and z, and from
 * {@code -BELOW} to {@code +ABOVE} in y, around the block of the player. The map reads it from the
 * client chunk data every {@link #RESAMPLE_TICKS} ticks, never per frame. The map keeps only the
 * block faces that touch air. It also keeps the solid faces on the box wall, as a cut section.
 *
 * <p><b>Coordinate frames.</b>
 * <ul>
 *   <li>Box frame: block offsets from the sample centre (the block position of the player at
 *       the last sample). Block (x, y, z) fills the unit cube from (x, y, z) to (x + 1, y + 1, z + 1).
 *       The box spans -RADIUS .. RADIUS + 1 in x and z.</li>
 *   <li>Camera frame: (u, up, depth). u is to the right of the heading. up is up on the screen.
 *       depth is the distance from the virtual camera.</li>
 *   <li>Screen: GUI pixels. Screen y is down.</li>
 * </ul>
 *
 * <p><b>Drawing.</b> In each frame, the map projects the kept faces that look at the viewer.
 * It sorts them from back to front (painter's algorithm) and draws them as translucent quads. Thus
 * it needs no depth buffer and no shader.
 *
 * <p><b>Memory bounds.</b> The face list holds at most {@link #MAX_FACES} faces (6 ints each,
 * 168 KB). Each sample allocates the occupancy and colour grids (27 x 16 x 27 cells, about 58 KB)
 * and a new face list. The contact list holds at most 48 entries.
 *
 * <p><b>Thread.</b> Call all methods on the client thread only.
 */
public final class HoloMap {
    private HoloMap() {}

    /** Half width of the box in blocks, in x and z. */
    public static final int RADIUS = 12;
    /** Box layers below the block of the player (BELOW) and above it (ABOVE). */
    public static final int BELOW = 7, ABOVE = 6;
    /** Ticks between two samples of the volume (2 samples per second). */
    public static final int RESAMPLE_TICKS = 10;
    /** Half size of the contact query box, in blocks. */
    public static final int CONTACT_RANGE = 24;
    /**
     * Maximum number of kept faces. The sample loop goes from the bottom layer to the top layer.
     * Thus when the limit applies, the top layers lose their faces first.
     */
    public static final int MAX_FACES = 7000;
    private static final int SIDE = RADIUS * 2 + 1;
    private static final int TALL = BELOW + ABOVE + 1;
    /** Camera elevation above the horizontal, 34 degrees. */
    private static final float TILT = (float) Math.toRadians(34);   // camera elevation

    /** Face directions: +x, -x, +y, -y, +z, -z. The index is the "dir" value of a face. */
    private static final int[][] DIR = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    /**
     * Packed face list, 6 ints per face: x, y, z (box frame), dir (index into {@link #DIR}),
     * colour (0xRRGGBB), flags. Flags: 1 = cut section on the box wall, 2 = fluid surface,
     * 4 = non-solid block (plants, glass, slabs and similar). Only the first {@link #faceCount}
     * faces are valid.
     */
    private static int[] faces = new int[0];
    private static int faceCount;
    /** Sample centre in world block coordinates. */
    private static int centreX, centreY, centreZ;
    private static long lastSample = -1000;   // not Long.MIN_VALUE: now - MIN overflows
    /** Wall-clock time of the last sample, in milliseconds. The "SCAN" read-out uses it. */
    private static long sampledAtMs;

    /**
     * One creature on the map.
     *
     * @param dx    x offset from the player position, in blocks.
     * @param dy    y offset from the player position, in blocks.
     * @param dz    z offset from the player position, in blocks.
     * @param color the marker colour, 0xRRGGBB.
     * @param self  true for the wearer. {@link #tick} always sets false.
     */
    public record Contact(double dx, double dy, double dz, int color, boolean self) { }
    private static List<Contact> contacts = List.of();

    /** Number of valid faces in the last sample. */
    public static int faceCount() { return faceCount; }

    /**
     * Client tick while the holomap unit is on. It samples the volume when
     * {@link #RESAMPLE_TICKS} ticks have passed. It also samples when the game time goes back,
     * for example in a new world. It updates the contacts on each tick.
     *
     * <p>Contact colours: red for threat level 2, yellow for threat level 1. Otherwise the colour
     * shows the class: hostile, player, neutral or passive.
     *
     * @param level  the client level. It must not be null.
     * @param player the local player.
     */
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

    /** True for a full opaque block with no fluid. */
    private static boolean solid(ClientLevel level, BlockPos.MutableBlockPos m) {
        BlockState s = level.getBlockState(m);
        return !s.isAir() && s.getFluidState().isEmpty() && s.isSolidRender(level, m);
    }

    /**
     * Reads the volume around {@code at} and builds the face list.
     *
     * <p>Pass 1 fills an occupancy grid. Codes: 0 = air or chunk not loaded, 1 = solid,
     * 2 = fluid (water, lava, or a waterlogged block), 3 = other non-air block.
     *
     * <p>Pass 2 tests the 6 faces of each occupied cell. A face is kept when:
     * <ul>
     *   <li>the neighbour is on the border (outside the box), the cell is solid, and the face is
     *       not the top face. This makes the cut section on the sides and the bottom.</li>
     *   <li>the cell is solid and the neighbour is not solid.</li>
     *   <li>the cell is fluid, the neighbour is air, and the face is the top face (fluid surface).</li>
     *   <li>the cell is "other" and the neighbour is air.</li>
     * </ul>
     *
     * @param at the new sample centre, in world block coordinates.
     */
    private static void sample(ClientLevel level, BlockPos at) {
        centreX = at.getX();
        centreY = at.getY();
        centreZ = at.getZ();
        // occupancy grid with a one-cell border (border = outside the box). Index: (iy * sx + iz) * sx + ix.
        // Grid index 1 .. SIDE (or TALL) is inside the box. Index 0 and SIDE + 1 are the border.
        int sx = SIDE + 2, sy = TALL + 2;
        byte[] occ = new byte[sx * sy * sx];
        int[] col = new int[sx * sy * sx];
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int ix = 1; ix <= SIDE; ix++) {
            for (int iz = 1; iz <= SIDE; iz++) {
                int x = centreX + ix - 1 - RADIUS, z = centreZ + iz - 1 - RADIUS;
                // a column in a chunk that is not loaded stays air
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
                        else exposed = no == 0 && (o != 2 || d == 2);   // fluid: top surface only. Plants and similar: all faces to air.
                        if (!exposed) continue;
                        int k = n * 6;
                        // grid index -> box frame: subtract the border cell and the half size
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

    /**
     * Projection state of the frame. {@link #render} sets it before the first call to
     * {@link #project}. rightX/rightZ and fwdX/fwdZ: unit heading vectors in the box frame.
     * cosT/sinT: cosine and sine of {@link #TILT}. focal: focal length in GUI pixels.
     * dist: camera distance from the box centre, in blocks. cx0/cy0: screen point of the box
     * centre, in GUI pixels.
     */
    private static float rightX, rightZ, fwdX, fwdZ, cosT, sinT, focal, dist, cx0, cy0;

    /**
     * Projects a box-frame point to the screen.
     *
     * <p>Math:
     * <ol>
     *   <li>Yaw: {@code u = p . right} and {@code w = p . forward}, in the horizontal plane.
     *       Thus the heading of the wearer points into the box.</li>
     *   <li>Tilt: a rotation of {@link #TILT} around the right axis.
     *       {@code up = w * sin(T) + y * cos(T)} and {@code depth = dist + w * cos(T) - y * sin(T)}.
     *       The camera is behind the box and above it. It looks down at 34 degrees. Points far
     *       forward are higher on the screen and further away.</li>
     *   <li>Perspective: {@code s = focal / max(1, depth)}. The minimum depth of 1 block prevents a
     *       divide by zero. The box is far from the camera ({@code dist} = 66 blocks), so the
     *       perspective is weak.</li>
     *   <li>Screen: {@code x = cx0 + u * s} and {@code y = cy0 - up * s}. The minus sign flips the
     *       axis, because GUI y is down.</li>
     * </ol>
     *
     * @param out receives {screen x, screen y, depth}. Depth is in blocks.
     */
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

    /**
     * Draws the map. Call it on the render thread in the HUD pass.
     *
     * <p>Draw order, from back to front: panel, floor grid, back box edges, voxel faces, slice
     * plane, contacts, self marker, front box edges, scan plane, labels.
     *
     * <p>Contacts use the live player position. The voxels use the sample centre, which can be up
     * to {@link #RESAMPLE_TICKS} ticks old.
     *
     * @param cx      the x of the map centre, in GUI pixels.
     * @param cy      the y of the map centre, in GUI pixels.
     * @param size    the frame width in GUI pixels.
     * @param yawDeg  the view yaw of the wearer in degrees (Minecraft convention).
     * @param primary the HUD colour, 0xRRGGBB.
     * @param partial the partial tick. The method does not use it.
     */
    public static void render(GuiGraphics g, float cx, float cy, float size, float yawDeg, int primary, float partial) {
        long now = System.currentTimeMillis();
        float yaw = (float) Math.toRadians(yawDeg);
        // look direction (-sin, cos). Right of it: (-cos, -sin). Minecraft yaw 0 looks to +Z.
        fwdX = -Mth.sin(yaw); fwdZ = Mth.cos(yaw);
        rightX = -Mth.cos(yaw); rightZ = -Mth.sin(yaw);
        cosT = Mth.cos(TILT); sinT = Mth.sin(TILT);
        dist = RADIUS * 5.5F;
        float half = size / 2;
        float panelH = size * 0.92F;
        // at depth dist, a width of SIDE * 1.18 blocks fills 66 % of the frame width
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
            // (fx, fy, fz): centre of the face in the box frame
            // camera-space position P and normal N. The face is visible when N points at the camera.
            float u = fx * rightX + fz * rightZ, w = fx * fwdX + fz * fwdZ;
            float px = u, py = w * sinT + fy * cosT, pz = dist + w * cosT - fy * sinT;
            // the normal gets the same rotation as the position, but no dist offset
            float nu = DIR[dir][0] * rightX + DIR[dir][2] * rightZ, nw = DIR[dir][0] * fwdX + DIR[dir][2] * fwdZ;
            float qy = nw * sinT + DIR[dir][1] * cosT, qz = nw * cosT - DIR[dir][1] * sinT;
            // back-face test: the camera is at the camera-space origin and looks along +z. The
            // vector from the face to the camera is -P. The face is visible when N . (-P) > 0,
            // thus the method skips the face when N . P >= 0. Screen y is up in this frame.
            if (nu * px + qy * py + qz * pz >= 0) continue;
            depth[nv] = pz;
            vis[nv++] = i;
        }
        // painter's sort: far faces first (descending depth of the face centre). The sort key is one
        // point per face. Thus two faces that overlap can be in the wrong order at some angles.
        Integer[] order = new Integer[nv];
        for (int i = 0; i < nv; i++) order[i] = i;
        final float[] dd = depth;
        java.util.Arrays.sort(order, (p, q) -> Float.compare(dd[q], dd[p]));
        // hologram flicker: alpha multiplier 0.84 .. 1, from two sine waves with no common period
        float flicker = 0.92F + 0.08F * (float) Math.sin(now / 47.0) * (float) Math.sin(now / 131.0);
        float scan = (now % 2600) / 2600F;   // scan band position, bottom -> top, one cycle in 2.6 s
        for (int oi = 0; oi < nv; oi++) {
            int i = vis[order[oi]];
            int k = i * 6;
            int x = f[k], y = f[k + 1], z = f[k + 2], dir = f[k + 3], rgb = f[k + 4], flags = f[k + 5];
            corners(x, y, z, dir, a, b, c, d);
            // fixed light: top faces full, bottom faces dark, x faces darker than z faces
            float shade = switch (dir) { case 2 -> 1.0F; case 3 -> 0.45F; case 0, 1 -> 0.72F; default -> 0.82F; };
            float hRel = (y + BELOW) / (float) TALL;
            float band = Math.max(0, 1 - Math.abs(hRel - scan) * 9);
            boolean cut = (flags & 1) != 0, water = (flags & 2) != 0, plant = (flags & 4) != 0;
            int base = mix(rgb, primary, water ? 0.25F : cut ? 0.45F : 0.28F);
            base = scale(base, (cut ? 0.62F : 1F) * shade + band * 0.5F);
            float alpha = (cut ? 0.26F : water ? 0.40F : plant ? 0.45F : 0.58F) * flicker;
            // player layer: the top faces of layer y = -1 (the floor under the wearer) get 0.12 more
            // alpha. Thus the wearer sees the floor level quickly.
            if (y == -1 && dir == 2) alpha = Math.min(0.85F, alpha + 0.12F);
            FcsDraw.quad(g, a[0], a[1], b[0], b[1], c[0], c[1], d[0], d[1], FcsDraw.argb(base, alpha));
            if (!cut && dir != 2 && dir != 3) {
                // vertical faces (walls, cliffs, block edges) get a bright outline. Thus the shape of the terrain is clear.
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

        // contacts: a stalk down to the floor of the box, and a diamond head. A contact out of the
        // box is clamped to the box wall and drawn smaller (edge).
        // The offsets are from the player position. The +0.5 below assumes that the player stands at
        // the centre of the sample centre block.
        for (Contact ct : contacts) {
            float ux = (float) ct.dx, uy = (float) Mth.clamp(ct.dy, -BELOW, ABOVE + 1), uz = (float) ct.dz;
            boolean edge = Math.abs(ux) > RADIUS + 0.5F || Math.abs(uz) > RADIUS + 0.5F;
            // Drawn x = ux + 0.5 must stay in the box (-RADIUS .. RADIUS + 1), so ux stays in
            // -RADIUS - 0.5 .. RADIUS + 0.5. Before 0.1.7.36 the upper bound was RADIUS + 1.5, and a
            // contact on the +x or +z side was drawn one block outside the box.
            ux = Mth.clamp(ux, -RADIUS - 0.5F, RADIUS + 0.5F);
            uz = Mth.clamp(uz, -RADIUS - 0.5F, RADIUS + 0.5F);
            project(ux + 0.5F, uy, uz + 0.5F, a);
            project(ux + 0.5F, y0, uz + 0.5F, b);
            FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.6F, FcsDraw.argb(ct.color, 0.45F));
            FcsDraw.disc(g, b[0], b[1], 0.9F, FcsDraw.argb(ct.color, 0.6F));
            float r = edge ? 1.6F : 2.4F;
            FcsDraw.quad(g, a[0], a[1] - r - 2, a[0] + r, a[1] - 2, a[0], a[1] + r - 2, a[0] - r, a[1] - 2, FcsDraw.argb(ct.color, edge ? 0.7F : 1F));
        }
        // self: a chevron at the centre of the centre block, pointing forward (into the box), and a
        // 1.8-block vertical line for the body height
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
        // "SCAN" shows for 250 ms after each sample
        if (age < 250) FcsDraw.textRight(g, "SCAN", cx + half - 4, cy - panelH / 2 + 2, FcsDraw.argb(0x050A0C, 0.9F), 0.5F);
    }

    /**
     * Projects the 4 corners of a face, in order around the face.
     *
     * @param x   the block x in the box frame.
     * @param y   the block y in the box frame.
     * @param z   the block z in the box frame.
     * @param dir the face direction, an index into {@link #DIR}.
     */
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
     * Draws the 12 edges of the hologram box. Front edges are the edges that touch the corner
     * nearest to the camera, and the 4 edges of the top face. The map draws the front edges last,
     * over the voxels. It draws the other edges first. Corner index i: bit 0 selects x, bit 1
     * selects y, bit 2 selects z. Each edge connects corner i to corner {@code i | bit}, so the loop
     * draws each edge one time.
     *
     * @param front true to draw the front edges and the corner marks, false to draw the back edges.
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

    /** Multiplies each channel of 0xRRGGBB by {@code f}. Each channel stops at 255. */
    private static int scale(int rgb, float f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * f)), g = Math.min(255, (int) (((rgb >> 8) & 255) * f)),
            b = Math.min(255, (int) ((rgb & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    /** Linear blend of two 0xRRGGBB colours. t = 0 gives a, t = 1 gives b. */
    private static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
