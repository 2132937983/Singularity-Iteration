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
 * Seismic ore scan. The scan walks a sphere around the wearer a few hundred blocks per tick, so
 * one full sweep takes {@link #SWEEP_TICKS} ticks and never stalls a frame. Results are drawn as
 * see-through wireframe cells, colour-coded by ore.
 */
public final class OreScanner {
    private OreScanner() {}

    public static final int RADIUS = 16;
    public static final int SWEEP_TICKS = 50;
    public static final int MAX_SHOWN = 384;

    public record Hit(BlockPos pos, int color, String label) { }

    private static final Map<Block, Integer> COLORS = new IdentityHashMap<>();
    private static final Map<Block, String> LABELS = new IdentityHashMap<>();
    private static List<Hit> shown = List.of();
    private static final List<Hit> pending = new ArrayList<>();
    private static BlockPos centre = BlockPos.ZERO;
    private static int cursor;
    private static long sweepStart;
    private static ClientLevel boundLevel;

    /** Echo wavefront: time the sonar pulse needs to reach {@link #RADIUS}. */
    public static final long ECHO_MS = 1600;
    /** Glitch burst at the start of a pulse. */
    public static final long GLITCH_MS = 850;
    private static long echoStart = -100_000;
    private static boolean wasActive;

    public static List<Hit> hits() { return shown; }
    public static long echoStartMs() { return echoStart; }
    /** 0..1 progress of the running echo, or -1 when no echo runs. */
    public static float echoProgress() {
        long age = System.currentTimeMillis() - echoStart;
        return age < 0 || age > ECHO_MS + 900 ? -1F : Math.min(1F, age / (float) ECHO_MS);
    }
    public static long sweepStartMs() { return sweepStart; }
    public static BlockPos centre() { return centre; }

    public static void reset() {
        wasActive = false;
        shown = List.of();
        pending.clear();
        cursor = 0;
        boundLevel = null;
    }

    /** Client tick while the scanner unit is active. */
    public static void tick(ClientLevel level, BlockPos playerPos) {
        if (level != boundLevel) { reset(); boundLevel = level; }
        if (!wasActive) {
            // switched on: scan the whole sphere at once so the first pulse has something to reveal
            wasActive = true;
            cursor = 0;
            fullScan(level, playerPos);
            pulse();
            return;
        }
        sweep(level, playerPos, false);
    }

    /** Starts a sonar pulse (sound + echo wavefront + glitch). */
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

    private static void sweep(ClientLevel level, BlockPos playerPos, boolean all) {
        int side = RADIUS * 2 + 1;
        int total = side * side * side;
        if (cursor == 0) {
            centre = playerPos;
            pending.clear();
            sweepStart = System.currentTimeMillis();
        }
        int perTick = all ? total : (total + SWEEP_TICKS - 1) / SWEEP_TICKS;
        int end = Math.min(total, cursor + perTick);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int r2 = RADIUS * RADIUS;
        for (int i = cursor; i < end; i++) {
            int dx = i % side - RADIUS, dz = (i / side) % side - RADIUS, dy = i / (side * side) - RADIUS;
            if (dx * dx + dy * dy + dz * dz > r2) continue;
            m.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
            if (!level.isLoaded(m)) continue;
            BlockState state = level.getBlockState(m);
            if (state.isAir() || !isOre(state)) continue;
            Block block = state.getBlock();
            pending.add(new Hit(m.immutable(), color(block), label(block)));
        }
        cursor = end;
        if (cursor >= total) {
            Vec3 eye = Vec3.atCenterOf(centre);
            pending.sort((a, b) -> Double.compare(a.pos.distToCenterSqr(eye), b.pos.distToCenterSqr(eye)));
            shown = List.copyOf(pending.size() > MAX_SHOWN ? pending.subList(0, MAX_SHOWN) : pending);
            cursor = 0;
        }
    }

    public static boolean isOre(BlockState state) {
        return state.is(Tags.Blocks.ORES) || state.is(Blocks.ANCIENT_DEBRIS);
    }

    /** Ore colour by its registry name (works for other mods' ores of the same metal). */
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

    public static String label(Block block) {
        return LABELS.computeIfAbsent(block, b -> {
            String p = BuiltInRegistries.BLOCK.getKey(b).getPath();
            p = p.replace("deepslate_", "").replace("nether_", "").replace("_ore", "").replace("ore_", "").replace("block_", "");
            int slash = p.lastIndexOf('/');
            if (slash >= 0) p = p.substring(slash + 1);
            return p.toUpperCase(java.util.Locale.ROOT);
        });
    }

    /** Nearest hit of each label (for the HUD list), nearest first. */
    public static List<Hit> nearestPerKind(int max) {
        Map<String, Hit> best = new HashMap<>();
        for (Hit h : shown) best.putIfAbsent(h.label, h);
        List<Hit> list = new ArrayList<>(best.values());
        Vec3 eye = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.position() : Vec3.ZERO;
        list.sort((a, b) -> Double.compare(a.pos.distToCenterSqr(eye), b.pos.distToCenterSqr(eye)));
        return list.size() > max ? list.subList(0, max) : list;
    }

    private static final int[][] EDGES = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
    /** Cube faces as corner indices (bottom, top, north, south, west, east). */
    private static final int[][] FACES = {{0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {3, 2, 6, 7}, {0, 3, 7, 4}, {1, 2, 6, 5}};
    static final int ECHO_RGB = 0x3FD4FF;

    /** Deterministic hash noise 0..1. */
    static float noise(long a, long b) {
        long h = a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 32;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    /**
     * HUD pass. While a pulse runs: a sonar wavefront sphere sweeps out through the terrain,
     * ripples cross the screen from the edges, and a digital glitch burst runs over the view. Each
     * ore lights up when the wavefront reaches it (with a short glitch flicker) and then stays lit:
     * a translucent filled cell with bright edges, drawn in screen space so it shows through rock.
     */
    public static void renderHud(net.minecraft.client.gui.GuiGraphics g, Vec3 cam, float pulse) {
        long now = System.currentTimeMillis();
        long age = now - echoStart;
        int w = g.guiWidth(), h = g.guiHeight();
        float front = age < 0 ? RADIUS : Math.min(RADIUS + 2, RADIUS * age / (float) ECHO_MS);
        boolean echo = age >= 0 && age <= ECHO_MS + 900;
        Vec3 origin = Vec3.atCenterOf(centre);
        if (echo) wavefront(g, origin, front, age);
        cells(g, cam, origin, front, age, now, pulse);
        if (echo) screenEcho(g, w, h, age, now);
        if (!shown.isEmpty() && age > ECHO_MS) tagsNearest(g, cam, now);
    }

    private static void cells(net.minecraft.client.gui.GuiGraphics g, Vec3 cam, Vec3 origin, float front, long age, long now, float pulse) {
        if (shown.isEmpty()) return;
        float[][] p = new float[8][];
        int drawn = 0;
        for (Hit hit : shown) {
            double dOrigin = Math.sqrt(hit.pos.distToCenterSqr(origin));
            if (dOrigin > front) continue;                 // the wave has not reached it yet
            float since = (float) (age - dOrigin / RADIUS * ECHO_MS);   // ms since the wave passed
            double d = Math.sqrt(hit.pos.distToCenterSqr(cam));
            float a = (float) Math.max(0.40, 1.0 - d / RADIUS * 0.5) * pulse;
            float jx = 0, jy = 0;
            if (since >= 0 && since < 380) {
                // reveal: flash, then a few frames of glitch flicker and offset
                float f = since / 380F;
                long bucket = now / 40;
                if (noise(bucket, hit.pos.asLong()) < 0.35F * (1 - f)) continue;
                a = Math.min(1F, a + (1 - f) * 0.8F);
                jx = (noise(bucket + 7, hit.pos.asLong()) - 0.5F) * 6 * (1 - f);
            }
            boolean ok = true;
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

    /** The wavefront sphere: latitude rings, clipped to the scan radius, cyan with a bright leading edge. */
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
     * Screen-space echo: ripple rings that start at the edge of the view and close in on the
     * centre (the pulse washes over the wearer from all sides), with an RGB split, plus a short
     * digital glitch burst (offset slices, noise blocks, a scan bar).
     */
    private static void screenEcho(net.minecraft.client.gui.GuiGraphics g, int w, int h, long age, long now) {
        float cx = w / 2F, cy = h / 2F;
        float maxR = (float) Math.sqrt(cx * cx + cy * cy);
        for (int k = 0; k < 3; k++) {
            float t = (age - k * 170) / 1100F;
            if (t < 0 || t > 1) continue;
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
        String label = age < ECHO_MS ? String.format(java.util.Locale.ROOT, "SONAR PULSE  %02dm", (int) (RADIUS * age / ECHO_MS))
            : String.format(java.util.Locale.ROOT, "ECHO  %d CONTACTS", shown.size());
        if (ta > 0) {
            float jitter = age < GLITCH_MS && noise(now / 60, 9) < 0.3F ? 2 : 0;
            FcsDraw.textCentered(g, label, cx + jitter, cy + 18, FcsDraw.argb(ECHO_RGB, ta), 0.62F);
        }
        g.flush();
    }

    /** Small in-world tags on the nearest ore of each kind (label and distance). */
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
