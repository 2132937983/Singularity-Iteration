// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import com.miophas.singularity_iteration.common.recipe.ElectricIngredient;
import com.miophas.singularity_iteration.common.recipe.charge_carrying.ChargeCarryingRecipe;
import com.miophas.singularity_iteration.common.recipe.charge_carrying.ChargeCarryingShapelessRecipe;
import com.miophas.singularity_iteration.core.runtime.uu.IndependentUuValueIndex;
import com.miophas.singularity_iteration.core.runtime.uu.UuRecipeSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import org.jetbrains.annotations.Nullable;

/** Fixed-output vanilla crafting only; unsupported dynamic/third-party recipes stay unquoted. */
public final class UuCraftingRecipes {
    public record Catalog(List<UuRecipeSolver.Rule<IndependentUuValueIndex.Key>> rules, int unsupported) { }
    // R93 public output: iron/diamond blocks, gold nuggets and table each add one raw unit per craft.
    public static final double CRAFT_OVERHEAD_BUCKETS = 1.0 / 100000.0;
    /** The most choices one ingredient slot may offer the solver. */
    private static final int MAX_CHOICES = 256;
    private UuCraftingRecipes() { }

    /** The recipes of the published generation: plain shaped and shapeless recipes with plain ingredients. */
    public static Catalog read(RecipeManager manager, HolderLookup.Provider registries) {
        return read(manager, registries, false);
    }

    /**
     * 0.1.11.218: the recipes the Scanner prices from: those of {@link #read}, plus SI's charge-carrying recipes (shaped and
     * shapeless; they consume their ingredients as the plain ones do and only pass the charge on) and SI's electric
     * ingredient ({@code mio_icif:electric}: the item whatever its charge), keyed as the item's plain stack, the form the
     * catalogs and the recipe results use (its empty stack would be a key nothing makes, which the solver would seed with an
     * estimate), and array ingredients (NeoForge reads {@code [a, b]} as a compound ingredient) made of those. Until 0.1.11.217
     * these 186 SI recipes were skipped; the array one is the Raw Carbon Fibre (coal or charcoal dust), so no carbon plate,
     * advanced machine casing, MFSU or EESU could be priced, and the Scanner priced an EESU or a TESU Energy Cell with the
     * formula for a natural block (0.33 mB, less than an iron ingot). The published generation keeps reading {@link #read}.
     */
    public static Catalog readForScanner(RecipeManager manager, HolderLookup.Provider registries) {
        return read(manager, registries, true);
    }

    private static Catalog read(RecipeManager manager, HolderLookup.Provider registries, boolean scanner) {
        var recipes = manager.getAllRecipesFor(RecipeType.CRAFTING).stream()
                .sorted(Comparator.comparing(row -> row.id().toString())).toList();
        if (recipes.size() > 32768) throw new IllegalArgumentException("Too many crafting recipes");
        List<UuRecipeSolver.Rule<IndependentUuValueIndex.Key>> rules = new ArrayList<>();
        int unsupported = 0;
        for (RecipeHolder<?> holder : recipes) {
            var recipe = holder.value();
            if (!readable(recipe, scanner)) {
                unsupported++; continue;
            }
            ItemStack output = recipe.getResultItem(registries);
            if (output.isEmpty() || output.getCount() > 64) { unsupported++; continue; }
            var slots = new ArrayList<List<UuRecipeSolver.Choice<IndependentUuValueIndex.Key>>>();
            boolean supported = true;
            for (var ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                List<ItemStack> items = scanner ? scannerItems(ingredient) : plainItems(ingredient);
                if (items == null || items.isEmpty() || items.size() > MAX_CHOICES) {
                    supported = false; break;
                }
                var choices = new ArrayList<UuRecipeSolver.Choice<IndependentUuValueIndex.Key>>();
                for (var item : items) {
                    supported = addChoice(choices, item);
                    if (!supported) break;
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

    /** The items of a plain ingredient, one each; null for a custom ingredient or none (the published generation). */
    private static @Nullable List<ItemStack> plainItems(Ingredient ingredient) {
        if (ingredient.isCustom() || ingredient.hasNoItems()) return null;
        ItemStack[] items = ingredient.getItems();
        if (items.length > MAX_CHOICES) return null;
        return Arrays.stream(items).map(stack -> stack.copyWithCount(1)).toList();
    }

    /**
     * The items the Scanner reads from an ingredient: a plain one as {@link #plainItems}; SI's electric ingredient as the
     * plain stacks of its items (it shows the empty item, the catalogs and recipe results key the plain stack); a compound
     * ingredient (an array in the recipe file) as the items of its parts. Null for any other custom ingredient.
     */
    private static @Nullable List<ItemStack> scannerItems(Ingredient ingredient) {
        if (!ingredient.isCustom()) return plainItems(ingredient);
        var custom = ingredient.getCustomIngredient();
        if (custom instanceof ElectricIngredient) {
            return Arrays.stream(ingredient.getItems()).map(stack -> stack.getItem()).distinct().map(ItemStack::new).toList();
        }
        if (custom instanceof CompoundIngredient compound) {
            List<ItemStack> out = new ArrayList<>();
            for (Ingredient child : compound.children()) {
                List<ItemStack> items = scannerItems(child);
                if (items == null) return null;
                out.addAll(items);
            }
            return out;
        }
        return null;
    }

    /** Plain shaped and shapeless recipes; for the Scanner also SI's charge-carrying ones (same ingredients, same use). */
    private static boolean readable(Recipe<?> recipe, boolean scanner) {
        if (recipe.getClass() == ShapedRecipe.class || recipe.getClass() == ShapelessRecipe.class) return true;
        return scanner && (recipe.getClass() == ChargeCarryingRecipe.class || recipe.getClass() == ChargeCarryingShapelessRecipe.class);
    }

    /** One choice: the item consumed and what the grid gives back (a bucket); false when that is not a single item. */
    private static boolean addChoice(List<UuRecipeSolver.Choice<IndependentUuValueIndex.Key>> choices, ItemStack item) {
        var returned = item.getCraftingRemainingItem();
        if (!returned.isEmpty() && returned.getCount() != 1) return false;
        choices.add(new UuRecipeSolver.Choice<>(IndependentUuValueIndex.keyOf(item),
                returned.isEmpty() ? null : IndependentUuValueIndex.keyOf(returned)));
        return true;
    }
}
