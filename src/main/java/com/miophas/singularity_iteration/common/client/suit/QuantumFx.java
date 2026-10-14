// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.suit;

import com.miophas.singularity_iteration.common.client.render.FxDraw;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitSensorData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Quantum suit effects (0.1.9.1), drawn after the particles with additive light.
 *
 * <ul>
 *   <li><b>Photon barrier.</b> Each deflector hit raises a geodesic barrier (a subdivided
 *       icosahedron, 80 facets) around the wearer for {@link SuitSensorData#SHIELD_HIT_MS} ms. A
 *       wave front runs over the facets from the impact direction; the edges light up as it
 *       passes, with a prism fringe (a violet and a gold copy of the edges just outside the cyan
 *       one). A lens star marks the impact point and photon motes stream into it. A crystalline
 *       chime plays. The existing hex patch at the impact stays.</li>
 *   <li><b>Photon afterglow.</b> Special Maneuver flight above 1.1 blocks per tick leaves the
 *       spatial distortion wake and photon afterimages ({@link com.miophas.singularity_iteration.common.client.suit.nano.NanoFx#photonWake}).</li>
 *   <li><b>Active scan.</b> While an entity sensor (ESP, threat sensor, behaviour predictor,
 *       holomap) runs, a scan sphere expands from the wearer every 3 s: latitude rings and
 *       meridians in the HUD colour. Where the front passes a creature, corner brackets flash
 *       around it.</li>
 * </ul>
 * Client options {@code suit_visuals.photon_fx} and {@code suit_visuals.scan_fx}.
 */
public final class QuantumFx {
    private QuantumFx() {}

    /** Geodesic sphere: unit vertices and edge pairs (frequency 2 icosahedron). */
    private static final List<Vec3> VERTS = new ArrayList<>();
    private static final List<int[]> EDGES = new ArrayList<>();
    static { buildGeodesic(); }

    private static final Set<Long> SEEN = new HashSet<>();
    private static final int SCAN_PERIOD = 60, SCAN_TICKS = 28;
    private static final float SCAN_RADIUS = 24;
    private static int ticks, scanStart = -1000;

    // ------------------------------------------------------------------ tick

    public static void tick(Minecraft mc) {
        ticks++;
        if (mc.level == null || mc.player == null) return;
        if (FcsClientConfig.photonFx()) {
            // new deflector hits: chime and photon motes
            for (SuitSensorData.Hit hit : SuitSensorData.shieldHits()) {
                long key = hit.startMs() * 31 + hit.entityId();
                if (!SEEN.add(key)) continue;
                Entity e = mc.level.getEntity(hit.entityId());
                if (e == null) continue;
                Vec3 c = e.position().add(0, e.getBbHeight() * 0.55, 0);
                Vec3 at = c.add(hit.dx() * (e.getBbWidth() * 0.5 + 0.45), hit.dy() * (e.getBbHeight() * 0.62 + 0.15), hit.dz() * (e.getBbWidth() * 0.5 + 0.45));
                mc.level.playLocalSound(at.x, at.y, at.z, mio_icif_sounds.PHOTON_BARRIER.get(), SoundSource.PLAYERS,
                    0.5F + 0.5F * hit.strength(), 0.95F + mc.level.random.nextFloat() * 0.1F, false);
                for (int i = 0; i < 16; i++) {
                    Vec3 from = at.add(mc.level.random.nextGaussian() * 0.9, mc.level.random.nextGaussian() * 0.9, mc.level.random.nextGaussian() * 0.9);
                    Vec3 v = at.subtract(from).scale(0.16);
                    mc.level.addParticle(ParticleTypes.END_ROD, from.x, from.y, from.z, v.x, v.y, v.z);
                }
            }
            if (SEEN.size() > 256) SEEN.clear();
            // photon afterglow of fast maneuver flight (all players near)
            if (ticks % 6 == 0) {
                for (Player p : mc.level.getEntitiesOfClass(Player.class, mc.player.getBoundingBox().inflate(128))) {
                    if (!ManeuverModeClient.inManeuver(p)) continue;
                    Vec3 v = p.position().subtract(p.xo, p.yo, p.zo);
                    if (v.length() < 1.1) continue;
                    com.miophas.singularity_iteration.common.client.suit.nano.NanoFx.photonWake(p.getId(), p.position(), v.normalize());
                }
            }
        }
        if (FcsClientConfig.scanFx() && ticks - scanStart >= SCAN_PERIOD && scanning()) scanStart = ticks;
    }

    private static boolean scanning() {
        var active = FcsState.activeSet();
        return active.contains(SuitModuleType.ENTITY_ESP) || active.contains(SuitModuleType.THREAT_SENSOR)
            || active.contains(SuitModuleType.BEHAVIOR_PREDICTOR) || active.contains(SuitModuleType.HOLOMAP);
    }

    // ------------------------------------------------------------------ render

    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        boolean photon = FcsClientConfig.photonFx();
        float scanAge = ticks - scanStart + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        boolean scan = FcsClientConfig.scanFx() && scanAge < SCAN_TICKS;
        List<SuitSensorData.Hit> hits = photon ? SuitSensorData.shieldHits() : List.of();
        if (hits.isEmpty() && !scan) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        long now = System.currentTimeMillis();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        for (SuitSensorData.Hit hit : hits) {
            Entity e = mc.level.getEntity(hit.entityId());
            if (e == null) continue;
            if (firstPerson && e == mc.player) continue;          // no shell over the camera
            barrier(vc, m, e, hit, now, cam, partial, event);
        }
        if (scan) scanSphere(vc, m, mc, scanAge / SCAN_TICKS, cam, partial, event);
        buffers.endBatch(SiRenderTypes.FIELD_GLOW);
        pose.popPose();
    }

    /** Geodesic photon barrier of one hit. */
    private static void barrier(VertexConsumer vc, Matrix4f m, Entity e, SuitSensorData.Hit hit, long now, Vec3 cam, float partial,
                                RenderLevelStageEvent event) {
        float age = hit.age(now);
        if (age >= 1) return;
        float flash = FcsClientConfig.flashScale();
        Vec3 c = e.getPosition(partial).add(0, e.getBbHeight() * 0.55, 0);
        double rx = e.getBbWidth() * 0.5 + 0.55, ry = e.getBbHeight() * 0.62 + 0.25;
        Vec3 impact = new Vec3(hit.dx(), hit.dy(), hit.dz());
        if (impact.lengthSqr() < 1.0E-4) impact = new Vec3(0, 0, 1);
        impact = impact.normalize();
        float front = age * 2.6F;                                  // wave front, in radians from the impact
        float fade = (1 - age) * (0.5F + 0.5F * hit.strength()) * flash;
        // 0.1.10.1: hex cell shield in the colour of the suit charge (cyan, amber below 50 %, red below 25 %)
        hexCells(vc, m, c, rx, ry, impact, front, fade, hit, cam);
        // the prism fringe of the photon barrier stays as a faint outer shell on the wave front
        float[][] fringe = {{0.72F, 0.48F, 1.0F, 1.06F}, {1.0F, 0.84F, 0.4F, 0.95F}};
        for (float[] f : fringe) {
            float scale = f[3];
            for (int[] edge : EDGES) {
                Vec3 a = VERTS.get(edge[0]), b = VERTS.get(edge[1]);
                Vec3 mid = a.add(b).normalize();
                float angle = (float) Math.acos(Mth.clamp(mid.dot(impact), -1, 1));
                float wave = Mth.clamp(1 - Math.abs(angle - front) * 2.2F, 0, 1);
                float alpha = wave * fade * 0.4F;
                if (alpha < 0.02F) continue;
                Vec3 pa = c.add(a.x * rx * scale, a.y * ry * scale, a.z * rx * scale);
                Vec3 pb = c.add(b.x * rx * scale, b.y * ry * scale, b.z * rx * scale);
                FxDraw.line(vc, m, pa, pb, cam, 0.018F, f[0], f[1], f[2], alpha);
            }
        }
        // lens star at the impact point and the inner glow of the struck facet
        Vec3 at = c.add(impact.x * rx, impact.y * ry, impact.z * rx);
        float hot = Mth.clamp(1 - age * 2.5F, 0, 1) * flash;
        if (hot > 0.02F) {
            FxDraw.star(vc, m, at, event.getCamera().getLeftVector(), event.getCamera().getUpVector(), 0.6F + 0.4F * hit.strength(),
                0.75F, 0.95F, 1.0F, hot);
            FxDraw.ring(vc, m, at, impact, 0.1F + age * 1.4F, 0.18F, 0.55F, 0.9F, 1.0F, hot * 0.8F, 24);
        }
    }

    /** 0.1.10.1: cell centres of the hex shield (a frequency-4 geodesic sphere, 162 cells: 150 hexagons and 12 pentagon spots). */
    private static final List<Vec3> CELLS = new ArrayList<>();
    static { buildCells(); }

    /**
     * Hex cell shield (0.1.10.1). Each cell is a hexagon tangent to the shell around its centre.
     * Light: a faint base near the impact, plus the wave front that runs out from the impact; a
     * cell fills softly while the front crosses it. Colour by the suit charge: cyan, amber below
     * 50 %, red below 25 %. Below 50 % more and more cells are cracked: two edges missing, a
     * jagged crack across, a flicker.
     */
    private static void hexCells(VertexConsumer vc, Matrix4f m, Vec3 c, double rx, double ry, Vec3 impact, float front, float fade,
                                 SuitSensorData.Hit hit, Vec3 cam) {
        float charge = Mth.clamp(hit.charge(), 0, 1);
        float[] col = charge >= 0.5F ? new float[]{0.36F, 0.86F, 1.0F} : charge >= 0.25F ? new float[]{1.0F, 0.66F, 0.22F} : new float[]{1.0F, 0.26F, 0.2F};
        float crackShare = charge < 0.5F ? (0.5F - charge) * 1.6F : 0F;
        long time = System.currentTimeMillis();
        double cell = 0.135;                                           // cell radius on the unit sphere
        for (int i = 0; i < CELLS.size(); i++) {
            Vec3 v = CELLS.get(i);
            float angle = (float) Math.acos(Mth.clamp(v.dot(impact), -1, 1));
            float wave = Mth.clamp(1 - Math.abs(angle - front) * 2.6F, 0, 1);
            float base = 0.10F + 0.22F * Mth.clamp(1 - angle / 1.1F, 0, 1);
            float alpha = (base + wave) * fade;
            if (alpha < 0.02F) continue;
            int h = (i * 0x9E3779B1) >>> 8;
            boolean cracked = crackShare > 0 && (h % 1000) / 1000F < crackShare;
            if (cracked && ((time / 70 + i) % 5 == 0)) continue;           // a cracked cell flickers out now and then
            Vec3 t1 = FxDraw.perp(v), t2 = v.cross(t1).normalize();
            Vec3[] corner = new Vec3[6];
            for (int k = 0; k < 6; k++) {
                double ang = k * Math.PI / 3 + (h % 7) * 0.05;
                Vec3 d = v.add(t1.scale(Math.cos(ang) * cell)).add(t2.scale(Math.sin(ang) * cell)).normalize();
                corner[k] = c.add(d.x * rx, d.y * ry, d.z * rx);
            }
            Vec3 centre = c.add(v.x * rx, v.y * ry, v.z * rx);
            int skipA = cracked ? h % 6 : -1, skipB = cracked ? (h / 6 + 2) % 6 : -1;
            for (int k = 0; k < 6; k++) {
                if (k == skipA || k == skipB) continue;
                FxDraw.line(vc, m, corner[k], corner[(k + 1) % 6], cam, 0.02F, col[0], col[1], col[2], alpha);
            }
            if (wave > 0.05F) {                                          // the front fills the cell softly
                for (int k = 0; k < 6; k++) {
                    FxDraw.vertex(vc, m, centre, col[0], col[1], col[2], 0.28F * wave * fade);
                    FxDraw.vertex(vc, m, corner[k], col[0], col[1], col[2], 0.06F * wave * fade);
                    FxDraw.vertex(vc, m, corner[(k + 1) % 6], col[0], col[1], col[2], 0.06F * wave * fade);
                    FxDraw.vertex(vc, m, centre, col[0], col[1], col[2], 0.28F * wave * fade);
                }
            }
            if (cracked) {                                               // a jagged crack across the cell
                Vec3 a = corner[skipA], b = corner[(skipA + 3) % 6];
                Vec3 mid = a.add(b).scale(0.5).add(centre.subtract(c).normalize().scale(0.02)).add(t1.scale(0.03));
                FxDraw.line(vc, m, a, mid, cam, 0.016F, 1F, 1F, 1F, alpha * 0.8F);
                FxDraw.line(vc, m, mid, b, cam, 0.016F, 1F, 1F, 1F, alpha * 0.8F);
            }
        }
    }

    private static void buildCells() {
        // icosahedron, subdivided twice (each triangle into four): 162 vertices on the unit sphere
        double t = (1 + Math.sqrt(5)) / 2;
        List<Vec3> v = new ArrayList<>();
        double[][] base = {{-1, t, 0}, {1, t, 0}, {-1, -t, 0}, {1, -t, 0}, {0, -1, t}, {0, 1, t}, {0, -1, -t}, {0, 1, -t},
            {t, 0, -1}, {t, 0, 1}, {-t, 0, -1}, {-t, 0, 1}};
        for (double[] p : base) v.add(new Vec3(p[0], p[1], p[2]).normalize());
        List<int[]> faces = new ArrayList<>(List.of(new int[]{0, 11, 5}, new int[]{0, 5, 1}, new int[]{0, 1, 7}, new int[]{0, 7, 10}, new int[]{0, 10, 11},
            new int[]{1, 5, 9}, new int[]{5, 11, 4}, new int[]{11, 10, 2}, new int[]{10, 7, 6}, new int[]{7, 1, 8}, new int[]{3, 9, 4}, new int[]{3, 4, 2},
            new int[]{3, 2, 6}, new int[]{3, 6, 8}, new int[]{3, 8, 9}, new int[]{4, 9, 5}, new int[]{2, 4, 11}, new int[]{6, 2, 10}, new int[]{8, 6, 7},
            new int[]{9, 8, 1}));
        for (int pass = 0; pass < 2; pass++) {
            java.util.Map<Long, Integer> mids = new java.util.HashMap<>();
            List<int[]> next = new ArrayList<>();
            for (int[] f : faces) {
                int ab = cellMid(v, mids, f[0], f[1]), bc = cellMid(v, mids, f[1], f[2]), ca = cellMid(v, mids, f[2], f[0]);
                next.add(new int[]{f[0], ab, ca});
                next.add(new int[]{f[1], bc, ab});
                next.add(new int[]{f[2], ca, bc});
                next.add(new int[]{ab, bc, ca});
            }
            faces = next;
        }
        CELLS.addAll(v);
    }

    private static int cellMid(List<Vec3> v, java.util.Map<Long, Integer> cache, int a, int b) {
        long key = Math.min(a, b) * 100_000L + Math.max(a, b);
        Integer have = cache.get(key);
        if (have != null) return have;
        v.add(v.get(a).add(v.get(b)).normalize());
        cache.put(key, v.size() - 1);
        return v.size() - 1;
    }

    /** Expanding scan sphere around the local player; brackets flash on the creatures the front passes. */
    private static void scanSphere(VertexConsumer vc, Matrix4f m, Minecraft mc, float k, Vec3 cam, float partial, RenderLevelStageEvent event) {
        Player p = mc.player;
        Vec3 c = p.getPosition(partial).add(0, 1.0, 0);
        float r = 1.5F + SCAN_RADIUS * (1 - (1 - k) * (1 - k));   // ease out
        float a = (1 - k) * 0.42F;
        float[] col = FxDraw.rgb(FcsClientConfig.primary());
        // latitude rings
        for (int i = -3; i <= 3; i++) {
            double lat = i * Math.PI / 8;
            float ringR = (float) (r * Math.cos(lat));
            FxDraw.ring(vc, m, c.add(0, r * Math.sin(lat), 0), new Vec3(0, 1, 0), ringR, 0.18F + 0.02F * r, col[0], col[1], col[2],
                a * (i == 0 ? 1F : 0.55F), 64);
        }
        // meridians
        for (int j = 0; j < 8; j++) {
            double lon = j * Math.PI / 4 + k * 0.6;
            Vec3 prev = null;
            for (int i = 0; i <= 16; i++) {
                double lat = -Math.PI / 2 + i * Math.PI / 16;
                Vec3 pt = c.add(Math.cos(lat) * Math.cos(lon) * r, Math.sin(lat) * r, Math.cos(lat) * Math.sin(lon) * r);
                if (prev != null) FxDraw.line(vc, m, prev, pt, cam, 0.04F + 0.004F * r, col[0], col[1], col[2], a * 0.45F);
                prev = pt;
            }
        }
        // creatures at the wave front
        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(SCAN_RADIUS + 2), x -> x != p && x.isAlive())) {
            double d = e.getPosition(partial).add(0, e.getBbHeight() * 0.5, 0).distanceTo(c);
            float near = Mth.clamp(1 - (float) Math.abs(d - r) / 1.6F, 0, 1);
            if (near <= 0) continue;
            brackets(vc, m, e, partial, cam, col, near * 0.9F);
        }
    }

    /** Corner brackets around an entity's box, facing the camera. */
    private static void brackets(VertexConsumer vc, Matrix4f m, LivingEntity e, float partial, Vec3 cam, float[] col, float alpha) {
        Vec3 base = e.getPosition(partial);
        float w = e.getBbWidth() * 0.5F + 0.15F, h = e.getBbHeight() + 0.2F;
        Vec3 view = base.add(0, h * 0.5, 0).subtract(cam);
        Vec3 side = new Vec3(-view.z, 0, view.x);
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize().scale(w);
        Vec3 up = new Vec3(0, 1, 0);
        float arm = Math.min(w, h) * 0.45F;
        Vec3[] corners = {base.subtract(side).add(0, -0.1, 0), base.add(side).add(0, -0.1, 0),
            base.add(side).add(0, h - 0.1, 0), base.subtract(side).add(0, h - 0.1, 0)};
        Vec3 sn = side.normalize();
        int[][] dirs = {{1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
        for (int i = 0; i < 4; i++) {
            Vec3 q = corners[i];
            FxDraw.line(vc, m, q, q.add(sn.scale(arm * dirs[i][0])), cam, 0.035F, col[0], col[1], col[2], alpha);
            FxDraw.line(vc, m, q, q.add(up.scale(arm * dirs[i][1])), cam, 0.035F, col[0], col[1], col[2], alpha);
        }
    }

    // ------------------------------------------------------------------ geodesic sphere

    private static void buildGeodesic() {
        double t = (1 + Math.sqrt(5)) / 2;
        List<Vec3> ico = new ArrayList<>();
        double[][] v = {{-1, t, 0}, {1, t, 0}, {-1, -t, 0}, {1, -t, 0}, {0, -1, t}, {0, 1, t}, {0, -1, -t}, {0, 1, -t},
            {t, 0, -1}, {t, 0, 1}, {-t, 0, -1}, {-t, 0, 1}};
        for (double[] p : v) ico.add(new Vec3(p[0], p[1], p[2]).normalize());
        int[][] faces = {{0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11}, {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8},
            {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9}, {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1}};
        java.util.Map<Long, Integer> midpoints = new java.util.HashMap<>();
        VERTS.addAll(ico);
        Set<Long> edges = new HashSet<>();
        for (int[] f : faces) {
            int a = f[0], b = f[1], c = f[2];
            int ab = mid(a, b, midpoints), bc = mid(b, c, midpoints), ca = mid(c, a, midpoints);
            int[][] tris = {{a, ab, ca}, {b, bc, ab}, {c, ca, bc}, {ab, bc, ca}};
            for (int[] tri : tris) {
                for (int i = 0; i < 3; i++) {
                    int x = tri[i], y = tri[(i + 1) % 3];
                    long key = Math.min(x, y) * 1000L + Math.max(x, y);
                    if (edges.add(key)) EDGES.add(new int[]{Math.min(x, y), Math.max(x, y)});
                }
            }
        }
    }

    private static int mid(int a, int b, java.util.Map<Long, Integer> cache) {
        long key = Math.min(a, b) * 1000L + Math.max(a, b);
        Integer have = cache.get(key);
        if (have != null) return have;
        VERTS.add(VERTS.get(a).add(VERTS.get(b)).normalize());
        cache.put(key, VERTS.size() - 1);
        return VERTS.size() - 1;
    }
}
