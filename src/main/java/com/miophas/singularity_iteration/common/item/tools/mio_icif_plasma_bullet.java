// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** Launcher projectile, using the admitted generic SI energy bullet's base damage policy. */
public class mio_icif_plasma_bullet extends IndependentElectricProjectile {
    // Independent candidate tuning; original launcher speed/damage await game observation.
    public static final double PLASMA_SPEED = 3.0D;
    public static final float PLASMA_DAMAGE = 5.0F;
    public mio_icif_plasma_bullet(EntityType<? extends mio_icif_plasma_bullet> type, Level level) {
        super(type, level);
    }
    public mio_icif_plasma_bullet(EntityType<? extends mio_icif_plasma_bullet> type, LivingEntity owner, Level level) {
        super(type, owner, level);
    }
    @Override protected void onHitEntity(EntityHitResult hit) {
        if (level().isClientSide) return;
        damage(hit.getEntity(), PLASMA_DAMAGE);
        discard();
    }
    @Override protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide) discard();
    }
}
