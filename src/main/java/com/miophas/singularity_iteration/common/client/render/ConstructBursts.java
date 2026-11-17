// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.suit.FlightImpact;
import com.mojang.blaze3d.vertex.PoseStack;
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
 * 0.1.11.63 (hit feel and constructivist FX phase Q2): the constructivist bursts: flat rings that grow and are cut, in
 * place of dust, sparks, vapour and shock shells. A take-off is a small white ring on the ground, a landing a larger one
 * with a red ring inside, the sound barrier a large white ring across the flight with a black one behind it. Drawn after
 * the particles into {@link SiRenderTypes#FLAT}.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class ConstructBursts {
    private ConstructBursts() {}

    private record Burst(Vec3 at, Vec3 normal, float from, float to, float width, long start, int life, float[] outer, float[] inner, int follow) { }

    private static final List<Burst> BURSTS = new ArrayList<>();

    private static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    private static void add(Burst b) {
        BURSTS.add(b);
        if (BURSTS.size() > 32) BURSTS.remove(0);
    }

    /** A take-off ({@code takeoff}) or a landing at {@code at} on the ground, {@code strength} 0..1. */
    public static void ground(Vec3 at, float strength, boolean takeoff) {
        float k = 0.5F + 0.5F * Math.min(1, Math.max(0, strength));
        add(new Burst(at.add(0, 0.04, 0), new Vec3(0, 1, 0), 0.3F, (takeoff ? 1.4F : 2.4F) * k, takeoff ? 0.12F : 0.18F, now(), takeoff ? 5 : 7,
            ConstructFx.WHITE, takeoff ? null : ConstructFx.RED, -1));
    }

    private record Slate(Vec3 at, long start) { }

    private static final List<Slate> SLATES = new ArrayList<>();
    /** Ticks a Clean Slate burst stands before the hard cut. */
    public static final int SLATE_LIFE = 12;

    /**
     * 0.1.11.64 (hit feel and constructivist FX phase Q3): one Wingman of the Clean Slate protocol bursts at {@code at}: a
     * white ring with a red one inside grows facing the viewer, eight red wedges are thrown out across it, a white pillar
     * stands through it; all is cut after {@link #SLATE_LIFE} ticks.
     */
    public static void cleanSlate(Vec3 at) {
        SLATES.add(new Slate(at, now()));
        if (SLATES.size() > 16) SLATES.remove(0);
    }

    /**
     * The sound barrier at {@code centre}, across the flight {@code axis}. The ring rides with the flyer {@code entityId}
     * (it would be behind a chase camera a few ticks later: past the barrier a flyer covers 2.4 blocks a tick).
     */
    public static void boom(Vec3 centre, Vec3 axis, int entityId) {
        add(new Burst(centre, axis, 0.8F, 6.5F, 0.35F, now(), 9, ConstructFx.WHITE, ConstructFx.BLACK, entityId));
    }

    /** 0.1.11.68 (R2): a star of hard wedges lying in a surface (cracks, a blast print), {@link FlightImpact#cracks}. */
    private record Star(Vec3 at, Vec3 normal, float[][] rays, long start, int life, float[] core) { }

    private static final List<Star> STARS = new ArrayList<>();

    private static void add(Star s) {
        STARS.add(s);
        if (STARS.size() > 16) STARS.remove(0);
    }

    /**
     * 0.1.11.68 (quantum flight phase R2): a hard landing at {@code at}, {@code strength} 0..1: the ground cracks in a star of
     * black wedges with red cores, standing 24 ticks before the cut, and a heavy double ring (white, red inside).
     */
    public static void hardLanding(Vec3 at, float strength) {
        Vec3 ground = at.add(0, 0.03, 0);
        long seed = (long) Math.floor(at.x * 7) * 31 + (long) Math.floor(at.z * 7) * 17 + now();
        add(new Star(ground, new Vec3(0, 1, 0), FlightImpact.cracks(seed, strength), now(), 24, ConstructFx.RED));
        float k = 0.6F + 0.4F * Math.min(1, Math.max(0, strength));
        add(new Burst(ground.add(0, 0.01, 0), new Vec3(0, 1, 0), 0.5F, 4.2F * k, 0.32F, now(), 8, ConstructFx.WHITE, ConstructFx.RED, -1));
    }

    /**
     * 0.1.11.68: the take-off burst at {@code at}: a short star of white wedges blasted into the ground under the soles, cut
     * after 7 ticks (the small take-off ring of {@link #ground} goes with it).
     */
    public static void takeoffBurst(Vec3 at) {
        float[][] rays = FlightImpact.cracks((long) Math.floor(at.x * 5) * 13 + now(), 0F);
        for (float[] r : rays) r[1] *= 0.75F;
        add(new Star(at.add(0, 0.03, 0), new Vec3(0, 1, 0), rays, now(), 7, ConstructFx.WHITE));
    }

    /**
     * 0.1.11.68: a high-speed wall hit at {@code at} on a wall facing {@code normal}: cracks in the wall and a large ring
     * standing out of it.
     */
    public static void wallHit(Vec3 at, Vec3 normal, float strength) {
        Vec3 n = normal.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : normal.normalize();
        long seed = (long) Math.floor(at.x * 7) * 31 + (long) Math.floor(at.y * 7) * 7 + now();
        add(new Star(at.add(n.scale(0.03)), n, FlightImpact.cracks(seed, strength), now(), 22, ConstructFx.RED));
        add(new Burst(at.add(n.scale(0.05)), n, 0.6F, 3.6F * (0.6F + 0.4F * strength), 0.36F, now(), 8, ConstructFx.WHITE, ConstructFx.BLACK, -1));
    }

    /** One wedge of a star: from {@code r0} to {@code r1} along {@code dir} in the plane of {@code normal}. */
    private static void rayWedge(VertexConsumer vc, Matrix4f m, Vec3 at, Vec3 dir, Vec3 normal, float r0, float r1, float width, float[] c) {
        Vec3 side = dir.cross(normal).normalize().scale(width / 2);
        Vec3 base = at.add(dir.scale(r0)), tip = at.add(dir.scale(r1));
        FxDraw.vertex(vc, m, base.add(side), c[0], c[1], c[2], 1F);
        FxDraw.vertex(vc, m, base.subtract(side), c[0], c[1], c[2], 1F);
        FxDraw.vertex(vc, m, tip, c[0], c[1], c[2], 1F);
        FxDraw.vertex(vc, m, tip, c[0], c[1], c[2], 1F);
    }

    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || BURSTS.isEmpty() && SLATES.isEmpty() && STARS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float t = now() + partial;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FLAT);
        for (Iterator<Burst> it = BURSTS.iterator(); it.hasNext(); ) {
            Burst b = it.next();
            float age = (t - b.start) / b.life;
            if (age >= 1 || age < -0.5F) { it.remove(); continue; }
            float k = Math.max(0, age);
            float r = b.from + (b.to - b.from) * (1 - (1 - k) * (1 - k));   // fast out, then slower: a hard shape
            Vec3 at = b.at;
            if (b.follow >= 0 && mc.level != null && mc.level.getEntity(b.follow) instanceof net.minecraft.world.entity.Entity e)
                at = e.getPosition(partial).add(0, e.getBbHeight() * 0.5, 0).subtract(b.normal.normalize().scale(0.6));
            if (b.inner != null) ConstructFx.ring(vc, m, at, b.normal, r * 0.72F, b.width * 0.7F, b.inner, 1F, 40);
            ConstructFx.ring(vc, m, at, b.normal, r, b.width, b.outer, 1F, 48);
        }
        for (Iterator<Star> it = STARS.iterator(); it.hasNext(); ) {
            Star st = it.next();
            float age = (t - st.start) / st.life;
            if (age >= 1 || age < -0.5F) { it.remove(); continue; }
            // out in two ticks, then held, then the hard cut
            float out = Math.min(1F, Math.max(0F, (t - st.start) / 2F));
            Vec3 n = st.normal.normalize(), u = FxDraw.perp(n).normalize(), w = n.cross(u).normalize();
            for (float[] ray : st.rays) {
                Vec3 dir = u.scale(Math.cos(ray[0])).add(w.scale(Math.sin(ray[0])));
                float len = ray[1] * out, width = 0.16F + 0.05F * ray[1];
                rayWedge(vc, m, st.at, dir, n, 0.2F, 0.2F + len * 1.04F + 0.03F, width * 1.25F, ConstructFx.BLACK);
                rayWedge(vc, m, st.at.add(n.scale(0.004)), dir, n, 0.22F, 0.2F + len, width, st.core);
                rayWedge(vc, m, st.at.add(n.scale(0.008)), dir, n, 0.24F, 0.2F + len * 0.45F, width * 0.4F, ConstructFx.WHITE);
            }
        }
        for (Iterator<Slate> it = SLATES.iterator(); it.hasNext(); ) {
            Slate sl = it.next();
            float age = (t - sl.start) / SLATE_LIFE;
            if (age >= 1 || age < -0.5F) { it.remove(); continue; }
            float k = Math.max(0, age), out = 1 - (1 - k) * (1 - k);
            Vec3 view = sl.at.subtract(cam);
            Vec3 toward = view.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : view.normalize();
            // the pillar first (behind the rest): a black edge and a white core, thinning out
            Vec3 bottom = sl.at.add(0, -10, 0), top = sl.at.add(0, 16, 0);
            ConstructFx.bar(vc, m, bottom, top, 0.55F * (1 - k) + 0.08F, cam, ConstructFx.BLACK, 1F);
            ConstructFx.bar(vc, m, bottom, top, 0.38F * (1 - k) + 0.04F, cam, ConstructFx.WHITE, 1F);
            // eight wedges thrown out in the view plane, on a diagonal grid (a constructivist star)
            Vec3 u = FxDraw.perp(toward).normalize(), w = toward.cross(u).normalize();
            for (int i = 0; i < 8; i++) {
                double a = Math.PI / 8 + i * Math.PI / 4;
                Vec3 dir = u.scale(Math.cos(a)).add(w.scale(Math.sin(a)));
                float reach = (i % 2 == 0 ? 4.8F : 3.2F) * out;
                ConstructFx.thrust(vc, m, sl.at.add(dir.scale(0.5 + reach * 0.35)), dir, 1.2F + reach * 0.6F, 0.7F, cam, 1F);
            }
            // the rings, facing the viewer
            ConstructFx.ring(vc, m, sl.at, toward, (0.6F + 5.2F * out) * 0.7F, 0.32F, ConstructFx.RED, 1F, 48);
            ConstructFx.ring(vc, m, sl.at, toward, 0.6F + 5.2F * out, 0.45F, ConstructFx.WHITE, 1F, 56);
        }
        buffers.endBatch(SiRenderTypes.FLAT);
        pose.popPose();
    }
}
