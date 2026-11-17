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
 * afterimages of the flyer (outlines of light fanned behind it). The star streams of the warp are {@link StarStreams}. Not in
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
     * The afterimages of the warp: up to three outlines of light the size of the lying flyer (in the warp it always flies
     * the cruise pose, along the flight), fanned to each side in turn, back and a little up, cyan and magenta in turn
     * ({@link WarpRules#ghost}); faded near the camera (filled boxes there read as big pale blocks).
     */
    private static void afterimages(VertexConsumer vc, Vec3 centre, Vec3 axis, float since) {
        Vec3 side = axis.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(axis).normalize();
        for (int i = 1; i <= 3; i++) {
            float a = WarpRules.ghost(i, since);
            if (a <= 0.01F) continue;
            Vec3 at = centre.subtract(axis.scale(1.6 * i)).add(side.scale((i % 2 == 1 ? 1 : -1) * (1.0 + 0.35 * i))).add(up.scale(0.2 * i));
            a *= PhaseRules.streamNear((float) at.length());
            if (a <= 0.01F) continue;
            float[] c = i % 2 == 1 ? PhaseRules.CYAN : PhaseRules.MAGENTA;
            outline(vc, at, axis.scale(0.9), side.scale(0.28), up.scale(0.18), c, a);
            outline(vc, at.add(axis.scale(1.08)), axis.scale(0.18), side.scale(0.22), up.scale(0.22), c, a);   // the helmet
            PhaseFx.blob(vc, at, 0.5F, c, a * 0.35F);
        }
    }

    /** The twelve edges of a box about {@code at} with half-axes {@code a}, {@code b}, {@code c} (camera-relative), as lines of light. */
    private static void outline(VertexConsumer vc, Vec3 at, Vec3 a, Vec3 b, Vec3 c, float[] rgb, float alpha) {
        Vec3[] ax = {a, b, c};
        for (int f = 0; f < 3; f++) {
            Vec3 n = ax[f], u = ax[(f + 1) % 3], v = ax[(f + 2) % 3];
            for (int su = -1; su <= 1; su += 2) for (int sv = -1; sv <= 1; sv += 2) {
                Vec3 mid = at.add(u.scale(su)).add(v.scale(sv));
                line(vc, mid.subtract(n), mid.add(n), 0.025F, rgb, alpha);
            }
        }
    }

    /** A camera-facing line of light from {@code p} to {@code q} (camera-relative). */
    private static void line(VertexConsumer vc, Vec3 p, Vec3 q, float width, float[] rgb, float alpha) {
        Vec3 side = q.subtract(p).cross(p);
        if (side.lengthSqr() < 1.0E-10) return;
        side = side.normalize().scale(width / 2);
        Matrix4f id = new Matrix4f();
        float[] core = PhaseRules.mix(rgb, PhaseRules.WHITE, 0.4F);
        FxDraw.vertex(vc, id, p.add(side), core[0], core[1], core[2], alpha);
        FxDraw.vertex(vc, id, p.subtract(side), core[0], core[1], core[2], alpha);
        FxDraw.vertex(vc, id, q.subtract(side), core[0], core[1], core[2], alpha);
        FxDraw.vertex(vc, id, q.add(side), core[0], core[1], core[2], alpha);
    }
}
