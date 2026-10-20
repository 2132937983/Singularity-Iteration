package com.miophas.singularity_iteration.common.recipe.fermenter;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * Fermenter recipe (IC2 {@code FermenterRecipeManager} / {@code FermentationProperty}): one run takes
 * {@code input} (fluid and amount) and {@code heat} HU and gives {@code output}; every
 * {@code byproduct_interval} mB of input fermented gives {@code byproduct} (IC2: 1 fertilizer per
 * 500 mB of biomass).
 *
 * <pre>{@code
 * {
 *   "type": "mio_icif:fermenter",
 *   "input": {"fluid": "mio_icif:biomass", "amount": 20},
 *   "heat": 4000,
 *   "output": {"id": "mio_icif:biogas", "amount": 400},
 *   "byproduct": {"id": "mio_icif:resource/item_fertilizer", "count": 1},
 *   "byproduct_interval": 500
 * }
 * }</pre>
 * {@code input} also takes a fluid tag ({@code "tag"}); {@code byproduct} is optional.
 */
public class FermenterRecipe implements Recipe<FermenterRecipeInput> {

    public static final int DEFAULT_BYPRODUCT_INTERVAL = 500;

    private final String group;
    private final SizedFluidIngredient input;
    private final int heat;
    private final FluidStack output;
    private final ItemStack byproduct;
    private final int byproductInterval;

    public FermenterRecipe(String group, SizedFluidIngredient input, int heat, FluidStack output,
                           ItemStack byproduct, int byproductInterval) {
        this.group = group;
        this.input = input;
        this.heat = heat;
        this.output = output;
        this.byproduct = byproduct;
        this.byproductInterval = byproductInterval;
    }

    /** Whether this recipe ferments {@code fluid} (the amount is checked by the machine). */
    public boolean accepts(Fluid fluid) {
        return input.ingredient().test(new FluidStack(fluid, 1));
    }

    @Override
    public boolean matches(FermenterRecipeInput recipeInput, Level level) {
        return !recipeInput.fluid().isEmpty() && input.ingredient().test(recipeInput.fluid());
    }

    @Override
    public ItemStack assemble(FermenterRecipeInput recipeInput, HolderLookup.Provider registries) {
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
        return NonNullList.create();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FermenterRecipes.FERMENTER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return FermenterRecipes.FERMENTER_TYPE.get();
    }

    /** The fluid and the amount one run takes. */
    public SizedFluidIngredient getInput() {
        return input;
    }

    /** HU one run takes. */
    public int getHeat() {
        return heat;
    }

    /** The fluid one run gives (a copy). */
    public FluidStack getOutput() {
        return output.copy();
    }

    /** The item given every {@link #getByproductInterval()} mB of input; empty for none (a copy). */
    public ItemStack getByproduct() {
        return byproduct.copy();
    }

    /** mB of input fermented per by-product. */
    public int getByproductInterval() {
        return byproductInterval;
    }
}
