package com.singularity_iteration.mio_icif.recipe.generic;

import com.singularity_iteration.mio_icif.api.recipe.IRecipeProcessingInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;


/**
 * 通用单输入单输出配方基类
 * 用于电、回收机、发酵机等机器
 */
@SuppressWarnings("null")
public abstract class AbstractSingleInputRecipe<T extends net.minecraft.world.item.crafting.RecipeInput> implements Recipe<T>, IRecipeProcessingInfo {

    protected final String group;
    protected final Ingredient ingredient;
    protected final ItemStack result;
    protected final int processingTime;
    protected final int energyPerTick;

    public AbstractSingleInputRecipe(String group, Ingredient ingredient, ItemStack result, int processingTime, int energyPerTick) {
        this.group = group;
        this.ingredient = ingredient;
        this.result = result;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public ItemStack assemble(T input, HolderLookup.Provider registries) {
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

    public Ingredient getIngredient() {
        return ingredient;
    }

    public ItemStack getResult() {
        return result;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public int getEnergyPerTick() {
        return energyPerTick;
    }
}