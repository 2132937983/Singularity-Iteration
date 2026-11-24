// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.armory.WingmanEntity;
import com.miophas.singularity_iteration.common.client.render.FxBudget;
import com.miophas.singularity_iteration.common.client.render.FxLodRules;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.suit.LightTrail;
import com.miophas.singularity_iteration.common.suit.LightTrailRules;
import com.miophas.singularity_iteration.common.suit.ManeuverMode;
import com.miophas.singularity_iteration.common.suit.QuantumNozzles;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 0.1.11.122: the light trails of the special flight ({@link LightTrailRules}): a short red line with a curtain of red light
 * hanging under it, one behind each back nozzle of a quantum flyer in maneuver flight and of a flying Wingman, growing with
 * the speed. A player's nozzles are sampled where its model is drawn ({@link ManeuverFlightLayer}), so the trail leaves the
 * real ports (0.1.11.123: since the finished quantum suit, the two pods' thruster_nozzle_left/right, which move as the pods open); a Wingman's from its pose. The trails are drawn in the
 * world pass ({@code AFTER_PARTICLES}): the curtains alpha-blended, then the lines additive. The own flyer in first person
 * is not drawn by the game, so it leaves no trail.
 */
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class LightTrails {
    private LightTrails() {}

    /** The two trails (right, left) of one flyer and the time of its last sample. */
    private static final class Flyer {
        final LightTrail[] sides = {new LightTrail(), new LightTrail()};
        double seen;
    }

    private static final Int2ObjectOpenHashMap<Flyer> FLYERS = new Int2ObjectOpenHashMap<>();
    private static ClientLevel lastLevel;
    /** Depths of the curtain's rows (0 the line, 1 the lower edge). */
    private static final float[] ROWS = {0F, 0.2F, 0.45F, 0.7F, 1F};

    private static Flyer flyer(int id) { return FLYERS.computeIfAbsent(id, k -> new Flyer()); }

    /**
     * Samples the back nozzles of a player in maneuver flight, from the posed model ({@code pose} is the camera-relative pose
     * of the render layer). The main nozzles on the body are averaged per side, so the base frame's four pods leave one
     * trail a side; a frame without main nozzles (the graviton core) leaves none.
     */
    public static void samplePlayer(PoseStack pose, HumanoidModel<?> model, Player player, float partial) {
        if (FxBudget.tier(player) == FxLodRules.FAR) return;
        double[] sum = new double[6];
        int[] count = new int[2];
        for (QuantumNozzles.Nozzle n : NozzleDraw.worn(player, partial)) {   // 0.1.11.123: the main nozzles move with the opening pods
            if (n.kind() != QuantumNozzles.Kind.MAIN || n.part() != QuantumNozzles.Part.BODY || !NozzleDraw.push(pose, model, n)) continue;
            Vector3f at = pose.last().pose().transformPosition(0, 0, 0, new Vector3f());
            pose.popPose();
            int side = n.x() < 0 ? 0 : 1;
            sum[side * 3] += at.x; sum[side * 3 + 1] += at.y; sum[side * 3 + 2] += at.z;
            count[side]++;
        }
        if (count[0] + count[1] == 0) return;
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 chest = player.getPosition(partial).add(0, 1, 0);
        double time = player.level().getGameTime() + partial;
        float level = LightTrailRules.level(ManeuverModeClient.machOf(player));
        Flyer f = null;
        for (int side = 0; side < 2; side++) {
            if (count[side] == 0) continue;
            Vec3 world = new Vec3(sum[side * 3] / count[side], sum[side * 3 + 1] / count[side], sum[side * 3 + 2] / count[side]).add(cam);
            if (world.distanceToSqr(chest) > 9) return;   // not a draw in the world (the inventory figure, a menu preview)
            if (f == null) f = flyer(player.getId());
            f.sides[side].sample(time, world, level);
            f.seen = time;
        }
    }

    /** Samples the back nozzles of the flying Wingmen, from their pose (as their thrust in {@code WingmanFx}). */
    private static void sampleWingmen(Minecraft mc, float partial, double time) {
        if (!FcsClientConfig.wingmanFx()) return;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof WingmanEntity w) || !w.isAlive() || w.landed() || FxBudget.tier(w) == FxLodRules.FAR) continue;
            double speed = w.position().subtract(w.xo, w.yo, w.zo).length();
            float level = LightTrailRules.level(ManeuverMode.mach(speed));
            Flyer f = FLYERS.get(w.getId());
            if (level <= 0 && f == null) continue;
            double yaw = Math.toRadians(Mth.lerp(partial, w.yBodyRotO, w.yBodyRot));
            Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw)), ahead = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            double pitch = Math.toRadians(w.lean(partial)[0]);
            Vec3 bodyUp = ahead.scale(Math.sin(pitch)).add(0, Math.cos(pitch), 0);
            Vec3 back = ahead.scale(-Math.cos(pitch)).add(0, Math.sin(pitch), 0);
            Vec3 base = w.getPosition(partial).add(0, 0.9, 0).add(bodyUp.scale(LightTrailRules.WINGMAN_UP)).add(back.scale(LightTrailRules.WINGMAN_BACK));
            if (f == null) f = flyer(w.getId());
            for (int side = 0; side < 2; side++)
                f.sides[side].sample(time, base.add(right.scale(LightTrailRules.WINGMAN_SIDE * (side == 0 ? 1 : -1))), level);
            f.seen = time;
        }
    }

    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != lastLevel) { FLYERS.clear(); lastLevel = mc.level; }
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double now = mc.level.getGameTime() + partial;
        sampleWingmen(mc, partial, now);
        if (FLYERS.isEmpty()) return;
        FLYERS.values().removeIf(f -> now - f.seen > 40);
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        Matrix4f m = pose.last().pose();
        java.util.List<Trail> trails = new java.util.ArrayList<>();
        for (Flyer f : FLYERS.values()) {
            if (now - f.seen > 0.5) continue;   // not drawn this frame (out of view, or the own flyer in first person)
            for (LightTrail t : f.sides) { Trail built = build(t.path(), now, cam); if (built != null) trails.add(built); }
        }
        if (!trails.isEmpty()) {
            RenderType curtainType = LightTrailRules.CURTAIN_ADDITIVE ? SiRenderTypes.FIELD_GLOW : SiRenderTypes.VAPOR;
            VertexConsumer vc = buffers.getBuffer(curtainType);
            for (Trail t : trails) curtain(vc, m, t);
            if (curtainType != SiRenderTypes.FIELD_GLOW) {
                buffers.endBatch(curtainType);
                vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
            }
            for (Trail t : trails) {
                line(vc, m, t, LightTrailRules.LINE_HALF_WIDTH, LightTrailRules.LINE_RED, LightTrailRules.LINE_ALPHA);
                line(vc, m, t, LightTrailRules.CORE_HALF_WIDTH, LightTrailRules.CORE_WHITE, LightTrailRules.CORE_ALPHA);
            }
            buffers.endBatch(SiRenderTypes.FIELD_GLOW);
        }
        pose.popPose();
    }

    /** A trail ready to draw: camera-relative points along a spline, the side of a camera-facing ribbon, the curtain's way down, alpha and depth. */
    private record Trail(Vec3[] p, Vec3[] side, Vec3[] hang, float[] alpha, float[] depth) {}

    private static Trail build(double[][] path, double now, Vec3 cam) {
        int n = path.length;
        if (n < 2) return null;
        int sub = LightTrailRules.SUBDIVISIONS, max = (n - 1) * sub + 1;
        Vec3[] p = new Vec3[max];
        float[] alpha = new float[max], depth = new float[max];
        int k = 0;
        boolean any = false;
        for (int i = 0; i < n - 1 && k < max; i++) {
            double[] a = path[Math.max(0, i - 1)], b = path[i], c = path[i + 1], d = path[Math.min(n - 1, i + 2)];
            for (int s = 0; s < sub; s++) {
                double u = s / (double) sub;
                k = point(p, alpha, depth, k, spline(a, b, c, d, u), b[3] + (c[3] - b[3]) * u, b[4] + (c[4] - b[4]) * u, now, cam);
                if (alpha[k - 1] > 0.005F) any = true;
                if (depth[k - 1] >= 1F) break;
            }
            if (k > 0 && depth[k - 1] >= 1F) break;
        }
        if (k > 0 && depth[k - 1] < 1F) {
            double[] last = path[n - 1];
            k = point(p, alpha, depth, k, new Vec3(last[0], last[1], last[2]), last[3], last[4], now, cam);
        }
        if (!any || k < 2) return null;
        Vec3[] side = new Vec3[k], hang = new Vec3[k];
        for (int i = 0; i < k; i++) {
            Vec3 tangent = p[Math.min(k - 1, i + 1)].subtract(p[Math.max(0, i - 1)]);
            Vec3 s = tangent.cross(p[i].scale(-1));
            side[i] = s.lengthSqr() < 1.0E-12 ? new Vec3(1, 0, 0) : s.normalize();
            hang[i] = LightTrailRules.hang(tangent, side[i]);
        }
        return new Trail(java.util.Arrays.copyOf(p, k), side, hang, java.util.Arrays.copyOf(alpha, k), java.util.Arrays.copyOf(depth, k));
    }

    /** Adds a point (world {@code at}, sampled at {@code time} with strength {@code level}) as camera-relative, with its alpha and age. */
    private static int point(Vec3[] p, float[] alpha, float[] depth, int k, Vec3 at, double time, double level, double now, Vec3 cam) {
        float age = (float) Mth.clamp((now - time) / LightTrailRules.MAX_AGE_TICKS, 0, 1);
        Vec3 rel = at.subtract(cam);
        p[k] = rel;
        depth[k] = age;
        alpha[k] = (float) level * LightTrailRules.along(age) * LightTrailRules.nearFade(rel.length());
        return k + 1;
    }

    private static Vec3 spline(double[] a, double[] b, double[] c, double[] d, double u) {
        double u2 = u * u, u3 = u2 * u;
        double[] out = new double[3];
        for (int i = 0; i < 3; i++)
            out[i] = 0.5 * (2 * b[i] + (c[i] - a[i]) * u + (2 * a[i] - 5 * b[i] + 4 * c[i] - d[i]) * u2 + (-a[i] + 3 * b[i] - 3 * c[i] + d[i]) * u3);
        return new Vec3(out[0], out[1], out[2]);
    }

    /** The curtain: rows of quads hanging from the line, brightest at the line, nothing at the lower edge. */
    private static void curtain(VertexConsumer vc, Matrix4f m, Trail t) {
        float[] c = LightTrailRules.CURTAIN_RED;
        for (int i = 0; i + 1 < t.p.length; i++) {
            float a0 = t.alpha[i] * LightTrailRules.CURTAIN_TOP_ALPHA, a1 = t.alpha[i + 1] * LightTrailRules.CURTAIN_TOP_ALPHA;
            if (a0 <= 0.003F && a1 <= 0.003F) continue;
            float h0 = LightTrailRules.curtainHeight(t.depth[i]), h1 = LightTrailRules.curtainHeight(t.depth[i + 1]);
            for (int r = 0; r + 1 < ROWS.length; r++) {
                float d0 = ROWS[r], d1 = ROWS[r + 1], f0 = LightTrailRules.curtainFade(d0), f1 = LightTrailRules.curtainFade(d1);
                put(vc, m, t.p[i].add(t.hang[i].scale(h0 * d0)), c, a0 * f0);
                put(vc, m, t.p[i + 1].add(t.hang[i + 1].scale(h1 * d0)), c, a1 * f0);
                put(vc, m, t.p[i + 1].add(t.hang[i + 1].scale(h1 * d1)), c, a1 * f1);
                put(vc, m, t.p[i].add(t.hang[i].scale(h0 * d1)), c, a0 * f1);
            }
        }
    }

    /** The line: a thin ribbon facing the camera along the trail. */
    private static void line(VertexConsumer vc, Matrix4f m, Trail t, float half, float[] c, float alpha) {
        for (int i = 0; i + 1 < t.p.length; i++) {
            float a0 = t.alpha[i] * alpha, a1 = t.alpha[i + 1] * alpha;
            if (a0 <= 0.003F && a1 <= 0.003F) continue;
            put(vc, m, t.p[i].add(t.side[i].scale(half)), c, a0);
            put(vc, m, t.p[i + 1].add(t.side[i + 1].scale(half)), c, a1);
            put(vc, m, t.p[i + 1].subtract(t.side[i + 1].scale(half)), c, a1);
            put(vc, m, t.p[i].subtract(t.side[i].scale(half)), c, a0);
        }
    }

    private static void put(VertexConsumer vc, Matrix4f m, Vec3 at, float[] c, float a) {
        vc.addVertex(m, (float) at.x, (float) at.y, (float) at.z).setColor(c[0], c[1], c[2], Math.max(0F, Math.min(1F, a)));
    }

    /** For captures: each flyer's trails (points, span from the head to the oldest point in blocks, the head's strength). */
    public static String describe() {
        StringBuilder out = new StringBuilder();
        for (var e : FLYERS.int2ObjectEntrySet()) {
            out.append(" [").append(e.getIntKey()).append(String.format(java.util.Locale.ROOT, " seen %.2f", e.getValue().seen));
            for (LightTrail t : e.getValue().sides) {
                double[][] p = t.path();
                if (p.length == 0) { out.append(" -"); continue; }
                double span = Math.sqrt(Math.pow(p[0][0] - p[p.length - 1][0], 2) + Math.pow(p[0][1] - p[p.length - 1][1], 2) + Math.pow(p[0][2] - p[p.length - 1][2], 2));
                out.append(String.format(java.util.Locale.ROOT, " %d pts %.2f m dt %.2f lvl %.2f", p.length, span, p[0][3] - p[p.length - 1][3], p[0][4]));
            }
            out.append(']');
        }
        return out.toString();
    }
}
