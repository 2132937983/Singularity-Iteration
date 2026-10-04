package com.miophas.singularity_iteration.common.recipe.canner.canning;

import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeProcessingInfo;

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

    /** Largest stack the output slot can take: the input cans consumed never exceed what comes out. */
    private static final int MAX_CANS = 64;

    /** Nutrition from the stack's food component (vanilla FoodProperties, data-driven for every mod), 0 if none. */
    public static int nutrition(ItemStack food) {
        if (food.isEmpty()) return 0;
        FoodProperties props = food.getFoodProperties(null);
        return props == null ? 0 : Math.max(0, props.nutrition());
    }

    /**
     * Any edible item is accepted - the recipe's food ingredient (the {@code cannable_foods} tag) is
     * no longer a whitelist, so food added by other mods (larvae, dishes, ...) cans as well. Filled
     * tin cans and food without nutrition are refused.
     */
    public boolean acceptsFood(ItemStack food) {
        if (food.isEmpty() || food.is(mio_icif_normal.TIN_FILLED_CAN.get())) return false;
        return nutrition(food) > 0;
    }

    @Override
    public boolean matches(CanningRecipeInput input, Level level) {
        if (!this.canIngredient.test(input.inputCan())) return false;
        if (!acceptsFood(input.material())) return false;
        int required = getRequiredCanCount(input);
        return required > 0 && input.inputCan().getCount() >= required;
    }

    /** Filled cans made from one food item: one per point of nutrition (IC2), at most a stack. */
    public int getOutputCount(CanningRecipeInput input) {
        return Math.min(MAX_CANS, nutrition(input.material()) * Math.max(1, nutritionMultiplier));
    }

    /** Empty cans used: exactly as many as come out. */
    public int getRequiredCanCount(CanningRecipeInput input) {
        return getOutputCount(input);
    }

    @Override
    public ItemStack assemble(CanningRecipeInput input, HolderLookup.Provider registries) {
        int count = getOutputCount(input);
        if (count <= 0) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(mio_icif_normal.TIN_FILLED_CAN.get(), count);
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