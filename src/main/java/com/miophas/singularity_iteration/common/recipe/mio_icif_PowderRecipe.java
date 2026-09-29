package com.miophas.singularity_iteration.common.recipe;

import com.miophas.singularity_iteration.core.api.recipe.IRecipeProcessingInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 打粉配方法? * 用于将矿石?物品打成粉末
 */
@SuppressWarnings("null")
public class mio_icif_PowderRecipe implements Recipe<mio_icif_SingleItemRecipeInput>, IRecipeProcessingInfo {

    private final String group;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final int processingTime;
    private final int energyPerTick;
    private final int ingredientCount;

    public mio_icif_PowderRecipe(String group, Ingredient ingredient, ItemStack result, int processingTime, int energyPerTick, int ingredientCount) {
        this.group = group;
        this.ingredient = ingredient;
        this.result = result;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
        this.ingredientCount = ingredientCount;
    }

    @Override
    public boolean matches(mio_icif_SingleItemRecipeInput input, Level level) {
        return this.ingredient.test(input.item()) && input.item().getCount() >= this.ingredientCount;
    }

    @Override
    public ItemStack assemble(mio_icif_SingleItemRecipeInput input, HolderLookup.Provider registries) {
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
        return mio_icif_ModRecipes.POWDER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return mio_icif_ModRecipes.POWDER_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.PISTON);
    }

    /**
     * 获取处理时间（ticks�?     */
    public int getProcessingTime() {
        return processingTime;
    }

    /**
     * 获取每tick消耗的能量
     */
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

    /**
     * 获取输入材料数量
     */
    @Override
    public int getIngredientCount() {
        return ingredientCount;
    }
}