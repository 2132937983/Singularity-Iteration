// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.client.render.FxBudget;
import com.miophas.singularity_iteration.common.client.render.FxLodRules;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.suit.ArcNetRules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

/**
 * 0.1.11.128 (armour and flight overhaul E5): the ionised sheath of a fast quantum flyer ({@link ArcNetRules}): short arcs of
 * white, pale yellow and violet light crawling over a box round the body, a new set every 2 to 3 ticks, drawn in the frame
 * of the posed body (it follows the flight pose). Additive. Third person and other players only (the flight layer does not
 * run for the own first-person view).
 */
public final class ArcNet {
    private ArcNet() {}

    static void draw(PoseStack pose, MultiBufferSource buffers, HumanoidModel<?> model, Player player) {
        float level = ArcNetRules.level(ManeuverModeClient.machOf(player));
        if (level <= 0.02F || FxBudget.tier(player) == FxLodRules.FAR || !model.body.visible) return;
        long tick = player.tickCount, seed = player.getId() * 0x5851F42D4C957F2DL + ArcNetRules.generation(tick);
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        pose.pushPose();
        model.body.translateAndRotate(pose);
        drawFrame(vc, pose.last().pose(), seed, tick, level);
        pose.popPose();
    }

    /**
     * 0.1.11.129 (E6): the net in a body frame {@code m} (origin at the neck, +y down the body, +z the back), for a flyer drawn
     * without a model pose (the Wingmen).
     */
    public static void drawFrame(VertexConsumer vc, Matrix4f m, long seed, long tick, float level) {
        var arcs = ArcNetRules.arcs(seed);
        for (int a = 0; a < arcs.size(); a++) {
            float[] c = ArcNetRules.colour(a, tick);
            float alpha = ArcNetRules.ALPHA * level * ArcNetRules.flicker(seed, a, tick);
            float[][] pts = arcs.get(a);
            for (int k = 0; k + 1 < pts.length; k++) {
                float edge = 1 - Math.abs(2F * (k + 0.5F) / (pts.length - 1) - 1);   // brighter in the middle of the arc
                segment(vc, m, pts[k], pts[k + 1], ArcNetRules.WIDTH, c, alpha * (0.45F + 0.55F * edge));
            }
        }
    }

    /** A thin line of light: two crossed quads square to the segment. */
    private static void segment(VertexConsumer vc, Matrix4f m, float[] p, float[] q, float w, float[] c, float alpha) {
        float dx = q[0] - p[0], dy = q[1] - p[1], dz = q[2] - p[2];
        float[] ref = Math.abs(dy) > 0.9F * (float) Math.sqrt(dx * dx + dy * dy + dz * dz) ? new float[]{1, 0, 0} : new float[]{0, 1, 0};
        float[] u = norm(cross(new float[]{dx, dy, dz}, ref)), v = norm(cross(new float[]{dx, dy, dz}, u));
        quad(vc, m, p, q, u, w, c, alpha);
        quad(vc, m, p, q, v, w, c, alpha);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float[] p, float[] q, float[] o, float w, float[] c, float a) {
        float ox = o[0] * w, oy = o[1] * w, oz = o[2] * w, al = Math.max(0, Math.min(1, a));
        vc.addVertex(m, p[0] - ox, p[1] - oy, p[2] - oz).setColor(c[0], c[1], c[2], al);
        vc.addVertex(m, p[0] + ox, p[1] + oy, p[2] + oz).setColor(c[0], c[1], c[2], al);
        vc.addVertex(m, q[0] + ox, q[1] + oy, q[2] + oz).setColor(c[0], c[1], c[2], al);
        vc.addVertex(m, q[0] - ox, q[1] - oy, q[2] - oz).setColor(c[0], c[1], c[2], al);
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static float[] norm(float[] a) {
        float l = (float) Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return l < 1.0E-6F ? new float[]{0, 0, 1} : new float[]{a[0] / l, a[1] / l, a[2] / l};
    }
}
