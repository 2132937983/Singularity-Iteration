// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.suit.FlightImpact;
import com.miophas.singularity_iteration.common.suit.PhaseRules;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * 0.1.11.74 (phase warp style phase W4): the bursts of the phase warp style, in light (additive, a world pass), in place
 * of the constructivist bursts. Every burst comes up in two ticks and eases out ({@link PhaseRules#burstFade}).
 * <ul>
 *   <li>{@link #ground}: a take-off or a landing: a ring of light on the ground and a flash.</li>
 *   <li>{@link #hardLanding}: fissures of light in the ground (white near the centre, cyan out), two rippling rings, a
 *       flash and motes of light rising.</li>
 *   <li>{@link #takeoffBurst}: short rays of light blasted into the ground under the soles.</li>
 *   <li>{@link #wallHit}: fissures in the wall, a ring standing out of it and a flash.</li>
 *   <li>{@link #cleanSlate}: a Wingman of the Clean Slate protocol bursts: a white flash, a cyan ring and a violet one
 *       growing towards the viewer, eight rays of light and a pillar of light through it.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class PhaseBursts {
    private PhaseBursts() {}

    private enum Kind { GROUND, TAKEOFF_RING, HARD_LANDING, TAKEOFF_RAYS, WALL, CLEAN_SLATE }

    private record Burst(Kind kind, Vec3 at, Vec3 normal, float strength, long start, int life, long seed) { }

    private static final List<Burst> BURSTS = new ArrayList<>();

    private static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    private static void add(Kind kind, Vec3 at, Vec3 normal, float strength, int life) {
        long seed = (long) Math.floor(at.x * 7) * 31 + (long) Math.floor(at.y * 7) * 7 + (long) Math.floor(at.z * 7) * 17 + now();
        BURSTS.add(new Burst(kind, at, normal, Math.max(0F, Math.min(1F, strength)), now(), life, seed));
        if (BURSTS.size() > 32) BURSTS.remove(0);
    }

    /** A take-off ({@code takeoff}) or a landing at {@code at} on the ground, {@code strength} 0..1. */
    public static void ground(Vec3 at, float strength, boolean takeoff) {
        add(takeoff ? Kind.TAKEOFF_RING : Kind.GROUND, at.add(0, 0.04, 0), new Vec3(0, 1, 0), strength, takeoff ? 8 : 12);
    }

    /** A hard landing at {@code at}, {@code strength} 0..1. */
    public static void hardLanding(Vec3 at, float strength) {
        add(Kind.HARD_LANDING, at.add(0, 0.03, 0), new Vec3(0, 1, 0), strength, 34);
    }

    /** The take-off burst under the soles at {@code at}. */
    public static void takeoffBurst(Vec3 at) {
        add(Kind.TAKEOFF_RAYS, at.add(0, 0.03, 0), new Vec3(0, 1, 0), 0F, 10);
    }

    /** A high-speed wall hit at {@code at} on a wall facing {@code normal}. */
    public static void wallHit(Vec3 at, Vec3 normal, float strength) {
        Vec3 n = normal.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : normal.normalize();
        add(Kind.WALL, at.add(n.scale(0.03)), n, strength, 30);
    }

    /** One Wingman of the Clean Slate protocol bursts at {@code at}. */
    public static void cleanSlate(Vec3 at) {
        add(Kind.CLEAN_SLATE, at, new Vec3(0, 1, 0), 1F, 20);
    }

    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || BURSTS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float t = now() + partial;
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        for (Iterator<Burst> it = BURSTS.iterator(); it.hasNext(); ) {
            Burst b = it.next();
            float age = t - b.start;
            if (age >= b.life || age < -1) { it.remove(); continue; }
            float fade = PhaseRules.burstFade(age, b.life), k = Math.max(0F, age) / b.life;
            Vec3 at = b.at.subtract(cam);
            switch (b.kind) {
                case GROUND, TAKEOFF_RING -> {
                    float r = (b.kind == Kind.GROUND ? 2.2F : 1.4F) * (0.5F + 0.5F * b.strength) * (float) (1 - Math.pow(1 - k, 3)) + 0.2F;
                    PhaseFx.ring(vc, at, b.normal, r, 0.05F, PhaseRules.CYAN, fade);
                    if (age < 4) PhaseFx.blob(vc, at.add(0, 0.2, 0), 0.6F, PhaseRules.CYAN, (1 - age / 4F) * 0.8F);
                }
                case HARD_LANDING -> {
                    fissures(vc, at, b.normal, b.seed, b.strength, Math.min(1F, age / 3F), fade);
                    for (int i = 0; i < 2; i++) {
                        float q = (age - i * 4) / 24F;
                        if (q <= 0 || q >= 1) continue;
                        float r = 0.4F + (2.4F + 2.6F * b.strength) * (float) (1 - Math.pow(1 - q, 3));
                        PhaseFx.ring(vc, at, b.normal, r, 0.06F, PhaseRules.mix(PhaseRules.CYAN, PhaseRules.VIOLET, q), (1 - q) * 0.9F);
                    }
                    if (age < 6) PhaseFx.blob(vc, at.add(0, 0.3, 0), 1.6F * (1 - age / 6F) + 0.4F, PhaseRules.CYAN, 1 - age / 6F);
                    motes(vc, at, b.seed, age, fade);
                }
                case TAKEOFF_RAYS -> {
                    float[][] rays = FlightImpact.cracks(b.seed, 0F);
                    Vec3 u = FxDraw.perp(b.normal).normalize(), w = b.normal.cross(u).normalize();
                    for (float[] r : rays) {
                        Vec3 d = u.scale(Math.cos(r[0])).add(w.scale(Math.sin(r[0])));
                        StarStreams.streak(vc, new Matrix4f(), at.add(d.scale(0.25)), at.add(d.scale(0.25 + r[1] * 0.8 * Math.min(1F, age / 2F))),
                            0.06F, PhaseRules.WHITE, PhaseRules.CYAN, fade);
                    }
                }
                case WALL -> {
                    fissures(vc, at, b.normal, b.seed, b.strength, Math.min(1F, age / 3F), fade);
                    float r = 0.6F + (2.4F + 1.4F * b.strength) * (float) (1 - Math.pow(1 - k, 3));
                    PhaseFx.ring(vc, at.add(b.normal.scale(0.05)), b.normal, r, 0.07F, PhaseRules.CYAN, fade);
                    if (age < 5) PhaseFx.blob(vc, at.add(b.normal.scale(0.3)), 1.4F * (1 - age / 5F) + 0.4F, PhaseRules.WHITE, 1 - age / 5F);
                }
                case CLEAN_SLATE -> cleanSlate(vc, at, age, b.life, fade);
            }
        }
        buffers.endBatch(SiRenderTypes.FIELD_GLOW);
    }

    /** Fissures of light in the plane of {@code normal}: white near the centre, cyan out, grown to {@code grow}. */
    private static void fissures(VertexConsumer vc, Vec3 at, Vec3 normal, long seed, float strength, float grow, float fade) {
        Vec3 u = FxDraw.perp(normal).normalize(), w = normal.cross(u).normalize();
        float[][] cracks = FlightImpact.cracks(seed, strength);
        Matrix4f id = new Matrix4f();
        for (int i = 0; i < cracks.length; i++) {
            float[][] pts = PhaseRules.fissure(seed, i, cracks[i][0], cracks[i][1], 4);
            int n = pts.length - 1;
            float upto = grow * n;
            for (int j = 0; j < n && j < upto; j++) {
                float g = Math.min(1F, upto - j);
                Vec3 p0 = at.add(u.scale(pts[j][0])).add(w.scale(pts[j][1]));
                Vec3 p1 = at.add(u.scale(pts[j][0] + (pts[j + 1][0] - pts[j][0]) * g)).add(w.scale(pts[j][1] + (pts[j + 1][1] - pts[j][1]) * g));
                float[] c = j < 2 ? PhaseRules.WHITE : PhaseRules.CYAN;
                StarStreams.streak(vc, id, p0, p1, 0.09F - 0.015F * j, c, PhaseRules.CYAN, fade);
                StarStreams.streak(vc, id, p0, p1, 0.3F - 0.05F * j, PhaseRules.CYAN, PhaseRules.VIOLET, fade * 0.25F);   // the glow round it
            }
        }
    }

    /** Motes of light rising from a hard landing. */
    private static void motes(VertexConsumer vc, Vec3 at, long seed, float age, float fade) {
        for (int i = 0; i < 18; i++) {
            double a = PhaseRules.streamSlot(i, seed)[0], d = 0.4 + 2.6 * (PhaseRules.streamSlot(i, seed + 1)[1] - PhaseRules.STREAM_INNER)
                / (PhaseRules.STREAM_OUTER - PhaseRules.STREAM_INNER);
            float rise = age * (0.03F + 0.03F * (i % 3));
            Vec3 p = at.add(Math.cos(a) * d, 0.1 + rise, Math.sin(a) * d);
            PhaseFx.blob(vc, p, 0.07F + 0.03F * (i % 2), i % 2 == 0 ? PhaseRules.CYAN : PhaseRules.VIOLET, fade * 0.9F);
        }
    }

    private static void cleanSlate(VertexConsumer vc, Vec3 at, float age, int life, float fade) {
        float out = (float) (1 - Math.pow(1 - Math.min(1F, age / life), 3));
        Vec3 toward = at.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : at.normalize();
        if (age < 4) PhaseFx.blob(vc, at, 4F * (1 - age / 4F) + 1F, PhaseRules.WHITE, 1 - age / 4F);
        PhaseFx.ring(vc, at, toward, 0.6F + 5.6F * out, 0.18F, PhaseRules.CYAN, fade);
        PhaseFx.ring(vc, at, toward, (0.6F + 5.6F * out) * 0.7F, 0.12F, PhaseRules.VIOLET, fade);
        Vec3 u = FxDraw.perp(toward).normalize(), w = toward.cross(u).normalize();
        Matrix4f id = new Matrix4f();
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 8 + i * Math.PI / 4;
            Vec3 dir = u.scale(Math.cos(a)).add(w.scale(Math.sin(a)));
            float reach = (i % 2 == 0 ? 6F : 4F) * out;
            StarStreams.streak(vc, id, at.add(dir.scale(0.4)), at.add(dir.scale(0.4 + reach)), 0.16F, PhaseRules.WHITE, PhaseRules.CYAN, fade);
        }
        StarStreams.streak(vc, id, at, at.add(0, 16, 0), 0.4F * (1 - out) + 0.08F, PhaseRules.WHITE, PhaseRules.CYAN, fade);
        StarStreams.streak(vc, id, at, at.add(0, -10, 0), 0.4F * (1 - out) + 0.08F, PhaseRules.WHITE, PhaseRules.CYAN, fade);
    }
}
