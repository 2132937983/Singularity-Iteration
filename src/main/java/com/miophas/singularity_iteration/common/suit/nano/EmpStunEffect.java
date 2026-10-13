// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit.nano;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;

/**
 * EMP stun (0.1.8.5): a creature hit by the nano EMP loses its target, stops walking and, for a
 * creeper, lets its fuse fall back. Sparks crackle on it while the effect lasts.
 */
public final class EmpStunEffect extends MobEffect {
    public EmpStunEffect() {
        super(MobEffectCategory.HARMFUL, 0x6FA8FF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath("mio_icif", "emp_stun"),
            -0.9, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, ResourceLocation.fromNamespaceAndPath("mio_icif", "emp_stun_attack"),
            -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Mob mob) {
            if (mob.getTarget() != null) mob.setTarget(null);
            mob.getNavigation().stop();
            if (mob instanceof Creeper creeper) creeper.setSwellDir(-1);
        }
        if (entity.level() instanceof ServerLevel level && entity.tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
                3, entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.4, 0.15);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
