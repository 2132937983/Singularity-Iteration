package com.miophas.singularity_iteration.core.runtime.crop;

import com.miophas.singularity_iteration.core.api.crop.ICropAPI;
import com.miophas.singularity_iteration.core.api.crop.PlantType;

import com.miophas.singularity_iteration.core.runtime.crop.PlantRegistry;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

/**
 * Implementation of the crop API. It delegates to {@link PlantRegistry}.
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
        // Register the base seed with the default properties.
        // stage=1 means that the seed starts at the first growth stage (not 0).
        PlantRegistry.instance.registerBaseSeed(seed, plantType, 1, 0, 0, 0, 0);
    }

    @Override
    public void registerBaseSeed(ItemStack seed, PlantType plantType, int growthRate, int gain, int resistance, int weedEx) {
        // Map the interface parameters to the PlantRegistry parameters:
        // growthRate -> growthSpeed (growth speed)
        // gain -> yield (yield)
        // resistance -> resilience (resistance)
        // weedEx -> weedResistance (weed resistance)
        // stage is 1 by default (the seed starts at the first growth stage, not 0).
        PlantRegistry.instance.registerBaseSeed(seed, plantType, 1, growthRate, gain, resistance, weedEx);
    }
}