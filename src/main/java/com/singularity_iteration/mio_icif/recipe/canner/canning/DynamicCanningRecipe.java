package com.singularity_iteration.mio_icif.recipe.canner.canning;

import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import com.singularity_iteration.mio_icif.api.recipe.IRecipeProcessingInfo;

@SuppressWarnings("null")
public class DynamicCanningRecipe implements Recipe<CanningRecipeInput>, IRecipeProcessingInfo {

    private final String group;
    private final Ingredient canIngredient;      // 空锡�
private final Ingredient foodIngredient;     // 食物（任意食物）
    private final int processingTime;
    private final int energyPerTick;
    private final int nutritionMultiplier;       // 营养值倍数（默�?�?
    public DynamicCanningRecipe(String group, Ingredient canIngredient, Ingredient foodIngredient,
                                int processingTime, int energyPerTick, int nutritionMultiplier) {
        this.group = group;
        this.canIngredient = canIngredient;
        this.foodIngredient = foodIngredient;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
        this.nutritionMultiplier = nutritionMultiplier;
    }

    @Override
    public boolean matches(CanningRecipeInput input, Level level) {
        // 检查输入槽是否为空锡罐
        if (!this.canIngredient.test(input.inputCan())) {
            return false;
        }

        // 检查材料槽是否为食�
    if (!this.foodIngredient.test(input.material())) {
            return false;
        }

        // 获取食物的营养�
    FoodProperties foodProps = input.material().getItem().getFoodProperties(input.material(), null);
        if (foodProps == null) {
            return false;
        }

        int nutrition = foodProps.nutrition();
        int requiredCans = nutrition * nutritionMultiplier;

        // 检查输入槽是否有足够的锡罐
        return input.inputCan().getCount() >= requiredCans;
    }

    /**
     * 根据输入的食物计算输出数据
 * @param input 配方输入
     * @return 输出的满锡罐数量
     */
    public int getOutputCount(CanningRecipeInput input) {
        FoodProperties foodProps = input.material().getItem().getFoodProperties(input.material(), null);
        if (foodProps == null) {
            return 0;
        }
        return foodProps.nutrition() * nutritionMultiplier;
    }

    /**
     * 根据输入的食物计算需要消耗的空锡罐数据
 * @param input 配方输入
     * @return 需要消耗的空锡罐数据
 */
    public int getRequiredCanCount(CanningRecipeInput input) {
        return getOutputCount(input);
    }

    @Override
    public ItemStack assemble(CanningRecipeInput input, HolderLookup.Provider registries) {
        int count = getOutputCount(input);
        if (count <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(mio_icif_normal.TIN_FILLED_CAN.get());
        result.setCount(Math.min(count, 64)); // 最终?4�
    return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        // 返回默认1个结果（实际数量在assemble时动态计算）
        return new ItemStack(mio_icif_normal.TIN_FILLED_CAN.get());
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(this.canIngredient);
        list.add(this.foodIngredient);
        return list;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CanningRecipes.DYNAMIC_CANNING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CanningRecipes.DYNAMIC_CANNING_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.CAULDRON);
    }

    // Getters
    public Ingredient getCanIngredient() {
        return canIngredient;
    }

    public Ingredient getFoodIngredient() {
        return foodIngredient;
    }

    @Override
    public int getProcessingTime() {
        return processingTime;
    }

    @Override
    public int getEnergyPerTick() {
        return energyPerTick;
    }

    public int getNutritionMultiplier() {
        return nutritionMultiplier;
    }
}