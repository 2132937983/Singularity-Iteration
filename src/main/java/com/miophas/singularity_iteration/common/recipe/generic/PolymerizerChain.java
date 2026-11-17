// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.recipe.generic;

import com.miophas.singularity_iteration.common.recipe.mio_icif_SingleItemRecipeInput;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeProcessingInfo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.8: chain polymerization. From an input item the Particle Polymerizer follows its own recipes (iron ingot →
 * niobium-titanium → nano living metal → neutron plate) and can run several steps as one job: one input item in, the
 * chosen product out, no intermediate items. A step joins the chain only when it takes one item and makes one, and the
 * chain stops at a loop or after {@link #MAX_STEPS}. Server (the job) and client (the screen) share this code.
 */
public final class PolymerizerChain {
    private PolymerizerChain() {}

    public static final int MAX_STEPS = 8;

    /** One step: the recipe, what goes in, what comes out, its time in ticks and its draw in EU/t. */
    public record Stage(NeutronPolymerizerRecipe recipe, ItemStack input, ItemStack result, int ticks, int eut, int ingredientCount) {
        public PolymerizerProcess process() { return recipe.getProcess(); }
    }

    /** The recipe for one stack, or null. */
    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Stage step(Level level, ItemStack input) {
        if (level == null || input.isEmpty()) return null;
        var found = level.getRecipeManager().getRecipeFor((net.minecraft.world.item.crafting.RecipeType) NeutronPolymerizerRecipes.NEUTRON_POLYMERIZER_TYPE.get(),
            new mio_icif_SingleItemRecipeInput(input), level);
        if (found.isEmpty()) return null;
        Recipe<?> r = ((RecipeHolder<?>) found.get()).value();
        if (!(r instanceof NeutronPolymerizerRecipe recipe)) return null;
        ItemStack result = recipe.getResult();
        if (result.isEmpty()) return null;
        int count = r instanceof IRecipeProcessingInfo info ? Math.max(1, info.getIngredientCount()) : 1;
        return new Stage(recipe, input.copyWithCount(count), result.copy(), Math.max(1, recipe.getProcessingTime()),
            Math.max(1, recipe.getEnergyPerTick()), count);
    }

    /** Every step reachable from {@code input}, first step first (empty when the input has no recipe). */
    public static List<Stage> chain(Level level, ItemStack input) {
        List<Stage> out = new ArrayList<>();
        Stage first = step(level, input);
        if (first == null) return out;
        out.add(first);
        List<Item> seen = new ArrayList<>();
        seen.add(input.getItem());
        seen.add(first.result().getItem());
        ItemStack cur = first.result();
        while (out.size() < MAX_STEPS && cur.getCount() == 1) {
            Stage next = step(level, cur.copyWithCount(1));
            if (next == null || next.ingredientCount() != 1 || next.result().getCount() != 1 || seen.contains(next.result().getItem())) break;
            out.add(next);
            seen.add(next.result().getItem());
            cur = next.result();
        }
        return out;
    }

    /** The job for {@code input}: the chain up to the step that makes {@code target}, or the first step alone. */
    public static List<Stage> job(Level level, ItemStack input, @Nullable Item target) {
        List<Stage> all = chain(level, input);
        if (all.isEmpty() || target == null) return all.isEmpty() ? all : all.subList(0, 1);
        for (int i = 0; i < all.size(); i++) if (all.get(i).result().is(target)) return all.subList(0, i + 1);
        return all.subList(0, 1);
    }

    /** Products a chain from {@code input} can stop at (steps after the first). */
    public static List<Item> targets(Level level, ItemStack input) {
        List<Item> out = new ArrayList<>();
        List<Stage> all = chain(level, input);
        for (int i = 1; i < all.size(); i++) out.add(all.get(i).result().getItem());
        return out;
    }

    public static int totalTicks(List<Stage> job) {
        int t = 0;
        for (Stage s : job) t += s.ticks();
        return t;
    }
}
