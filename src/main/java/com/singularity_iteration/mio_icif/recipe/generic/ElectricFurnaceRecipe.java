package com.singularity_iteration.mio_icif.recipe.generic;

import com.singularity_iteration.mio_icif.recipe.mio_icif_SingleItemRecipeInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 电配方
 */
public class ElectricFurnaceRecipe extends AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput> {

    private final float experience;

    public ElectricFurnaceRecipe(String group, Ingredient ingredient, ItemStack result, int processingTime, int energyPerTick, float experience) {
        super(group, ingredient, result, processingTime, energyPerTick);
        this.experience = experience;
    }

    @Override
    public boolean matches(mio_icif_SingleItemRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ElectricFurnaceRecipes.ELECTRIC_FURNACE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ElectricFurnaceRecipes.ELECTRIC_FURNACE_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.FURNACE);
    }

    public float getExperience() {
        return experience;
    }
}
