package com.singularity_iteration.mio_icif.recipe.canner.fill_from_tank;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import com.singularity_iteration.mio_icif.api.recipe.IRecipeProcessingInfo;

@SuppressWarnings("null")
public class FillFromTankRecipe implements Recipe<FillFromTankRecipeInput>, IRecipeProcessingInfo {

    private final String group;
    private final Ingredient emptyCellIngredient;  // 空单独
private final FluidStack fluidInput;           // 需要的流体类型和数据
private final ItemStack filledCellResult;      // 装满的单独
private final int processingTime;
    private final int energyPerTick;

    public FillFromTankRecipe(String group, Ingredient emptyCellIngredient, FluidStack fluidInput,
                              ItemStack filledCellResult, int processingTime, int energyPerTick) {
        this.group = group;
        this.emptyCellIngredient = emptyCellIngredient;
        this.fluidInput = fluidInput;
        this.filledCellResult = filledCellResult;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public boolean matches(FillFromTankRecipeInput input, Level level) {
        return this.emptyCellIngredient.test(input.emptyCell());
    }

    @Override
    public ItemStack assemble(FillFromTankRecipeInput input, HolderLookup.Provider registries) {
        return this.filledCellResult.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.filledCellResult;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(this.emptyCellIngredient);
        return list;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FillFromTankRecipes.FILL_FROM_TANK_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return FillFromTankRecipes.FILL_FROM_TANK_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.CAULDRON);
    }

    // Getters
    public Ingredient getEmptyCellIngredient() {
        return emptyCellIngredient;
    }

    public FluidStack getFluidInput() {
        return fluidInput;
    }

    public ItemStack getFilledCellResult() {
        return filledCellResult;
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