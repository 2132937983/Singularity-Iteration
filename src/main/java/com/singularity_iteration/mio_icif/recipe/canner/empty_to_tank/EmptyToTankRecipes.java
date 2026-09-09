package com.singularity_iteration.mio_icif.recipe.canner.empty_to_tank;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 单元灌入水槽模式配方注册�? */
@SuppressWarnings("null")
public class EmptyToTankRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Singularity_Iteration.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<EmptyToTankRecipe>> EMPTY_TO_TANK_TYPE =
        RECIPE_TYPES.register("empty_to_tank", () -> new RecipeType<EmptyToTankRecipe>() {
            @Override
            public String toString() {
                return "empty_to_tank";
            }
        });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EmptyToTankRecipe>> EMPTY_TO_TANK_SERIALIZER =
        RECIPE_SERIALIZERS.register("empty_to_tank", EmptyToTankRecipeSerializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }
}


