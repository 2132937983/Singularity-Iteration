package com.miophas.singularity_iteration.common.recipe.generic;

import com.miophas.singularity_iteration.core.prefab.recipe.AbstractSingleInputRecipe;

import com.miophas.singularity_iteration.common.recipe.mio_icif_SingleItemRecipeInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 回收机配方: recycling an item that matches the ingredient gives the result (instead of scrap) with
 * {@link #getChance()}, else nothing. A negative chance means the recycler's own scrap chance.
 */
public class RecyclerRecipe extends AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput> {

    /** No chance of its own: the recycler's scrap chance applies. */
    public static final float DEFAULT_CHANCE = -1.0f;

    private final float chance;

    public RecyclerRecipe(String group, Ingredient ingredient, ItemStack result, int processingTime, int energyPerTick, float chance) {
        super(group, ingredient, result, processingTime, energyPerTick);
        this.chance = chance;
    }

    /** The chance (0 to 1) that recycling gives the result; negative for the recycler's own chance. */
    public float getChance() {
        return chance;
    }

    @Override
    public boolean matches(mio_icif_SingleItemRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecyclerRecipes.RECYCLER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecyclerRecipes.RECYCLER_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.GREEN_WOOL);
    }
}