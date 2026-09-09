package com.singularity_iteration.mio_icif.recipe.generic;

import com.singularity_iteration.mio_icif.recipe.mio_icif_SingleItemRecipeInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 复制机配方
 */
public class ReplicatorRecipe extends AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput> {

    public ReplicatorRecipe(String group, Ingredient ingredient, ItemStack result, int processingTime, int energyPerTick, float experience) {
        super(group, ingredient, result, processingTime, energyPerTick);
    }

    @Override
    public boolean matches(mio_icif_SingleItemRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ReplicatorRecipes.REPLICATOR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ReplicatorRecipes.REPLICATOR_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK);
    }
}