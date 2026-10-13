// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.client.suit.FcsClientConfig;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Shared drawing of the energy bolts (0.1.8.3): the rifles, plasma gun and tachyon disruptor
 * ({@code energy_bullet}) and the mining laser ({@code laser_bullet}).
 *
 * <p>A bolt is three additive, camera-facing ribbons (wide halo, coloured glow, white-hot core)
 * from its head back to its tail, plus a round bloom at the head, in {@link SiRenderTypes#ENERGY_BEAM}
 * (lightning shader: full bright, additive, no depth write).
 *
 * <p><b>Tracer.</b> Up to 0.1.8.2 a bolt was drawn only behind its own position. Seen from the
 * shooter's eye it pointed straight away from the camera and collapsed to a dot, so the own
 * shots looked as if no beam was drawn. The tail now starts at the muzzle of the shooter's gun
 * for the first {@link #TRACER_TICKS} ticks and then pulls in behind the head. In first person the
 * muzzle sits right and below the eye, so the beam crosses the view at an angle and shows.
 */
public final class BeamFx {
    private BeamFx() { }

    /** Ticks during which the tail is anchored at the muzzle. */
    public static final float TRACER_TICKS = 3.0F;

    /**
     * World position of the muzzle of the gun held by {@code shooter} in {@code mainHand}: in first
     * person (own view) right and below the eye, else at the gun of the third-person model.
     */
    public static Vec3 muzzle(LivingEntity shooter, boolean mainHand, float partial) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 look = shooter.getViewVector(partial);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = look.cross(up).lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : look.cross(up).normalize();
        Vec3 upv = right.cross(look).normalize();
        boolean rightHand = mainHand == (shooter.getMainArm() == HumanoidArm.RIGHT);
        boolean firstPerson = shooter == mc.player && mc.options.getCameraType().isFirstPerson();
        double side = (rightHand ? 1 : -1) * (firstPerson ? 0.26 : 0.36);
        return shooter.getEyePosition(partial).add(look.scale(firstPerson ? 0.85 : 0.95))
            .add(right.scale(side)).add(upv.scale(firstPerson ? -0.2 : -0.3));
    }

    /**
     * Draws one bolt. All positions are relative to the pose origin (the interpolated entity
     * position for an entity renderer).
     *
     * @param head    bolt head
     * @param tail    bolt tail
     * @param toCam   unit vector from the bolt toward the camera
     * @param rgb     glow colour 0xRRGGBB
     * @param width   core half width in blocks (the glow is 3.2x, the halo 7.5x)
     * @param alpha   overall opacity 0..1
     */
    public static void bolt(VertexConsumer vc, Matrix4f m, Vec3 head, Vec3 tail, Vec3 toCam, int rgb, float width, float alpha) {
        Vec3 axis = head.subtract(tail);
        if (axis.lengthSqr() < 1.0E-6) return;
        Vec3 side = axis.cross(toCam);
        if (side.lengthSqr() < 1.0E-8) side = axis.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-8) side = new Vec3(1, 0, 0);
        side = side.normalize();
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        int cr = Math.min(255, 200 + r / 4), cg = Math.min(255, 200 + g / 4), cb = Math.min(255, 200 + b / 4);
        ribbon(vc, m, head, tail, side, width * 7.5F, r, g, b, (int) (40 * alpha));
        ribbon(vc, m, head, tail, side, width * 3.2F, r, g, b, (int) (150 * alpha));
        ribbon(vc, m, head, tail, side, width, cr, cg, cb, (int) (255 * alpha));
        Vec3 up = side.cross(toCam).normalize();
        bloom(vc, m, head, side, up, width * 6.5F, r, g, b, (int) (130 * alpha));
        bloom(vc, m, head, side, up, width * 2.4F, cr, cg, cb, (int) (255 * alpha));
    }

    /**
     * Tail of a bolt: anchored at the muzzle while young, then {@code length} behind the head.
     * Null while the head is still behind the muzzle (the bolt spawns at the eye): draw nothing.
     */
    @org.jetbrains.annotations.Nullable
    public static Vec3 tail(Projectile bolt, Vec3 head, Vec3 dir, float length, float age, float partial) {
        Vec3 natural = head.subtract(dir.scale(length));
        if (!FcsClientConfig.beamTracers() || age >= TRACER_TICKS || !(bolt.getOwner() instanceof LivingEntity shooter)) return natural;
        boolean main = !(shooter.getOffhandItem().getItem() instanceof com.miophas.singularity_iteration.core.api.item.IWeaponItem)
            || shooter.getMainHandItem().getItem() instanceof com.miophas.singularity_iteration.core.api.item.IWeaponItem;
        Vec3 muzzle = muzzle(shooter, main, partial).subtract(bolt.getPosition(partial));
        float t = Mth.clamp(age / TRACER_TICKS, 0, 1);
        t = t * t;
        // the head has not left the barrel yet: nothing to draw this frame
        if (muzzle.subtract(head).dot(dir) > 0) return null;
        return new Vec3(Mth.lerp(t, muzzle.x, natural.x), Mth.lerp(t, muzzle.y, natural.y), Mth.lerp(t, muzzle.z, natural.z));
    }

    /** Unit vector from {@code entity} (interpolated) toward the camera. */
    public static Vec3 toCamera(Entity entity, Vec3 cam, float partial) {
        Vec3 d = cam.subtract(entity.getPosition(partial));
        return d.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : d.normalize();
    }

    private static void ribbon(VertexConsumer vc, Matrix4f m, Vec3 head, Vec3 tail, Vec3 side, float w, int r, int g, int b, int a) {
        if (a <= 2) return;
        float tw = w * 0.3F;
        vc.addVertex(m, (float) (head.x + side.x * w), (float) (head.y + side.y * w), (float) (head.z + side.z * w)).setColor(r, g, b, a);
        vc.addVertex(m, (float) (head.x - side.x * w), (float) (head.y - side.y * w), (float) (head.z - side.z * w)).setColor(r, g, b, a);
        vc.addVertex(m, (float) (tail.x - side.x * tw), (float) (tail.y - side.y * tw), (float) (tail.z - side.z * tw)).setColor(r, g, b, 0);
        vc.addVertex(m, (float) (tail.x + side.x * tw), (float) (tail.y + side.y * tw), (float) (tail.z + side.z * tw)).setColor(r, g, b, 0);
    }

    /** Round soft glow: a fan of 12 triangles (as degenerate quads) fading from the centre to the rim. */
    private static void bloom(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 s, Vec3 u, float w, int r, int g, int b, int a) {
        if (a <= 2) return;
        int n = 12;
        for (int i = 0; i < n; i++) {
            float a0 = i * Mth.TWO_PI / n, a1 = (i + 1) * Mth.TWO_PI / n;
            float c0 = Mth.cos(a0) * w, s0 = Mth.sin(a0) * w, c1 = Mth.cos(a1) * w, s1 = Mth.sin(a1) * w;
            vc.addVertex(m, (float) c.x, (float) c.y, (float) c.z).setColor(r, g, b, a);
            vc.addVertex(m, (float) (c.x + s.x * c0 + u.x * s0), (float) (c.y + s.y * c0 + u.y * s0), (float) (c.z + s.z * c0 + u.z * s0)).setColor(r, g, b, 0);
            vc.addVertex(m, (float) (c.x + s.x * c1 + u.x * s1), (float) (c.y + s.y * c1 + u.y * s1), (float) (c.z + s.z * c1 + u.z * s1)).setColor(r, g, b, 0);
            vc.addVertex(m, (float) c.x, (float) c.y, (float) c.z).setColor(r, g, b, a);
        }
    }
}
