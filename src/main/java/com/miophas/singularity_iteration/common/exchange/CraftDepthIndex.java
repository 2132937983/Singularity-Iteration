// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;

/**
 * 0.1.10.21 (futures M3): the craft depth ({@link CraftDepth}) of every item that the recipes of the server make,
 * built from the recipe manager the first time it is needed and again after a reload (a reload replaces the
 * recipe manager). All recipe types count: crafting, smelting and the machine recipes; a recipe without item
 * ingredients does not (a crop, a fluid or a generator makes a raw material).
 */
public final class CraftDepthIndex {
    private CraftDepthIndex() {}

    /** Relaxation passes; a deeper chain keeps its upper value (rare: the depth only chooses the currency). */
    static final int PASSES = 24;

    private record Index(RecipeManager manager, Map<Item, Integer> depth) { }

    private static final Map<MinecraftServer, Index> INDEX = new WeakHashMap<>();

    /** The craft depth of {@code item}: 0 when no recipe makes it. */
    public static int depth(MinecraftServer server, Item item) {
        return index(server).getOrDefault(item, 0);
    }

    static synchronized Map<Item, Integer> index(MinecraftServer server) {
        RecipeManager manager = server.getRecipeManager();
        Index known = INDEX.get(server);
        if (known != null && known.manager() == manager) return known.depth();
        List<CraftDepth.Recipe<Item>> recipes = new ArrayList<>();
        for (var holder : manager.getRecipes()) {
            ItemStack result;
            try {
                result = com.miophas.singularity_iteration.common.util.RecipeResults.of(holder.value(), server.registryAccess());
            } catch (RuntimeException e) {
                continue;
            }
            if (result.isEmpty()) continue;
            List<Set<Item>> slots = new ArrayList<>();
            boolean usable = true;
            try {
                for (Ingredient ingredient : holder.value().getIngredients()) {
                    if (ingredient.isEmpty()) continue;
                    Set<Item> choices = new HashSet<>();
                    for (ItemStack stack : ingredient.getItems()) if (!stack.isEmpty()) choices.add(stack.getItem());
                    if (!choices.isEmpty()) slots.add(choices);
                }
            } catch (RuntimeException e) {
                usable = false;
            }
            // a recipe without item ingredients (crops, fluids, generators) does not make its result deeper
            if (usable && !slots.isEmpty()) recipes.add(new CraftDepth.Recipe<>(result.getItem(), slots));
        }
        Map<Item, Integer> depth = CraftDepth.compute(recipes, PASSES);
        INDEX.put(server, new Index(manager, depth));
        return depth;
    }
}
