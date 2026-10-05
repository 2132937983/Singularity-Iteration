// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.suit.SuitSensorData;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Deflector field effect. No bubble: the field becomes visible only where it works. Each hit
 * lights a patch of hexagonal cells on a hull that follows the body; a shock ring runs outward
 * from the impact point across the cells, three deflector plates fan out and sweep the patch,
 * and the patch fades in {@link SuitSensorData#SHIELD_HIT_MS} ms.
 */
public final class ShieldFieldRenderer {
    private ShieldFieldRenderer() {}

    private static final float CELL = 0.17F;
    private static final int RINGS = 4;
    private static final int[][] AXIAL;
    static {
        java.util.List<int[]> cells = new java.util.ArrayList<>();
        for (int q = -RINGS; q <= RINGS; q++)
            for (int r = Math.max(-RINGS, -q - RINGS); r <= Math.min(RINGS, -q + RINGS); r++) cells.add(new int[]{q, r});
        AXIAL = cells.toArray(new int[0][]);
    }
    private static final java.util.Set<Long> sparked = new java.util.HashSet<>();

    public static void render(RenderLevelStageEvent event) {
        List<SuitSensorData.Hit> hits = SuitSensorData.shieldHits();
        if (hits.isEmpty()) { sparked.clear(); return; }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        long now = System.currentTimeMillis();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        for (SuitSensorData.Hit hit : hits) {
            Entity e = mc.level.getEntity(hit.entityId());
            if (e == null) continue;
            float age = hit.age(now);
            if (age >= 1) continue;
            Vec3 centre = e.getPosition(partial).add(0, e.getBbHeight() * 0.55, 0);
            float rx = e.getBbWidth() * 0.5F + 0.45F, ry = e.getBbHeight() * 0.62F + 0.15F;
            Vec3 n = new Vec3(hit.dx(), hit.dy(), hit.dz());
            if (firstPerson && e == mc.player) {
                // seen from inside: pull the patch forward of the eye so it is not clipped by the near plane
                rx += 0.35F;
            }
            drawPatch(vc, m, centre, n, rx, ry, age, hit.strength());
            long key = ((long) hit.entityId() << 32) ^ hit.startMs();
            if (sparked.add(key)) {
                Vec3 p = surface(centre, n, rx, ry);
                for (int i = 0; i < 10; i++) {
                    Vec3 v = n.scale(0.15).add((mc.level.random.nextDouble() - 0.5) * 0.3, mc.level.random.nextDouble() * 0.2, (mc.level.random.nextDouble() - 0.5) * 0.3);
                    mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, v.x, v.y, v.z);
                }
            }
        }
        buffers.endBatch(SiRenderTypes.FIELD_GLOW);
        pose.popPose();
        if (sparked.size() > 128) sparked.clear();
    }

    /** Point on the ellipsoid hull in direction d from the centre. */
    private static Vec3 surface(Vec3 c, Vec3 d, float rx, float ry) {
        Vec3 u = d.normalize();
        double k = 1.0 / Math.sqrt((u.x * u.x + u.z * u.z) / (rx * rx) + (u.y * u.y) / (ry * ry));
        return c.add(u.scale(k));
    }

    private static void drawPatch(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 n, float rx, float ry, float age, float strength) {
        Vec3 t = Math.abs(n.y) < 0.95 ? n.cross(new Vec3(0, 1, 0)).normalize() : n.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 b = n.cross(t).normalize();
        float fade = (1 - age) * (1 - age);
        float wave = age * (RINGS + 1.5F);          // shock ring position, in cells
        float sweep = -RINGS + age * RINGS * 2.6F;  // plate sweep position along t, in cells
        for (int[] cell : AXIAL) {
            float q = cell[0], r = cell[1];
            float u = CELL * 1.5F * q;
            float v = CELL * (float) Math.sqrt(3) * (r + q / 2F);
            float dist = (Math.abs(q) + Math.abs(r) + Math.abs(-q - r)) / 2F;
            float ringGlow = Math.max(0, 1 - Math.abs(dist - wave) * 1.3F);
            float core = Math.max(0, 1 - dist / (RINGS + 0.5F));
            float plate = Math.max(0, 1 - Math.abs(u / (CELL * 1.5F) - sweep) * 0.9F) * (dist <= RINGS - 1 ? 1 : 0.4F);
            float intensity = (0.25F * core + 0.85F * ringGlow + 0.55F * plate) * fade * (0.5F + strength * 0.6F);
            if (intensity < 0.02F) continue;
            // cell colour: white-blue core, cyan ring, deep blue edge
            // additive blend: colour carries the hue, alpha carries the brightness
            float cr = 0.35F + 0.65F * ringGlow, cg = 0.75F + 0.25F * ringGlow, cb = 1.0F;
            float a = Math.min(1F, intensity * 1.6F);
            hexOutline(vc, m, c, n, t, b, u, v, rx, ry, cr, cg, cb, a);
            if (plate > 0.4F) hexFill(vc, m, c, n, t, b, u, v, rx, ry, 0.3F, 0.6F, 1.0F, a * 0.45F);
        }
    }

    private static Vec3 onHull(Vec3 c, Vec3 n, Vec3 t, Vec3 b, float u, float v, float rx, float ry) {
        return surface(c, n.add(t.scale(u)).add(b.scale(v)), rx, ry);
    }

    private static void hexOutline(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 n, Vec3 t, Vec3 b, float u, float v,
                                   float rx, float ry, float r, float g, float bl, float a) {
        float outer = CELL * 0.92F, inner = CELL * 0.74F;
        for (int k = 0; k < 6; k++) {
            double a0 = Math.PI / 3 * k, a1 = Math.PI / 3 * (k + 1);
            Vec3 o0 = onHull(c, n, t, b, u + (float) Math.cos(a0) * outer, v + (float) Math.sin(a0) * outer, rx, ry);
            Vec3 o1 = onHull(c, n, t, b, u + (float) Math.cos(a1) * outer, v + (float) Math.sin(a1) * outer, rx, ry);
            Vec3 i1 = onHull(c, n, t, b, u + (float) Math.cos(a1) * inner, v + (float) Math.sin(a1) * inner, rx, ry);
            Vec3 i0 = onHull(c, n, t, b, u + (float) Math.cos(a0) * inner, v + (float) Math.sin(a0) * inner, rx, ry);
            put(vc, m, o0, r, g, bl, a);
            put(vc, m, o1, r, g, bl, a);
            put(vc, m, i1, r * 0.4F, g * 0.4F, bl * 0.6F, a * 0.4F);
            put(vc, m, i0, r * 0.4F, g * 0.4F, bl * 0.6F, a * 0.4F);
        }
    }

    private static void hexFill(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 n, Vec3 t, Vec3 b, float u, float v,
                                float rx, float ry, float r, float g, float bl, float a) {
        Vec3 mid = onHull(c, n, t, b, u, v, rx, ry);
        float inner = CELL * 0.74F;
        for (int k = 0; k < 6; k++) {
            double a0 = Math.PI / 3 * k, a1 = Math.PI / 3 * (k + 1);
            Vec3 p0 = onHull(c, n, t, b, u + (float) Math.cos(a0) * inner, v + (float) Math.sin(a0) * inner, rx, ry);
            Vec3 p1 = onHull(c, n, t, b, u + (float) Math.cos(a1) * inner, v + (float) Math.sin(a1) * inner, rx, ry);
            put(vc, m, mid, r, g, bl, a);
            put(vc, m, p0, r, g, bl, a);
            put(vc, m, p1, r, g, bl, a);
            put(vc, m, mid, r, g, bl, a);
        }
    }

    private static void put(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(Math.min(1, r), Math.min(1, g), Math.min(1, b), Math.min(1, a));
    }
}
