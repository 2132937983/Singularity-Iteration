package com.singularity_iteration.mio_icif.util;

import com.singularity_iteration.mio_icif.effect.mio_icif_effects;
import com.singularity_iteration.mio_icif.event.mio_icif_DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NuclearRadiationHandler {

    public static final int POISON_DURATION = 100;
    public static final int POISON_AMPLIFIER = 1;
    public static final float BASE_RADIATION_DAMAGE = 0.5F;

    public static void applyRadiationEffect(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(mio_icif_effects.RADIATION, POISON_DURATION, POISON_AMPLIFIER, false, true));

        float actualDamage = RadiationProtectionUtil.calculateRadiationDamage(entity, BASE_RADIATION_DAMAGE);
        if (actualDamage > 0.0F) {
            entity.hurt(new net.minecraft.world.damagesource.DamageSource(
                entity.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(mio_icif_DamageTypes.RADIATION), null, null), actualDamage);
        }
    }

    public static boolean hasNuclearItem(LivingEntity entity) {
        for (ItemStack stack : entity.getArmorAndBodyArmorSlots()) {
            if (stack.is(mio_icif_tags.NUCLEAR)) {
                return true;
            }
        }
        if (entity instanceof Player player) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(mio_icif_tags.NUCLEAR)) {
                    return true;
                }
            }
        }
        return false;
    }
}


