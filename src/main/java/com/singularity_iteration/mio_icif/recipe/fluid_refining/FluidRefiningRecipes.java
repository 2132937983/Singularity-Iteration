package com.singularity_iteration.mio_icif.recipe.fluid_refining;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class FluidRefiningRecipes {

    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Singularity_Iteration.MOD_ID);

    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>, net.minecraft.world.item.crafting.RecipeType<FluidRefiningRecipe>> FLUID_REFINING_TYPE =
        RECIPE_TYPES.register("fluid_refining", () -> new net.minecraft.world.item.crafting.RecipeType<FluidRefiningRecipe>() {
            @Override
            public String toString() {
                return "fluid_refining";
            }
        });

    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, FluidRefiningRecipeSerializer> FLUID_REFINING_SERIALIZER =
        RECIPE_SERIALIZERS.register("fluid_refining", FluidRefiningRecipeSerializer::new);

    public static final int DEFAULT_PROCESSING_TIME = 200;
    public static final int DEFAULT_ENERGY_PER_TICK = 10;
}