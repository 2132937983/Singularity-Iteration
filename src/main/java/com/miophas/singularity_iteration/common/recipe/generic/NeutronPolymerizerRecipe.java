package com.miophas.singularity_iteration.common.recipe.generic;

import com.miophas.singularity_iteration.core.prefab.recipe.AbstractSingleInputRecipe;

import com.miophas.singularity_iteration.common.recipe.mio_icif_SingleItemRecipeInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

@SuppressWarnings("null")
public class NeutronPolymerizerRecipe extends AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput> {

    /** 0.1.11.8: what the recipe does physically ({@link PolymerizerProcess}); only the screen and the radiation read it. */
    private final PolymerizerProcess process;

    public NeutronPolymerizerRecipe(String group, Ingredient ingredient, ItemStack result, int processingTime, int energyPerTick, float experience) {
        this(group, ingredient, result, processingTime, energyPerTick, experience, PolymerizerProcess.UNKNOWN);
    }

    public NeutronPolymerizerRecipe(String group, Ingredient ingredient, ItemStack result, int processingTime, int energyPerTick, float experience,
                                    PolymerizerProcess process) {
        super(group, ingredient, result, processingTime, energyPerTick);
        this.process = process == null ? PolymerizerProcess.UNKNOWN : process;
    }

    public PolymerizerProcess getProcess() {
        return process;
    }

    @Override
    public boolean matches(mio_icif_SingleItemRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return NeutronPolymerizerRecipes.NEUTRON_POLYMERIZER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return NeutronPolymerizerRecipes.NEUTRON_POLYMERIZER_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(net.minecraft.world.level.block.Blocks.BEACON);
    }
}