// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.block.fusion.mio_icif_block_fusion_controller;
import com.miophas.singularity_iteration.common.block.fusion.mio_icif_block_fusion_part;
import com.miophas.singularity_iteration.common.blockentity.fusion.FusionControllerBlockEntity;
import com.miophas.singularity_iteration.common.fusion.FusionPlasma;
import com.miophas.singularity_iteration.common.fusion.FusionStructure;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * 0.1.11.139 (fusion overhaul F7): the plasma in the tokamak's channel, seen through the diagnostic viewports (the user's
 * pick from the demo: helical field lines and viewports). Additive light only, depth-tested, so the structure hides it except
 * where a viewport (glass) lets it through.
 *
 * <ul>
 *   <li>A faint glow fills every plasma cell, coloured by the temperature (dull red, orange, yellow-white); in H-mode the
 *       cells at the channel's edge glow pink (the pedestal).</li>
 *   <li>Bright trails run round the torus along helical field lines (safety factor {@link #Q}), faster the hotter it is.</li>
 *   <li>Events, from the console's synced state ({@link FusionControllerBlockEntity.Fx}): a breakdown (a thin violet ring on the
 *       axis, then the plasma grows in over two seconds), an ELM (the edge flashes white, the exhaust port puffs flame), a
 *       disruption (a flare, then the plasma collapses in a second and a half, sparks flying), a gentle end (a one-second
 *       fade). While it burns the exhaust port smokes now and then.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class FusionPlasmaRenderer implements BlockEntityRenderer<FusionControllerBlockEntity> {
    /** Field lines, trail dots each, safety factor, minor radii of the helices (blocks: radial, vertical). */
    static final int LINES = 64, TRAIL = 8;
    static final double Q = 2.5, MINOR = 0.85, KAPPA = 1.25;
    static final int BREAKDOWN_TICKS = 20, GROW_TICKS = 40, FLARE_TICKS = 6, COLLAPSE_TICKS = 24, FADE_TICKS = 20, ELM_TICKS = 12;

    /** The channel of one console: plasma cells relative to it, which are at the edge, the torus's centre and major radius. */
    private record Channel(List<int[]> cells, boolean[] edge, double cx, double cy, double cz, double major,
                           List<BlockPos> exhausts, Direction[] exhaustFacing, long nextPortCheckTick) {}

    /** What one console's plasma is doing on this client. */
    private static final class Anim {
        int state = -1, elms = -1;
        double startAt = -1e9, elmAt = -1e9, disruptAt = -1e9, fadeAt = -1e9;
        float lastKev;
        long particleTick;
    }

    private final Map<BlockPos, Channel> channels = new HashMap<>();
    private final Map<BlockPos, Anim> anims = new HashMap<>();
    private Level lastLevel;

    public FusionPlasmaRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public boolean shouldRenderOffScreen(FusionControllerBlockEntity be) {
        FusionControllerBlockEntity.Fx fx = be.fx();
        return fx.plasma() || fx.state() == FusionPlasma.State.DISRUPTED.ordinal();
    }
    @Override public int getViewDistance() { return 128; }
    @Override public AABB getRenderBoundingBox(FusionControllerBlockEntity be) { return new AABB(be.getBlockPos()).inflate(28); }

    private Channel channel(FusionControllerBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        Level level = be.getLevel();
        long now = level == null ? 0L : level.getGameTime();
        Channel c = channels.get(pos);
        if (c != null && (!c.exhausts().isEmpty() || level == null || now < c.nextPortCheckTick())) return c;
        BlockState state = be.getBlockState();
        Direction facing = state.hasProperty(mio_icif_block_fusion_controller.FACING) ? state.getValue(mio_icif_block_fusion_controller.FACING) : Direction.EAST;
        FusionStructure s = FusionStructure.tokamak();
        List<int[]> cells;
        boolean[] edge;
        double cx, cy, cz, major;
        if (c != null) {
            cells = c.cells(); edge = c.edge(); cx = c.cx(); cy = c.cy(); cz = c.cz(); major = c.major();
        } else {
            List<BlockPos> plasma = s.positions(pos, facing, FusionStructure.Part.PLASMA);
            it.unimi.dsi.fastutil.longs.LongOpenHashSet set = new it.unimi.dsi.fastutil.longs.LongOpenHashSet(plasma.size());
            for (BlockPos p : plasma) set.add(p.asLong());
            cells = new ArrayList<>(plasma.size());
            edge = new boolean[plasma.size()];
            double sx = 0, sy = 0, sz = 0;
            for (int idx = 0; idx < plasma.size(); idx++) {
                BlockPos p = plasma.get(idx);
                sx += p.getX() + 0.5; sy += p.getY() + 0.5; sz += p.getZ() + 0.5;
                boolean isEdge = false;
                for (Direction d : Direction.values()) if (!set.contains(p.relative(d).asLong())) { isEdge = true; break; }
                edge[idx] = isEdge;
                cells.add(new int[]{p.getX() - pos.getX(), p.getY() - pos.getY(), p.getZ() - pos.getZ()});
            }
            int n = Math.max(1, plasma.size());
            double wcx = sx / n, wcy = sy / n, wcz = sz / n, mSum = 0;
            for (BlockPos p : plasma) mSum += Math.hypot(p.getX() + 0.5 - wcx, p.getZ() + 0.5 - wcz);
            major = mSum / n;
            cx = wcx - pos.getX(); cy = wcy - pos.getY(); cz = wcz - pos.getZ();
        }
        List<BlockPos> exhausts = new ArrayList<>();
        List<Direction> faces = new ArrayList<>();
        if (level != null)
            for (BlockPos p : s.positions(pos, facing, FusionStructure.Part.PORT_SLOT)) {
                BlockState ps = level.getBlockState(p);
                if (ps.is(mio_icif_blocks.FUSION_EXHAUST_PORT.get())) {
                    exhausts.add(p);
                    faces.add(ps.hasProperty(mio_icif_block_fusion_part.Facing.FACING) ? ps.getValue(mio_icif_block_fusion_part.Facing.FACING) : Direction.DOWN);
                }
            }
        c = new Channel(cells, edge, cx, cy, cz, major, exhausts, faces.toArray(new Direction[0]), exhausts.isEmpty() ? now + 40L : Long.MAX_VALUE);
        channels.put(pos.immutable(), c);
        return c;
    }

    /** The plasma's colour at a temperature (keV), as {r, g, b} in 0..1. */
    static float[] heat(double kev) {
        double k = Math.max(0, Math.min(1, kev / 15));
        return new float[]{(float) ((150 + 105 * k) / 255), (float) ((40 + 180 * k * k) / 255), (float) ((60 + 120 * k * k * k) / 255)};
    }

    @Override
    public void render(FusionControllerBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        if (level != lastLevel) { channels.clear(); anims.clear(); lastLevel = level; }
        FusionControllerBlockEntity.Fx fx = be.fx();
        double time = level.getGameTime() + partialTick;
        Anim a = anims.computeIfAbsent(be.getBlockPos().immutable(), p -> new Anim());
        int disrupted = FusionPlasma.State.DISRUPTED.ordinal();
        boolean wasPlasma = a.state == FusionPlasma.State.HEATING.ordinal() || a.state == FusionPlasma.State.BURNING.ordinal();
        if (a.state != fx.state()) {
            if (fx.plasma() && !wasPlasma) a.startAt = a.state < 0 ? -1e9 : time;   // a plasma already burning when first seen: no breakdown
            if (wasPlasma && fx.state() == disrupted) a.disruptAt = time;
            else if (wasPlasma && !fx.plasma()) a.fadeAt = time;
            a.state = fx.state();
        }
        if (a.elms >= 0 && fx.elms() > a.elms && fx.hMode()) a.elmAt = time;
        a.elms = fx.elms();
        if (fx.plasma()) a.lastKev = fx.kev();

        // how bright the plasma is
        double level01, breakdown = 0;
        if (fx.plasma()) {
            double age = time - a.startAt;
            breakdown = age < BREAKDOWN_TICKS ? 1 - age / BREAKDOWN_TICKS : 0;
            level01 = age < BREAKDOWN_TICKS ? 0 : Math.min(1, (age - BREAKDOWN_TICKS) / GROW_TICKS);
        } else if (fx.state() == disrupted && time - a.disruptAt < FLARE_TICKS + COLLAPSE_TICKS) {
            double age = time - a.disruptAt;
            level01 = age < FLARE_TICKS ? 1.4 : Math.max(0, 1 - (age - FLARE_TICKS) / COLLAPSE_TICKS);
        } else if (time - a.fadeAt < FADE_TICKS) {
            level01 = 1 - (time - a.fadeAt) / FADE_TICKS;
        } else {
            level01 = 0;
        }
        if (level01 <= 0.01 && breakdown <= 0.01) return;

        Channel ch = channel(be);
        float kev = fx.plasma() ? fx.kev() : a.lastKev;
        float[] c = heat(kev);
        double sinceElm = time - a.elmAt;
        float elm = fx.hMode() && sinceElm < ELM_TICKS ? (float) (1 - sinceElm / ELM_TICKS) : 0;
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        Matrix4f m = pose.last().pose();

        if (level01 > 0.01) {
            // the glow in every cell; the edge pink in H-mode, white at an ELM
            for (int i = 0; i < ch.cells().size(); i++) {
                int[] p = ch.cells().get(i);
                boolean isEdge = ch.edge()[i];
                float r = c[0], g = c[1], b = c[2], al = (float) (0.16 * level01);
                if (isEdge && fx.hMode()) { r = 0.7F * r + 0.3F; g = 0.7F * g + 0.3F * 0.55F; b = 0.7F * b + 0.3F * 0.75F; }   // the pedestal: a pink tint
                if (isEdge && elm > 0) { r = 1F; g = 1F; b = 0.94F; al = Math.max(al, 0.35F * elm); }
                box(vc, m, p[0] + 0.06F, p[1] + 0.06F, p[2] + 0.06F, p[0] + 0.94F, p[1] + 0.94F, p[2] + 0.94F, r, g, b, al);
            }
            // the field lines: bright trails running round the torus
            double flow = time * (0.004 + 0.006 * Math.min(1, kev / 15));
            float[] bright = heat(Math.min(15, kev * 1.1 + 2));
            for (int j = 0; j < LINES; j++) {
                double theta0 = j / (double) LINES * 2 * Math.PI, phi0 = j * 0.26;
                for (int k = 0; k < TRAIL; k++) {
                    double phi = flow + phi0 - k * 0.035, theta = theta0 + Q * phi;
                    double rr = ch.major() + MINOR * Math.cos(theta);
                    float x = (float) (ch.cx() + rr * Math.cos(phi)), y = (float) (ch.cy() + MINOR * KAPPA * Math.sin(theta)), z = (float) (ch.cz() + rr * Math.sin(phi));
                    float s = 0.07F, al = (float) (Math.min(1, level01) * (1 - k * 0.11));
                    box(vc, m, x - s, y - s, z - s, x + s, y + s, z + s, bright[0], bright[1], bright[2], al);
                }
            }
        }
        // the breakdown: a thin violet ring on the axis
        if (breakdown > 0.01) {
            for (int[] p : ch.cells())
                box(vc, m, p[0] + 0.06F, p[1] + 0.06F, p[2] + 0.06F, p[0] + 0.94F, p[1] + 0.94F, p[2] + 0.94F, 0.6F, 0.45F, 1F, (float) (0.08 * breakdown));
            for (int i = 0; i < 320; i++) {
                double phi = i / 320.0 * 2 * Math.PI;
                float x = (float) (ch.cx() + ch.major() * Math.cos(phi)), y = (float) ch.cy(), z = (float) (ch.cz() + ch.major() * Math.sin(phi)), s = 0.07F;
                box(vc, m, x - s, y - s, z - s, x + s, y + s, z + s, 0.8F, 0.65F, 1F, (float) breakdown);
            }
        }

        // particles, once a tick: sparks at a disruption, flame at an ELM, smoke while it burns
        long tick = level.getGameTime();
        if (tick != a.particleTick) {
            a.particleTick = tick;
            BlockPos o = be.getBlockPos();
            var rnd = level.random;
            if (fx.state() == disrupted && time - a.disruptAt < 20 && !ch.cells().isEmpty())
                for (int i = 0; i < 6; i++) {
                    int[] p = ch.cells().get(rnd.nextInt(ch.cells().size()));
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, o.getX() + p[0] + rnd.nextDouble(), o.getY() + p[1] + rnd.nextDouble(), o.getZ() + p[2] + rnd.nextDouble(),
                        rnd.nextGaussian() * 0.2, rnd.nextGaussian() * 0.2, rnd.nextGaussian() * 0.2);
                }
            for (int e = 0; e < ch.exhausts().size(); e++) {
                BlockPos ex = ch.exhausts().get(e);
                Direction d = ch.exhaustFacing()[e];
                double px = ex.getX() + 0.5 + d.getStepX() * 0.6, py = ex.getY() + 0.5 + d.getStepY() * 0.6, pz = ex.getZ() + 0.5 + d.getStepZ() * 0.6;
                if (elm > 0.9) for (int i = 0; i < 4; i++)
                    level.addParticle(ParticleTypes.SMALL_FLAME, px, py, pz, d.getStepX() * 0.05 + rnd.nextGaussian() * 0.01, d.getStepY() * 0.05, d.getStepZ() * 0.05 + rnd.nextGaussian() * 0.01);
                if (fx.state() == FusionPlasma.State.BURNING.ordinal() && rnd.nextInt(20) == 0)
                    level.addParticle(ParticleTypes.SMOKE, px, py, pz, d.getStepX() * 0.03, 0.02, d.getStepZ() * 0.03);
            }
        }
    }

    /** An additive box (six faces). */
    static void box(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float r, float g, float b, float a) {
        a = Math.max(0, Math.min(1, a));
        quad(vc, m, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, r, g, b, a);
        quad(vc, m, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, r, g, b, a);
        quad(vc, m, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, r, g, b, a);
        quad(vc, m, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
        quad(vc, m, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, r, g, b, a);
        quad(vc, m, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r, g, b, a);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                             float dx, float dy, float dz, float r, float g, float b, float a) {
        vc.addVertex(m, ax, ay, az).setColor(r, g, b, a);
        vc.addVertex(m, bx, by, bz).setColor(r, g, b, a);
        vc.addVertex(m, cx, cy, cz).setColor(r, g, b, a);
        vc.addVertex(m, dx, dy, dz).setColor(r, g, b, a);
    }
}
