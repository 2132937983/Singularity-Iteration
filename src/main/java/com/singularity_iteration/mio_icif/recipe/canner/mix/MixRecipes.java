package com.singularity_iteration.mio_icif.recipe.canner.mix;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 混合模式配方注册�? */
@SuppressWarnings("null")
public class MixRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Singularity_Iteration.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MixRecipe>> MIX_TYPE =
        RECIPE_TYPES.register("mix", () -> new RecipeType<MixRecipe>() {
            @Override
            public String toString() {
                return "mix";
            }
        });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MixRecipe>> MIX_SERIALIZER =
        RECIPE_SERIALIZERS.register("mix", MixRecipeSerializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }
}


