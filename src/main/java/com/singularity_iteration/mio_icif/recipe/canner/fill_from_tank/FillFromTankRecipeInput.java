package com.singularity_iteration.mio_icif.recipe.canner.fill_from_tank;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 水槽灌满单元模式配方输入
 * 包装空单元物�? */
public record FillFromTankRecipeInput(ItemStack emptyCell) implements RecipeInput {

    @Override
    public ItemStack getItem(int slot) {
        if (slot != 0) {
            throw new IllegalArgumentException("No item for slot " + slot);
        }
        return this.emptyCell;
    }

    @Override
    public int size() {
        return 1;
    }
}


