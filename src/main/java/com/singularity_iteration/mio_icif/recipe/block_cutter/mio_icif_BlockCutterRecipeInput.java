package com.singularity_iteration.mio_icif.recipe.block_cutter;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 方块切割机配方输�? * 包装输入槽中的单个物�? */
public record mio_icif_BlockCutterRecipeInput(ItemStack item) implements RecipeInput {

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


