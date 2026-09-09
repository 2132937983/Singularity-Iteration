package com.singularity_iteration.mio_icif.recipe.fluid_refining;

import com.singularity_iteration.mio_icif.api.recipe.IRecipeProcessingInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;

public class FluidRefiningRecipe implements Recipe<FluidRefiningRecipeInput>, IRecipeProcessingInfo {

    private final String group;
    private final Ingredient inputIngredient;
    private final Fluid inputFluid;
    @Nullable
    private final TagKey<Fluid> inputFluidTag;
    private final FluidStack outputFluid;
    private final int processingTime;
    private final int energyPerTick;

    public FluidRefiningRecipe(String group, Ingredient inputIngredient, Fluid inputFluid,
                                @Nullable TagKey<Fluid> inputFluidTag,
                                FluidStack outputFluid, int processingTime, int energyPerTick) {
        this.group = group;
        this.inputIngredient = inputIngredient;
        this.inputFluid = inputFluid;
        this.inputFluidTag = inputFluidTag;
        this.outputFluid = outputFluid;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public boolean matches(FluidRefiningRecipeInput input, Level level) {
        if (input.inputItem().isEmpty()) return false;
        if (!inputIngredient.isEmpty() && !inputIngredient.test(input.inputItem())) return false;
        return matchesFluid(input.inputFluid().getFluid());
    }

    @SuppressWarnings("deprecation")
    public boolean matchesFluid(Fluid fluid) {
        if (inputFluidTag != null) {
            return fluid.is(inputFluidTag);
        }
        return fluid == inputFluid;
    }

    @Override
    public ItemStack assemble(FluidRefiningRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(inputIngredient);
        return list;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FluidRefiningRecipes.FLUID_REFINING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return FluidRefiningRecipes.FLUID_REFINING_TYPE.get();
    }

    public Ingredient getInputIngredient() {
        return inputIngredient;
    }

    public Fluid getInputFluid() {
        return inputFluid;
    }

    @Nullable
    public TagKey<Fluid> getInputFluidTag() {
        return inputFluidTag;
    }

    public FluidStack getOutputFluid() {
        return outputFluid;
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