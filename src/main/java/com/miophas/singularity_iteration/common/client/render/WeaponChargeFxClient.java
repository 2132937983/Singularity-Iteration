// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.item.tools.WeaponChargeFx;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Client side of {@link WeaponChargeFx} (0.1.8.0): converging spark ring, compressed core and
 * release at the muzzle; since 0.1.8.3 also the muzzle flash and the camera kick.
 *
 * <p><b>0.1.8.3 screen flash fix.</b> Shots of 7 500 EU and more (power 0.75: the tactical laser
 * rifle, the tachyon disruptor and the strong modes of the plasma gun and mining laser) spawned
 * the vanilla {@code FLASH} particle at the muzzle. That particle is the firework flash: a
 * camera-facing quad about 7 blocks across. In first person the muzzle is under a block from the
 * eye, so it covered the whole screen in white, every shot (every 3 ticks with the tactical
 * rifle). It is replaced by a muzzle flash of the right size: a short star flare and bloom in the
 * gun colour at the muzzle (0.1 to 0.26 blocks, 90 ms), drawn additive in the world, which
 * follows the gun while it lives. The client options {@code muzzle_flash} and
 * {@code reduced_flashes} control it.
 *
 * <p><b>Camera kick.</b> The own shots tip the view up by 0.5 to 2.4 degrees (by shot power) with
 * a little roll, then it settles in about 150 ms. Only the rendered camera moves; the aim and the
 * player rotation do not. Option {@code recoil_camera} scales it (0 = off).
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
public final class WeaponChargeFxClient {
    private WeaponChargeFxClient() {}

    private static final long FLASH_NANOS = 90_000_000L;
    private static final int MAX_FLASHES = 32;

    private record Flash(int entityId, boolean mainHand, int color, float power, long start) { }
    private static final List<Flash> FLASHES = new ArrayList<>();

    private static float kick, kickRoll;
    private static long kickStart;

    public static void play(WeaponChargeFx.Shot shot) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(shot.entityId());
        if (!(e instanceof LivingEntity shooter)) return;
        Vec3 look = shooter.getViewVector(1.0F);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = look.cross(up).lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : look.cross(up).normalize();
        Vec3 upv = right.cross(look).normalize();
        boolean firstPerson = shooter == mc.player && mc.options.getCameraType().isFirstPerson();
        Vec3 muzzle = BeamFx.muzzle(shooter, shot.mainHand(), 1.0F);
        float p = shot.power();
        int c = shot.color();
        Vector3f rgb = new Vector3f((c >> 16 & 255) / 255F, (c >> 8 & 255) / 255F, (c & 255) / 255F);
        // converging ring: particles start on a ring around the muzzle and fly into it in about 4 ticks
        int ring = (6 + Math.round(18 * p)) / (firstPerson ? 2 : 1);
        double radius = (0.25 + 0.45 * p) * (firstPerson ? 0.6 : 1.0);
        for (int i = 0; i < ring; i++) {
            double a = i * Math.PI * 2 / ring;
            Vec3 off = right.scale(Math.cos(a) * radius).add(upv.scale(Math.sin(a) * radius)).add(look.scale(-0.1 * p));
            Vec3 at = muzzle.add(off);
            Vec3 v = off.scale(-0.25);
            mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, v.x, v.y, v.z);
        }
        // compressed core: coloured dust packed at the muzzle, larger with power
        for (int i = 0; i < 2 + Math.round(5 * p); i++) {
            mc.level.addParticle(new DustParticleOptions(rgb, 0.6F + 1.2F * p), muzzle.x, muzzle.y, muzzle.z,
                (mc.level.random.nextDouble() - 0.5) * 0.02, (mc.level.random.nextDouble() - 0.5) * 0.02, (mc.level.random.nextDouble() - 0.5) * 0.02);
        }
        // release: a forward burst along the bolt
        for (int i = 0; i < 3 + Math.round(3 * p); i++) {
            Vec3 v = look.scale(0.3 + 0.4 * mc.level.random.nextDouble());
            mc.level.addParticle(ParticleTypes.END_ROD, muzzle.x, muzzle.y, muzzle.z, v.x, v.y, v.z);
        }
        if (FcsClientConfig.muzzleFlash()) {
            if (FLASHES.size() >= MAX_FLASHES) FLASHES.remove(0);
            FLASHES.add(new Flash(shot.entityId(), shot.mainHand(), c, p, System.nanoTime()));
        }
        if (shooter == mc.player) {
            float strength = FcsClientConfig.recoilCamera();
            if (strength > 0) {
                kick = Math.min(4.0F, (0.5F + 1.9F * p) * strength);
                kickRoll = (mc.level.random.nextFloat() - 0.5F) * 1.2F * strength * p;
                kickStart = System.nanoTime();
            }
        }
    }

    /** Muzzle flashes: a 4-point star and a bloom in the gun colour, additive, at the live muzzle position. */
    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || FLASHES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { FLASHES.clear(); return; }
        long now = System.nanoTime();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        Vector3f left = new Vector3f(event.getCamera().getLeftVector()), upv = new Vector3f(event.getCamera().getUpVector());
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = null;
        Matrix4f m = event.getPoseStack().last().pose();
        float scale = FcsClientConfig.flashScale();
        for (Iterator<Flash> it = FLASHES.iterator(); it.hasNext(); ) {
            Flash f = it.next();
            float t = (now - f.start) / (float) FLASH_NANOS;
            if (t >= 1 || !(mc.level.getEntity(f.entityId) instanceof LivingEntity shooter)) { it.remove(); continue; }
            Vec3 at = BeamFx.muzzle(shooter, f.mainHand, partial).subtract(cam);
            boolean own = shooter == mc.player && mc.options.getCameraType().isFirstPerson();
            float size = (0.10F + 0.16F * f.power) * (own ? 0.75F : 1.0F) * (1.0F + 0.35F * t);
            float a = (1 - t) * (1 - t) * scale;
            int r = f.color >> 16 & 255, g = f.color >> 8 & 255, b = f.color & 255;
            if (vc == null) vc = buffers.getBuffer(SiRenderTypes.ENERGY_BEAM);
            float spin = (f.entityId * 0.7F + f.start % 1000 * 0.0063F);
            star(vc, m, at, left, upv, size * 1.9F, spin, r, g, b, (int) (170 * a));
            glow(vc, m, at, left, upv, size, r, g, b, (int) (150 * a));
            glow(vc, m, at, left, upv, size * 0.42F, 255, 250, 240, (int) (255 * a));
        }
        if (vc != null) buffers.endBatch(SiRenderTypes.ENERGY_BEAM);
    }

    /** Camera kick of the own shots: quick rise (20 ms), exponential settle (about 150 ms). */
    @SubscribeEvent
    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (kick <= 0) return;
        float t = (System.nanoTime() - kickStart) / 1.0E9F;
        if (t > 0.4F) { kick = 0; return; }
        float env = Mth.clamp(t / 0.02F, 0, 1) * (float) Math.exp(-t / 0.05F);
        event.setPitch(event.getPitch() - kick * env);
        event.setRoll(event.getRoll() + kickRoll * env);
    }

    private static void glow(VertexConsumer vc, Matrix4f m, Vec3 c, Vector3f l, Vector3f u, float w, int r, int g, int b, int a) {
        if (a <= 2) return;
        int n = 12;
        for (int i = 0; i < n; i++) {
            float a0 = i * Mth.TWO_PI / n, a1 = (i + 1) * Mth.TWO_PI / n;
            float c0 = Mth.cos(a0) * w, s0 = Mth.sin(a0) * w, c1 = Mth.cos(a1) * w, s1 = Mth.sin(a1) * w;
            vc.addVertex(m, (float) c.x, (float) c.y, (float) c.z).setColor(r, g, b, a);
            vc.addVertex(m, (float) c.x + l.x * c0 + u.x * s0, (float) c.y + l.y * c0 + u.y * s0, (float) c.z + l.z * c0 + u.z * s0).setColor(r, g, b, 0);
            vc.addVertex(m, (float) c.x + l.x * c1 + u.x * s1, (float) c.y + l.y * c1 + u.y * s1, (float) c.z + l.z * c1 + u.z * s1).setColor(r, g, b, 0);
            vc.addVertex(m, (float) c.x, (float) c.y, (float) c.z).setColor(r, g, b, a);
        }
    }

    /** Four thin rays from the centre, turned by {@code spin} radians. */
    private static void star(VertexConsumer vc, Matrix4f m, Vec3 c, Vector3f l, Vector3f u, float len, float spin, int r, int g, int b, int a) {
        if (a <= 2) return;
        for (int i = 0; i < 4; i++) {
            float ang = spin + i * Mth.HALF_PI;
            float dx = Mth.cos(ang), dy = Mth.sin(ang);
            float px = -dy * len * 0.09F, py = dx * len * 0.09F;      // ray half width
            float tx = dx * len, ty = dy * len;
            vc.addVertex(m, (float) c.x + l.x * px + u.x * py, (float) c.y + l.y * px + u.y * py, (float) c.z + l.z * px + u.z * py).setColor(r, g, b, a);
            vc.addVertex(m, (float) c.x - l.x * px - u.x * py, (float) c.y - l.y * px - u.y * py, (float) c.z - l.z * px - u.z * py).setColor(r, g, b, a);
            vc.addVertex(m, (float) c.x + l.x * tx + u.x * ty, (float) c.y + l.y * tx + u.y * ty, (float) c.z + l.z * tx + u.z * ty).setColor(r, g, b, 0);
            vc.addVertex(m, (float) c.x + l.x * tx + u.x * ty, (float) c.y + l.y * tx + u.y * ty, (float) c.z + l.z * tx + u.z * ty).setColor(r, g, b, 0);
        }
    }
}
