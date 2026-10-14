// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/**
 * Seismic ore scan of the suit HUD. The scanner reads blocks from the client chunk data. It sends
 * no packets.
 *
 * <p><b>Sweep cycle.</b> The scan volume is the cube of side {@code 2 * RADIUS + 1} = 33 blocks
 * around the sweep centre (35 937 cells). The scanner tests only the cells in the sphere of
 * {@link #RADIUS} (about 17 000 cells). Each tick tests the next
 * {@code ceil(35937 / SWEEP_TICKS)} = 719 cube cells. Thus one sweep takes {@link #SWEEP_TICKS}
 * ticks (2.5 seconds), and a tick reads about 340 block states in the sphere. Then the next sweep
 * starts at once. The cell order is x fastest, then z, then y. Thus a sweep goes from the bottom
 * layer to the top layer.
 *
 * <p>Exception: on the first tick after the unit comes on, the scanner tests the full sphere in
 * one tick. Thus the first sonar pulse has results to show. This one tick reads all 35 937 cells.
 *
 * <p><b>Coordinate frames.</b> The sweep centre is the block position of the player at the start
 * of the sweep. It does not move during the sweep. {@link Hit#pos()} is an absolute world block
 * position. The HUD pass projects the block corners with {@link FcsProjection}, in world
 * coordinates.
 *
 * <p><b>Memory bounds.</b> The pending list holds at most the number of ore cells in the sphere.
 * The shown list holds at most {@link #MAX_SHOWN} hits. The colour and label caches hold one entry
 * per block type.
 *
 * <p><b>Rendering.</b> The HUD pass draws each hit as a see-through wireframe cell in the ore
 * colour. The cells are in screen space, with no depth test. Thus they show through rock.
 *
 * <p><b>Thread.</b> Call all methods on the client thread only.
 */
public final class OreScanner {
    private OreScanner() {}

    /** Scan sphere radius in blocks. */
    public static final int RADIUS = 16;
    /** 0.1.10.1: scan radius of a Mk.II scanner (+50 %). */
    public static final int RADIUS_MK2 = 24;
    /** Radius of the sweep that runs now (fixed for the whole sweep; the first switch-on scan uses {@link #RADIUS}). */
    private static int sweepRadius = RADIUS;
    /** 0.1.10.1: radius of the sweep that made {@link #shown} (the echo and the cell fade use it). */
    private static int shownRadius = RADIUS;
    /** 0.1.10.1 Mk.II: the ore tag filter (null = all ores); the filter of the sweep that runs now. */
    @org.jetbrains.annotations.Nullable private static net.minecraft.tags.TagKey<Block> filter, sweepFilter;

    /** 0.1.10.1: radius of the sweep that runs now. */
    public static int radius() { return sweepRadius; }

    /** Radius for the next sweep: Mk.II on the local wearer, else the base. */
    private static int targetRadius() {
        var p = Minecraft.getInstance().player;
        return p != null && com.miophas.singularity_iteration.common.suit.SuitModules.tier(p,
            com.miophas.singularity_iteration.common.suit.SuitModuleType.ORE_SCANNER) >= 2 ? RADIUS_MK2 : RADIUS;
    }
    /** True when the local wearer has a Mk.II scanner. */
    private static boolean mk2() {
        return targetRadius() == RADIUS_MK2;
    }

    /** 0.1.10.1 Mk.II: the ore tag filter, or null for all ores (a Mk.I scanner ignores it). */
    @org.jetbrains.annotations.Nullable
    public static net.minecraft.tags.TagKey<Block> filter() {
        return filter;
    }

    /** Short name of the filter: "ALL" or the tag path after "ores/" in upper case ("DIAMOND"). */
    public static String filterName() {
        if (filter == null) return "ALL";
        String p = filter.location().getPath();
        return p.substring(p.indexOf('/') + 1).toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * 0.1.10.1 Mk.II: steps the filter through all ores and every common ore tag ({@code c:ores/*})
     * that the game knows (the server sends the tags, so ores of other mods with the common tags
     * are in the list). The scan starts again with a pulse.
     */
    public static void cycleFilter(int direction) {
        List<net.minecraft.tags.TagKey<Block>> options = new ArrayList<>();
        options.add(null);
        BuiltInRegistries.BLOCK.getTagNames()
            .filter(t -> t.location().getNamespace().equals("c") && t.location().getPath().startsWith("ores/"))
            .sorted((a, b) -> a.location().getPath().compareTo(b.location().getPath()))
            .forEach(options::add);
        int i = Math.max(0, options.indexOf(filter));
        filter = options.get(Math.floorMod(i + direction, options.size()));
        wasActive = false;                 // the next tick scans the sphere at once and pulses
        cursor = 0;
    }

    /** Ticks for one full sweep of the scan cube. */
    public static final int SWEEP_TICKS = 50;
    /** Maximum number of hits that the scanner keeps and draws, nearest to the sweep centre first. */
    public static final int MAX_SHOWN = 384;

    /**
     * One ore block.
     *
     * @param pos   the absolute world block position.
     * @param color the ore colour, 0xRRGGBB.
     * @param label the short HUD name of the ore, for example "IRON".
     */
    public record Hit(BlockPos pos, int color, String label) { }

    /** Colour per block type. One entry per block type, so the size has a limit. */
    private static final Map<Block, Integer> COLORS = new IdentityHashMap<>();
    /** Label per block type. One entry per block type, so the size has a limit. */
    private static final Map<Block, String> LABELS = new IdentityHashMap<>();
    /** Result of the last full sweep. Immutable. Sorted by distance to the sweep centre. */
    private static List<Hit> shown = List.of();
    /** Hits of the sweep that runs now. They go to {@link #shown} when the sweep ends. */
    private static final List<Hit> pending = new ArrayList<>();
    /** Centre of the sweep that runs now, in world block coordinates. */
    private static BlockPos centre = BlockPos.ZERO;
    /** Index of the next cube cell to test, 0 .. 35 936. 0 means that a new sweep starts. */
    private static int cursor;
    /** Wall-clock start time of the sweep that runs now, in milliseconds. */
    private static long sweepStart;
    /** The level of the last tick. A new level resets the scanner. */
    private static ClientLevel boundLevel;

    /** Echo wavefront: time in milliseconds that the sonar pulse needs to reach {@link #RADIUS}. */
    public static final long ECHO_MS = 1600;
    /** Length in milliseconds of the glitch burst at the start of a pulse. */
    public static final long GLITCH_MS = 850;
    /** Wall-clock start time of the last pulse, in milliseconds. The start value means "no pulse". */
    private static long echoStart = -100_000;
    /** True while the unit is on. {@link #tick} uses it to find the first tick after switch-on. */
    private static boolean wasActive;

    /** Hits of the last full sweep, nearest to the sweep centre first. */
    public static List<Hit> hits() { return shown; }
    /** Wall-clock start time of the last pulse, in milliseconds. */
    public static long echoStartMs() { return echoStart; }
    /**
     * Progress of the running echo, from 0 to 1. It stays at 1 for 900 ms after the wavefront
     * reaches {@link #RADIUS}, while the echo fades. It is -1 when no echo runs.
     */
    public static float echoProgress() {
        long age = System.currentTimeMillis() - echoStart;
        return age < 0 || age > ECHO_MS + 900 ? -1F : Math.min(1F, age / (float) ECHO_MS);
    }
    /** Wall-clock start time of the sweep that runs now, in milliseconds. */
    public static long sweepStartMs() { return sweepStart; }
    /** Centre of the sweep that runs now, in world block coordinates. */
    public static BlockPos centre() { return centre; }

    /**
     * Removes all hits and stops the sweep. {@code FcsClient} calls it on each tick while the unit
     * is off. It does not stop a running echo.
     */
    public static void reset() {
        wasActive = false;
        shown = List.of();
        pending.clear();
        cursor = 0;
        boundLevel = null;
    }

    /**
     * Client tick while the scanner unit is on. Call it one time per tick.
     *
     * @param level     the client level. It must not be null.
     * @param playerPos the block position of the player. The next sweep uses it as the centre.
     */
    public static void tick(ClientLevel level, BlockPos playerPos) {
        if (level != boundLevel) { reset(); boundLevel = level; }
        if (!wasActive) {
            // switched on: scan the whole sphere in this tick. Thus the first pulse has hits to show.
            wasActive = true;
            cursor = 0;
            fullScan(level, playerPos);
            pulse();
            return;
        }
        sweep(level, playerPos, false);
    }

    /**
     * Starts a sonar pulse: the echo sound, the echo wavefront and the glitch burst. The scanner
     * starts a pulse only when the unit comes on. Later sweeps change the hits without a pulse.
     */
    public static void pulse() {
        echoStart = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                com.miophas.singularity_iteration.common.registry.mio_icif_sounds.FCS_ECHO.get(), 1.0F, 0.55F));
        }
    }

    private static void fullScan(ClientLevel level, BlockPos playerPos) {
        sweep(level, playerPos, true);
    }

    /**
     * Tests the next part of the scan cube.
     *
     * <p>The cell index {@code i} maps to an offset from the centre:
     * {@code dx = i % side - RADIUS}, {@code dz = (i / side) % side - RADIUS},
     * {@code dy = i / side^2 - RADIUS}. The method skips cells out of the sphere, cells in
     * chunks that are not loaded, and cells that are not ore.
     *
     * <p>At the end of a sweep, the method sorts the hits by distance to the centre of the centre
     * block. It keeps the nearest {@link #MAX_SHOWN} as the new {@link #shown} list.
     *
     * @param all true to test all cells in this call, false to test one tick budget of cells.
     */
    private static void sweep(ClientLevel level, BlockPos playerPos, boolean all) {
        if (cursor == 0) {                                              // 0.1.10.1: fixed for the whole sweep
            sweepRadius = all ? RADIUS : targetRadius();
            sweepFilter = mk2() ? filter : null;
        }
        int rad = sweepRadius;
        int side = rad * 2 + 1;
        int total = side * side * side;
        if (cursor == 0) {
            // new sweep: fix the centre for the full sweep
            centre = playerPos;
            pending.clear();
            sweepStart = System.currentTimeMillis();
        }
        // tick budget: total / SWEEP_TICKS, rounded up, so that a sweep ends in SWEEP_TICKS ticks
        int perTick = all ? total : (total + SWEEP_TICKS - 1) / SWEEP_TICKS;
        int end = Math.min(total, cursor + perTick);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int r2 = rad * rad;
        for (int i = cursor; i < end; i++) {
            int dx = i % side - rad, dz = (i / side) % side - rad, dy = i / (side * side) - rad;
            if (dx * dx + dy * dy + dz * dz > r2) continue;
            m.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
            if (!level.isLoaded(m)) continue;
            BlockState state = level.getBlockState(m);
            if (state.isAir() || !isOre(state) || sweepFilter != null && !state.is(sweepFilter)) continue;
            Block block = state.getBlock();
            pending.add(new Hit(m.immutable(), color(block), label(block)));
        }
        cursor = end;
        if (cursor >= total) {
            Vec3 eye = Vec3.atCenterOf(centre);
            pending.sort((a, b) -> Double.compare(a.pos.distToCenterSqr(eye), b.pos.distToCenterSqr(eye)));
            shown = List.copyOf(pending.size() > MAX_SHOWN ? pending.subList(0, MAX_SHOWN) : pending);
            shownRadius = rad;
            cursor = 0;
        }
    }

    /**
     * Tests a block state for ore. Ore is a block in the common {@code c:ores} tag, or ancient
     * debris. Thus ores of other mods count when they use the common tag.
     */
    public static boolean isOre(BlockState state) {
        return state.is(Tags.Blocks.ORES) || state.is(Blocks.ANCIENT_DEBRIS);
    }

    /**
     * Ore colour from the registry path of the block, as 0xRRGGBB. The method finds a metal name in
     * the path. Thus it also works for ores of other mods with the same metal. The first name that
     * matches wins. The colour is white when no name matches. The result is cached per block type.
     */
    public static int color(Block block) {
        return COLORS.computeIfAbsent(block, b -> {
            String p = BuiltInRegistries.BLOCK.getKey(b).getPath();
            if (p.contains("diamond")) return 0x5CF2FF;
            if (p.contains("emerald")) return 0x3DFF6E;
            if (p.contains("ancient_debris") || p.contains("netherite")) return 0xB06BFF;
            if (p.contains("gold")) return 0xFFD23F;
            if (p.contains("redstone")) return 0xFF3B30;
            if (p.contains("lapis")) return 0x3D6BFF;
            if (p.contains("copper")) return 0xFF8A4C;
            if (p.contains("iron")) return 0xE8C4A8;
            if (p.contains("coal")) return 0x8A8F96;
            if (p.contains("quartz")) return 0xF4F0E8;
            if (p.contains("uranium")) return 0x9DFF3D;
            if (p.contains("thorium")) return 0x3DFFD8;
            if (p.contains("tin")) return 0xC9D6E0;
            if (p.contains("lead")) return 0x7C83B8;
            if (p.contains("silver")) return 0xD8E4F0;
            if (p.contains("iridium")) return 0xF0F0FF;
            return 0xFFFFFF;
        });
    }

    /**
     * Short HUD label from the registry path of the block, in upper case. The method removes the
     * prefixes and suffixes "deepslate_", "nether_", "_ore", "ore_" and "block_", and any folder
     * part before the last '/'. Example: "deepslate_iron_ore" becomes "IRON". The result is cached
     * per block type.
     */
    public static String label(Block block) {
        return LABELS.computeIfAbsent(block, b -> {
            String p = BuiltInRegistries.BLOCK.getKey(b).getPath();
            p = p.replace("deepslate_", "").replace("nether_", "").replace("_ore", "").replace("ore_", "").replace("block_", "");
            int slash = p.lastIndexOf('/');
            if (slash >= 0) p = p.substring(slash + 1);
            return p.toUpperCase(java.util.Locale.ROOT);
        });
    }

    /**
     * One hit for each label, for the HUD list. For each label, the method takes the first hit in
     * {@link #shown}. That is the hit nearest to the sweep centre, not always the hit nearest to the
     * player. Then it sorts these hits by distance to the player feet, nearest first.
     *
     * @param max the maximum number of hits to return.
     */
    public static List<Hit> nearestPerKind(int max) {
        Map<String, Hit> best = new HashMap<>();
        for (Hit h : shown) best.putIfAbsent(h.label, h);
        List<Hit> list = new ArrayList<>(best.values());
        Vec3 eye = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.position() : Vec3.ZERO;
        list.sort((a, b) -> Double.compare(a.pos.distToCenterSqr(eye), b.pos.distToCenterSqr(eye)));
        return list.size() > max ? list.subList(0, max) : list;
    }

    /**
     * The 12 cube edges as pairs of corner indices. Corner layout: corners 0..3 are the bottom
     * (y = 0) and 4..7 are the top (y = 1). Corner i + 4 is above corner i. Around the bottom, the
     * corners are 0 = (0, 0), 1 = (1, 0), 2 = (1, 1), 3 = (0, 1) as (x, z).
     */
    private static final int[][] EDGES = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
    /** Cube faces as corner indices (bottom, top, north, south, west, east). Same layout as {@link #EDGES}. */
    private static final int[][] FACES = {{0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {3, 2, 6, 7}, {0, 3, 7, 4}, {1, 2, 6, 5}};
    /** Colour of the echo effects, 0xRRGGBB. */
    static final int ECHO_RGB = 0x3FD4FF;

    /**
     * Deterministic hash noise from 0 to 1 (1 excluded). The same inputs always give the same
     * value. The effects use a time bucket as one input, so the noise changes in steps.
     */
    static float noise(long a, long b) {
        long h = a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 32;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    /**
     * HUD pass. Call it on the render thread, after {@link FcsProjection#capture} for this frame.
     *
     * <p>While a pulse runs, a sonar wavefront sphere moves out through the terrain. Ripples cross
     * the screen from the edges, and a digital glitch burst runs over the view. Each ore comes on
     * when the wavefront reaches it, with a short glitch flicker. Then it stays on. Each ore is a
     * translucent filled cell with bright edges. The cells are in screen space, so they show
     * through rock.
     *
     * <p>The wavefront radius is {@code RADIUS * age / ECHO_MS} blocks. It grows at a constant
     * speed and stops at {@code RADIUS + 2}.
     *
     * @param cam   the camera position in world blocks.
     * @param pulse the alpha multiplier of the slow HUD pulse, about 0.6..1.
     */
    public static void renderHud(net.minecraft.client.gui.GuiGraphics g, Vec3 cam, float pulse) {
        long now = System.currentTimeMillis();
        long age = now - echoStart;
        int w = g.guiWidth(), h = g.guiHeight();
        // wavefront radius in blocks. A start time in the future shows the full sphere.
        // 0.1.10.1: the radius of the sweep that made the hits (Mk.II: 24)
        int rad = shownRadius;
        float front = age < 0 ? rad : Math.min(rad + 2, rad * age / (float) ECHO_MS);
        boolean echo = age >= 0 && age <= ECHO_MS + 900;
        Vec3 origin = Vec3.atCenterOf(centre);
        if (echo) wavefront(g, origin, front, age);
        cells(g, cam, origin, front, age, now, pulse);
        if (echo) screenEcho(g, w, h, age, now);
        if (!shown.isEmpty() && age > ECHO_MS) tagsNearest(g, cam, now);
    }

    /**
     * Draws the ore cells that the wavefront has reached.
     *
     * <p>Each cell projects its 8 corners with {@link FcsProjection}. When one corner is behind the
     * camera, the method skips the full cell. Then no cell is drawn with a wrong, mirrored shape.
     * The alpha falls with the camera distance, from 1 at the camera to 0.4 at {@code 1.2 * RADIUS}
     * and more.
     *
     * <p>Reveal: for 380 ms after the wavefront passes a cell, the cell flashes brighter and
     * flickers. The flicker changes every 40 ms. In a flicker step, the cell is hidden with a
     * probability of up to 35 % and moves sideways by up to 3 GUI pixels. Both values fall to 0 at
     * the end of the reveal.
     */
    private static void cells(net.minecraft.client.gui.GuiGraphics g, Vec3 cam, Vec3 origin, float front, long age, long now, float pulse) {
        if (shown.isEmpty()) return;
        float[][] p = new float[8][];
        int drawn = 0;
        for (Hit hit : shown) {
            double dOrigin = Math.sqrt(hit.pos.distToCenterSqr(origin));
            if (dOrigin > front) continue;                 // the wave has not reached it yet
            float since = (float) (age - dOrigin / shownRadius * ECHO_MS);   // ms since the wave passed
            double d = Math.sqrt(hit.pos.distToCenterSqr(cam));
            float a = (float) Math.max(0.40, 1.0 - d / shownRadius * 0.5) * pulse;
            // jx, jy: GUI pixel offset of the flicker. Only jx changes. jy stays 0.
            float jx = 0, jy = 0;
            if (since >= 0 && since < 380) {
                // reveal: a flash, then some frames of glitch flicker and offset
                float f = since / 380F;
                long bucket = now / 40;
                if (noise(bucket, hit.pos.asLong()) < 0.35F * (1 - f)) continue;
                a = Math.min(1F, a + (1 - f) * 0.8F);
                jx = (noise(bucket + 7, hit.pos.asLong()) - 0.5F) * 6 * (1 - f);
            }
            boolean ok = true;
            // corner i: x = 1 for i in {1, 2, 5, 6}, z = 1 for i in {2, 3, 6, 7}, y = 1 for i >= 4
            for (int i = 0; i < 8 && ok; i++) {
                double x = hit.pos.getX() + ((i == 1 || i == 2 || i == 5 || i == 6) ? 1.0 : 0.0);
                double y = hit.pos.getY() + (i >= 4 ? 1.0 : 0.0);
                double z = hit.pos.getZ() + ((i == 2 || i == 3 || i == 6 || i == 7) ? 1.0 : 0.0);
                p[i] = FcsProjection.project(x, y, z);
                ok = p[i] != null;
                if (ok) { p[i][0] += jx; p[i][1] += jy; }
            }
            if (!ok) continue;
            int fill = FcsDraw.argb(hit.color, 0.10F * a);
            for (int[] f : FACES) {
                FcsDraw.quad(g, p[f[0]][0], p[f[0]][1], p[f[1]][0], p[f[1]][1], p[f[2]][0], p[f[2]][1], p[f[3]][0], p[f[3]][1], fill);
            }
            int col = FcsDraw.argb(hit.color, 0.95F * a);
            for (int[] e : EDGES) FcsDraw.line(g, p[e[0]][0], p[e[0]][1], p[e[1]][0], p[e[1]][1], 0.9F, col);
            if (++drawn >= MAX_SHOWN) break;
        }
    }

    /**
     * Draws the wavefront sphere in cyan. It has 8 latitude rings (48 segments each) and 12 faint
     * meridians (16 segments each). Rings near the equator are brighter. Ring k = lat / 2 = 4 is
     * thicker. With lat = 9 this ring is at 80 degrees from the top, not at the equator. A segment
     * is not drawn when one of its ends is behind the camera. The sphere fades out in the 900 ms
     * after {@link #ECHO_MS}.
     *
     * @param o   the sphere centre in world blocks.
     * @param r   the sphere radius in blocks.
     * @param age the time since the pulse start, in milliseconds.
     */
    private static void wavefront(net.minecraft.client.gui.GuiGraphics g, Vec3 o, float r, long age) {
        float fade = age < ECHO_MS ? 1F : Math.max(0F, 1F - (age - ECHO_MS) / 900F);
        if (fade <= 0 || r < 0.5F) return;
        int lat = 9, seg = 48;
        for (int k = 1; k < lat; k++) {
            double phi = Math.PI * k / lat;               // 0 = top
            double ry = Math.cos(phi) * r, rr = Math.sin(phi) * r;
            float eq = (float) Math.sin(phi);
            int col = FcsDraw.argb(ECHO_RGB, (0.18F + 0.5F * eq * eq) * fade);
            float[] prev = null;
            for (int i = 0; i <= seg; i++) {
                double th = i * Math.PI * 2 / seg;
                float[] q = FcsProjection.project(o.x + Math.cos(th) * rr, o.y + ry, o.z + Math.sin(th) * rr);
                if (q != null && prev != null) FcsDraw.line(g, prev[0], prev[1], q[0], q[1], k == lat / 2 ? 1.6F : 0.7F, col);
                prev = q;
            }
        }
        // meridians, faint
        for (int m = 0; m < 12; m++) {
            double th = m * Math.PI * 2 / 12;
            float[] prev = null;
            for (int i = 0; i <= 16; i++) {
                double phi = Math.PI * i / 16;
                float[] q = FcsProjection.project(o.x + Math.cos(th) * Math.sin(phi) * r, o.y + Math.cos(phi) * r, o.z + Math.sin(th) * Math.sin(phi) * r);
                if (q != null && prev != null) FcsDraw.line(g, prev[0], prev[1], q[0], q[1], 0.5F, FcsDraw.argb(ECHO_RGB, 0.16F * fade));
                prev = q;
            }
        }
    }

    /**
     * Screen-space echo. Three ripple rings start at the edge of the view and close in on the
     * centre. This shows the pulse as it comes back to the wearer from all sides. Each ring has a
     * red and a cyan copy, offset by 1.6 GUI pixels (RGB split). Ring k starts 170 ms after ring
     * k - 1 and lives 1100 ms. For {@link #GLITCH_MS} after the pulse start, a digital glitch burst
     * also runs: offset slices, noise blocks and a scan bar. The glitch pattern changes every 45 ms.
     */
    private static void screenEcho(net.minecraft.client.gui.GuiGraphics g, int w, int h, long age, long now) {
        float cx = w / 2F, cy = h / 2F;
        float maxR = (float) Math.sqrt(cx * cx + cy * cy);
        for (int k = 0; k < 3; k++) {
            float t = (age - k * 170) / 1100F;
            if (t < 0 || t > 1) continue;
            // ease-out: the ring moves fast at the start and slows near the centre
            float e = 1 - (1 - t) * (1 - t);
            float r = maxR * (1.05F - e * 0.95F);
            float a = (float) Math.sin(t * Math.PI) * (0.55F - k * 0.12F);
            FcsDraw.circle(g, cx - 1.6F, cy, r, 2.2F, FcsDraw.argb(0xFF3050, a * 0.35F));
            FcsDraw.circle(g, cx + 1.6F, cy, r, 2.2F, FcsDraw.argb(0x30FFFF, a * 0.35F));
            FcsDraw.circle(g, cx, cy, r, 1.2F, FcsDraw.argb(0xDFFBFF, a));
            FcsDraw.circle(g, cx, cy, r + 5, 5F, FcsDraw.argb(ECHO_RGB, a * 0.12F));
        }
        // edge tint that drains towards the centre
        float edge = Math.max(0, 1 - age / 900F);
        if (edge > 0) {
            int c = FcsDraw.argb(ECHO_RGB, 0.20F * edge);
            int b = (int) (6 + 30 * (1 - edge));
            g.fill(0, 0, w, b, c); g.fill(0, h - b, w, h, c);
            g.fill(0, b, b, h - b, c); g.fill(w - b, b, w, h - b, c);
        }
        if (age < GLITCH_MS) {
            float f = 1 - age / (float) GLITCH_MS;
            long bucket = now / 45;
            // offset slices: thin full-width bars, some with an RGB split
            int slices = (int) (6 + 16 * f);
            for (int i = 0; i < slices; i++) {
                float y = noise(bucket, i) * h;
                float hh = 1 + noise(bucket, i + 100) * 6 * f;
                float off = (noise(bucket, i + 200) - 0.5F) * 40 * f;
                g.fill((int) off, (int) y, (int) (w + off), (int) (y + hh), FcsDraw.argb(ECHO_RGB, 0.14F + 0.30F * f));
                if (noise(bucket, i + 300) < 0.4F) {
                    g.fill((int) (off + 3), (int) y, (int) (w * 0.6F + off), (int) (y + 1), FcsDraw.argb(0xFF3050, 0.35F * f));
                    g.fill((int) (off - 3), (int) (y + hh - 1), (int) (w * 0.8F + off), (int) (y + hh), FcsDraw.argb(0x30FFFF, 0.35F * f));
                }
            }
            // noise blocks
            for (int i = 0; i < (int) (26 * f); i++) {
                int bx = (int) (noise(bucket, i + 400) * w), by = (int) (noise(bucket, i + 500) * h);
                int bw = 3 + (int) (noise(bucket, i + 600) * 18), bh = 2 + (int) (noise(bucket, i + 700) * 5);
                g.fill(bx, by, bx + bw, by + bh, FcsDraw.argb(noise(bucket, i + 800) < 0.5F ? 0xDFFBFF : ECHO_RGB, 0.45F * f));
            }
            // fast scan bar
            float sy = (age / (float) GLITCH_MS) * h;
            g.fill(0, (int) sy, w, (int) sy + 2, FcsDraw.argb(0xDFFBFF, 0.35F * f));
        }
        // pulse read-out under the crosshair
        float ta = age < ECHO_MS ? 1F : Math.max(0, 1 - (age - ECHO_MS) / 900F);
        String label = age < ECHO_MS ? String.format(java.util.Locale.ROOT, "SONAR PULSE  %02dm", (int) (shownRadius * age / ECHO_MS))
            : String.format(java.util.Locale.ROOT, "ECHO  %d CONTACTS", shown.size());
        if (sweepFilter != null) label += "  [" + filterName() + "]";
        if (ta > 0) {
            float jitter = age < GLITCH_MS && noise(now / 60, 9) < 0.3F ? 2 : 0;
            FcsDraw.textCentered(g, label, cx + jitter, cy + 18, FcsDraw.argb(ECHO_RGB, ta), 0.62F);
        }
        g.flush();
    }

    /**
     * Small tags on up to 6 ores, one for each label (see {@link #nearestPerKind}). Each tag shows
     * the label and the distance from the camera in blocks (1 block = 1 m). The tag is 0.5 blocks
     * above the block centre. A tag is not drawn when its point is behind the camera.
     */
    private static void tagsNearest(net.minecraft.client.gui.GuiGraphics g, Vec3 cam, long now) {
        for (Hit hit : nearestPerKind(6)) {
            Vec3 c = Vec3.atCenterOf(hit.pos);
            float[] q = FcsProjection.project(c.x, c.y + 0.5, c.z);
            if (q == null) continue;
            String s = String.format(java.util.Locale.ROOT, "%s %.0fm", hit.label, c.distanceTo(cam));
            FcsDraw.line(g, q[0], q[1], q[0] + 6, q[1] - 6, 0.7F, FcsDraw.argb(hit.color, 0.9F));
            FcsDraw.line(g, q[0] + 6, q[1] - 6, q[0] + 10 + FcsDraw.width(s, 0.5F), q[1] - 6, 0.7F, FcsDraw.argb(hit.color, 0.6F));
            FcsDraw.text(g, s, q[0] + 8, q[1] - 11.5F, FcsDraw.argb(hit.color, 1F), 0.5F);
        }
    }
}
