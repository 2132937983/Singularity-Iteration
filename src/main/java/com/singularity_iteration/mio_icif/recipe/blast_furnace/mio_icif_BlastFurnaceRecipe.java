package com.singularity_iteration.mio_icif.recipe.blast_furnace;

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
public class mio_icif_BlastFurnaceRecipe implements Recipe<mio_icif_BlastFurnaceRecipeInput>, IRecipeProcessingInfo {

    private final String group;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final ItemStack secondaryResult;
    private final int duration;
    private final int airCostPerTick;
    private final int heatCostPerTick;
    private final int ingredientCount;

    public mio_icif_BlastFurnaceRecipe(String group, Ingredient ingredient, ItemStack result, ItemStack secondaryResult,
                                       int duration, int airCostPerTick, int heatCostPerTick, int ingredientCount) {
        this.group = group;
        this.ingredient = ingredient;
        this.result = result;
        this.secondaryResult = secondaryResult;
        this.duration = duration;
        this.airCostPerTick = airCostPerTick;
        this.heatCostPerTick = heatCostPerTick;
        this.ingredientCount = ingredientCount;
    }

    @Override
    public boolean matches(mio_icif_BlastFurnaceRecipeInput input, Level level) {
        return this.ingredient.test(input.item()) && input.item().getCount() >= this.ingredientCount;
    }

    @Override
    public ItemStack assemble(mio_icif_BlastFurnaceRecipeInput input, HolderLookup.Provider registries) {
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

    public ItemStack getSecondaryResultItem() {
        return this.secondaryResult;
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
        return mio_icif_BlastFurnaceRecipes.BLAST_FURNACE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return mio_icif_BlastFurnaceRecipes.BLAST_FURNACE_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.BLAST_FURNACE);
    }

    public int getDuration() {
        return duration;
    }

    public int getAirCostPerTick() {
        return airCostPerTick;
    }

    public int getHeatCostPerTick() {
        return heatCostPerTick;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public ItemStack getResult() {
        return result;
    }

    public ItemStack getSecondaryResult() {
        return secondaryResult;
    }

    public int getIngredientCount() {
        return ingredientCount;
    }

    @Override
    public int getProcessingTime() {
        return duration;
    }

    @Override
    public int getEnergyPerTick() {
        return heatCostPerTick;
    }
}