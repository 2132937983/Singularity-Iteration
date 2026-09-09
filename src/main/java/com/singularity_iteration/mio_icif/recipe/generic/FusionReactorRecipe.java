package com.singularity_iteration.mio_icif.recipe.generic;

import com.singularity_iteration.mio_icif.api.recipe.IRecipeProcessingInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 聚变反应堆配方（双输入单输出）
 */
public class FusionReactorRecipe implements Recipe<net.minecraft.world.item.crafting.RecipeInput>, IRecipeProcessingInfo {

    private final String group;
    private final Ingredient ingredient1;
    private final Ingredient ingredient2;
    private final ItemStack result;
    private final int processingTime;
    private final long energyRequired;

    public FusionReactorRecipe(String group, Ingredient ingredient1, Ingredient ingredient2, ItemStack result, int processingTime, long energyRequired) {
        this.group = group;
        this.ingredient1 = ingredient1;
        this.ingredient2 = ingredient2;
        this.result = result;
        this.processingTime = processingTime;
        this.energyRequired = energyRequired;
    }

    @Override
    public boolean matches(net.minecraft.world.item.crafting.RecipeInput input, Level level) {
        // 聚变反应堆不使用标准合成表，此方法不会被调用
        return false;
    }

    @Override
    public ItemStack assemble(net.minecraft.world.item.crafting.RecipeInput input, HolderLookup.Provider registries) {
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
        list.add(this.ingredient1);
        list.add(this.ingredient2);
        return list;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FusionReactorRecipes.FUSION_REACTOR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return FusionReactorRecipes.FUSION_REACTOR_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.END_STONE);
    }

    public Ingredient getIngredient1() {
        return ingredient1;
    }

    public Ingredient getIngredient2() {
        return ingredient2;
    }

    public ItemStack getResult() {
        return result;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public long getEnergyRequired() {
        return energyRequired;
    }

    @Override
    public int getEnergyPerTick() {
        return processingTime > 0 ? (int) (energyRequired / processingTime) : 0;
    }
}