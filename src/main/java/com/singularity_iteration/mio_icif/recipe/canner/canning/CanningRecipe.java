package com.singularity_iteration.mio_icif.recipe.canner.canning;

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
public class CanningRecipe implements Recipe<CanningRecipeInput>, IRecipeProcessingInfo {

    private final String group;
    private final Ingredient canIngredient;      // 空锡�
private final Ingredient foodIngredient;     // 食物
    private final ItemStack result;              // 结果：装满的锡罐
    private final int processingTime;
    private final int energyPerTick;

    public CanningRecipe(String group, Ingredient canIngredient, Ingredient foodIngredient, 
                         ItemStack result, int processingTime, int energyPerTick) {
        this.group = group;
        this.canIngredient = canIngredient;
        this.foodIngredient = foodIngredient;
        this.result = result;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public boolean matches(CanningRecipeInput input, Level level) {
        return this.canIngredient.test(input.inputCan()) && this.foodIngredient.test(input.material());
    }

    @Override
    public ItemStack assemble(CanningRecipeInput input, HolderLookup.Provider registries) {
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
        return CanningRecipes.CANNING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CanningRecipes.CANNING_TYPE.get();
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

    public ItemStack getResult() {
        return result;
    }

    @Override
    public int getProcessingTime() {
        return processingTime;
    }

    @Override
    public int getEnergyPerTick() {
        return energyPerTick;
    }
}