package com.singularity_iteration.mio_icif.recipe.compressor;

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
 * 压缩机配方类
 * 用于将物品压缩成更密集的形式（如�?>块，矿物->板，粉末->锭等�? */
@SuppressWarnings("null")
public class mio_icif_CompressorRecipe implements Recipe<mio_icif_CompressorRecipeInput>, IRecipeProcessingInfo {

    private final String group;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final int processingTime;
    private final int energyPerTick;
    private final int ingredientCount;

    public mio_icif_CompressorRecipe(String group, Ingredient ingredient, ItemStack result, int processingTime, int energyPerTick, int ingredientCount) {
        this.group = group;
        this.ingredient = ingredient;
        this.result = result;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
        this.ingredientCount = ingredientCount;
    }

    @Override
    public boolean matches(mio_icif_CompressorRecipeInput input, Level level) {
        return this.ingredient.test(input.item()) && input.item().getCount() >= this.ingredientCount;
    }

    @Override
    public ItemStack assemble(mio_icif_CompressorRecipeInput input, HolderLookup.Provider registries) {
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
        return mio_icif_CompressorRecipes.COMPRESSOR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return mio_icif_CompressorRecipes.COMPRESSOR_TYPE.get();
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
     * 获取输入原料数量
     */
    public int getIngredientCount() {
        return ingredientCount;
    }
}