// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

/**
 * 0.1.10.13 (GitHub issue #18): the result of a recipe that can come from any mod.
 *
 * <p>{@link Recipe#getResultItem} should never return {@code null}, but recipes of some mods do (or
 * throw, for a dynamic recipe without a fixed result). Code that reads the recipes of every mod (the
 * machine salvage drop, the electric furnaces that run the vanilla smelting recipes) reads the result
 * here: {@code null} and an exception both give {@link ItemStack#EMPTY}, which the callers already
 * treat as "no usable result".
 */
public final class RecipeResults {
    private RecipeResults() { }

    /** The result of {@code recipe}, or {@link ItemStack#EMPTY} when the recipe has none or fails. */
    public static ItemStack of(Recipe<?> recipe, HolderLookup.Provider registries) {
        if (recipe == null) return ItemStack.EMPTY;
        try {
            ItemStack result = recipe.getResultItem(registries);
            return result == null ? ItemStack.EMPTY : result;
        } catch (RuntimeException ignored) {
            return ItemStack.EMPTY;   // a dynamic recipe without a fixed result
        }
    }
}
