// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.client.render.FxBudget;
import com.miophas.singularity_iteration.common.client.render.FxLodRules;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.suit.LightTrailRules;
import com.miophas.singularity_iteration.common.suit.PhaseWingRules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * High-speed geometric Phase Energy Wings and Air-Brake palm reverse-thrust rings for the Quantum Suit.
 *
 * <p>Model space: 1 unit = 1 block, +Y points down, +Z points toward the player's back. Drawn in
 * {@link SiRenderTypes#FIELD_GLOW} (additive, no depth write) and LOD-gated by {@link FxBudget}.
 */
public final class PhaseEnergyWings {
    private PhaseEnergyWings() {}

    /** Draws the translucent segmented electromagnetic energy wings unfolding from the dorsal pods at high throttle. */
    public static void drawWings(PoseStack pose, MultiBufferSource buffers, PlayerModel<?> model,
                                 AbstractClientPlayer player, float partial) {
        if (FxBudget.tier(player) == FxLodRules.FAR) return;
        float trans = ManeuverModeClient.transitionOf(player);
        if (trans <= 0.01F) return;
        float thr = ManeuverModeClient.throttleOf(player, partial);
        float unfurl = PhaseWingRules.unfurl(thr) * trans;
        if (unfurl <= 0.01F) return;

        double mach = ManeuverModeClient.machOf(player);
        float time = (player.tickCount + partial) * 0.18F;
        float pulse = 0.5F + 0.5F * Mth.sin(time * 2.4F);
        float alpha = PhaseWingRules.alpha(unfurl, mach, pulse);
        if (alpha <= 0.01F) return;

        float sweepRad = (float) Math.toRadians(PhaseWingRules.sweepDeg(mach, unfurl));
        float span = PhaseWingRules.spanScale(mach, unfurl);
        float[] edge = LightTrailRules.playerLineColor(mach);
        float[] core = LightTrailRules.playerCoreColor(mach);

        pose.pushPose();
        model.body.translateAndRotate(pose);
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);

        for (int side : new int[]{-1, 1}) {
            pose.pushPose();
            // Anchor right outside the open dorsal thruster pods
            pose.translate(side * 0.22F, 0.18F, 0.22F);
            // Sweep back along +Y (toward feet in horizontal cruise) and angle slightly rearward (+Z)
            pose.mulPose(new Quaternionf()
                .rotateY(side * -0.24F)
                .rotateZ(side * (0.18F + sweepRad * 0.65F)));

            Matrix4f m = pose.last().pose();
            // 1. Primary main phase blade (wide sleek angular vane)
            float mainLen = 1.28F * span;
            float mainChord = 0.34F * (0.65F + 0.35F * unfurl);
            drawBlade(vc, m, side, 0.0F, 0.0F, 0.0F, mainLen, mainChord, 0.22F * span, edge, core, alpha);

            // 2. Upper swept stabilizer fin (higher, sharper, faster shimmer)
            float upperPulse = 0.5F + 0.5F * Mth.sin(time * 3.1F + 1.1F);
            float upperAlpha = alpha * (0.72F + 0.18F * upperPulse);
            drawBlade(vc, m, side, 0.06F, -0.08F, 0.05F, 0.86F * span, 0.20F * unfurl, 0.12F * span, edge, core, upperAlpha);

            // 3. Lower trailing energy feather (tucked behind primary blade)
            float lowerPulse = 0.5F + 0.5F * Mth.sin(time * 2.8F + 2.4F);
            float lowerAlpha = alpha * (0.62F + 0.18F * lowerPulse);
            drawBlade(vc, m, side, 0.04F, 0.16F, 0.06F, 0.94F * span, 0.22F * unfurl, 0.32F * span, edge, core, lowerAlpha);

            pose.popPose();
        }
        pose.popPose();
    }

    /**
     * Draws one sleek 2-sided geometric energy fin blade + bright leading-edge spine in local wing coordinates.
     * +X*side is outward span, +Y is trailing aft, +Z is dorsal.
     */
    private static void drawBlade(VertexConsumer vc, Matrix4f m, int side,
                                  float ox, float oy, float oz,
                                  float len, float chord, float sweepY,
                                  float[] edge, float[] core, float alpha) {
        float x0 = side * ox;
        float xMid = side * (ox + len * 0.58F);
        float xTip = side * (ox + len);
        float yLead0 = oy;
        float yLeadMid = oy + sweepY * 0.42F;
        float yTip = oy + sweepY;
        float yTrail0 = oy + chord;
        float yTrailMid = oy + sweepY * 0.65F + chord * 0.68F;
        float z0 = oz;
        float zMid = oz + 0.03F;
        float zTip = oz + 0.06F;

        // Inner panel (root -> mid-span)
        quadBothSides(vc, m,
            x0, yLead0, z0, core[0], core[1], core[2], alpha * 0.92F,
            xMid, yLeadMid, zMid, edge[0], edge[1], edge[2], alpha * 0.72F,
            xMid, yTrailMid, zMid, edge[0], edge[1], edge[2], alpha * 0.22F,
            x0, yTrail0, z0, edge[0], edge[1], edge[2], alpha * 0.28F);

        // Outer tapered tip panel (mid-span -> sharp wingtip)
        quadBothSides(vc, m,
            xMid, yLeadMid, zMid, edge[0], edge[1], edge[2], alpha * 0.72F,
            xTip, yTip, zTip, core[0], core[1], core[2], alpha * 0.06F,
            xTip, yTip + 0.04F, zTip, edge[0], edge[1], edge[2], 0F,
            xMid, yTrailMid, zMid, edge[0], edge[1], edge[2], alpha * 0.22F);

        // Crisp glowing leading-edge spine rib
        float rib = 0.032F;
        quadBothSides(vc, m,
            x0, yLead0, z0, core[0], core[1], core[2], alpha,
            xTip, yTip, zTip, edge[0], edge[1], edge[2], alpha * 0.30F,
            xTip, yTip + rib * 0.5F, zTip, edge[0], edge[1], edge[2], 0F,
            x0, yLead0 + rib, z0, core[0], core[1], core[2], alpha * 0.15F);
    }

    /** Draws cyan-white reverse-thrust electromagnetic rings at both palms during high-speed Air-Brake Flare. */
    public static void drawBrakePalms(PoseStack pose, MultiBufferSource buffers, PlayerModel<?> model,
                                      AbstractClientPlayer player, float partial) {
        if (FxBudget.tier(player) == FxLodRules.FAR) return;
        float trans = ManeuverModeClient.transitionOf(player);
        float brake = ManeuverModeClient.brakeOf(player);
        float alpha = PhaseWingRules.brakeRingAlpha(brake) * trans;
        if (alpha <= 0.01F) return;

        float phase = ((player.tickCount + partial) * 0.22F) % 1.0F;
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);

        drawPalmRings(pose, vc, model.rightArm, -1, alpha, phase);
        drawPalmRings(pose, vc, model.leftArm, 1, alpha, (phase + 0.5F) % 1.0F);
    }

    private static void drawPalmRings(PoseStack pose, VertexConsumer vc, ModelPart arm, int side, float alpha, float phase) {
        pose.pushPose();
        arm.translateAndRotate(pose);
        // Bottom of arm (palm), pointing along +Y in arm-local space (which points forward when arm xRot ~ -1.35)
        pose.translate(side * 0.06F, 0.66F, 0.0F);
        Matrix4f m = pose.last().pose();

        // Inner bright white-cyan palm emitter ring
        ringXZ(vc, m, 0.02F, 0.09F, 0.17F, 0.85F, 0.98F, 1.0F, alpha);
        // Outer pulsing reverse-thrust shock ring pushing outward along +Y
        float rInner = 0.14F + 0.16F * phase;
        float rOuter = rInner + 0.07F;
        float yOff = 0.06F + 0.22F * phase;
        float pulseA = alpha * (1.0F - phase) * 0.78F;
        ringXZ(vc, m, yOff, rInner, rOuter, 0.24F, 0.91F, 1.0F, pulseA);
        pose.popPose();
    }

    private static void ringXZ(VertexConsumer vc, Matrix4f m, float y, float rIn, float rOut,
                               float r, float g, float b, float alpha) {
        int seg = 20;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            quadBothSides(vc, m,
                c0 * rIn, y, s0 * rIn, r, g, b, alpha,
                c1 * rIn, y, s1 * rIn, r, g, b, alpha,
                c1 * rOut, y, s1 * rOut, r, g, b, 0F,
                c0 * rOut, y, s0 * rOut, r, g, b, 0F);
        }
    }

    private static void quadBothSides(VertexConsumer vc, Matrix4f m,
                                      float x0, float y0, float z0, float r0, float g0, float b0, float a0,
                                      float x1, float y1, float z1, float r1, float g1, float b1, float a1,
                                      float x2, float y2, float z2, float r2, float g2, float b2, float a2,
                                      float x3, float y3, float z3, float r3, float g3, float b3, float a3) {
        vc.addVertex(m, x0, y0, z0).setColor(r0, g0, b0, a0);
        vc.addVertex(m, x1, y1, z1).setColor(r1, g1, b1, a1);
        vc.addVertex(m, x2, y2, z2).setColor(r2, g2, b2, a2);
        vc.addVertex(m, x3, y3, z3).setColor(r3, g3, b3, a3);

        vc.addVertex(m, x3, y3, z3).setColor(r3, g3, b3, a3);
        vc.addVertex(m, x2, y2, z2).setColor(r2, g2, b2, a2);
        vc.addVertex(m, x1, y1, z1).setColor(r1, g1, b1, a1);
        vc.addVertex(m, x0, y0, z0).setColor(r0, g0, b0, a0);
    }
}
