package com.singularity_iteration.mio_icif.Items.Tools;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 纳米剑激活状态属性函�?
 * 返回0=关闭�?=开始?
 * 用于模型override切换纹理
 */
@SuppressWarnings({"null", "deprecation"})
public class NanoSaberActiveProperty implements ItemPropertyFunction {

    @Override
    public float call(ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
        return mio_icif_nanosaber.isActive(stack) ? 1.0F : 0.0F;
    }
}

