package com.singularity_iteration.mio_icif.recipe.metal_former.cutting;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class mio_icif_CuttingRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Singularity_Iteration.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_CuttingRecipe>> CUTTING_TYPE =
        RECIPE_TYPES.register("metal_former_cutting", () -> new RecipeType<mio_icif_CuttingRecipe>() {
            @Override
            public String toString() {
                return "metal_former_cutting";
            }
        });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_CuttingRecipe>> CUTTING_SERIALIZER =
        RECIPE_SERIALIZERS.register("metal_former_cutting", mio_icif_CuttingRecipeSerializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }
}


