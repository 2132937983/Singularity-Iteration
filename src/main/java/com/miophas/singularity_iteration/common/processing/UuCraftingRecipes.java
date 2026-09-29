// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import com.miophas.singularity_iteration.core.runtime.uu.IndependentUuValueIndex;
import com.miophas.singularity_iteration.core.runtime.uu.UuRecipeSolver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/** Fixed-output vanilla crafting only; unsupported dynamic/third-party recipes stay unquoted. */
public final class UuCraftingRecipes {
    public record Catalog(List<UuRecipeSolver.Rule<IndependentUuValueIndex.Key>> rules, int unsupported) { }
    // R93 public output: iron/diamond blocks, gold nuggets and table each add one raw unit per craft.
    public static final double CRAFT_OVERHEAD_BUCKETS = 1.0 / 100000.0;
    private UuCraftingRecipes() { }

    public static Catalog read(RecipeManager manager, HolderLookup.Provider registries) {
        var recipes = manager.getAllRecipesFor(RecipeType.CRAFTING).stream()
                .sorted(Comparator.comparing(row -> row.id().toString())).toList();
        if (recipes.size() > 32768) throw new IllegalArgumentException("Too many crafting recipes");
        List<UuRecipeSolver.Rule<IndependentUuValueIndex.Key>> rules = new ArrayList<>();
        int unsupported = 0;
        for (RecipeHolder<?> holder : recipes) {
            var recipe = holder.value();
            if (recipe.getClass() != ShapedRecipe.class && recipe.getClass() != ShapelessRecipe.class) {
                unsupported++; continue;
            }
            ItemStack output = recipe.getResultItem(registries);
            if (output.isEmpty() || output.getCount() > 64) { unsupported++; continue; }
            var slots = new ArrayList<List<UuRecipeSolver.Choice<IndependentUuValueIndex.Key>>>();
            boolean supported = true;
            for (var ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                if (ingredient.isCustom() || ingredient.hasNoItems() || ingredient.getItems().length > 256) {
                    supported = false; break;
                }
                var choices = new ArrayList<UuRecipeSolver.Choice<IndependentUuValueIndex.Key>>();
                for (var value : ingredient.getItems()) {
                    ItemStack item = value.copyWithCount(1);
                    var returned = item.getCraftingRemainingItem();
                    if (!returned.isEmpty() && returned.getCount() != 1) { supported = false; break; }
                    choices.add(new UuRecipeSolver.Choice<>(IndependentUuValueIndex.keyOf(item),
                            returned.isEmpty() ? null : IndependentUuValueIndex.keyOf(returned)));
                }
                if (!supported) break;
                slots.add(List.copyOf(choices));
            }
            if (!supported || slots.isEmpty()) { unsupported++; continue; }
            rules.add(new UuRecipeSolver.Rule<>(holder.id().toString(), IndependentUuValueIndex.keyOf(output),
                    output.getCount(), slots, CRAFT_OVERHEAD_BUCKETS));
        }
        return new Catalog(List.copyOf(rules), unsupported);
    }
}