package com.singularity_iteration.mio_icif.recipe.molecular_transformer;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class mio_icif_MolecularTransformerRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<mio_icif_MolecularTransformerRecipe>> MOLECULAR_TRANSFORMER_SERIALIZER =
        RECIPE_SERIALIZERS.register("molecular_transformer", () -> new mio_icif_MolecularTransformerRecipe.Serializer());

    public static final DeferredHolder<RecipeType<?>, RecipeType<mio_icif_MolecularTransformerRecipe>> MOLECULAR_TRANSFORMER_TYPE =
        RECIPE_TYPES.register("molecular_transformer", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "molecular_transformer")));

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
        RECIPE_TYPES.register(bus);
    }
}