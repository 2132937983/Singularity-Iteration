package com.singularity_iteration.mio_icif.recipe.canner.fill_from_tank;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 水槽灌满单元模式配方注册�? */
@SuppressWarnings("null")
public class FillFromTankRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Singularity_Iteration.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<FillFromTankRecipe>> FILL_FROM_TANK_TYPE =
        RECIPE_TYPES.register("fill_from_tank", () -> new RecipeType<FillFromTankRecipe>() {
            @Override
            public String toString() {
                return "fill_from_tank";
            }
        });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FillFromTankRecipe>> FILL_FROM_TANK_SERIALIZER =
        RECIPE_SERIALIZERS.register("fill_from_tank", FillFromTankRecipeSerializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }
}


