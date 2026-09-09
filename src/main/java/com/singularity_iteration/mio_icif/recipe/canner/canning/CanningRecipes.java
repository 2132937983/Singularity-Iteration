package com.singularity_iteration.mio_icif.recipe.canner.canning;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 装罐模式配方注册�? */
@SuppressWarnings("null")
public class CanningRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Singularity_Iteration.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CanningRecipe>> CANNING_TYPE =
        RECIPE_TYPES.register("canning", () -> new RecipeType<CanningRecipe>() {
            @Override
            public String toString() {
                return "canning";
            }
        });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CanningRecipe>> CANNING_SERIALIZER =
        RECIPE_SERIALIZERS.register("canning", CanningRecipeSerializer::new);

    // 动态装罐配方类�
public static final DeferredHolder<RecipeType<?>, RecipeType<DynamicCanningRecipe>> DYNAMIC_CANNING_TYPE =
        RECIPE_TYPES.register("dynamic_canning", () -> new RecipeType<DynamicCanningRecipe>() {
            @Override
            public String toString() {
                return "dynamic_canning";
            }
        });

    // 动态装罐配方序列化学
public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DynamicCanningRecipe>> DYNAMIC_CANNING_SERIALIZER =
        RECIPE_SERIALIZERS.register("dynamic_canning", DynamicCanningRecipeSerializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }
}


