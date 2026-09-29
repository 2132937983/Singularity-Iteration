// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import com.miophas.singularity_iteration.core.runtime.uu.IndependentUuValueIndex;
import com.miophas.singularity_iteration.core.runtime.uu.UuProcessingFamily;
import com.miophas.singularity_iteration.core.runtime.uu.UuRecipeSolver;

import com.miophas.singularity_iteration.common.recipe.mio_icif_PowderRecipe;
import com.miophas.singularity_iteration.common.recipe.compressor.mio_icif_CompressorRecipe;
import com.miophas.singularity_iteration.common.recipe.extractor.mio_icif_ExtractorRecipe;
import com.miophas.singularity_iteration.common.recipe.metal_former.cutting.mio_icif_CuttingRecipe;
import com.miophas.singularity_iteration.common.recipe.metal_former.extruding.mio_icif_ExtrudingRecipe;
import com.miophas.singularity_iteration.common.recipe.metal_former.rolling.mio_icif_RollingRecipe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Fixed item-only processing adapters. Recipe shape is separate from authority to assign a price.
 * Unknown recipe/ingredient subclasses and container/fluid semantics are not approximated.
 */
public final class UuProcessingRecipes {

    public record Catalog(List<UuRecipeSolver.Rule<IndependentUuValueIndex.Key>> rules,
                          int unsupported, int policyDisabled) {
        public Catalog { rules = List.copyOf(rules); }
    }

    // R93 console rows 871/435 uniquely identify ordinary sand/glass: 29 - 15 = 14 raw.
    // R94 establishes raw / 100000 = buckets. Clay's R93 display identity remains ambiguous.
    public static final double OBSERVED_SAND_SMELTING_OVERHEAD_BUCKETS = 14.0 / 100000.0;
    private static final int MAX_RECIPES = 32768;
    private static final int MAX_ALTERNATIVES = 2000000;
    private static final Map<Class<?>, UuProcessingFamily.Family> CLASSES = Map.of(
            SmeltingRecipe.class, UuProcessingFamily.Family.SMELTING,
            mio_icif_PowderRecipe.class, UuProcessingFamily.Family.POWDER,
            mio_icif_CompressorRecipe.class, UuProcessingFamily.Family.COMPRESSOR,
            mio_icif_ExtractorRecipe.class, UuProcessingFamily.Family.EXTRACTOR,
            mio_icif_RollingRecipe.class, UuProcessingFamily.Family.ROLLING,
            mio_icif_CuttingRecipe.class, UuProcessingFamily.Family.CUTTING,
            mio_icif_ExtrudingRecipe.class, UuProcessingFamily.Family.EXTRUDING);

    private UuProcessingRecipes() { }

    public static Catalog read(RecipeManager manager, HolderLookup.Provider registries) {
        return read(manager.getRecipes(), registries, UuProcessingFamily.PricingPolicy.observedOnly());
    }

    public static Catalog read(RecipeManager manager, HolderLookup.Provider registries, UuProcessingFamily.PricingPolicy policy) {
        return read(manager.getRecipes(), registries, policy);
    }

    /** Collection overload permits a frozen catalog to be inspected without changing the recipe manager. */
    public static Catalog read(Collection<? extends RecipeHolder<?>> recipes,
                               HolderLookup.Provider registries, UuProcessingFamily.PricingPolicy policy) {
        Objects.requireNonNull(registries, "registries");
        Objects.requireNonNull(policy, "policy");
        if (recipes.size() > MAX_RECIPES) throw new IllegalArgumentException("Too many recipes");
        var sorted = recipes.stream().sorted(Comparator.comparing(row -> row.id().toString())).toList();
        List<UuRecipeSolver.Rule<IndependentUuValueIndex.Key>> rules = new ArrayList<>();
        int unsupported = 0;
        int policyDisabled = 0;
        int alternatives = 0;
        for (RecipeHolder<?> holder : sorted) {
            Recipe<?> recipe = holder.value();
            // Crafting has its own adapter and rejection statistics.
            if (recipe instanceof CraftingRecipe) continue;
            UuProcessingFamily.Family family = CLASSES.get(recipe.getClass());
            if (family == null) { unsupported++; continue; }
            int inputCount = inputCount(recipe);
            ItemStack output = recipe.getResultItem(registries).copy();
            if (inputCount < 1 || inputCount > 64 || output.isEmpty() || output.getCount() > 64
                    || output.getCount() > output.getMaxStackSize() || !plainItemSemantics(output)) {
                unsupported++; continue;
            }
            var ingredients = recipe.getIngredients();
            if (ingredients.size() != 1) { unsupported++; continue; }
            List<ItemStack> inputs = simpleInputs(ingredients.getFirst());
            alternatives += inputs.size();
            if (alternatives > MAX_ALTERNATIVES) throw new IllegalArgumentException("Too many processing alternatives");
            if (inputs.isEmpty() || inputs.stream().anyMatch(stack -> inputCount > stack.getMaxStackSize())) {
                unsupported++; continue;
            }

            Double overhead = policy.explicitFamilyOverheads().get(family);
            if (overhead == null && policy.observedSandSmelting() && family == UuProcessingFamily.Family.SMELTING
                    && inputCount == 1 && output.getCount() == 1 && output.is(Items.GLASS)
                    && output.isComponentsPatchEmpty()) {
                // Restrict a tag/alternative recipe to the observed sand route; no red-sand inference.
                inputs = inputs.stream().filter(stack -> stack.is(Items.SAND)).toList();
                if (!inputs.isEmpty()) overhead = OBSERVED_SAND_SMELTING_OVERHEAD_BUCKETS;
            }
            if (overhead == null) { policyDisabled++; continue; }
            var choices = inputs.stream().map(stack -> new UuRecipeSolver.Choice<>(
                    IndependentUuValueIndex.keyOf(stack), (IndependentUuValueIndex.Key) null)).toList();
            var slots = new ArrayList<List<UuRecipeSolver.Choice<IndependentUuValueIndex.Key>>>();
            for (int i = 0; i < inputCount; i++) slots.add(choices);
            rules.add(new UuRecipeSolver.Rule<>(holder.id().toString(), IndependentUuValueIndex.keyOf(output),
                    output.getCount(), slots, overhead));
        }
        return new Catalog(rules, unsupported, policyDisabled);
    }

    private static int inputCount(Recipe<?> recipe) {
        if (recipe instanceof mio_icif_CompressorRecipe compressor) return compressor.getIngredientCount();
        if (recipe instanceof mio_icif_RollingRecipe rolling) return rolling.getIngredientCount();
        if (recipe instanceof mio_icif_CuttingRecipe cutting) return cutting.getIngredientCount();
        if (recipe instanceof mio_icif_ExtrudingRecipe extruding) return extruding.getIngredientCount();
        return 1;
    }

    private static List<ItemStack> simpleInputs(Ingredient ingredient) {
        if (ingredient.getClass() != Ingredient.class || ingredient.isCustom() || ingredient.isEmpty()
                || ingredient.hasNoItems()) return List.of();
        for (Ingredient.Value value : ingredient.getValues()) {
            if (value.getClass() != Ingredient.ItemValue.class && value.getClass() != Ingredient.TagValue.class)
                return List.of();
        }
        ItemStack[] items = ingredient.getItems();
        if (items.length < 1 || items.length > 256) return List.of();
        Map<IndependentUuValueIndex.Key, ItemStack> unique = new LinkedHashMap<>();
        for (ItemStack value : items) {
            // Vanilla Ingredient tests item identity only. Component-bearing examples cannot establish
            // a matching constraint and count-bearing examples cannot establish consumption quantity.
            if (value.isEmpty() || value.getCount() != 1 || !value.isComponentsPatchEmpty()) return List.of();
            ItemStack item = value.copy();
            if (!plainItemSemantics(item)) return List.of();
            unique.putIfAbsent(IndependentUuValueIndex.keyOf(item), item);
        }
        return List.copyOf(unique.values());
    }

    private static boolean plainItemSemantics(ItemStack stack) {
        // Capabilities are queried on defensive copies after mod registration at server recipe load.
        // Reject containers rather than guessing return credits, fluid amounts or nested contents.
        return !(stack.getItem() instanceof BucketItem)
                && stack.getCraftingRemainingItem().isEmpty()
                && !stack.has(DataComponents.CONTAINER)
                && !stack.has(DataComponents.BLOCK_ENTITY_DATA)
                && !stack.has(DataComponents.BUCKET_ENTITY_DATA)
                && stack.getCapability(Capabilities.FluidHandler.ITEM) == null;
    }
}