package com.singularity_iteration.mio_icif.recipe.compressor;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 压缩机配方输�? * 包装单个物品输入
 */
public record mio_icif_CompressorRecipeInput(ItemStack item) implements RecipeInput {

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


