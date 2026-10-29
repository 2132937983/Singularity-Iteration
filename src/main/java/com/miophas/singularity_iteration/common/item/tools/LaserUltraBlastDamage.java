// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;

/**
 * 0.1.10.36: the damage of the Mining Laser's Ultra-High Energy Blast. Every entity within the blast radius (the explosion
 * power, about 18.6 blocks) takes {@link #DAMAGE}, behind cover too; farther out, to twice the radius, the vanilla falloff
 * applies. Blocks are handled as by the vanilla calculator of an explosion caused by an entity, so terrain damage, protection
 * events and the ENABLE_NUCLEAR_EXPLOSION switch are unchanged. Armor, enchantments and totems still apply as for any blast.
 */
final class LaserUltraBlastDamage extends EntityBasedExplosionDamageCalculator {
    /** Short.MAX_VALUE: the direct hit and every target in the radius. */
    static final float DAMAGE = 32767.0F;

    LaserUltraBlastDamage(Entity source) {
        super(source);
    }

    @Override
    public float getEntityDamageAmount(Explosion explosion, Entity entity) {
        if (entity.position().distanceTo(explosion.center()) <= explosion.radius()) return DAMAGE;
        return super.getEntityDamageAmount(explosion, entity);
    }
}
