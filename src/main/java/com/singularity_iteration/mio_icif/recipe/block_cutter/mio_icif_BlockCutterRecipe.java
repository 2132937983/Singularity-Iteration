package com.singularity_iteration.mio_icif.recipe.block_cutter;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import com.singularity_iteration.mio_icif.api.recipe.IRecipeProcessingInfo;

@SuppressWarnings("null")
public class mio_icif_BlockCutterRecipe implements Recipe<mio_icif_BlockCutterRecipeInput>, IRecipeProcessingInfo {

    private final String group;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final int processingTime;
    private final int energyPerTick;
    private final int ingredientCount;
    private final int hardness;

    public mio_icif_BlockCutterRecipe(String group, Ingredient ingredient, ItemStack result,
                                      int processingTime, int energyPerTick, int ingredientCount, int hardness) {
        this.group = group;
        this.ingredient = ingredient;
        this.result = result;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
        this.ingredientCount = ingredientCount;
        this.hardness = hardness;
    }

    @Override
    public boolean matches(mio_icif_BlockCutterRecipeInput input, Level level) {
        return this.ingredient.test(input.item()) && input.item().getCount() >= this.ingredientCount;
    }

    @Override
    public ItemStack assemble(mio_icif_BlockCutterRecipeInput input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(this.ingredient);
        return list;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return mio_icif_BlockCutterRecipes.BLOCK_CUTTER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return mio_icif_BlockCutterRecipes.BLOCK_CUTTER_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE);
    }

    @Override
    public int getProcessingTime() {
        return processingTime;
    }

    @Override
    public int getEnergyPerTick() {
        return energyPerTick;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public ItemStack getResult() {
        return result;
    }

    @Override
    public int getIngredientCount() {
        return ingredientCount;
    }

    public int getHardness() {
        return hardness;
    }
}