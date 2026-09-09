package com.singularity_iteration.mio_icif.recipe.generic;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.recipe.mio_icif_SingleItemRecipeInput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RecyclerRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Singularity_Iteration.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<RecyclerRecipe>> RECYCLER_TYPE =
        RECIPE_TYPES.register("recycler", () -> new RecipeType<RecyclerRecipe>() {
            @Override
            public String toString() {
                return "recycler";
            }
        });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AbstractSingleInputRecipe<mio_icif_SingleItemRecipeInput>>> RECYCLER_SERIALIZER =
        RECIPE_SERIALIZERS.register("recycler", () -> new GenericSingleInputRecipeSerializer());

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
