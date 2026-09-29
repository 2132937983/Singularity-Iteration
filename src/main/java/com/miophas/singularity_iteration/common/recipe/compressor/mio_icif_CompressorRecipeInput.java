// SCEX 2026-09-12: repaired malformed UTF-8 bytes in comments only.
package com.miophas.singularity_iteration.common.recipe.compressor;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 压缩机配方输??? * 包装单个物品输入
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


