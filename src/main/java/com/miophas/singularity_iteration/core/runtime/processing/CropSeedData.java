// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Existing SI seed keys; unknown identities and unrelated components stay intact. */
public final class CropSeedData {
    private CropSeedData() { }
    public record Traits(int growth, int yield, int resilience, int scan) { }
    public static CompoundTag read(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
    public static boolean isEmptyBag(ItemStack stack) {
        var tag=read(stack);
        // A missing registration or malformed ID must never turn a saved seed
        // into an empty bag. Preserve it until its provider or data is repaired.
        return !stack.isEmpty() && !tag.contains("PlantModId") && !tag.contains("PlantId");
    }
    public static Traits traits(ItemStack stack) {
        var tag=read(stack);
        return new Traits(Math.clamp(tag.getInt("GrowthSpeed"),0,31),Math.clamp(tag.getInt("Yield"),0,31),
            Math.clamp(tag.getInt("Resilience"),0,31),Math.clamp(tag.getInt("ScanLevel"),0,4));
    }
    public static void setScan(ItemStack stack,int value) {
        if(stack.isEmpty())return;
        var tag=read(stack);tag.putInt("ScanLevel",Math.clamp(value,0,4));
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    /** Prepare precisely one filled bag without changing the held stack. */
    public static ItemStack fillOne(ItemStack empty,String mod,String plant,int growth,int yield,int resilience) {
        if(!isEmptyBag(empty) || mod==null || mod.isBlank() || plant==null || plant.isBlank())return ItemStack.EMPTY;
        var result=empty.copyWithCount(1);var tag=read(empty);
        tag.putString("PlantModId",mod);tag.putString("PlantId",plant);
        tag.putInt("GrowthSpeed",Math.clamp(growth,0,31));tag.putInt("Yield",Math.clamp(yield,0,31));
        tag.putInt("Resilience",Math.clamp(resilience,0,31));tag.putInt("ScanLevel",0);
        result.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return result;
    }
}
