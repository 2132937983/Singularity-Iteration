// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import java.util.function.Consumer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Shots of the energy guns for shooters that are not players (0.1.8.4,
 * {@link com.miophas.singularity_iteration.core.api.item.IWeaponItem#fireFrom}). The same rules as
 * for a player: the gun pays from its own charge, the bolt has the same damage, speed, life and
 * colour, and the same charge effect and sound play. The bolt starts at the shooter's eye.
 */
final class MobWeaponFire {
    private MobWeaponFire() {}

    /**
     * Fires one energy bolt. Returns {@code cooldown}, or -1 when the gun has too little charge or
     * the bolt type is not registered.
     */
    static int energyBolt(mio_icif_tool_elc gun, ItemStack stack, LivingEntity shooter, Vec3 direction, boolean mainHand,
                          EntityType<mio_icif_energy_bullet> type, long cost, int color, float damage, int life,
                          float speed, Consumer<mio_icif_energy_bullet> extra, SoundEvent sound, float volume, float pitch, int cooldown) {
        if (shooter.level().isClientSide || type == null || direction.lengthSqr() < 1.0E-6) return -1;
        if (!gun.hasEnoughEnergy(stack, cost) || !gun.consumeEnergy(stack, cost)) return -1;
        mio_icif_energy_bullet bullet = new mio_icif_energy_bullet(type, shooter, shooter.level());
        bullet.setBeam(color, cost);
        bullet.setDamage(damage);
        bullet.setMaxLife(life);
        extra.accept(bullet);
        bullet.setTerrainDamage(false);           // an AI shooter never breaks blocks with its blasts
        Vec3 d = direction.normalize();
        bullet.shoot(d.x, d.y, d.z, speed, 0.0F);
        shooter.level().addFreshEntity(bullet);
        WeaponChargeFx.emit(shooter, mainHand, cost, color);
        shooter.level().playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), sound, SoundSource.NEUTRAL, volume,
            pitch + shooter.getRandom().nextFloat() * 0.08F);
        return cooldown;
    }
}
