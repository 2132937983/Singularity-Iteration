package com.singularity_iteration.mio_icif.Items.Normal;

import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
/**
 * 电池能量属性函数
 * 根据电池当前能量返回对应的level 值（0-4）
 * 基于电池最大能量的百分比：
 * 0: 0-20% 最大能量
 * 1: 21-40% 最大能量
 * 2: 41-60% 最大能量
 * 3: 61-80% 最大能量
 * 4: 81-100% 最大能量
 */
@SuppressWarnings({"null", "deprecation"})
public class BatteryEnergyProperty implements ItemPropertyFunction {
    
    @Override
    public float call(ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
        var api = MioIcifAPI.instance().getItemAPI();
        if (api.isBattery(stack)) {
            long energy = api.getBatteryStored(stack);
            long maxEnergy = api.getBatteryCapacity(stack);
            
            float percentage = maxEnergy > 0 ? (float) energy / maxEnergy : 0;
            if (percentage <= 0.2f) {
                return 0;
            } else if (percentage <= 0.4f) {
                return 1;
            } else if (percentage <= 0.6f) {
                return 2;
            } else if (percentage <= 0.8f) {
                return 3;
            } else {
                return 4;
            }
        }
        return 0;
    }
}