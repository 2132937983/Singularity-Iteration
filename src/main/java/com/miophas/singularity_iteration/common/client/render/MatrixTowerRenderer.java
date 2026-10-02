// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.block.producer.mio_icif_block_laser_tower;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_laser_tower;
import com.miophas.singularity_iteration.common.network.LaserTowerBeamPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Matrix Core Lightning Tower. The tower itself is a fixed, static block model (no turret, no
 * moving head); this renderer only draws the energy: a breathing glow around the floating core,
 * two counter-rotating containment rings, short crackling arcs from the core to the pylon coils,
 * and — on every discharge — a white flash and an omnidirectional shockwave. All tower bolts
 * leave from the core (published as the single emitter point for {@link LaserBeamRenderer}).
 */
@SuppressWarnings("null")
public class MatrixTowerRenderer implements BlockEntityRenderer<mio_icif_laser_tower> {
    /** Core centre in block pixels (matches tools/matrix_tower.py). */
    static final float CORE_Y = 7.75F / 16F;
    private static final float[][] COILS = {   // inner corners of the pylon coils, block units
        {4.2F / 16, 5.75F / 16, 4.2F / 16}, {11.8F / 16, 9.25F / 16, 4.2F / 16},
        {4.2F / 16, 9.25F / 16, 11.8F / 16}, {11.8F / 16, 5.75F / 16, 11.8F / 16},
        {4.2F / 16, 9.25F / 16, 4.2F / 16}, {11.8F / 16, 5.75F / 16, 4.2F / 16},
        {4.2F / 16, 5.75F / 16, 11.8F / 16}, {11.8F / 16, 9.25F / 16, 11.8F / 16}};
    private static final int SHOCK_TICKS = 9, FLASH_TICKS = 5, ALERT_TICKS = 60;

    public MatrixTowerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(mio_icif_laser_tower tower, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        Level level = tower.getLevel();
        if (level == null) return;
        BlockPos pos = tower.getBlockPos();
        // publish the emitter: every bolt leaves the core
        tower.clientMuzzles[0] = pos.getX() + 0.5;
        tower.clientMuzzles[1] = pos.getY() + CORE_Y;
        tower.clientMuzzles[2] = pos.getZ() + 0.5;
        tower.clientMuzzleCount = 1;
        tower.clientMuzzleFrame = System.nanoTime();

        boolean active = tower.getBlockState().hasProperty(mio_icif_block_laser_tower.ACTIVE)
            && tower.getBlockState().getValue(mio_icif_block_laser_tower.ACTIVE);
        if (!active) return;
        boolean sky = tower.spec().beamStyle() == LaserTowerBeamPacket.STYLE_SKY;
        float[] c = sky ? new float[]{0.78F, 0.45F, 1.0F} : new float[]{0.30F, 0.85F, 1.0F};

        long tick = level.getGameTime();
        float t = tick + partialTick;
        float sinceFire = tower.clientFireTime == Long.MIN_VALUE ? 1e9F : (tick - tower.clientFireTime) + partialTick;
        float sinceAlert = tower.clientAimTime == Long.MIN_VALUE ? 1e9F : (tick - tower.clientAimTime) + partialTick;
        float alert = sinceAlert < ALERT_TICKS ? 1 - sinceAlert / ALERT_TICKS : 0;
        float flash = sinceFire < FLASH_TICKS ? 1 - sinceFire / FLASH_TICKS : 0;

        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3f toCam = new Vector3f((float) (cam.x - pos.getX() - 0.5), (float) (cam.y - pos.getY() - CORE_Y),
            (float) (cam.z - pos.getZ() - 0.5)).normalize();
        Vector3f right = new Vector3f(0, 1, 0).cross(toCam);
        if (right.lengthSquared() < 1e-4F) right.set(1, 0, 0);
        right.normalize();
        Vector3f up = new Vector3f(toCam).cross(right).normalize();

        VertexConsumer vc = buffers.getBuffer(SiRenderTypes.ENERGY_BEAM);
        pose.pushPose();
        pose.translate(0.5, CORE_Y, 0.5);
        Matrix4f m = pose.last().pose();

        // ---- breathing core glow
        float breathe = 0.5F + 0.5F * Mth.sin(t * (0.18F + 0.25F * alert));
        float g = 0.22F + 0.05F * breathe + 0.06F * alert + 0.5F * flash;
        quad(vc, m, right, up, g * 1.9F, c, 0.10F + 0.08F * alert + 0.3F * flash);
        quad(vc, m, right, up, g, c, 0.35F + 0.15F * breathe + 0.4F * flash);
        quad(vc, m, right, up, 0.09F + 0.2F * flash, new float[]{1, 1, 1}, 0.55F + 0.45F * flash);

        // ---- two counter-rotating containment rings
        float spin = t * (0.05F + 0.12F * alert);
        ring(vc, m, toCam, 0.30F, spin, 0.6F, c, 0.55F + 0.3F * alert);
        ring(vc, m, toCam, 0.25F, -spin * 1.4F, -0.9F, c, 0.45F + 0.3F * alert);

        // ---- crackling arcs to the coils: a new pair every few ticks, faster when alerted
        int period = alert > 0 ? 2 : 5;
        long epoch = tick / period;
        for (int k = 0; k < (alert > 0 ? 3 : 1); k++) {
            long seed = mix(pos.asLong() * 31 + epoch * 7 + k);
            if (alert == 0 && (seed & 3) == 0) continue;   // idle: occasional silence
            float[] coil = COILS[(int) ((seed >>> 8) & 7)];
            arc(vc, m, toCam, new Vector3f(coil[0] - 0.5F, coil[1] - CORE_Y, coil[2] - 0.5F), seed, c,
                1 - (tick % period + partialTick) / period);
        }
        if (flash > 0) {   // a discharge lights every coil at once
            for (int i = 0; i < 4; i++) {
                float[] coil = COILS[i];
                arc(vc, m, toCam, new Vector3f(coil[0] - 0.5F, coil[1] - CORE_Y, coil[2] - 0.5F),
                    mix(tower.clientFireTime * 13 + i), c, flash);
            }
        }

        // ---- omnidirectional shockwave: a horizontal ring and a vertical halo expanding outward
        if (sinceFire < SHOCK_TICKS) {
            float p = sinceFire / SHOCK_TICKS;
            float r = 0.35F + 2.6F * (1 - (1 - p) * (1 - p));
            float a = (1 - p) * 0.7F;
            flatRing(vc, m, r, 0.10F + 0.12F * (1 - p), c, a);
            flatRing(vc, m, r * 0.7F, 0.05F, new float[]{1, 1, 1}, a * 0.6F);
            quad(vc, m, right, up, r * 0.9F, c, a * 0.25F);
        }
        pose.popPose();
    }

    /** Camera-facing square of half-size {@code s}, centred at the origin (soft glow via low alpha). */
    private static void quad(VertexConsumer vc, Matrix4f m, Vector3f r, Vector3f u, float s, float[] c, float a) {
        int R = (int) (c[0] * 255), G = (int) (c[1] * 255), B = (int) (c[2] * 255), A = Mth.clamp((int) (a * 255), 0, 255);
        // diamond-shaped (rotated 45°) so additive layers read as a round-ish glow
        vc.addVertex(m, -r.x * s, -r.y * s, -r.z * s).setColor(R, G, B, 0);
        vc.addVertex(m, -u.x * s, -u.y * s, -u.z * s).setColor(R, G, B, 0);
        vc.addVertex(m, 0, 0, 0).setColor(R, G, B, A);
        vc.addVertex(m, 0, 0, 0).setColor(R, G, B, A);
        vc.addVertex(m, -u.x * s, -u.y * s, -u.z * s).setColor(R, G, B, 0);
        vc.addVertex(m, r.x * s, r.y * s, r.z * s).setColor(R, G, B, 0);
        vc.addVertex(m, 0, 0, 0).setColor(R, G, B, A);
        vc.addVertex(m, 0, 0, 0).setColor(R, G, B, A);
        vc.addVertex(m, r.x * s, r.y * s, r.z * s).setColor(R, G, B, 0);
        vc.addVertex(m, u.x * s, u.y * s, u.z * s).setColor(R, G, B, 0);
        vc.addVertex(m, 0, 0, 0).setColor(R, G, B, A);
        vc.addVertex(m, 0, 0, 0).setColor(R, G, B, A);
        vc.addVertex(m, u.x * s, u.y * s, u.z * s).setColor(R, G, B, 0);
        vc.addVertex(m, -r.x * s, -r.y * s, -r.z * s).setColor(R, G, B, 0);
        vc.addVertex(m, 0, 0, 0).setColor(R, G, B, A);
        vc.addVertex(m, 0, 0, 0).setColor(R, G, B, A);
    }

    /** Thin tilted ring around the core, drawn as camera-facing segments. */
    private static void ring(VertexConsumer vc, Matrix4f m, Vector3f toCam, float radius, float spin, float tilt,
                             float[] c, float a) {
        int n = 28;
        Vector3f prev = null;
        float ct = Mth.cos(tilt), st = Mth.sin(tilt);
        for (int i = 0; i <= n; i++) {
            float ang = spin + i * Mth.TWO_PI / n;
            float x = Mth.cos(ang) * radius, z = Mth.sin(ang) * radius;
            Vector3f p = new Vector3f(x, z * st, z * ct);
            float cs = Mth.cos(spin * 0.5F), ss = Mth.sin(spin * 0.5F);   // slow precession about Y
            p.set(p.x * cs + p.z * ss, p.y, -p.x * ss + p.z * cs);
            if (prev != null) segment(vc, m, toCam, prev, p, 0.012F, c, a * (0.55F + 0.45F * ((i & 1) == 0 ? 1 : 0.6F)), a * 0.9F);
            prev = p;
        }
    }

    /** Jagged lightning from the core to {@code end}. */
    private static void arc(VertexConsumer vc, Matrix4f m, Vector3f toCam, Vector3f end, long seed, float[] c, float a) {
        int n = 6;
        Vector3f prev = new Vector3f();
        for (int i = 1; i <= n; i++) {
            float f = i / (float) n;
            Vector3f p = new Vector3f(end).mul(f);
            if (i < n) {
                float j = 0.06F * Mth.sin(f * Mth.PI);
                p.add(unit(seed, i * 3) * j, unit(seed, i * 3 + 1) * j, unit(seed, i * 3 + 2) * j);
            }
            segment(vc, m, toCam, prev, p, 0.035F, c, a * 0.35F, a * 0.35F);
            segment(vc, m, toCam, prev, p, 0.010F, new float[]{1, 1, 1}, a * 0.9F, a * 0.9F);
            prev = p;
        }
    }

    private static void segment(VertexConsumer vc, Matrix4f m, Vector3f toCam, Vector3f a, Vector3f b, float w,
                                float[] c, float alphaA, float alphaB) {
        Vector3f d = new Vector3f(b).sub(a);
        Vector3f s = d.cross(toCam, new Vector3f());
        if (s.lengthSquared() < 1e-8F) return;
        s.normalize().mul(w);
        int R = (int) (c[0] * 255), G = (int) (c[1] * 255), B = (int) (c[2] * 255);
        int A0 = Mth.clamp((int) (alphaA * 255), 0, 255), A1 = Mth.clamp((int) (alphaB * 255), 0, 255);
        vc.addVertex(m, a.x + s.x, a.y + s.y, a.z + s.z).setColor(R, G, B, A0);
        vc.addVertex(m, a.x - s.x, a.y - s.y, a.z - s.z).setColor(R, G, B, A0);
        vc.addVertex(m, b.x - s.x, b.y - s.y, b.z - s.z).setColor(R, G, B, A1);
        vc.addVertex(m, b.x + s.x, b.y + s.y, b.z + s.z).setColor(R, G, B, A1);
    }

    /** Horizontal annulus of radius r and band width w at core height. */
    private static void flatRing(VertexConsumer vc, Matrix4f m, float r, float w, float[] c, float a) {
        int n = 40;
        int R = (int) (c[0] * 255), G = (int) (c[1] * 255), B = (int) (c[2] * 255), A = Mth.clamp((int) (a * 255), 0, 255);
        for (int i = 0; i < n; i++) {
            float a0 = i * Mth.TWO_PI / n, a1 = (i + 1) * Mth.TWO_PI / n;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float ri = r - w;
            vc.addVertex(m, c0 * ri, 0, s0 * ri).setColor(R, G, B, 0);
            vc.addVertex(m, c1 * ri, 0, s1 * ri).setColor(R, G, B, 0);
            vc.addVertex(m, c1 * r, 0, s1 * r).setColor(R, G, B, A);
            vc.addVertex(m, c0 * r, 0, s0 * r).setColor(R, G, B, A);
            float ro = r + w * 0.5F;
            vc.addVertex(m, c0 * r, 0, s0 * r).setColor(R, G, B, A);
            vc.addVertex(m, c1 * r, 0, s1 * r).setColor(R, G, B, A);
            vc.addVertex(m, c1 * ro, 0, s1 * ro).setColor(R, G, B, 0);
            vc.addVertex(m, c0 * ro, 0, s0 * ro).setColor(R, G, B, 0);
        }
    }

    private static float unit(long seed, int i) {
        return ((mix(seed + i * 0x9E3779B97F4A7C15L) >>> 40) / (float) (1L << 24)) * 2 - 1;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    @Override
    public AABB getRenderBoundingBox(mio_icif_laser_tower tower) {
        return new AABB(tower.getBlockPos()).inflate(3.0);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
