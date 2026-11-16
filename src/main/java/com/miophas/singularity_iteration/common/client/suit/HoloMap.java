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
 * <p><b>Memory bounds.</b> The face list holds at most {@link #MAX_FACES} faces (8 ints each since 0.1.11.5,
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
    /** Camera elevation above the horizontal: 42 degrees since 0.1.11.5 (34 before), closer to a floor plan. */
    private static final float TILT = (float) Math.toRadians(42);   // camera elevation
    /** 0.1.11.5 (H3): the detail view's zoom and camera elevation, set with the mouse wheel while the detail key is held. */
    private static float detailZoom = 1F, detailTiltDeg = 42F;
    public static final float MIN_ZOOM = 0.6F, MAX_ZOOM = 2.4F, MIN_TILT = 20F, MAX_TILT = 80F;
    /** 0.1.11.5 (H3): the heading the map shows, eased toward the view heading so the box does not jitter. */
    private static float shownYaw;
    private static long lastFrameMs;
    private static boolean yawKnown;
    /** 0.1.11.5 (H4): how long the small map stays after the last hostile or sonar result, in milliseconds. */
    public static final long LINGER_MS = 4000;
    /** 0.1.11.5 (H4): wall-clock time of the last reason to show the small map (a hostile, a sonar result). */
    private static long lastReasonMs = Long.MIN_VALUE / 2;
    /** 0.1.11.5 (H4): how long a sonar result keeps the small map up, in milliseconds. */
    private static final long SONAR_SHOW_MS = 6000;

    /** Face directions: +x, -x, +y, -y, +z, -z. The index is the "dir" value of a face. */
    private static final int[][] DIR = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    /**
     * Packed face list, {@link #STRIDE} ints per face: x, y, z (box frame, the lowest block of a merged face), dir (index
     * into {@link #DIR}),
     * colour (0xRRGGBB), flags. Flags: 1 = cut section on the box wall, 2 = fluid surface,
     * 4 = non-solid block (plants, glass, slabs and similar), 8 = lava (0.1.11.5), 16 = the top of a ledge with a drop
     * of 4 or more blocks beside it (0.1.11.5), then (0.1.11.5) the width and height of a merged face in blocks (see
     * {@link #merge}). Only the first {@link #faceCount} faces are valid.
     */
    private static int[] faces = new int[0];
    /** Ints per face in {@link #faces} (0.1.11.5: 8, with the merged width and height). */
    private static final int STRIDE = 8;
    private static int faceCount;
    /** Sample centre in world block coordinates. */
    private static int centreX, centreY, centreZ;
    private static long lastSample = -1000;   // not Long.MIN_VALUE: now - MIN overflows
    /**
     * 0.1.11.5: the first layer (box frame) that is cut away, or {@code ABOVE + 1} when nothing is. When there is a roof over
     * the wearer (a solid block 2..6 above the feet), every layer from the roof up is left out, so a cave or a room shows
     * its inside.
     */
    private static int cutFrom = ABOVE + 1;
    /** Wall-clock time of the last sample, in milliseconds. The "SCAN" read-out uses it. */
    private static long sampledAtMs;

    /** Contact kinds (0.1.11.5): the marker shape. */
    public static final int HOSTILE = 0, PLAYER = 1, ALLY = 2, NEUTRAL = 3, PASSIVE = 4;

    /**
     * One creature on the map.
     *
     * @param dx      x offset from the player position, in blocks.
     * @param dy      y offset from the player position, in blocks.
     * @param dz      z offset from the player position, in blocks.
     * @param color   the marker colour, 0xRRGGBB (its meaning: threat, ally, neutral, passive).
     * @param kind    {@link #HOSTILE}, {@link #PLAYER}, {@link #ALLY}, {@link #NEUTRAL} or {@link #PASSIVE}: the shape.
     * @param threat  the threat level 0..2 of the threat sensor.
     * @param visible true when nothing solid is between the wearer's eyes and the creature (hollow marker otherwise).
     * @param dist    the distance in blocks.
     */
    public record Contact(double dx, double dy, double dz, int color, int kind, int threat, boolean visible, double dist) { }
    private static List<Contact> contacts = List.of();
    /** 0.1.11.5: line of sight per entity id, refreshed every {@link #LOS_TICKS} ticks (a ray per creature). */
    private static final java.util.Map<Integer, Boolean> LOS = new java.util.HashMap<>();
    private static final int LOS_TICKS = 5;

    /** Number of valid faces in the last sample. */
    public static int faceCount() { return faceCount; }

    /**
     * Client tick while the holomap unit is on. It samples the volume when
     * {@link #RESAMPLE_TICKS} ticks have passed. It also samples when the game time goes back,
     * for example in a new world. It updates the contacts on each tick.
     *
     * <p>0.1.11.5: contacts carry a kind (the marker shape), the threat level and the line of sight, and are ranked:
     * threat first, then hostiles, then distance. The nearest 48 are kept (it was the first 48 found).
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
        boolean losTick = now % LOS_TICKS == 0;
        if (losTick) LOS.clear();
        var eye = player.getEyePosition();
        List<Contact> list = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(CONTACT_RANGE),
                e -> e != player && e.isAlive() && !e.isInvisible())) {
            int threat = SuitSensorData.threatLevel(e.getId());
            boolean wingman = e instanceof com.miophas.singularity_iteration.common.armory.WingmanEntity;
            boolean linked = com.miophas.singularity_iteration.common.client.suit.nano.QuantumClient.datalinkSeen().contains(e.getId());
            // the shape follows the creature's kind; the threat level only changes its colour (a watched wolf stays neutral)
            int kind = wingman ? ALLY : e instanceof Player ? PLAYER : e instanceof Enemy || threat == 2 ? HOSTILE
                : e instanceof NeutralMob ? NEUTRAL : PASSIVE;
            // colour = meaning only: red danger, amber caution, cyan friends, pale yellow neutral, grey-green passive
            int color = threat == 2 ? FcsDraw.RED : kind == HOSTILE ? 0xFF6A4D : threat == 1 ? FcsDraw.YELLOW
                : kind == ALLY ? 0x5CEBFF : kind == PLAYER ? FcsDraw.CYAN : linked ? 0x6FF3D6
                : kind == NEUTRAL ? 0xFFE08A : 0x9FB8AA;
            Boolean seen = LOS.get(e.getId());
            if (seen == null) {
                seen = level.clip(new net.minecraft.world.level.ClipContext(eye, e.getEyePosition(),
                    net.minecraft.world.level.ClipContext.Block.VISUAL, net.minecraft.world.level.ClipContext.Fluid.NONE, player))
                    .getType() == net.minecraft.world.phys.HitResult.Type.MISS;
                LOS.put(e.getId(), seen);
            }
            double dx = e.getX() - player.getX(), dy = e.getY() - player.getY(), dz = e.getZ() - player.getZ();
            list.add(new Contact(dx, dy, dz, color, kind, threat, seen, Math.sqrt(dx * dx + dy * dy + dz * dz)));
        }
        list.sort((p, q) -> p.threat != q.threat ? Integer.compare(q.threat, p.threat)
            : (p.kind == HOSTILE) != (q.kind == HOSTILE) ? (p.kind == HOSTILE ? -1 : 1) : Double.compare(p.dist, q.dist));
        contacts = list.size() > 48 ? List.copyOf(list.subList(0, 48)) : list;
        // 0.1.11.5 (H4): a hostile in range, or a fresh sonar result, brings the small map up
        long wall = System.currentTimeMillis();
        boolean hostile = false;
        for (Contact c : contacts) if (c.kind == HOSTILE) { hostile = true; break; }
        boolean sonar = !OreScanner.hits().isEmpty() && wall - OreScanner.echoStartMs() < SONAR_SHOW_MS;
        if (hostile || sonar) lastReasonMs = wall;
    }

    /** 0.1.11.5 (H4): true while the small map should show (a reason in the last {@link #LINGER_MS}). */
    public static boolean popupVisible() {
        return System.currentTimeMillis() - lastReasonMs < LINGER_MS;
    }

    /** 0.1.11.5 (H3): the mouse wheel in the detail view: zoom, or with Shift the camera elevation. */
    public static void scroll(double delta, boolean tilt) {
        if (delta == 0) return;
        if (tilt) detailTiltDeg = Mth.clamp(detailTiltDeg + (float) Math.signum(delta) * 5F, MIN_TILT, MAX_TILT);
        else detailZoom = Mth.clamp(detailZoom * (delta > 0 ? 1.15F : 1 / 1.15F), MIN_ZOOM, MAX_ZOOM);
    }

    /** 0.1.11.5 (H3): resets the detail view's zoom and elevation (tests, the reset key). */
    public static void resetDetailView() { detailZoom = 1F; detailTiltDeg = 42F; }

    public static float detailZoom() { return detailZoom; }
    public static float detailTiltDeg() { return detailTiltDeg; }

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
        // 0.1.11.5: a roof over the wearer cuts away its layer and everything above it
        cutFrom = ABOVE + 1;
        BlockPos.MutableBlockPos roof = new BlockPos.MutableBlockPos();
        for (int up = 2; up <= 6 && up <= ABOVE; up++) {
            roof.set(centreX, centreY + up, centreZ);
            if (solid(level, roof)) { cutFrom = up; break; }
        }
        byte[] occ = new byte[sx * sy * sx];
        boolean[] lava = new boolean[sx * sy * sx];
        int[] col = new int[sx * sy * sx];
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int ix = 1; ix <= SIDE; ix++) {
            for (int iz = 1; iz <= SIDE; iz++) {
                int x = centreX + ix - 1 - RADIUS, z = centreZ + iz - 1 - RADIUS;
                // a column in a chunk that is not loaded stays air
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                for (int iy = 1; iy <= TALL; iy++) {
                    if (iy - 1 - BELOW >= cutFrom) break;   // 0.1.11.5: above the roof
                    int y = centreY + iy - 1 - BELOW;
                    m.set(x, y, z);
                    BlockState s = level.getBlockState(m);
                    if (s.isAir()) continue;
                    int idx = (iy * sx + iz) * sx + ix;
                    boolean water = !s.getFluidState().isEmpty();
                    occ[idx] = (byte) (water ? 2 : solid(level, m) ? 1 : 3);
                    if (water && s.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) lava[idx] = true;
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
                        int flags = (border ? 1 : 0) | (o == 2 ? 2 : 0) | (o == 3 ? 4 : 0) | (lava[idx] ? 8 : 0);
                        // 0.1.11.5: the top of a solid block near the wearer's layer with a drop of 4+ beside it
                        if (d == 2 && o == 1 && !border && Math.abs(iy - 1 - BELOW) <= 3 && ledge(occ, sx, ix, iy, iz)) flags |= 16;
                        out[k + 5] = flags;
                        if (++n >= MAX_FACES) break outer;
                    }
                }
            }
        }
        merge(out, n);
        sampledAtMs = System.currentTimeMillis();
    }

    /** 0.1.11.5: true when a horizontal neighbour of the cell above (ix, iy, iz) is open for 4 layers down. */
    private static boolean ledge(byte[] occ, int sx, int ix, int iy, int iz) {
        int[][] side = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : side) {
            int nx = ix + d[0], nz = iz + d[1];
            if (nx < 1 || nz < 1 || nx > SIDE || nz > SIDE || iy + 1 > TALL || iy - 3 < 1) continue;
            boolean open = true;
            for (int y = iy + 1; y >= iy - 3 && open; y--) open = occ[(y * sx + nz) * sx + nx] == 0;
            if (open) return true;
        }
        return false;
    }

    /**
     * 0.1.11.5 (H5): merges neighbouring faces that look the same into rectangles and stores them in {@link #faces}.
     * Floors and ceilings (+y, -y) merge in both directions of their plane. Walls merge along their horizontal direction
     * only, since a wall's fade depends on its layer. Faces merge only with equal flags. It cuts the faces to draw and
     * sort by about 4 to 10 times on open ground, and a wall on the wearer's layer becomes one outlined rectangle.
     *
     * @param raw the single-block faces, 6 ints each (x, y, z, dir, colour, flags)
     * @param n   the number of raw faces
     */
    private static void merge(int[] raw, int n) {
        int sx = SIDE, sy = TALL, sz = SIDE;
        int[] mask = new int[6 * sx * sy * sz];   // flags + 1 per (dir, x, y, z); 0 = no face
        for (int i = 0; i < n; i++) {
            int k = i * 6;
            int ix = raw[k] + RADIUS, iy = raw[k + 1] + BELOW, iz = raw[k + 2] + RADIUS, dir = raw[k + 3];
            mask[((dir * sx + ix) * sy + iy) * sz + iz] = raw[k + 5] + 1;
        }
        int[] out = new int[Math.max(1, n) * STRIDE];
        int m = 0;
        for (int dir = 0; dir < 6; dir++) {
            boolean floor = dir == 2 || dir == 3;
            boolean alongX = dir != 0 && dir != 1;   // walls facing x run along z; the others run along x
            for (int iy = 0; iy < sy; iy++) for (int iz = 0; iz < sz; iz++) for (int ix = 0; ix < sx; ix++) {
                int at = ((dir * sx + ix) * sy + iy) * sz + iz;
                int v = mask[at];
                if (v == 0) continue;
                int w = 1, h = 1;
                if (alongX) {
                    while (ix + w < sx && mask[((dir * sx + ix + w) * sy + iy) * sz + iz] == v) w++;
                    if (floor) {
                        grow:
                        while (iz + h < sz) {
                            for (int t = 0; t < w; t++) if (mask[((dir * sx + ix + t) * sy + iy) * sz + iz + h] != v) break grow;
                            h++;
                        }
                    }
                    for (int dz = 0; dz < h; dz++) for (int t = 0; t < w; t++) mask[((dir * sx + ix + t) * sy + iy) * sz + iz + dz] = 0;
                } else {
                    while (iz + w < sz && mask[((dir * sx + ix) * sy + iy) * sz + iz + w] == v) w++;
                    for (int t = 0; t < w; t++) mask[((dir * sx + ix) * sy + iy) * sz + iz + t] = 0;
                }
                int k = m * STRIDE;
                out[k] = ix - RADIUS; out[k + 1] = iy - BELOW; out[k + 2] = iz - RADIUS; out[k + 3] = dir;
                out[k + 4] = 0; out[k + 5] = v - 1; out[k + 6] = w; out[k + 7] = h;
                m++;
            }
        }
        faces = out;
        faceCount = m;
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

    /** 0.1.11.5: terrain is neutral; colour is kept for meaning (creatures, ores, water, lava, ledges). */
    private static final int TERRAIN = 0xA9B6BF, WATER = 0x3E8BFF, LAVA = 0xFF5A1F, LEDGE = 0xFF4D3D;

    /**
     * Draws the map. Call it on the render thread in the HUD pass.
     *
     * <p>Draw order, from back to front: panel, floor grid, back box edges, voxel faces, range rings on the wearer's
     * plane, contacts, ores, self marker, front box edges, labels.
     *
     * <p>0.1.11.5 (readability pass):
     * <ul>
     *   <li>Terrain is one neutral tone, brightest on the wearer's layers and fading with the layer distance. Walls on
     *       the wearer's layers get a white outline (a floor plan); floors are faint. Water is blue, lava orange, the
     *       top of a ledge with a drop beside it is outlined red. A roof over the wearer is cut away (see sampling).</li>
     *   <li>Contacts stand on stalks down to the wearer's plane (solid above it, dashed below) with a foot ring there.
     *       The marker shape tells the kind (triangle hostile, circle player or ally, square neutral, dot passive);
     *       a hollow marker has no line of sight. The first three get "distance, height" labels. A creature beyond the
     *       box is an arrow on the box rim with its distance (it was clamped onto the wall).</li>
     *   <li>Range rings at 5 and 10 blocks and the four directions on the wearer's plane.</li>
     *   <li>Scanned ores show whenever the ore scanner unit is on (it needed the Ore Hologram synergy).</li>
     * </ul>
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
        render(g, cx, cy, size, yawDeg, primary, partial, false);
    }

    /**
     * 0.1.11.5: draws the map; {@code detail} is the large view of the detail key (its own zoom and elevation, a legend).
     * The shown heading eases toward {@code yawDeg} (about 90 % in 0.15 s) so the box does not jitter while turning.
     */
    public static void render(GuiGraphics g, float cx, float cy, float size, float yawDeg, int primary, float partial, boolean detail) {
        long now = System.currentTimeMillis();
        float dt = yawKnown ? Math.min(0.25F, (now - lastFrameMs) / 1000F) : 1F;
        lastFrameMs = now;
        float diff = Mth.wrapDegrees(yawDeg - shownYaw);
        shownYaw = !yawKnown || Math.abs(diff) > 120 ? yawDeg : shownYaw + diff * (1F - (float) Math.exp(-dt * 15F));
        yawKnown = true;
        float yaw = (float) Math.toRadians(shownYaw);
        float tilt = detail ? (float) Math.toRadians(detailTiltDeg) : TILT;
        float zoom = detail ? detailZoom : 1F;
        // look direction (-sin, cos). Right of it: (-cos, -sin). Minecraft yaw 0 looks to +Z.
        fwdX = -Mth.sin(yaw); fwdZ = Mth.cos(yaw);
        rightX = -Mth.cos(yaw); rightZ = -Mth.sin(yaw);
        cosT = Mth.cos(tilt); sinT = Mth.sin(tilt);
        dist = RADIUS * 5.5F;
        float half = size / 2;
        float panelH = size * 0.92F;
        float k = Math.max(1F, size / 220F);   // marker and label scale
        // at depth dist, a width of SIDE * 1.18 blocks fills 84 % of the frame width (66 % before 0.1.11.5)
        focal = dist * (size * 0.84F * zoom) / (SIDE * 1.18F);
        cx0 = cx;
        cy0 = cy + panelH * 0.06F;

        // panel
        FcsDraw.panel(g, (int) (cx - half), (int) (cy - panelH / 2), (int) size, (int) panelH,
            net.minecraft.network.chat.Component.translatable("hud.mio_icif.holo.title", SIDE, TALL, SIDE).getString(), primary);
        // 0.1.11.5: the map stays inside its panel (a zoomed view, labels of creatures beyond the box)
        g.enableScissor((int) (cx - half) + 1, (int) (cy - panelH / 2) + 12, (int) (cx + half) - 1, (int) (cy + panelH / 2) - 1);
        labels.clear();

        float[] a = new float[3], b = new float[3], c = new float[3], d = new float[3];
        float x0 = -RADIUS, x1 = RADIUS + 1, y0 = -BELOW, y1 = ABOVE + 1, z0 = -RADIUS, z1 = RADIUS + 1;
        // floor grid of the box
        int grid = FcsDraw.argb(primary, 0.10F);
        for (int i = 0; i <= SIDE; i += 5) {
            project(x0 + i, y0, z0, a); project(x0 + i, y0, z1, b);
            FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.5F, grid);
            project(x0, y0, z0 + i, a); project(x1, y0, z0 + i, b);
            FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.5F, grid);
        }
        // back edges of the box (behind the voxels)
        boxEdges(g, primary, false);

        // voxel faces: collect visible ones, sort back to front
        int n = faceCount;
        int[] f = faces;
        // 0.1.11.5: one primitive sort key per face (depth bits high, face index low), no boxed Integer[] per frame.
        // The depth is always positive (the camera is dist = 66 blocks away), so its float bits sort as ints.
        long[] keys = new long[n];
        float[] ctr = new float[3];
        int nv = 0;
        for (int i = 0; i < n; i++) {
            int o = i * STRIDE;
            int dir = f[o + 3];
            centre(f[o], f[o + 1], f[o + 2], dir, f[o + 6], f[o + 7], ctr);
            float fx = ctr[0], fy = ctr[1], fz = ctr[2];
            float u = fx * rightX + fz * rightZ, w = fx * fwdX + fz * fwdZ;
            float px = u, py = w * sinT + fy * cosT, pz = dist + w * cosT - fy * sinT;
            float nu = DIR[dir][0] * rightX + DIR[dir][2] * rightZ, nw = DIR[dir][0] * fwdX + DIR[dir][2] * fwdZ;
            float qy = nw * sinT + DIR[dir][1] * cosT, qz = nw * cosT - DIR[dir][1] * sinT;
            if (nu * px + qy * py + qz * pz >= 0) continue;   // back face
            keys[nv++] = ((long) Float.floatToIntBits(pz) << 32) | i;
        }
        java.util.Arrays.sort(keys, 0, nv);
        float flicker = 0.95F + 0.05F * (float) Math.sin(now / 47.0) * (float) Math.sin(now / 131.0);
        int outline = FcsDraw.argb(0xF2F7FA, 0.78F * flicker);
        for (int oi = nv - 1; oi >= 0; oi--) {   // far faces first
            int i = (int) keys[oi];
            int o = i * STRIDE;
            int x = f[o], y = f[o + 1], z = f[o + 2], dir = f[o + 3], flags = f[o + 5];
            corners(x, y, z, dir, f[o + 6], f[o + 7], a, b, c, d);
            boolean cut = (flags & 1) != 0, fluid = (flags & 2) != 0, plant = (flags & 4) != 0, lava = (flags & 8) != 0, ledge = (flags & 16) != 0;
            // layer distance from the wearer's layers (the floor y = -1 and the body y = 0, 1)
            int layer = y < -1 ? -1 - y : y > 1 ? y - 1 : 0;
            float fade = layer == 0 ? 1F : Math.max(0.14F, 1F - layer * 0.22F);
            float shade = switch (dir) { case 2 -> 1.0F; case 3 -> 0.5F; case 0, 1 -> 0.72F; default -> 0.84F; };
            int rgb = lava ? LAVA : fluid ? WATER : TERRAIN;
            boolean vertical = dir != 2 && dir != 3;
            float alpha = lava ? 0.85F : fluid ? 0.42F : cut ? 0.12F : plant ? 0.16F : vertical ? 0.46F : 0.17F;
            if (!lava) alpha *= fade;
            FcsDraw.quad(g, a[0], a[1], b[0], b[1], c[0], c[1], d[0], d[1], FcsDraw.argb(scale(rgb, lava || fluid ? 1F : shade), alpha * flicker));
            if (cut || plant) continue;
            if (vertical && layer == 0) {
                // the wearer's layers: walls drawn as a floor plan
                FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.7F, outline);
                FcsDraw.line(g, b[0], b[1], c[0], c[1], 0.7F, outline);
                FcsDraw.line(g, c[0], c[1], d[0], d[1], 0.7F, outline);
                FcsDraw.line(g, d[0], d[1], a[0], a[1], 0.7F, outline);
            } else if (vertical && layer <= 3) {
                int edge = FcsDraw.argb(0xF2F7FA, 0.22F * fade);
                FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.5F, edge);
                FcsDraw.line(g, c[0], c[1], d[0], d[1], 0.5F, edge);
            }
            if (ledge) {
                int red = FcsDraw.argb(LEDGE, 0.9F);
                FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.8F, red);
                FcsDraw.line(g, b[0], b[1], c[0], c[1], 0.8F, red);
                FcsDraw.line(g, c[0], c[1], d[0], d[1], 0.8F, red);
                FcsDraw.line(g, d[0], d[1], a[0], a[1], 0.8F, red);
            }
            if (lava) {
                int hot = FcsDraw.argb(0xFFC04D, 0.8F * (0.7F + 0.3F * (float) Math.sin(now / 160.0 + x + z)));
                FcsDraw.line(g, a[0], a[1], c[0], c[1], 0.6F, hot);
            }
        }
        g.flush();

        // 0.1.11.5: range rings (5 and 10 blocks) and the four directions on the wearer's plane
        int ring = FcsDraw.argb(primary, 0.55F);
        for (int r : new int[] {5, 10}) {
            float px = 0, py = 0;
            for (int seg = 0; seg <= 48; seg++) {
                double t = seg * Math.PI * 2 / 48;
                project(0.5F + (float) Math.cos(t) * r, 0, 0.5F + (float) Math.sin(t) * r, a);
                if (seg > 0) FcsDraw.line(g, px, py, a[0], a[1], 0.6F, ring);
                px = a[0]; py = a[1];
            }
            project(0.5F + fwdX * r, 0, 0.5F + fwdZ * r, a);
            String rl = r + "m";
            float[] at = place(a[0] + 2, a[1] - 8 * k, FcsDraw.width(rl, 0.65F * k), 6.5F * k);
            if (at != null) FcsDraw.text(g, rl, at[0], at[1], FcsDraw.argb(primary, 0.9F), 0.65F * k);
        }
        String[] dirs = {tr("hud.mio_icif.holo.s"), tr("hud.mio_icif.holo.w"), tr("hud.mio_icif.holo.n"), tr("hud.mio_icif.holo.e")};   // +z, -x, -z, +x
        float[][] dv = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}};
        for (int i = 0; i < 4; i++) {
            project(0.5F + dv[i][0] * (RADIUS + 0.2F), 0, 0.5F + dv[i][1] * (RADIUS + 0.2F), a);
            float dw = FcsDraw.width(dirs[i], 0.8F * k);
            labels.add(new float[] {a[0] - dw / 2, a[1] - 5 * k, dw, 7 * k});   // directions always show; others step aside
            FcsDraw.textCentered(g, dirs[i], a[0], a[1] - 5 * k, FcsDraw.argb(i == 2 ? 0xFFFFFF : primary, i == 2 ? 1F : 0.75F), 0.8F * k);
        }

        // contacts
        int labelled = 0;
        float pulse = 0.6F + 0.4F * (float) Math.sin(now / 140.0);
        for (Contact ct : contacts) {
            float ux = (float) ct.dx, uz = (float) ct.dz;
            boolean outside = Math.abs(ux) > RADIUS + 0.5F || Math.abs(uz) > RADIUS + 0.5F;
            if (outside) {
                // an arrow on the rim of the box, in the creature's direction, with its distance
                float m = (RADIUS + 0.5F) / Math.max(Math.abs(ux), Math.abs(uz));
                float rx = ux * m + 0.5F, rz = uz * m + 0.5F;
                project(rx, 0, rz, a);
                project(rx - ux / Math.max(1F, (float) Math.hypot(ux, uz)) * 1.4F, 0, rz - uz / Math.max(1F, (float) Math.hypot(ux, uz)) * 1.4F, b);
                float ax = a[0] - b[0], ay = a[1] - b[1], al = Math.max(0.01F, (float) Math.hypot(ax, ay));
                ax /= al; ay /= al;
                float s3 = 3.2F * k;
                int col = FcsDraw.argb(ct.color, ct.threat == 2 ? pulse : 0.9F);
                FcsDraw.triangle(g, a[0] + ax * s3, a[1] + ay * s3, a[0] - ay * s3 * 0.7F, a[1] + ax * s3 * 0.7F, a[0] + ay * s3 * 0.7F, a[1] - ax * s3 * 0.7F, col);
                String far = Math.round(ct.dist) + "m";
                float fw = FcsDraw.width(far, 0.7F * k);
                float[] at = place(a[0] + ax * 9 * k - fw / 2, a[1] + ay * 9 * k - 4 * k, fw, 7 * k);
                if (at != null) FcsDraw.text(g, far, at[0], at[1], FcsDraw.argb(ct.color, 0.9F), 0.7F * k);
                continue;
            }
            float uy = (float) Mth.clamp(ct.dy, -BELOW, ABOVE + 1);
            project(ux + 0.5F, uy, uz + 0.5F, a);    // head
            project(ux + 0.5F, 0, uz + 0.5F, b);     // foot on the wearer's plane
            int stalk = FcsDraw.argb(ct.color, 0.6F);
            if (uy >= 0) FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.7F, stalk);
            else FcsDraw.dashed(g, a[0], a[1], b[0], b[1], 2F, 0.7F, stalk);
            FcsDraw.circle(g, b[0], b[1], 1.6F * k, 0.6F, FcsDraw.argb(ct.color, 0.75F));
            float r = (ct.kind == PASSIVE ? 1.8F : 3.6F) * k;
            int col = FcsDraw.argb(ct.color, ct.threat == 2 ? pulse : 1F);
            marker(g, ct.kind, a[0], a[1] - r, r, col, ct.visible);
            if (labelled < 3 && ct.kind != PASSIVE) {
                labelled++;
                int h = (int) Math.round(ct.dy);
                String tag = Math.round(ct.dist) + "m " + (h > 0 ? "\u2191" + h : h < 0 ? "\u2193" + (-h) : "=");
                float[] at = place(a[0] + r + 2, a[1] - r - 4 * k, FcsDraw.width(tag, 0.75F * k), 7.5F * k);
                if (at != null) FcsDraw.text(g, tag, at[0], at[1], FcsDraw.argb(ct.color, 1F), 0.75F * k);
            }
        }
        // ores of the last scan: with the ore scanner unit (0.1.11.5), or the Ore Hologram synergy as before
        var wearer = net.minecraft.client.Minecraft.getInstance().player;
        if (FcsState.active(com.miophas.singularity_iteration.common.suit.SuitModuleType.ORE_SCANNER)
                || wearer != null && com.miophas.singularity_iteration.common.suit.quantum.QuantumLinks.active(wearer,
                com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergy.ORE_HOLO)) {
            oreVoxels(g, now, a, b);
        }
        // self: a chevron at the centre of the centre block, pointing forward (into the box), and a
        // 1.8-block vertical line for the body height
        project(0.5F, 0, 0.5F, a);
        project(0.5F + fwdX * 1.6F, 0, 0.5F + fwdZ * 1.6F, b);
        project(0.5F - fwdX * 0.9F + rightX * 1.1F, 0, 0.5F - fwdZ * 0.9F + rightZ * 1.1F, c);
        project(0.5F - fwdX * 0.9F - rightX * 1.1F, 0, 0.5F - fwdZ * 0.9F - rightZ * 1.1F, d);
        FcsDraw.quad(g, b[0], b[1], c[0], c[1], a[0], a[1], d[0], d[1], FcsDraw.argb(0xFFFFFF, 0.95F));
        project(0.5F, 0, 0.5F, a);
        project(0.5F, 1.8F, 0.5F, b);
        FcsDraw.line(g, a[0], a[1], b[0], b[1], 0.8F, FcsDraw.argb(0xFFFFFF, 0.7F));

        // front edges of the box
        boxEdges(g, primary, true);

        g.flush();   // draw the batched lines while the scissor is still on
        g.disableScissor();
        // read-outs: the wearer's height, the roof cut, hostiles / all
        float ty = cy + panelH / 2 - 8;
        float ts = detail ? 0.7F : 0.5F;
        FcsDraw.text(g, String.format(java.util.Locale.ROOT, "Y%+d", centreY) + (cutFrom <= ABOVE ? "  " + tr("hud.mio_icif.holo.roof", cutFrom) : ""),
            cx - half + 5, ty, FcsDraw.argb(primary, 0.9F), ts);
        long hostile = contacts.stream().filter(ct -> ct.kind == HOSTILE).count();
        FcsDraw.textRight(g, tr("hud.mio_icif.holo.hostile", hostile, contacts.size()), cx + half - 4, ty,
            FcsDraw.argb(hostile > 0 ? 0xFF6A4D : primary, 0.9F), ts);
        if (detail) {
            // legend and controls under the read-outs
            FcsDraw.text(g, tr("hud.mio_icif.holo.legend"), cx - half + 5, ty - 11, FcsDraw.argb(0xEAF6FF, 0.85F), 0.6F);
            FcsDraw.text(g, tr("hud.mio_icif.holo.controls", Math.round(detailZoom * 100), Math.round(detailTiltDeg)),
                cx - half + 5, ty - 21, FcsDraw.argb(primary, 0.75F), 0.6F);
        }
        long age = now - sampledAtMs;
        if (age < 250) FcsDraw.textRight(g, "SCAN", cx + half - 4, cy - panelH / 2 + 2, FcsDraw.argb(0x050A0C, 0.9F), 0.5F);
    }

    /** 0.1.11.5: label boxes placed in this frame {x, y, w, h}, so labels do not cover each other. */
    private static final List<float[]> labels = new ArrayList<>();

    /** 0.1.11.5: a free spot for a label of size (w, h) at (x, y), moved down up to 3 lines; null when there is none. */
    private static float[] place(float x, float y, float w, float h) {
        for (int i = 0; i < 4; i++) {
            float yy = y + i * (h + 1);
            boolean free = true;
            for (float[] o : labels) {
                if (x < o[0] + o[2] && x + w > o[0] && yy < o[1] + o[3] && yy + h > o[1]) { free = false; break; }
            }
            if (free) {
                labels.add(new float[] {x, yy, w, h});
                return new float[] {x, yy};
            }
        }
        return null;
    }

    private static String tr(String key, Object... args) {
        return net.minecraft.network.chat.Component.translatable(key, args).getString();
    }

    /** 0.1.11.5: the contact marker of a kind; hollow without line of sight. (x, y) is the centre. */
    private static void marker(GuiGraphics g, int kind, float x, float y, float r, int argb, boolean filled) {
        switch (kind) {
            case HOSTILE -> {
                if (filled) FcsDraw.triangle(g, x, y - r, x + r, y + r * 0.8F, x - r, y + r * 0.8F, argb);
                else {
                    FcsDraw.line(g, x, y - r, x + r, y + r * 0.8F, 0.8F, argb);
                    FcsDraw.line(g, x + r, y + r * 0.8F, x - r, y + r * 0.8F, 0.8F, argb);
                    FcsDraw.line(g, x - r, y + r * 0.8F, x, y - r, 0.8F, argb);
                }
            }
            case NEUTRAL -> {
                float q = r * 0.8F;
                if (filled) FcsDraw.quad(g, x - q, y - q, x + q, y - q, x + q, y + q, x - q, y + q, argb);
                else {
                    FcsDraw.line(g, x - q, y - q, x + q, y - q, 0.8F, argb);
                    FcsDraw.line(g, x + q, y - q, x + q, y + q, 0.8F, argb);
                    FcsDraw.line(g, x + q, y + q, x - q, y + q, 0.8F, argb);
                    FcsDraw.line(g, x - q, y + q, x - q, y - q, 0.8F, argb);
                }
            }
            default -> {
                if (filled) FcsDraw.disc(g, x, y, r, argb);
                else FcsDraw.circle(g, x, y, r, 0.8F, argb);
            }
        }
    }

    /** 0.1.10.1 Ore Hologram: each scanned ore inside the box as a wire cube with a pulsing core (at most 160). */
    private static void oreVoxels(GuiGraphics g, long now, float[] a, float[] b) {
        int drawn = 0;
        float pulse = 0.65F + 0.35F * (float) Math.sin(now / 180.0);
        float[][] p = new float[8][3];
        for (OreScanner.Hit hit : OreScanner.hits()) {
            int rx = hit.pos().getX() - centreX, ry = hit.pos().getY() - centreY, rz = hit.pos().getZ() - centreZ;
            if (Math.abs(rx) > RADIUS || Math.abs(rz) > RADIUS || ry < -BELOW || ry > ABOVE) continue;
            for (int i = 0; i < 8; i++) project(rx + 0.15F + (i & 1) * 0.7F, ry + 0.15F + ((i >> 1) & 1) * 0.7F, rz + 0.15F + ((i >> 2) & 1) * 0.7F, p[i]);
            int edge = FcsDraw.argb(hit.color(), 0.9F * pulse);
            int[][] edges = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
            for (int[] e : edges) FcsDraw.line(g, p[e[0]][0], p[e[0]][1], p[e[1]][0], p[e[1]][1], 0.6F, edge);
            project(rx + 0.5F, ry + 0.5F, rz + 0.5F, a);
            FcsDraw.disc(g, a[0], a[1], 1.2F, FcsDraw.argb(hit.color(), pulse));
            if (++drawn >= 160) break;
        }
    }

    /**
     * Projects the 4 corners of a face, in order around the face.
     *
     * @param x   the lowest block x of the face in the box frame.
     * @param y   the lowest block y of the face in the box frame.
     * @param z   the lowest block z of the face in the box frame.
     * @param dir the face direction, an index into {@link #DIR}.
     * @param w   0.1.11.5: the merged width (along x, or along z for a wall facing x).
     * @param h   0.1.11.5: the merged height (along z for a floor or ceiling, else 1).
     */
    private static void corners(int x, int y, int z, int dir, int w, int h, float[] a, float[] b, float[] c, float[] d) {
        switch (dir) {
            case 0 -> { project(x + 1, y, z, a); project(x + 1, y + 1, z, b); project(x + 1, y + 1, z + w, c); project(x + 1, y, z + w, d); }
            case 1 -> { project(x, y, z, a); project(x, y, z + w, b); project(x, y + 1, z + w, c); project(x, y + 1, z, d); }
            case 2 -> { project(x, y + 1, z, a); project(x, y + 1, z + h, b); project(x + w, y + 1, z + h, c); project(x + w, y + 1, z, d); }
            case 3 -> { project(x, y, z, a); project(x + w, y, z, b); project(x + w, y, z + h, c); project(x, y, z + h, d); }
            case 4 -> { project(x, y, z + 1, a); project(x + w, y, z + 1, b); project(x + w, y + 1, z + 1, c); project(x, y + 1, z + 1, d); }
            default -> { project(x, y, z, a); project(x, y + 1, z, b); project(x + w, y + 1, z, c); project(x + w, y, z, d); }
        }
    }

    /** 0.1.11.5: the centre of a merged face in the box frame, written into {@code out}. */
    private static void centre(int x, int y, int z, int dir, int w, int h, float[] out) {
        switch (dir) {
            case 0 -> { out[0] = x + 1; out[1] = y + 0.5F; out[2] = z + w / 2F; }
            case 1 -> { out[0] = x; out[1] = y + 0.5F; out[2] = z + w / 2F; }
            case 2 -> { out[0] = x + w / 2F; out[1] = y + 1; out[2] = z + h / 2F; }
            case 3 -> { out[0] = x + w / 2F; out[1] = y; out[2] = z + h / 2F; }
            case 4 -> { out[0] = x + w / 2F; out[1] = y + 0.5F; out[2] = z + 1; }
            default -> { out[0] = x + w / 2F; out[1] = y + 0.5F; out[2] = z; }
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
        int col = FcsDraw.argb(primary, front ? 0.38F : 0.16F);   // 0.1.11.5: a quiet frame (0.85 / 0.35)
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
            for (int i = 0; i < 8; i++) FcsDraw.disc(g, p[i][0], p[i][1], 0.9F, FcsDraw.argb(primary, 0.6F));
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
