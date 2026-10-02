package com.miophas.singularity_iteration.common.client.screen.widget;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * Lightweight pie / donut chart for GUIs: wedges are streamed as quads into the
 * shared GUI batch (no textures, no allocation per frame). A wedge spans
 * {@code ceil(fraction * SEGMENTS)} rim segments, so a full chart is 64 quads.
 */
@OnlyIn(Dist.CLIENT)
public final class PieChart {
    private PieChart() {}

    private static final int SEGMENTS = 64;
    /** Distinct, readable on the dark display; ordered by legend position. */
    public static final int[] PALETTE = {
        0xFF3A6EA5, 0xFFD2A43A, 0xFFC8523C, 0xFF4C9A5A, 0xFF7A62B0, 0xFFD27A3A, 0xFF6E9ACB, 0xFF9AA0A6,
    };

    /**
     * @param values  wedge weights (non-negative); zero total draws an empty ring
     * @param inner   inner radius for a donut, 0 for a full pie
     * @param highlight index of a wedge drawn slightly enlarged, or -1
     */
    public static void draw(GuiGraphics g, float cx, float cy, float radius, float inner, long[] values, int count,
                            int highlight) {
        long total = 0;
        for (int i = 0; i < count; i++) total += Math.max(0, values[i]);
        Matrix4f pose = g.pose().last().pose();
        VertexConsumer out = g.bufferSource().getBuffer(RenderType.gui());
        if (total <= 0) {
            wedge(out, pose, cx, cy, radius, inner, 0, Mth.TWO_PI, 0xFFD5D8DB);
        } else {
            double start = -Math.PI / 2;
            for (int i = 0; i < count; i++) {
                if (values[i] <= 0) continue;
                double sweep = Mth.TWO_PI * values[i] / (double) total;
                float r = i == highlight ? radius + 2 : radius;
                wedge(out, pose, cx, cy, r, inner, start, sweep, PALETTE[i % PALETTE.length]);
                start += sweep;
            }
        }
        g.flush();
    }

    private static void wedge(VertexConsumer out, Matrix4f pose, float cx, float cy, float r, float inner,
                              double start, double sweep, int argb) {
        int steps = Math.max(1, (int) Math.ceil(SEGMENTS * sweep / Mth.TWO_PI));
        int a = argb >>> 24, red = (argb >> 16) & 255, gr = (argb >> 8) & 255, b = argb & 255;
        // darker inner edge gives the donut some depth
        int ir = red * 3 / 4, ig = gr * 3 / 4, ib = b * 3 / 4;
        for (int s = 0; s < steps; s++) {
            double a0 = start + sweep * s / steps, a1 = start + sweep * (s + 1) / steps;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            // counter-clockwise on screen (y down): inner0, outer0... GUI type is cull-free anyway
            out.addVertex(pose, cx + c0 * inner, cy + s0 * inner, 0).setColor(ir, ig, ib, a);
            out.addVertex(pose, cx + c1 * inner, cy + s1 * inner, 0).setColor(ir, ig, ib, a);
            out.addVertex(pose, cx + c1 * r, cy + s1 * r, 0).setColor(red, gr, b, a);
            out.addVertex(pose, cx + c0 * r, cy + s0 * r, 0).setColor(red, gr, b, a);
        }
    }

    /** Index of the wedge under (mx, my), or -1. */
    public static int hit(float cx, float cy, float radius, float inner, long[] values, int count, double mx, double my) {
        double dx = mx - cx, dy = my - cy, d = Math.sqrt(dx * dx + dy * dy);
        if (d > radius + 2 || d < inner) return -1;
        long total = 0;
        for (int i = 0; i < count; i++) total += Math.max(0, values[i]);
        if (total <= 0) return -1;
        double ang = Math.atan2(dy, dx) + Math.PI / 2;
        if (ang < 0) ang += Mth.TWO_PI;
        double acc = 0;
        for (int i = 0; i < count; i++) {
            if (values[i] <= 0) continue;
            acc += Mth.TWO_PI * values[i] / (double) total;
            if (ang <= acc) return i;
        }
        return count - 1;
    }
}
