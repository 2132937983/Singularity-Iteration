package com.miophas.singularity_iteration.core.api.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

/** Seed contract. Defaults preserve SI seed NBT; addons may override their storage format. */
public interface ICropSeedItem {
    default net.minecraft.nbt.CompoundTag cropData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
    default String cropModId(ItemStack stack) { return cropData(stack).getString("PlantModId"); }
    default String cropId(ItemStack stack) { return cropData(stack).getString("PlantId"); }
    default int cropGrowth(ItemStack stack) { return Math.clamp(cropData(stack).getInt("GrowthSpeed"), 0, 31); }
    default int cropGain(ItemStack stack) { return Math.clamp(cropData(stack).getInt("Yield"), 0, 31); }
    default int cropResistance(ItemStack stack) { return Math.clamp(cropData(stack).getInt("Resilience"), 0, 31); }
    default int cropScanLevel(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : Math.clamp(data.copyTag().getInt("ScanLevel"), 0, 4);
    }
    default void setCropScanLevel(ItemStack stack, int level) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("ScanLevel", Math.clamp(level, 0, 4)));
    }
}
