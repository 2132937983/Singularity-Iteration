package com.singularity_iteration.mio_icif.recipe.compressor;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 压缩机配方注册类
 * 管理压缩机配方的类型和序列化器注册? */
@SuppressWarnings("null")
public class mio_icif_CompressorRecipes {

    // 配方类型注册�
public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Singularity_Iteration.MOD_ID);

    // 配方序列化器注册�
public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);

    // 注册压缩机配方类�
public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_CompressorRecipe>> COMPRESSOR_TYPE =
        RECIPE_TYPES.register("compressor", () -> new RecipeType<mio_icif_CompressorRecipe>() {
            @Override
            public String toString() {
                return "compressor";
            }
        });

    // 注册压缩机配方序列化学
public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_CompressorRecipe>> COMPRESSOR_SERIALIZER =
        RECIPE_SERIALIZERS.register("compressor", mio_icif_CompressorRecipeSerializer::new);

    /**
     * 注册所有压缩机配方类型和序列化学
 * @param eventBus 事件总线
     */
    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }
}


