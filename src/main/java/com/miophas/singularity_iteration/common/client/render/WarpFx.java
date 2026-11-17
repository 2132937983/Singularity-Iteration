// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient;
import com.miophas.singularity_iteration.common.suit.PhaseRules;
import com.miophas.singularity_iteration.common.suit.WarpRules;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * 0.1.11.72 (phase warp style phase W2): the phase warp at the sound barrier, in light (a world pass; the timeline is
 * {@link WarpRules}). The gather: a violet ring closes on the flyer across its flight and a violet glow grows on it. The
 * flash: a white burst, a gate of light growing round the flyer and a thin line of light across the flight (not round the
 * own first-person camera, where it would cut the view in two). The warp: rings of rippled space left behind and
 * afterimages of the flyer (slabs of light fanned behind it). The star streams of the warp are {@link StarStreams}. Not in
 * the classic style.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class WarpFx {
    private WarpFx() {}

    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || FcsClientConfig.classicFlightFx()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = null;
        for (Player p : mc.level.players()) {
            if (p.distanceToSqr(cam) > 128 * 128) continue;
            float gather = ManeuverModeClient.warpGather(p, partial), since = ManeuverModeClient.warpSince(p, partial);
            if (gather <= 0.01F && WarpRules.phase(since) == WarpRules.Phase.NONE && since > 6) continue;
            if (vc == null) vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
            boolean own = p == mc.player && mc.options.getCameraType().isFirstPerson();
            draw(vc, p, gather, since, own, partial, cam);
        }
        if (vc != null) buffers.endBatch(SiRenderTypes.FIELD_GLOW);
    }

    private static void draw(VertexConsumer vc, Player p, float gather, float since, boolean own, float partial, Vec3 cam) {
        Vec3 flow = ManeuverModeClient.flow(p);
        Vec3 axis = flow.lengthSqr() > 1.0E-4 ? flow.normalize() : p.getViewVector(partial);
        Vec3 centre = p.getPosition(partial).add(0, p.getBbHeight() * 0.5, 0).subtract(cam);
        if (gather > 0.01F) {
            PhaseFx.ring(vc, centre.add(axis.scale(0.6)), axis, 4F * (1 - gather) + 0.9F, 0.06F, PhaseRules.VIOLET, 0.35F + 0.65F * gather);
            if (!own) PhaseFx.blob(vc, centre, 0.5F + 0.9F * gather, PhaseRules.VIOLET, 0.3F + 0.4F * gather);
        }
        WarpRules.Phase phase = WarpRules.phase(since);
        if (phase == WarpRules.Phase.FLASH) {
            float k = WarpRules.progress(since);
            PhaseFx.blob(vc, centre.add(axis.scale(own ? 2.5 : 0)), (own ? 2.2F : 3.2F) * (1 - 0.4F * k), PhaseRules.CYAN, 1 - 0.5F * k);
            if (!own) {
                // the line of light across the flight, square to the view
                Vec3 side = axis.cross(centre);
                if (side.lengthSqr() > 1.0E-6) {
                    side = side.normalize().scale(7);
                    Matrix4f id = new Matrix4f();
                    StarStreams.streak(vc, id, centre, centre.add(side), 0.06F, PhaseRules.WHITE, PhaseRules.CYAN, 1 - k);
                    StarStreams.streak(vc, id, centre, centre.subtract(side), 0.06F, PhaseRules.WHITE, PhaseRules.CYAN, 1 - k);
                }
            }
        }
        float[] gate = WarpRules.gate(since);
        if (gate[1] > 0.01F) PhaseFx.ring(vc, centre.add(axis.scale(0.3)), axis, gate[0], 0.08F, PhaseRules.WHITE, gate[1]);
        if (!own) afterimages(vc, centre, axis, since);
        if (phase == WarpRules.Phase.WARP || phase == WarpRules.Phase.DROP) {
            for (int j = 0; j < WarpRules.RIPPLES; j++) {
                float[] r = WarpRules.ripple(j, since);
                if (r == null) continue;
                float q = 1 - r[2];
                PhaseFx.ring(vc, centre.subtract(axis.scale(r[0])), axis, r[1], 0.05F, PhaseRules.mix(PhaseRules.CYAN, PhaseRules.VIOLET, q), r[2] * 0.8F);
            }
        }
    }

    /**
     * The afterimages of the warp: up to three slabs of light the size of the lying flyer (in the warp it always flies the
     * cruise pose, along the flight), fanned to each side in turn, a little back and up, cyan and magenta in turn
     * ({@link WarpRules#ghost}).
     */
    private static void afterimages(VertexConsumer vc, Vec3 centre, Vec3 axis, float since) {
        Vec3 side = axis.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(axis).normalize();
        for (int i = 1; i <= 3; i++) {
            float a = WarpRules.ghost(i, since);
            if (a <= 0.01F) continue;
            Vec3 at = centre.subtract(axis.scale(1.4 * i)).add(side.scale((i % 2 == 1 ? 1 : -1) * (0.7 + 0.2 * i))).add(up.scale(0.15 * i));
            float[] c = i % 2 == 1 ? PhaseRules.CYAN : PhaseRules.MAGENTA;
            slab(vc, at, axis.scale(0.95), side.scale(0.3), up.scale(0.2), c, a * 0.6F);
            slab(vc, at.add(axis.scale(0.95)), axis.scale(0.22), side.scale(0.24), up.scale(0.24), c, a * 0.7F);   // the helmet
        }
    }

    /** A box of light about {@code at} with half-axes {@code a}, {@code b}, {@code c} (camera-relative), faces additive. */
    private static void slab(VertexConsumer vc, Vec3 at, Vec3 a, Vec3 b, Vec3 c, float[] rgb, float alpha) {
        Matrix4f id = new Matrix4f();
        Vec3[] axes = {a, b, c};
        for (int f = 0; f < 3; f++) {
            Vec3 n = axes[f], u = axes[(f + 1) % 3], v = axes[(f + 2) % 3];
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                Vec3 o = at.add(n.scale(sgn));
                FxDraw.vertex(vc, id, o.add(u).add(v), rgb[0], rgb[1], rgb[2], alpha);
                FxDraw.vertex(vc, id, o.add(u).subtract(v), rgb[0], rgb[1], rgb[2], alpha);
                FxDraw.vertex(vc, id, o.subtract(u).subtract(v), rgb[0], rgb[1], rgb[2], alpha);
                FxDraw.vertex(vc, id, o.subtract(u).add(v), rgb[0], rgb[1], rgb[2], alpha);
            }
        }
    }
}
