// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.render;

import com.miophas.singularity_iteration.common.item.tools.WeaponChargeFx;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * Client side of {@link WeaponChargeFx} (0.1.8.0): converging spark ring, compressed core and
 * release flash at the muzzle. The particle count is at most 6 + 26 x power (32).
 */
@OnlyIn(Dist.CLIENT)
public final class WeaponChargeFxClient {
    private WeaponChargeFxClient() {}

    public static void play(WeaponChargeFx.Shot shot) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(shot.entityId());
        if (!(e instanceof LivingEntity shooter)) return;
        Vec3 look = shooter.getViewVector(1.0F);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = look.cross(up).lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : look.cross(up).normalize();
        Vec3 upv = right.cross(look).normalize();
        boolean rightHand = shot.mainHand() == (shooter.getMainArm() == HumanoidArm.RIGHT);
        boolean firstPerson = shooter == mc.player && mc.options.getCameraType().isFirstPerson();
        double side = (rightHand ? 1 : -1) * (firstPerson ? 0.28 : 0.38);
        Vec3 muzzle = shooter.getEyePosition().add(look.scale(firstPerson ? 0.9 : 1.0)).add(right.scale(side)).add(upv.scale(firstPerson ? -0.22 : -0.3));
        float p = shot.power();
        int c = shot.color();
        Vector3f rgb = new Vector3f((c >> 16 & 255) / 255F, (c >> 8 & 255) / 255F, (c & 255) / 255F);
        // converging ring: particles start on a ring around the muzzle and fly into it in about 4 ticks
        int ring = 6 + Math.round(18 * p);
        double radius = 0.25 + 0.45 * p;
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
        // release: a forward burst along the bolt and (for strong shots) a flash
        for (int i = 0; i < 3 + Math.round(3 * p); i++) {
            Vec3 v = look.scale(0.3 + 0.4 * mc.level.random.nextDouble());
            mc.level.addParticle(ParticleTypes.END_ROD, muzzle.x, muzzle.y, muzzle.z, v.x, v.y, v.z);
        }
        if (p >= 0.75F) mc.level.addParticle(ParticleTypes.FLASH, muzzle.x, muzzle.y, muzzle.z, 0, 0, 0);
    }
}
