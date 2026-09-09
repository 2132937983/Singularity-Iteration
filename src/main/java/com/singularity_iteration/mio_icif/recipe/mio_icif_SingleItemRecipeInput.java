package com.singularity_iteration.mio_icif.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 单物品配方输�? * 用于打粉机等单输入配方的机器
 */
public record mio_icif_SingleItemRecipeInput(ItemStack item) implements RecipeInput {

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


