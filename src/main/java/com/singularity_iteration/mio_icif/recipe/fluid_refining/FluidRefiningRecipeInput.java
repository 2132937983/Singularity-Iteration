package com.singularity_iteration.mio_icif.recipe.fluid_refining;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

public record FluidRefiningRecipeInput(ItemStack inputItem, FluidStack inputFluid) implements RecipeInput {

    @Override
    public ItemStack getItem(int slot) {
        if (slot != 0) {
            throw new IllegalArgumentException("No item for slot " + slot);
        }
        return this.inputItem;
    }

    @Override
    public int size() {
        return 1;
    }
}