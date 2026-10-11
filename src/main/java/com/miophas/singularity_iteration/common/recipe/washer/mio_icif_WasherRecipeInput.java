package com.miophas.singularity_iteration.common.recipe.washer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * Ore washer recipe input wrapper.
 * It wraps one item as the input of an ore washer recipe.
 */
public record mio_icif_WasherRecipeInput(ItemStack item) implements RecipeInput {

    /**
     * 获取输入物品
     */
    public ItemStack item() {
        return item;
    }

    /**
     * 获取输入物品数量
     */
    public int count() {
        return item.getCount();
    }

    @Override
    public ItemStack getItem(int i) {
        return item;
    }

    @Override
    public int size() {
        return 1;
    }

    /**
     * Checks if the input is empty.
     */
    public boolean isEmpty() {
        return item.isEmpty();
    }
}


