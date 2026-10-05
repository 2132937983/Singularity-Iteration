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

    public static List<Hit> hits() { return shown; }
    public static long sweepStartMs() { return sweepStart; }
    public static BlockPos centre() { return centre; }

    public static void reset() {
        shown = List.of();
        pending.clear();
        cursor = 0;
        boundLevel = null;
    }

    /** Client tick while the scanner unit is active. */
    public static void tick(ClientLevel level, BlockPos playerPos) {
        if (level != boundLevel) { reset(); boundLevel = level; }
        int side = RADIUS * 2 + 1;
        int total = side * side * side;
        if (cursor == 0) {
            centre = playerPos;
            pending.clear();
            sweepStart = System.currentTimeMillis();
        }
        int perTick = (total + SWEEP_TICKS - 1) / SWEEP_TICKS;
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

    /**
     * HUD pass: each ore cell as a projected wireframe. Drawing in screen space keeps the cells
     * visible through terrain without a depth-state render type, and costs 12 hairlines per cell.
     */
    public static void renderHud(net.minecraft.client.gui.GuiGraphics g, Vec3 cam, float pulse) {
        if (shown.isEmpty()) return;
        float[][] p = new float[8][];
        for (Hit h : shown) {
            double d = Math.sqrt(h.pos.distToCenterSqr(cam));
            float a = (float) Math.max(0.35, 1.0 - d / RADIUS * 0.55) * pulse;
            boolean ok = true;
            for (int i = 0; i < 8 && ok; i++) {
                double x = h.pos.getX() + ((i == 1 || i == 2 || i == 5 || i == 6) ? 0.96 : 0.04);
                double y = h.pos.getY() + (i >= 4 ? 0.96 : 0.04);
                double z = h.pos.getZ() + ((i == 2 || i == 3 || i == 6 || i == 7) ? 0.96 : 0.04);
                p[i] = FcsProjection.project(x, y, z);
                ok = p[i] != null;
            }
            if (!ok) continue;
            int col = FcsDraw.argb(h.color, a);
            for (int[] e : EDGES) FcsDraw.line(g, p[e[0]][0], p[e[0]][1], p[e[1]][0], p[e[1]][1], 0.8F, col);
            FcsDraw.line(g, p[4][0], p[4][1], p[6][0], p[6][1], 0.5F, FcsDraw.argb(h.color, a * 0.45F));
            FcsDraw.line(g, p[5][0], p[5][1], p[7][0], p[7][1], 0.5F, FcsDraw.argb(h.color, a * 0.45F));
        }
    }
}
