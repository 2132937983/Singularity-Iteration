package com.singularity_iteration.mio_icif.recipe.block_cutter;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块切割机配方注册类
 */
@SuppressWarnings("null")
public class mio_icif_BlockCutterRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Singularity_Iteration.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_BlockCutterRecipe>> BLOCK_CUTTER_TYPE =
        RECIPE_TYPES.register("block_cutter", () -> new RecipeType<mio_icif_BlockCutterRecipe>() {
            @Override
            public String toString() {
                return "block_cutter";
            }
        });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_BlockCutterRecipe>> BLOCK_CUTTER_SERIALIZER =
        RECIPE_SERIALIZERS.register("block_cutter", mio_icif_BlockCutterRecipeSerializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }
}


