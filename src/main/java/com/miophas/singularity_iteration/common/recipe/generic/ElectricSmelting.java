package com.miophas.singularity_iteration.common.recipe.generic;

import com.miophas.singularity_iteration.common.recipe.mio_icif_SingleItemRecipeInput;
import com.miophas.singularity_iteration.core.runtime.processing.RecipeLookupCache;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * What the Electric and Induction Furnaces smelt: an {@code mio_icif:electric_furnace} recipe (from the
 * recipe API or a datapack) first, then the vanilla smelting recipe. The electric furnace recipe comes
 * back as a smelting recipe under its own id, so the furnaces bank its XP like any other smelting.
 * One instance per lookup site; each keeps its own memo.
 */
public final class ElectricSmelting {
    private final RecipeLookupCache<mio_icif_SingleItemRecipeInput, ElectricFurnaceRecipe> own =
        new RecipeLookupCache<>(ElectricFurnaceRecipes.ELECTRIC_FURNACE_TYPE);
    private final RecipeLookupCache<SingleRecipeInput, SmeltingRecipe> vanilla = RecipeLookupCache.of(RecipeType.SMELTING);
    private @Nullable RecipeHolder<ElectricFurnaceRecipe> lastOwn;
    private @Nullable RecipeHolder<SmeltingRecipe> lastConverted;

    public Optional<RecipeHolder<SmeltingRecipe>> find(@Nullable Level level, ItemStack input) {
        if (level == null || input.isEmpty()) return Optional.empty();
        var electric = own.find(level, new mio_icif_SingleItemRecipeInput(input));
        if (electric.isPresent()) {
            if (electric.get() != lastOwn) {
                lastOwn = electric.get();
                lastConverted = asSmelting(lastOwn);
            }
            return Optional.of(lastConverted);
        }
        return vanilla.find(level, new SingleRecipeInput(input));
    }

    /** An electric furnace recipe as a smelting recipe under the same id. */
    public static RecipeHolder<SmeltingRecipe> asSmelting(RecipeHolder<ElectricFurnaceRecipe> holder) {
        ElectricFurnaceRecipe recipe = holder.value();
        return new RecipeHolder<>(holder.id(), new SmeltingRecipe(recipe.getGroup(), CookingBookCategory.MISC,
            recipe.getIngredient(), recipe.getResult(), recipe.getExperience(), recipe.getProcessingTime()));
    }
}
