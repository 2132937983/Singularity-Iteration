package com.singularity_iteration.mio_icif.api.crop;

import com.singularity_iteration.mio_icif.api.internal.crop.PlantRegistry;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

/**
 * 作物 API 实现
 */
public class CropAPIImpl implements ICropAPI {

    @Override
    public void registerPlant(PlantType plantType) {
        PlantRegistry.instance.registerPlant(plantType);
    }

    @Override
    public PlantType getPlant(String modId, String typeId) {
        return PlantRegistry.instance.getPlant(modId, typeId);
    }

    @Override
    public Collection<PlantType> getAllPlants() {
        return PlantRegistry.instance.getAllPlants();
    }

    @Override
    public boolean isBaseSeed(ItemStack stack) {
        return PlantRegistry.instance.isBaseSeed(stack);
    }

    @Override
    public void registerBaseSeed(ItemStack seed, PlantType plantType) {
        // 使用默认属性注册基础种子
        // stage=1 表示种子从初始生长阶段开始（而非 0）
        PlantRegistry.instance.registerBaseSeed(seed, plantType, 1, 0, 0, 0, 0);
    }

    @Override
    public void registerBaseSeed(ItemStack seed, PlantType plantType, int growthRate, int gain, int resistance, int weedEx) {
        // 映射接口参数到 PlantRegistry 参数：
        // growthRate -> growthSpeed (生长速度)
        // gain -> yield (产量)
        // resistance -> resilience (抗性)
        // weedEx -> weedResistance (杂草抗性)
        // stage 默认为 1（种子从初始生长阶段开始，而非 0）
        PlantRegistry.instance.registerBaseSeed(seed, plantType, 1, growthRate, gain, resistance, weedEx);
    }
}