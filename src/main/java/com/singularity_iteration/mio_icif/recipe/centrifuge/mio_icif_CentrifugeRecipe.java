package com.singularity_iteration.mio_icif.recipe.centrifuge;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import com.singularity_iteration.mio_icif.api.recipe.IRecipeProcessingInfo;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public class mio_icif_CentrifugeRecipe implements Recipe<mio_icif_CentrifugeRecipeInput>, IRecipeProcessingInfo {

    private final String group;
    private final Ingredient ingredient;
    // 主输出（输出�?�
private final ItemStack primaryResult;
    private final int primaryCount;
    // 副输�?（输出槽2�
private final ItemStack secondaryResult;
    private final int secondaryCount;
    // 副输�?（输出槽3�
private final ItemStack tertiaryResult;
    private final int tertiaryCount;
    
    private final int processingTime;
    private final int energyPerTick;
    private final int minHeatRequired;

    public mio_icif_CentrifugeRecipe(String group, Ingredient ingredient, 
                                      ItemStack primaryResult, int primaryCount,
                                      ItemStack secondaryResult, int secondaryCount,
                                      ItemStack tertiaryResult, int tertiaryCount,
                                      int processingTime, int energyPerTick, int minHeatRequired) {
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
        this.minHeatRequired = minHeatRequired;
    }

    @Override
    public boolean matches(mio_icif_CentrifugeRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(mio_icif_CentrifugeRecipeInput input, HolderLookup.Provider registries) {
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
        return mio_icif_CentrifugeRecipes.CENTRIFUGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return mio_icif_CentrifugeRecipes.CENTRIFUGE_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.FURNACE);
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
     * 获取所需最小热量（HU�
 */
    public int getMinHeatRequired() {
        return minHeatRequired;
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
     * 获取副输�?结果
     */
    public ItemStack getSecondaryResult() {
        return secondaryResult;
    }

    /**
     * 获取副输�?数量
     */
    public int getSecondaryCount() {
        return secondaryCount;
    }

    /**
     * 获取副输�?结果
     */
    public ItemStack getTertiaryResult() {
        return tertiaryResult;
    }

    /**
     * 获取副输�?数量
     */
    public int getTertiaryCount() {
        return tertiaryCount;
    }
}