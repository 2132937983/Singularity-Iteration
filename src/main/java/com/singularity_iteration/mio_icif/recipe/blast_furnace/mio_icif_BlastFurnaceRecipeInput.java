package com.singularity_iteration.mio_icif.recipe.blast_furnace;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 楂樼倝閰嶆柟杈撳叆
 * 包装单个物品输入
 */
public record mio_icif_BlastFurnaceRecipeInput(ItemStack item) implements RecipeInput {

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


