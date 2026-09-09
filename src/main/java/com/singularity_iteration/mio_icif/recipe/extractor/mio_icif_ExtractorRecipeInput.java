package com.singularity_iteration.mio_icif.recipe.extractor;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 提取机配方输�?
 * 用于提取机单输入配方的机器?
 */
public record mio_icif_ExtractorRecipeInput(ItemStack item) implements RecipeInput {

    @Override
    public ItemStack getItem(int slot) {
        if (slot != 0) {
            throw new IllegalArgumentException("No item for slot " + slot);
        }
        return this.item;
    }

    @Override
    public int size() {
        return 1;
    }
}


