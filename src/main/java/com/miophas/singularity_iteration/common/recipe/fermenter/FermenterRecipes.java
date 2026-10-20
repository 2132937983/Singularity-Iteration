package com.miophas.singularity_iteration.common.recipe.fermenter;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The fermenter recipe type ({@code data/<namespace>/recipe/...json} with {@code "type": "mio_icif:fermenter"})
 * and the lookups the fermenter uses. As in IC2, a fluid has one fermenter recipe: the first that takes it.
 */
public final class FermenterRecipes {
    private FermenterRecipes() {}

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Singularity_Iteration.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Singularity_Iteration.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<FermenterRecipe>> FERMENTER_TYPE =
        RECIPE_TYPES.register("fermenter", () -> new RecipeType<FermenterRecipe>() {
            @Override
            public String toString() {
                return "fermenter";
            }
        });

    public static final DeferredHolder<RecipeSerializer<?>, FermenterRecipeSerializer> FERMENTER_SERIALIZER =
        RECIPE_SERIALIZERS.register("fermenter", FermenterRecipeSerializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }

    public static List<RecipeHolder<FermenterRecipe>> all(@Nullable Level level) {
        return level == null ? List.of() : level.getRecipeManager().getAllRecipesFor(FERMENTER_TYPE.get());
    }

    /** The recipe that ferments {@code fluid}, or null. */
    @Nullable
    public static RecipeHolder<FermenterRecipe> find(@Nullable Level level, @Nullable Fluid fluid) {
        if (fluid == null) return null;
        for (RecipeHolder<FermenterRecipe> holder : all(level)) {
            if (holder.value().accepts(fluid)) return holder;
        }
        return null;
    }

    /** Whether any recipe ferments {@code fluid} (the input tank's filter). */
    public static boolean accepts(@Nullable Level level, @Nullable Fluid fluid) {
        return find(level, fluid) != null;
    }

    /** Whether any recipe gives {@code fluid} (the output tank's filter). */
    public static boolean isOutput(@Nullable Level level, @Nullable Fluid fluid) {
        if (fluid == null) return false;
        for (RecipeHolder<FermenterRecipe> holder : all(level)) {
            if (holder.value().getOutput().getFluid() == fluid) return true;
        }
        return false;
    }

    /** The recipe with this id if it is still loaded (a datapack reload replaces the holders). */
    @Nullable
    public static RecipeHolder<FermenterRecipe> byId(@Nullable Level level, @Nullable ResourceLocation id) {
        if (level == null || id == null) return null;
        var holder = level.getRecipeManager().byKey(id).orElse(null);
        return holder != null && holder.value() instanceof FermenterRecipe ? cast(holder) : null;
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<FermenterRecipe> cast(RecipeHolder<?> holder) {
        return (RecipeHolder<FermenterRecipe>) holder;
    }
}
