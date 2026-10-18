// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.miophas.singularity_iteration.common.item.tools.mio_icif_plasma_bullet;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Plasma bolt of the plasma launcher (0.1.10.4 rewrite).
 *
 * <p>Up to 0.1.10.3 this was one camera-facing quad with {@code beam.png} (a flat red square) at
 * the bolt position. The bolt is now a heavy plasma lance, all additive and full bright in
 * {@link SiRenderTypes#ENERGY_BEAM}:
 * <ul>
 *   <li>a white-hot core inside a violet glow and a wide blue sheath ({@link BeamFx#bolt}), from
 *       the head back along the flight path (at most {@link #TRAIL} blocks, never behind the
 *       launch point);</li>
 *   <li>two cyan magnetic-confinement strands that spiral around the lance and pinch toward the
 *       tail;</li>
 *   <li>three confinement rings that slide back along the lance and open as they fall behind;</li>
 *   <li>a pulsing head bloom with a four-point flare.</li>
 * </ul>
 * For the first {@link #TRACER_TICKS} ticks the tail starts at the muzzle of the gun, so the own
 * shot leaves the barrel visibly and then joins the line of the crosshair (the bolt itself flies
 * exactly along the view, from the eye).
 */
@SuppressWarnings("null")
public class mio_icif_PlasmaBullet extends EntityRenderer<mio_icif_plasma_bullet> {

    public static final ResourceLocation BEAM_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "mio_icif", "textures/entity/beam.png"
    );

    /** Longest lance behind the head, in blocks. */
    private static final float TRAIL = 9.0F;
    /** Ticks during which the tail is anchored at the muzzle (the bolt covers 8 blocks per tick). */
    private static final float TRACER_TICKS = 1.25F;
    private static final int CORE = 0xB45CFF, SHEATH = 0x4A6BFF, STRAND = 0x7FE9FF;

    public mio_icif_PlasmaBullet(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(mio_icif_plasma_bullet entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Vec3 motion = entity.getDeltaMovement();
        double speed = motion.length();
        if (speed < 1.0E-4) return;
        Vec3 dir = motion.scale(1 / speed);
        float age = entity.tickCount + partialTicks;
        Vec3 pos = entity.getPosition(partialTicks);
        // length: the path flown since the launch point, at most TRAIL
        Vec3 origin = entity.origin();
        double flown = origin.lengthSqr() > 0 ? pos.subtract(origin).dot(dir) : speed * age;
        float length = (float) Mth.clamp(flown, 0.4, TRAIL);
        Vec3 head = Vec3.ZERO;
        Vec3 tail = tail(entity, dir, length, age, partialTicks);
        if (tail == null) return;
        Vec3 toCam = BeamFx.toCamera(entity, this.entityRenderDispatcher.camera.getPosition(), partialTicks);
        float alpha = 0.6F + 0.4F * FcsClientConfig.flashScale();
        float pulse = 0.85F + 0.15F * Mth.sin(age * 3.1F + entity.getId());
        Matrix4f m = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(SiRenderTypes.ENERGY_BEAM);

        // sheath, then the violet lance with its white core and head bloom
        BeamFx.bolt(vc, m, head, tail, toCam, SHEATH, 0.06F * pulse, 0.45F * alpha);
        BeamFx.bolt(vc, m, head, tail, toCam, CORE, 0.034F * pulse, alpha);

        Vec3 axis = tail.subtract(head);
        double len = axis.length();
        if (len > 1.0E-3) {
            Vec3 back = axis.scale(1 / len);
            Vec3 u = perp(back), w = back.cross(u).normalize();
            strands(vc, m, head, back, u, w, len, age, toCam, alpha);
            rings(vc, m, head, back, u, w, len, age, alpha);
        }
        flare(vc, m, head, toCam, 0.55F + 0.2F * pulse, alpha);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Tail of the lance: from the muzzle while young (own shots leave the barrel), then behind the head. */
    @org.jetbrains.annotations.Nullable
    private static Vec3 tail(mio_icif_plasma_bullet bolt, Vec3 dir, float length, float age, float partial) {
        Vec3 natural = dir.scale(-length);
        if (!FcsClientConfig.beamTracers() || age >= TRACER_TICKS || !(bolt.getOwner() instanceof LivingEntity shooter)) return natural;
        boolean main = shooter.getMainHandItem().getItem() instanceof com.miophas.singularity_iteration.common.item.tools.mio_icif_plasma_launcher
            || !(shooter.getOffhandItem().getItem() instanceof com.miophas.singularity_iteration.common.item.tools.mio_icif_plasma_launcher);
        Vec3 muzzle = BeamFx.muzzle(shooter, main, partial).subtract(bolt.getPosition(partial));
        if (muzzle.dot(dir) > 0) return null;                    // still inside the barrel
        float t = Mth.clamp(age / TRACER_TICKS, 0, 1);
        t = t * t;
        return new Vec3(Mth.lerp(t, muzzle.x, natural.x), Mth.lerp(t, muzzle.y, natural.y), Mth.lerp(t, muzzle.z, natural.z));
    }

    /** Two cyan strands that spiral around the lance; the radius pinches from 0.2 at the head to 0.05 at the tail. */
    private static void strands(VertexConsumer vc, Matrix4f m, Vec3 head, Vec3 back, Vec3 u, Vec3 w, double len, float age,
                                Vec3 toCam, float alpha) {
        int n = Math.max(8, (int) (len * 6));
        int r = STRAND >> 16 & 255, g = STRAND >> 8 & 255, b = STRAND & 255;
        for (int strand = 0; strand < 2; strand++) {
            Vec3 prev = null;
            float prevA = 0;
            for (int i = 0; i <= n; i++) {
                double k = i / (double) n;
                double z = k * len;
                double radius = 0.2 - 0.15 * k;
                double ang = z * 2.4 - age * 1.7 + strand * Math.PI;
                Vec3 p = head.add(back.scale(z)).add(u.scale(Math.cos(ang) * radius)).add(w.scale(Math.sin(ang) * radius));
                float a = (float) (alpha * 0.85 * (1 - k) * (1 - k));
                if (prev != null) segment(vc, m, prev, p, toCam, 0.012F, r, g, b, (int) (255 * prevA), (int) (255 * a));
                prev = p;
                prevA = a;
            }
        }
    }

    /** Three rings that slide back along the lance and open while they fade. */
    private static void rings(VertexConsumer vc, Matrix4f m, Vec3 head, Vec3 back, Vec3 u, Vec3 w, double len, float age, float alpha) {
        double spacing = 2.4;
        double shift = (age * 3.0) % spacing;
        for (int i = 0; i < 3; i++) {
            double z = 0.6 + shift + i * spacing;
            if (z > len) continue;
            double k = z / Math.max(len, 1.0);
            float rad = (float) (0.18 + 0.32 * k);
            int a = (int) (200 * alpha * (1 - k));
            ring(vc, m, head.add(back.scale(z)), u, w, rad, rad * 0.55F, CORE, a);
        }
    }

    /** Pulsing head: a white core bloom, a violet halo and a four-point flare across the view. */
    private static void flare(VertexConsumer vc, Matrix4f m, Vec3 head, Vec3 toCam, float size, float alpha) {
        Vec3 s = perp(toCam), up = s.cross(toCam).normalize();
        disc(vc, m, head, s, up, size, CORE, (int) (150 * alpha));
        disc(vc, m, head, s, up, size * 0.4F, 0xFFFFFF, (int) (255 * alpha));
        for (int k = 0; k < 2; k++) spike(vc, m, head, k == 0 ? s : up, toCam, size * 1.9F, 0.035F, (int) (230 * alpha));
    }

    // ------------------------------------------------------------------ primitives

    private static Vec3 perp(Vec3 v) {
        Vec3 c = v.cross(new Vec3(0, 1, 0));
        if (c.lengthSqr() < 1.0E-6) c = v.cross(new Vec3(1, 0, 0));
        return c.normalize();
    }

    /** Camera-facing ribbon from a to b, alpha aA at a and aB at b. */
    private static void segment(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 toCam, float w, int r, int g, int bl, int aA, int aB) {
        if (aA <= 2 && aB <= 2) return;
        Vec3 d = b.subtract(a);
        Vec3 side = d.cross(toCam);
        if (side.lengthSqr() < 1.0E-10) return;
        side = side.normalize().scale(w);
        put(vc, m, a.add(side), r, g, bl, aA);
        put(vc, m, a.subtract(side), r, g, bl, aA);
        put(vc, m, b.subtract(side), r, g, bl, aB);
        put(vc, m, b.add(side), r, g, bl, aB);
    }

    /** A thin spike of the flare: bright at the centre, fading to both ends. */
    private static void spike(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 axis, Vec3 toCam, float half, float w, int a) {
        int r = 255, g = 236, b = 255;
        segment(vc, m, c, c.add(axis.scale(half)), toCam, w, r, g, b, a, 0);
        segment(vc, m, c, c.subtract(axis.scale(half)), toCam, w, r, g, b, a, 0);
    }

    /** Ring in the plane (u, w): opaque at the mid radius, clear at the inner and outer edge. */
    private static void ring(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 u, Vec3 w, float outer, float inner, int rgb, int a) {
        if (a <= 2) return;
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        float mid = (outer + inner) / 2;
        int n = 20;
        for (int i = 0; i < n; i++) {
            double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n;
            Vec3 d0 = u.scale(Math.cos(a0)).add(w.scale(Math.sin(a0))), d1 = u.scale(Math.cos(a1)).add(w.scale(Math.sin(a1)));
            put(vc, m, c.add(d0.scale(inner)), r, g, b, 0);
            put(vc, m, c.add(d1.scale(inner)), r, g, b, 0);
            put(vc, m, c.add(d1.scale(mid)), r, g, b, a);
            put(vc, m, c.add(d0.scale(mid)), r, g, b, a);
            put(vc, m, c.add(d0.scale(mid)), r, g, b, a);
            put(vc, m, c.add(d1.scale(mid)), r, g, b, a);
            put(vc, m, c.add(d1.scale(outer)), r, g, b, 0);
            put(vc, m, c.add(d0.scale(outer)), r, g, b, 0);
        }
    }

    /** Soft round glow facing the camera. */
    private static void disc(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 s, Vec3 up, float rad, int rgb, int a) {
        if (a <= 2) return;
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        int n = 14;
        for (int i = 0; i < n; i++) {
            float a0 = i * Mth.TWO_PI / n, a1 = (i + 1) * Mth.TWO_PI / n;
            put(vc, m, c, r, g, b, a);
            put(vc, m, c.add(s.scale(Mth.cos(a0) * rad)).add(up.scale(Mth.sin(a0) * rad)), r, g, b, 0);
            put(vc, m, c.add(s.scale(Mth.cos(a1) * rad)).add(up.scale(Mth.sin(a1) * rad)), r, g, b, 0);
            put(vc, m, c, r, g, b, a);
        }
    }

    private static void put(VertexConsumer vc, Matrix4f m, Vec3 p, int r, int g, int b, int a) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, Math.max(0, Math.min(255, a)));
    }

    @Override
    public boolean shouldRender(mio_icif_plasma_bullet entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        // the lance reaches back up to TRAIL blocks: test a box that covers it
        return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(TRAIL));
    }

    @Override
    public ResourceLocation getTextureLocation(mio_icif_plasma_bullet entity) {
        return BEAM_TEXTURE;
    }
}
