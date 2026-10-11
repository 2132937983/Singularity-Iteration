package com.miophas.singularity_iteration.common.client.item;

import com.miophas.singularity_iteration.common.item.tools.mio_icif_nanosaber;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Item property function for the nano saber active state.
 * It returns 0 for off and 1 for on.
 * The model override uses it to switch the texture.
 */
@SuppressWarnings({"null", "deprecation"})
public class NanoSaberActiveProperty implements ItemPropertyFunction {

    @Override
    public float call(ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
        return mio_icif_nanosaber.isActive(stack) ? 1.0F : 0.0F;
    }
}

