package com.miophas.singularity_iteration.common.recipe.fermenter;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/** The fermenter's input: the fluid in its input tank (no items). */
public record FermenterRecipeInput(FluidStack fluid) implements RecipeInput {

    @Override
    public ItemStack getItem(int slot) {
        throw new IllegalArgumentException("The fermenter has no item input (slot " + slot + ")");
    }

    @Override
    public int size() {
        return 0;
    }
}
