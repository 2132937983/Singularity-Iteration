package com.singularity_iteration.mio_icif.integration.curios;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings({"null", "deprecation"})
public class LifeSupportRingProperty implements ItemPropertyFunction {

    @Override
    public float call(ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
        if (stack.getItem() instanceof ElectricLifeSupportRingCurio ring) {
            long energy = ring.getEnergy(stack);
            long maxEnergy = ring.getMaxEnergy();
            if (maxEnergy <= 0 || energy <= 0) {
                return 0.0f;
            }
            float ratio = (float) energy / maxEnergy;
            return Math.round(ratio * 4.0f) / 4.0f;
        }
        return 0.0f;
    }
}