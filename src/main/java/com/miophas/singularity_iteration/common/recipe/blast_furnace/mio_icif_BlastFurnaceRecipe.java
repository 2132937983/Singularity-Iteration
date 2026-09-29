package com.miophas.singularity_iteration.common.recipe.blast_furnace;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeProcessingInfo;

@SuppressWarnings("null")
public class mio_icif_BlastFurnaceRecipe implements Recipe<mio_icif_BlastFurnaceRecipeInput>, IRecipeProcessingInfo {

    private final String group;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final ItemStack secondaryResult;
    private final int duration;
    private final int airCostPerTick;
    private final int ingredientCount;

    public mio_icif_BlastFurnaceRecipe(String group, Ingredient ingredient, ItemStack result, ItemStack secondaryResult,
                                       int duration, int airCostPerTick, int ingredientCount) {
        this.group = group;
        this.ingredient = ingredient;
        this.result = result;
        this.secondaryResult = secondaryResult;
        this.duration = duration;
        this.airCostPerTick = airCostPerTick;
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
        // 热力机器不消耗 EU；热量仅作为“是否够热”的开关（对齐 IC2）。
        return 0;
    }
}