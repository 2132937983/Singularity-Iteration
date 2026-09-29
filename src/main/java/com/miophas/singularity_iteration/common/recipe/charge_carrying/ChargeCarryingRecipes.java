package com.miophas.singularity_iteration.common.recipe.charge_carrying;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ChargeCarryingRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Singularity_Iteration.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ChargeCarryingRecipe>> CHARGE_CARRYING_TYPE =
        RECIPE_TYPES.register("charge_carrying", () -> new RecipeType<ChargeCarryingRecipe>() {
            @Override
            public String toString() {
                return "charge_carrying";
            }
        });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChargeCarryingRecipe>> CHARGE_CARRYING_SERIALIZER =
        RECIPE_SERIALIZERS.register("charge_carrying", ChargeCarryingRecipeSerializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ChargeCarryingShapelessRecipe>> CHARGE_CARRYING_SHAPELESS_TYPE =
        RECIPE_TYPES.register("charge_carrying_shapeless", () -> new RecipeType<ChargeCarryingShapelessRecipe>() {
            @Override
            public String toString() {
                return "charge_carrying_shapeless";
            }
        });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChargeCarryingShapelessRecipe>> CHARGE_CARRYING_SHAPELESS_SERIALIZER =
        RECIPE_SERIALIZERS.register("charge_carrying_shapeless", ChargeCarryingShapelessRecipeSerializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<GradualChargeRecipe>> GRADUAL_CHARGE_TYPE =
        RECIPE_TYPES.register("gradual_charge", () -> new RecipeType<GradualChargeRecipe>() {
            @Override
            public String toString() {
                return "gradual_charge";
            }
        });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GradualChargeRecipe>> GRADUAL_CHARGE_SERIALIZER =
        RECIPE_SERIALIZERS.register("gradual_charge", GradualChargeRecipeSerializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }
}