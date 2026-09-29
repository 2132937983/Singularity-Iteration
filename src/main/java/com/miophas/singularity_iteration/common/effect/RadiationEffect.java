package com.miophas.singularity_iteration.common.effect;

import com.miophas.singularity_iteration.common.util.RadiationProtectionUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

@SuppressWarnings("null")
public class RadiationEffect extends MobEffect {

    public RadiationEffect() {
        super(MobEffectCategory.HARMFUL, 0x39FF14);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (RadiationProtectionUtil.isRadiationProtected(livingEntity)) {
            return true;
        }
        livingEntity.hurt(livingEntity.damageSources().magic(), 1.0F);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int j = 25 >> amplifier;
        if (j > 0) {
            return duration % j == 0;
        } else {
            return true;
        }
    }
}
