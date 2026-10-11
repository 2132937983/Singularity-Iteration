package com.miophas.singularity_iteration.common.recipe.washer;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeProcessingInfo;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public class mio_icif_WasherRecipe implements Recipe<mio_icif_WasherRecipeInput>, IRecipeProcessingInfo {

    private final String group;
    private final Ingredient ingredient;
    // Primary output (output slot 1).
private final ItemStack primaryResult;
    private final int primaryCount;
    // Secondary output (output slot 2).
private final ItemStack secondaryResult;
    private final int secondaryCount;
    // Tertiary output (output slot 3).
private final ItemStack tertiaryResult;
    private final int tertiaryCount;
    
    private final int processingTime;
    private final int energyPerTick;
    private final int waterAmount;

    public mio_icif_WasherRecipe(String group, Ingredient ingredient, 
                                  ItemStack primaryResult, int primaryCount,
                                  ItemStack secondaryResult, int secondaryCount,
                                  ItemStack tertiaryResult, int tertiaryCount,
                                  int processingTime, int energyPerTick, int waterAmount) {
        this.group = group;
        this.ingredient = ingredient;
        this.primaryResult = primaryResult;
        this.primaryCount = primaryCount;
        this.secondaryResult = secondaryResult;
        this.secondaryCount = secondaryCount;
        this.tertiaryResult = tertiaryResult;
        this.tertiaryCount = tertiaryCount;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
        this.waterAmount = waterAmount;
    }

    @Override
    public boolean matches(mio_icif_WasherRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(mio_icif_WasherRecipeInput input, HolderLookup.Provider registries) {
        // 返回主输出作为默认结果
    ItemStack output = this.primaryResult.copy();
        output.setCount(this.primaryCount);
        return output;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack output = this.primaryResult.copy();
        output.setCount(this.primaryCount);
        return output;
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
        return mio_icif_WasherRecipes.WASHER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return mio_icif_WasherRecipes.WASHER_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.WATER);
    }

    /**
     * 获取所有输出结果
 * @return 包含3个输出的列表
     */
    public List<ItemStack> getAllResults() {
        List<ItemStack> results = new ArrayList<>();
        if (!primaryResult.isEmpty() && primaryCount > 0) {
            ItemStack stack = primaryResult.copy();
            stack.setCount(primaryCount);
            results.add(stack);
        }
        if (!secondaryResult.isEmpty() && secondaryCount > 0) {
            ItemStack stack = secondaryResult.copy();
            stack.setCount(secondaryCount);
            results.add(stack);
        }
        if (!tertiaryResult.isEmpty() && tertiaryCount > 0) {
            ItemStack stack = tertiaryResult.copy();
            stack.setCount(tertiaryCount);
            results.add(stack);
        }
        return results;
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
     * Returns the water that the recipe consumes, in mB.
 */
    public int getWaterAmount() {
        return waterAmount;
    }

    /**
     * 获取输入原料
     */
    public Ingredient getIngredient() {
        return ingredient;
    }

    /**
     * 获取主输出结果
 */
    public ItemStack getPrimaryResult() {
        return primaryResult;
    }

    /**
     * 获取主输出数据
 */
    public int getPrimaryCount() {
        return primaryCount;
    }

    /**
     * Returns the secondary output (output slot 2).
     */
    public ItemStack getSecondaryResult() {
        return secondaryResult;
    }

    /**
     * Returns the item count of the secondary output.
     */
    public int getSecondaryCount() {
        return secondaryCount;
    }

    /**
     * Returns the tertiary output (output slot 3).
     */
    public ItemStack getTertiaryResult() {
        return tertiaryResult;
    }

    /**
     * Returns the item count of the tertiary output.
     */
    public int getTertiaryCount() {
        return tertiaryCount;
    }
}