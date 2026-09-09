package com.singularity_iteration.mio_icif.recipe.washer;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 洗矿配方注册�?
 * 统一管理洗矿配方的注册?
 */
@SuppressWarnings("null")
public class mio_icif_WasherRecipes {

    // 配方类型注册�?
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Singularity_Iteration.MOD_ID);

    // 配方序列化器注册�?
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);

    // 注册洗矿配方类型
    public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_WasherRecipe>> WASHER_TYPE =
        RECIPE_TYPES.register("washer", () -> new RecipeType<mio_icif_WasherRecipe>() {
            @Override
            public String toString() {
                return "washer";
            }
        });

    // 注册洗矿配方序列化器
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_WasherRecipe>> WASHER_SERIALIZER =
        RECIPE_SERIALIZERS.register("washer", mio_icif_WasherRecipeSerializer::new);

    /**
     * 注册所有洗矿配方类型和序列化器
     * @param eventBus 事件总线
     */
    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }
}


