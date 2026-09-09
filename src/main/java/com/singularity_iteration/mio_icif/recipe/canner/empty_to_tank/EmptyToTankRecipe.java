package com.singularity_iteration.mio_icif.recipe.canner.empty_to_tank;

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
public class EmptyToTankRecipe implements Recipe<EmptyToTankRecipeInput>, IRecipeProcessingInfo {

    private final String group;
    private final Ingredient cellIngredient;     // 装有流体的单独
private final FluidStack fluidOutput;        // 输出的流体类型和数量
    private final ItemStack emptyCellResult;     // 空单元（副产物）
    private final int processingTime;
    private final int energyPerTick;

    public EmptyToTankRecipe(String group, Ingredient cellIngredient, FluidStack fluidOutput,
                             ItemStack emptyCellResult, int processingTime, int energyPerTick) {
        this.group = group;
        this.cellIngredient = cellIngredient;
        this.fluidOutput = fluidOutput;
        this.emptyCellResult = emptyCellResult;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public boolean matches(EmptyToTankRecipeInput input, Level level) {
        return this.cellIngredient.test(input.filledCell());
    }

    @Override
    public ItemStack assemble(EmptyToTankRecipeInput input, HolderLookup.Provider registries) {
        return this.emptyCellResult.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.emptyCellResult;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(this.cellIngredient);
        return list;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EmptyToTankRecipes.EMPTY_TO_TANK_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return EmptyToTankRecipes.EMPTY_TO_TANK_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.CAULDRON);
    }

    // Getters
    public Ingredient getCellIngredient() {
        return cellIngredient;
    }

    public FluidStack getFluidOutput() {
        return fluidOutput;
    }

    public ItemStack getEmptyCellResult() {
        return emptyCellResult;
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