package com.singularity_iteration.mio_icif.recipe.extractor;

import com.singularity_iteration.mio_icif.recipe.mio_icif_ModRecipes;
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
public class mio_icif_ExtractorRecipe implements Recipe<mio_icif_ExtractorRecipeInput>, IRecipeProcessingInfo {

    private final String group;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final int processingTime;
    private final int energyPerTick;

    public mio_icif_ExtractorRecipe(String group, Ingredient ingredient, ItemStack result, int processingTime, int energyPerTick) {
        this.group = group;
        this.ingredient = ingredient;
        this.result = result;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public boolean matches(mio_icif_ExtractorRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(mio_icif_ExtractorRecipeInput input, HolderLookup.Provider registries) {
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
        return mio_icif_ModRecipes.EXTRACTOR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return mio_icif_ModRecipes.EXTRACTOR_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.PISTON);
    }

    @Override
    public int getProcessingTime() {
        return processingTime;
    }

    @Override
    public int getEnergyPerTick() {
        return energyPerTick;
    }

    /**
     * 获取输入原料
     */
    public Ingredient getIngredient() {
        return ingredient;
    }

    /**
     * 获取输出结果
     */
    public ItemStack getResult() {
        return result;
    }
}