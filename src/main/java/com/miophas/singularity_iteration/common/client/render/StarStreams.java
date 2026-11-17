// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.client.suit.ManeuverModeClient;
import com.miophas.singularity_iteration.common.suit.PhaseRules;
import com.mojang.blaze3d.vertex.PoseStack;
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
 * 0.1.11.71 (phase warp style phase W1): the star streams of a fast flyer, in place of the speed bars. From 0.9 blocks per
 * tick, {@link PhaseRules#STREAMS} streaks of light run in a tube round the flight ({@link PhaseRules#STREAM_INNER} to
 * {@link PhaseRules#STREAM_OUTER} blocks out, never through the body) and slide back past the flyer; they grow with the
 * speed, a white-cyan head and a tail that shifts to violet and magenta as the flight gets fast. A world pass, so they
 * also stand round the own camera in first person (a tunnel of light). Not in the classic style. 0.1.11.72 (W2): the gather
 * before a warp pulls them in, shortens and dims them; the warp stretches and heats them ({@link
 * com.miophas.singularity_iteration.common.suit.WarpRules#streamBoost}).
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class StarStreams {
    private StarStreams() {}

    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || FcsClientConfig.classicFlightFx()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = null;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        Matrix4f m = pose.last().pose();
        for (Player p : mc.level.players()) {
            if (p.distanceToSqr(cam) > 64 * 64 || !ManeuverModeClient.inManeuver(p)) continue;
            Vec3 flow = ManeuverModeClient.flow(p);
            float speed = (float) flow.length(), alpha = PhaseRules.streamAlpha(speed);
            // 0.1.11.72 (W2): the gather pulls the streams in and dims them; the warp stretches them and heats them
            float gather = ManeuverModeClient.warpGather(p, partial);
            float boost = com.miophas.singularity_iteration.common.suit.WarpRules.streamBoost(ManeuverModeClient.warpSince(p, partial));
            alpha = Math.min(1F, alpha * boost * (1 - 0.4F * gather));
            if (alpha <= 0.01F) continue;
            if (vc == null) vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
            boolean own = p == mc.player && mc.options.getCameraType().isFirstPerson();
            // round the own first-person camera the tube is twice as wide (it would brush the eye)
            draw(vc, m, p, flow, speed, alpha * (own ? 0.7F : 0.85F), own ? 2F : 1F, gather, boost, partial, cam);
        }
        pose.popPose();
        if (vc != null) buffers.endBatch(SiRenderTypes.FIELD_GLOW);
    }

    private static void draw(VertexConsumer vc, Matrix4f m, net.minecraft.world.entity.Entity p, Vec3 flow, float speed, float alpha, float widen, float gather, float boost,
                             float partial, Vec3 cam) {
        Vec3 axis = flow.normalize(), u = FxDraw.perp(axis).normalize(), v = axis.cross(u).normalize();
        Vec3 centre = p.getPosition(partial).add(0, p.getBbHeight() * 0.5, 0).subtract(cam);
        float seconds = (p.tickCount + partial) / 20F, len = PhaseRules.streamLength(speed) * boost * (1 - 0.6F * gather);
        float[][] colours = PhaseRules.streamColours(speed * boost);
        widen *= 1 - 0.45F * gather;
        long seed = p.getId();
        for (int i = 0; i < PhaseRules.STREAMS; i++) {
            float place = PhaseRules.streamPlace(i, seed, seconds, speed);
            float[] slot = PhaseRules.streamSlot(i, seed);
            Vec3 radial = u.scale(Math.cos(slot[0]) * slot[1] * widen).add(v.scale(Math.sin(slot[0]) * slot[1] * widen));
            // ahead of the flyer at 0, behind it at 1; fading in and out at the ends of the run
            // round the own first-person camera the run starts further ahead, so the streams come at the eye from in front
            Vec3 head = centre.add(radial).add(axis.scale(PhaseRules.STREAM_SPAN * (0.45 - place) + (widen > 1 ? 7 : 0)));
            Vec3 tail = head.subtract(axis.scale(len));
            float a = alpha * (float) Math.sin(Math.PI * place) * (0.55F + 0.45F * (i % 3) / 2F) * PhaseRules.streamNear((float) head.add(tail).scale(0.5).length());
            streak(vc, m, head, tail, 0.03F + 0.015F * (i % 3), colours[0], colours[1], a);
        }
    }

    /** 0.1.11.74 (W4): the star streams of any flyer (a Wingman) moving by {@code flow} blocks per tick, no warp. */
    public static void drawFor(VertexConsumer vc, net.minecraft.world.entity.Entity e, Vec3 flow, float partial, Vec3 cam) {
        float speed = (float) flow.length(), alpha = PhaseRules.streamAlpha(speed);
        if (alpha > 0.01F) draw(vc, new Matrix4f(), e, flow, speed, alpha * 0.8F, 1F, 0F, 1F, partial, cam);
    }

    /** A camera-facing ribbon from {@code head} (bright) to {@code tail} (gone), camera-relative points. */
    public static void streak(VertexConsumer vc, Matrix4f m, Vec3 head, Vec3 tail, float width, float[] headRgb, float[] tailRgb, float alpha) {
        if (alpha <= 0.01F) return;
        Vec3 d = tail.subtract(head);
        Vec3 side = d.cross(head);
        if (side.lengthSqr() < 1.0E-10) return;
        side = side.normalize().scale(width / 2);
        FxDraw.vertex(vc, m, head.add(side), headRgb[0], headRgb[1], headRgb[2], alpha);
        FxDraw.vertex(vc, m, head.subtract(side), headRgb[0], headRgb[1], headRgb[2], alpha);
        FxDraw.vertex(vc, m, tail.subtract(side.scale(0.3)), tailRgb[0], tailRgb[1], tailRgb[2], 0F);
        FxDraw.vertex(vc, m, tail.add(side.scale(0.3)), tailRgb[0], tailRgb[1], tailRgb[2], 0F);
    }
}
