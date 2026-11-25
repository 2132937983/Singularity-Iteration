// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.render.FxBudget;
import com.miophas.singularity_iteration.common.client.render.FxLodRules;
import com.miophas.singularity_iteration.common.client.render.PhaseFx;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.suit.HoverFieldRules;
import com.miophas.singularity_iteration.common.suit.PhaseRules;
import com.miophas.singularity_iteration.common.suit.QuantumNozzles;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 0.1.11.126 (armour and flight overhaul E3): the electromagnetic hover of the quantum suit ({@link HoverFieldRules}): a thin
 * breathing ring of light flat under each quantum boot with a ripple running out of it on every stroke of the float (drawn
 * on the posed model by {@link ManeuverFlightLayer}), and a wide faint ripple over the ground within reach (a world pass,
 * also under the own camera in first person). It fades in and out with the hover ({@link QuantumWornMounts#hoverWeight}).
 * Nothing is thrown downwards. A graviton core keeps its own field rings at the soles instead of the sole rings.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class HoverField {
    private HoverField() {}

    private static double seconds(Player p, float partial) { return (p.tickCount + partial) / 20.0; }

    /** The sole rings of a hovering player, from the posed model (the layer pose). */
    static void drawSoles(PoseStack pose, MultiBufferSource buffers, HumanoidModel<?> model, Player player, float partial) {
        if (com.miophas.singularity_iteration.common.client.render.IrisCompat.isShadowPass()) return;
        float weight = QuantumWornMounts.hoverWeight(player, partial);
        if (weight <= 0.02F || FxBudget.tier(player) == FxLodRules.FAR) return;
        double t = seconds(player, partial);
        float phase = HoverFieldRules.phase(t), breath = HoverFieldRules.soleBreath(t);
        VertexConsumer vc = null;
        for (QuantumNozzles.Nozzle n : NozzleDraw.worn(player, partial)) {
            if (n.kind() != QuantumNozzles.Kind.LIFT || !n.name().startsWith("sole") || !NozzleDraw.push(pose, model, n)) continue;
            if (vc == null) vc = com.miophas.singularity_iteration.common.client.render.SiFxBuffers.glow(buffers);
            var m = pose.last().pose();
            PhaseFx.ringAt(vc, m, 0.01F, HoverFieldRules.SOLE_RADIUS, HoverFieldRules.SOLE_WIDTH, PhaseRules.CYAN,
                HoverFieldRules.SOLE_ALPHA * breath * weight);
            PhaseFx.ringAt(vc, m, 0.012F, HoverFieldRules.soleRippleRadius(phase), HoverFieldRules.SOLE_WIDTH * 0.8F, PhaseRules.CYAN,
                HoverFieldRules.soleRippleAlpha(phase) * weight);
            pose.popPose();
        }
    }

    /** The ground ripples under the hovering players. */
    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (!com.miophas.singularity_iteration.common.client.render.IrisCompat.isFxStage(event)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = null;
        for (Player p : mc.level.players()) {
            if (!ManeuverModeClient.inManeuver(p) || FxBudget.tier(p) == FxLodRules.FAR) continue;
            float weight = QuantumWornMounts.hoverWeight(p, partial);
            if (weight <= 0.02F) continue;
            Vec3 feet = p.getPosition(partial);
            BlockHitResult hit = mc.level.clip(new ClipContext(feet.add(0, 0.05, 0), feet.add(0, -HoverFieldRules.GROUND_RANGE, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, p));
            if (hit.getType() != HitResult.Type.BLOCK) continue;
            double height = feet.y - hit.getLocation().y;
            float phase = HoverFieldRules.phase(seconds(p, partial));
            float alpha = HoverFieldRules.groundAlpha(phase, height) * weight;
            if (alpha <= 0.01F) continue;
            if (vc == null) vc = com.miophas.singularity_iteration.common.client.render.SiFxBuffers.glow(buffers);
            Vec3 centre = new Vec3(feet.x, hit.getLocation().y + 0.03, feet.z).subtract(cam);
            PhaseFx.ring(vc, centre, new Vec3(0, 1, 0), HoverFieldRules.groundRadius(phase), HoverFieldRules.GROUND_WIDTH, PhaseRules.CYAN, alpha);
        }
        if (vc != null) com.miophas.singularity_iteration.common.client.render.SiFxBuffers.endGlow(buffers);
    }
}
