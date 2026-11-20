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
 * Powder recipe.
 * It grinds ores and other items into powder.
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
     * Returns the processing time in ticks (the serializer default when the recipe gives none).
     */
    public int getProcessingTime() {
        return processingTime < 0 ? mio_icif_PowderRecipeSerializer.DEFAULT_PROCESSING_TIME : processingTime;
    }

    /** 0.1.11.108: the time as written (-1 when the recipe gives none; what the serializer writes). */
    public int getProcessingTimeRaw() {
        return processingTime;
    }

    /**
     * 0.1.11.108 (processing overhaul M1): a time the recipe sets for the macerator, which then runs it for that many
     * ticks at its own power; empty when the recipe gives none (the time follows the material's work index).
     */
    public java.util.OptionalInt processingTimeOverride() {
        return processingTime > 0 ? java.util.OptionalInt.of(processingTime) : java.util.OptionalInt.empty();
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