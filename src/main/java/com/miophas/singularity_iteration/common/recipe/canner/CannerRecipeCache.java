// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.recipe.canner;

import com.miophas.singularity_iteration.common.recipe.canner.canning.CanningRecipeInput;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** Machine-owned lookup data; matching and transaction admission always use current state. */
public final class CannerRecipeCache {
    private RecipeManager manager;
    private Collection<RecipeHolder<?>> generation;
    private final Map<RecipeType<?>, List<?>> recipesByType = new IdentityHashMap<>();
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack material = ItemStack.EMPTY;
    private CanningRecipeInput canningInput;

    /** Copies only changed item values, including counts and data components. */
    public CanningRecipeInput canningInput(ItemStack nextInput, ItemStack nextMaterial) {
        updateInput(nextInput);
        if (!ItemStack.matches(material, nextMaterial)) {
            material = nextMaterial.copy();
            canningInput = null;
        }
        if (canningInput == null) canningInput = new CanningRecipeInput(input, material);
        return canningInput;
    }

    private void updateInput(ItemStack nextInput) {
        if (ItemStack.matches(input, nextInput)) return;
        input = nextInput.copy();
        canningInput = null;
    }

    /**
     * RecipeManager exposes the immutable by-name values view. Both reload and
     * replaceRecipes replace that view, including replacement on the same manager.
     */
    @SuppressWarnings("unchecked")
    public <I extends RecipeInput, R extends Recipe<I>> List<RecipeHolder<R>> recipes(
            RecipeManager current, RecipeType<R> type) {
        Collection<RecipeHolder<?>> currentGeneration = current.getRecipes();
        if (manager != current || generation != currentGeneration) {
            recipesByType.clear();
            manager = current;
            generation = currentGeneration;
        }
        return (List<RecipeHolder<R>>) (List<?>) recipesByType.computeIfAbsent(type,
                ignored -> current.getAllRecipesFor(type));
    }

    /** Do not memoize matches: addon recipes may depend on live world or tag state. */
    public <I extends RecipeInput, R extends Recipe<I>> Optional<RecipeHolder<R>> find(
            RecipeManager current, RecipeType<R> type, I input, Level level) {
        if (input.isEmpty()) return Optional.empty();
        for (RecipeHolder<R> holder : recipes(current, type)) {
            if (holder.value().matches(input, level)) return Optional.of(holder);
        }
        return Optional.empty();
    }
}
