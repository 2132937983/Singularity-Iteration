// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.recipe.block_cutter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * 0.1.10.29: the planks of a log for the block cutter's wood recipes ({@code "wood": true}). Before 0.1.10.29 one recipe turned
 * every log into oak planks. Now the planks follow the crafting recipe of that log: the crafting recipe whose result is in
 * {@code #minecraft:planks} and whose only ingredient takes the log (vanilla: 11 woods by their log tags, logs, wood and
 * stripped variants alike; bamboo blocks; any mod wood with such a recipe). The cutter gives {@code perFour} planks for every
 * 4 crafted ({@link #count}; the recipe's result count, 6 in {@code logs_to_planks}: IC2 1.12.2 cuts a log into 6 planks, 4 by
 * hand; bamboo blocks give 3 for 2).
 *
 * <p>The table of a recipe manager is built once (recipe ids in order, the recipe with the most planks wins). A reload
 * replaces the recipe holders (the client keeps its recipe manager and replaces its recipes), so the table is rebuilt when the
 * holder it was stamped with is no longer the manager's.
 */
public final class WoodPlanks {
    private WoodPlanks() {}

    /** A crafting recipe that makes planks from one ingredient. */
    public record Source(RecipeHolder<CraftingRecipe> recipe, Ingredient ingredient, ItemStack planks) { }

    private record Table(@Nullable RecipeHolder<CraftingRecipe> stamp, List<Source> sources, Map<Item, ItemStack> known) { }

    private static final Map<RecipeManager, Table> TABLES = Collections.synchronizedMap(new WeakHashMap<>());

    /** Planks the block cutter gives for {@code crafted} planks of the crafting table: {@code perFour} for 4, at least 1. */
    public static int count(int crafted, int perFour) {
        if (crafted <= 0) return 0;
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE / 2, (long) crafted * Math.max(1, perFour) / 4));
    }

    /** The planks recipes of a recipe manager, by recipe id. */
    public static List<Source> sources(RecipeManager manager, @Nullable HolderLookup.Provider access) {
        return table(manager, access).sources();
    }

    /** The crafted planks of {@code log} (one item, its crafting count), or empty when no planks recipe takes it. */
    public static ItemStack crafted(@Nullable RecipeManager manager, @Nullable HolderLookup.Provider access, ItemStack log) {
        if (log.isEmpty() || manager == null) return ItemStack.EMPTY;
        Table t = table(manager, access);
        synchronized (t.known()) {
            ItemStack hit = t.known().get(log.getItem());
            if (hit != null) return hit;
        }
        ItemStack best = ItemStack.EMPTY;
        for (Source s : t.sources()) {
            if (!s.ingredient().test(log)) continue;
            if (best.isEmpty() || s.planks().getCount() > best.getCount()) best = s.planks();
        }
        synchronized (t.known()) {
            t.known().put(log.getItem(), best);
        }
        return best;
    }

    /** The cutter's planks for {@code log} ({@link #count} of the crafted count), or empty. */
    public static ItemStack planks(@Nullable RecipeManager manager, @Nullable HolderLookup.Provider access, ItemStack log, int perFour) {
        ItemStack c = crafted(manager, access, log);
        return c.isEmpty() ? ItemStack.EMPTY : c.copyWithCount(count(c.getCount(), perFour));
    }

    private static Table table(RecipeManager manager, @Nullable HolderLookup.Provider access) {
        Table t = TABLES.get(manager);
        if (t != null && t.stamp() != null && manager.byKey(t.stamp().id()).orElse(null) == t.stamp()) return t;
        List<RecipeHolder<CraftingRecipe>> all = manager.getAllRecipesFor(RecipeType.CRAFTING);
        List<Source> sources = new ArrayList<>();
        for (RecipeHolder<CraftingRecipe> h : all) {
            ItemStack out;
            try {
                out = h.value().getResultItem(access);
            } catch (RuntimeException e) {
                continue;
            }
            if (out == null || out.isEmpty() || !out.is(ItemTags.PLANKS)) continue;
            Ingredient only = single(h.value());
            if (only == null) continue;
            sources.add(new Source(h, only, out.copy()));
        }
        sources.sort(Comparator.comparing(s -> s.recipe().id().toString()));
        t = new Table(all.isEmpty() ? null : all.get(0), List.copyOf(sources), new IdentityHashMap<>());
        TABLES.put(manager, t);
        return t;
    }

    /** The only non-empty ingredient of a recipe, or null. */
    static @Nullable Ingredient single(CraftingRecipe r) {
        Ingredient only = null;
        for (Ingredient i : r.getIngredients()) {
            if (i.isEmpty()) continue;
            if (only != null) return null;
            only = i;
        }
        return only;
    }
}
