// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.armory;

import com.miophas.singularity_iteration.common.armory.ArmoryFlight;
import com.miophas.singularity_iteration.common.armory.ArmoryPieceEntity;
import com.miophas.singularity_iteration.common.client.render.SiRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 0.1.10.31 (A-6): the flight effects of an Armory piece, drawn by {@link ArmoryPieceRenderer} (additive {@code FIELD_GLOW}).
 * <ul>
 *   <li><b>Ion trail:</b> a blue-white ribbon through the last positions of the piece, narrowing and fading
 *       ({@link PieceFxRules#trailAlpha}); the positions are recorded once a tick by {@link #record}.</li>
 *   <li><b>Re-entry glow:</b> a piece that comes down fast from high above its wearer wears a white-orange shock cap in
 *       front of its path and drags an orange plasma tail ({@link PieceFxRules#reentry}); its trail turns orange too.</li>
 *   <li><b>Seam light:</b> when a segment of a baked suit piece locks, a line of light runs down the edges of its quads
 *       ({@link #seams}, {@link PieceFxRules#seam}).</li>
 * </ul>
 * Option {@code motion_fx}.
 */
public final class PieceFx {
    private PieceFx() {}

    private static final class Trail {
        final double[] xyz = new double[PieceFxRules.TRAIL_POINTS * 3];
        int n;
        float glow;
    }

    private static final Map<ArmoryPieceEntity, Trail> TRAILS = new WeakHashMap<>();
    private static final int CAP_RINGS = 5, CAP_SEGMENTS = 14;

    /** Once a client tick per piece: a new trail point while the piece flies fast, else the trail shortens. */
    static void record(ArmoryPieceEntity piece) {
        Trail t = TRAILS.computeIfAbsent(piece, k -> new Trail());
        float age = piece.age(0);
        boolean flying = piece.isReturn() ? age >= ArmoryFlight.PURGE_TICKS : age >= 0 && age < piece.duration();
        Vec3 now = piece.flightPosition(0), next = piece.flightPosition(1);
        boolean fast = now.distanceTo(next) >= PieceFxRules.TRAIL_SPEED;
        if (flying && fast) {
            System.arraycopy(t.xyz, 0, t.xyz, 3, t.xyz.length - 3);
            t.xyz[0] = now.x;
            t.xyz[1] = now.y;
            t.xyz[2] = now.z;
            t.n = Math.min(PieceFxRules.TRAIL_POINTS, t.n + 1);
        } else if (t.n > 0) {
            t.n--;
        }
    }

    /**
     * Trail and re-entry glow of {@code piece}, in the pose of its renderer before the piece transform: the origin is the
     * render position of the entity ({@code origin}), axes of the world.
     */
    static void flight(ArmoryPieceEntity piece, PoseStack pose, MultiBufferSource buffers, Vec3 origin, Vec3 at, float partial) {
        Trail t = TRAILS.get(piece);
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(origin);
        Vec3 v = piece.flightPosition(partial + 0.5F).subtract(piece.flightPosition(partial - 0.5F));
        double speed = v.length();
        Entity target = piece.target();
        double height = target == null ? 0 : at.y - ArmoryFlight.dockPoint(target, partial, piece.piece()).y;
        float glow = piece.isReturn() ? 0F : PieceFxRules.reentry(speed, v.y, height);
        if (t != null) t.glow += (glow - t.glow) * 0.3F;
        float g = t == null ? glow : t.glow;
        // 0.1.10.32 (P-3): half the trail points beyond 24 blocks, no trail beyond 64; a simpler cap
        int tier = com.miophas.singularity_iteration.common.client.render.FxBudget.tier(at);
        boolean trail = t != null && t.n >= 1 && tier < com.miophas.singularity_iteration.common.client.render.FxLodRules.FAR;
        if (!trail && g < 0.02F) return;
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        Vec3 here = at.subtract(origin);
        if (trail) {
            // blue-white ion trail, orange while it burns in
            float r = 0.55F + 0.45F * g, gr = 0.8F - 0.25F * g, b = 1F - 0.7F * g;
            int n = t.n + 1;
            double px = here.x, py = here.y, pz = here.z;
            int step = tier == com.miophas.singularity_iteration.common.client.render.FxLodRules.NEAR ? 1 : 2;
            for (int i = step; i < n; i += step) {
                double qx = t.xyz[(i - 1) * 3] - origin.x, qy = t.xyz[(i - 1) * 3 + 1] - origin.y, qz = t.xyz[(i - 1) * 3 + 2] - origin.z;
                ribbon(vc, m, px, py, pz, qx, qy, qz, cam, PieceFxRules.trailHalfWidth(i - step, n), PieceFxRules.trailHalfWidth(i, n),
                    r, gr, b, 0.55F * PieceFxRules.trailAlpha(i - step, n), 0.55F * PieceFxRules.trailAlpha(i, n));
                px = qx; py = qy; pz = qz;
            }
        }
        if (g >= 0.02F && speed > 1.0E-4) cap(vc, m, here, v.scale(1 / speed), cam, g,
            tier == com.miophas.singularity_iteration.common.client.render.FxLodRules.NEAR ? CAP_RINGS : 2);
    }

    /** Shock cap in front of the path (apex 0.5 ahead, opening backwards) and a plasma tail behind. */
    private static void cap(VertexConsumer vc, Matrix4f m, Vec3 at, Vec3 dir, Vec3 cam, float g, int rings) {
        Vec3 side = Math.abs(dir.y) < 0.95 ? dir.cross(new Vec3(0, 1, 0)).normalize() : dir.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 up = side.cross(dir).normalize();
        double apex = 0.5, depth = 0.55, radius = 0.42;
        for (int r = 0; r < rings; r++) {
            double k0 = r / (double) rings, k1 = (r + 1) / (double) rings;
            double d0 = apex - depth * k0, d1 = apex - depth * k1;
            double r0 = radius * Math.sqrt(k0), r1 = radius * Math.sqrt(k1);
            float a0 = g * 0.75F * (1 - (float) k0), a1 = g * 0.75F * (1 - (float) k1);
            // white-hot at the apex, orange towards the rim
            float c0 = 1F - 0.55F * (float) k0, c1 = 1F - 0.55F * (float) k1;
            for (int s = 0; s < CAP_SEGMENTS; s++) {
                double t0 = s * Math.PI * 2 / CAP_SEGMENTS, t1 = (s + 1) * Math.PI * 2 / CAP_SEGMENTS;
                vertex(vc, m, at, dir, side, up, d0, r0, t0, 1F, 0.55F + 0.4F * c0, 0.25F + 0.6F * c0 * c0, a0);
                vertex(vc, m, at, dir, side, up, d0, r0, t1, 1F, 0.55F + 0.4F * c0, 0.25F + 0.6F * c0 * c0, a0);
                vertex(vc, m, at, dir, side, up, d1, r1, t1, 1F, 0.55F + 0.4F * c1, 0.25F + 0.6F * c1 * c1, a1);
                vertex(vc, m, at, dir, side, up, d1, r1, t0, 1F, 0.55F + 0.4F * c1, 0.25F + 0.6F * c1 * c1, a1);
            }
        }
        Vec3 tail = at.subtract(dir.scale(1.6 + 1.2 * g));
        ribbon(vc, m, at.x, at.y, at.z, tail.x, tail.y, tail.z, cam, 0.22F * g, 0.05F, 1F, 0.5F, 0.18F, 0.6F * g, 0F);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Vec3 at, Vec3 dir, Vec3 side, Vec3 up, double along, double radius, double angle,
                               float r, float g, float b, float a) {
        double c = Math.cos(angle) * radius, s = Math.sin(angle) * radius;
        vc.addVertex(m, (float) (at.x + dir.x * along + side.x * c + up.x * s), (float) (at.y + dir.y * along + side.y * c + up.y * s),
            (float) (at.z + dir.z * along + side.z * c + up.z * s)).setColor(r, g, b, Math.max(0F, Math.min(1F, a)));
    }

    /** Camera-facing ribbon segment with its own half width and alpha at each end. */
    private static void ribbon(VertexConsumer vc, Matrix4f m, double ax, double ay, double az, double bx, double by, double bz, Vec3 cam,
                               float wa, float wb, float r, float g, float b, float aa, float ab) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double mx = (ax + bx) * 0.5 - cam.x, my = (ay + by) * 0.5 - cam.y, mz = (az + bz) * 0.5 - cam.z;
        double sx = dy * mz - dz * my, sy = dz * mx - dx * mz, sz = dx * my - dy * mx;
        double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (len < 1.0E-9) return;
        sx /= len; sy /= len; sz /= len;
        vc.addVertex(m, (float) (ax + sx * wa), (float) (ay + sy * wa), (float) (az + sz * wa)).setColor(r, g, b, Math.min(1F, aa));
        vc.addVertex(m, (float) (ax - sx * wa), (float) (ay - sy * wa), (float) (az - sz * wa)).setColor(r, g, b, Math.min(1F, aa));
        vc.addVertex(m, (float) (bx - sx * wb), (float) (by - sy * wb), (float) (bz - sz * wb)).setColor(r, g, b, Math.min(1F, ab));
        vc.addVertex(m, (float) (bx + sx * wb), (float) (by + sy * wb), (float) (bz + sz * wb)).setColor(r, g, b, Math.min(1F, ab));
    }

    /**
     * Seam light of one locking segment part: a thin bright frame inside every quad edge (lifted off the face), lit by the
     * sweep of {@link PieceFxRules#seam} at the quad's height. {@code quads}: the part's quads of the segment (pixels, part
     * space); {@code age}: ticks since the segment locked.
     */
    static void seams(PoseStack pose, MultiBufferSource buffers, ModelPart part, float[][] quads, float age) {
        if (quads == null || quads.length == 0 || age < 0 || age >= PieceFxRules.SEAM_TICKS) return;
        float top = Float.MAX_VALUE, bottom = -Float.MAX_VALUE;
        for (float[] q : quads) {
            for (int i = 0; i < 4; i++) {
                top = Math.min(top, q[i * 5 + 1]);
                bottom = Math.max(bottom, q[i * 5 + 1]);
            }
        }
        float span = Math.max(0.5F, bottom - top);
        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.FIELD_GLOW);
        pose.pushPose();
        part.translateAndRotate(pose);
        Matrix4f m = pose.last().pose();
        for (float[] q : quads) {
            float cx = (q[0] + q[5] + q[10] + q[15]) / 4F, cy = (q[1] + q[6] + q[11] + q[16]) / 4F, cz = (q[2] + q[7] + q[12] + q[17]) / 4F;
            float light = PieceFxRules.seam((cy - top) / span, age);
            if (light < 0.03F) continue;
            float nx = q[20] * 0.05F, ny = q[21] * 0.05F, nz = q[22] * 0.05F;
            for (int e = 0; e < 4; e++) {
                int a = e * 5, b = ((e + 1) & 3) * 5;
                float ax = q[a], ay = q[a + 1], az = q[a + 2], bx = q[b], by = q[b + 1], bz = q[b + 2];
                // inner edge: 0.35 px towards the centre (clamped for small faces)
                float ka = inset(ax, ay, az, cx, cy, cz), kb = inset(bx, by, bz, cx, cy, cz);
                put(vc, m, ax + nx, ay + ny, az + nz, light);
                put(vc, m, bx + nx, by + ny, bz + nz, light);
                put(vc, m, bx + (cx - bx) * kb + nx, by + (cy - by) * kb + ny, bz + (cz - bz) * kb + nz, 0F);
                put(vc, m, ax + (cx - ax) * ka + nx, ay + (cy - ay) * ka + ny, az + (cz - az) * ka + nz, 0F);
            }
        }
        pose.popPose();
    }

    private static float inset(float x, float y, float z, float cx, float cy, float cz) {
        float d = (float) Math.sqrt((cx - x) * (cx - x) + (cy - y) * (cy - y) + (cz - z) * (cz - z));
        return d < 1.0E-4F ? 0F : Math.min(0.45F, 0.35F / d);
    }

    private static void put(VertexConsumer vc, Matrix4f m, float x, float y, float z, float light) {
        vc.addVertex(m, x / 16F, y / 16F, z / 16F).setColor(0.75F, 0.95F, 1F, Math.min(1F, light));
    }

    static void clear() {
        TRAILS.clear();
    }
}
