package com.singularity_iteration.mio_icif.recipe.blast_furnace;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 高炉配方注册器
 * 管理高炉配方的类型和序列化器注册
 */
@SuppressWarnings("null")
public class mio_icif_BlastFurnaceRecipes {

    // 配方类型注册器
public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Singularity_Iteration.MOD_ID);

    // 配方序列化器注册器
public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);

    // 注册高炉配方类型
    public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_BlastFurnaceRecipe>> BLAST_FURNACE_TYPE =
        RECIPE_TYPES.register("blast_furnace", () -> new RecipeType<mio_icif_BlastFurnaceRecipe>() {
            @Override
            public String toString() {
                return "blast_furnace";
            }
        });

 // 注册高炉配方序列化器
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_BlastFurnaceRecipe>> BLAST_FURNACE_SERIALIZER =
        RECIPE_SERIALIZERS.register("blast_furnace", mio_icif_BlastFurnaceRecipeSerializer::new);

    /**
 * 注册所有高炉配方类型和序列化器
     * @param eventBus 事件总线
     */
    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }
}