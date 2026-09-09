package com.singularity_iteration.mio_icif.recipe.canner.empty_to_tank;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 单元灌入水槽模式配方输入
 * 包装装有流体的单元物�? */
public record EmptyToTankRecipeInput(ItemStack filledCell) implements RecipeInput {

    @Override
    public ItemStack getItem(int slot) {
        if (slot != 0) {
            throw new IllegalArgumentException("No item for slot " + slot);
        }
        return this.filledCell;
    }

    @Override
    public int size() {
        return 1;
    }
}


