// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration.emi;

import com.miophas.singularity_iteration.common.client.integration.RecipeViews;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 0.1.11.209: the look of the native EMI categories, the same as the JEI ones ({@code jei.category.SiJei}): the
 * positions are those of the JEI layouts (where the 16 px stack is drawn), so both viewers show a recipe alike.
 * 18 px slots for inputs and by-products, a 26 px slot for the main output, the arrow filling over the processing
 * time, gray lines centred under the recipe.
 */
public final class SiEmi {
    private SiEmi() {}

    public static final int TEXT_COLOR = 0xFF808080;
    public static final int LINE_HEIGHT = 10;

    /** An input slot whose stack is drawn at {@code x, y}. */
    public static SlotWidget input(WidgetHolder widgets, EmiIngredient ingredient, int x, int y) {
        return widgets.addSlot(ingredient, x - 1, y - 1);
    }

    /** A secondary output slot (18 px) whose stack is drawn at {@code x, y}. */
    public static SlotWidget output(WidgetHolder widgets, EmiRecipe recipe, EmiIngredient stack, int x, int y) {
        return widgets.addSlot(stack, x - 1, y - 1).recipeContext(recipe);
    }

    /** The main output slot (26 px) whose stack is drawn at {@code x, y}. */
    public static SlotWidget mainOutput(WidgetHolder widgets, EmiRecipe recipe, EmiIngredient stack, int x, int y) {
        return widgets.addSlot(stack, x - 5, y - 5).large(true).recipeContext(recipe);
    }

    /** The arrow at (x, y), filling over {@code ticks}; empty when there is no fixed time. */
    public static void arrow(WidgetHolder widgets, int x, int y, int ticks) {
        if (ticks > 0) widgets.addFillingArrow(x, y, ticks * 50);
        else widgets.addTexture(EmiTexture.EMPTY_ARROW, x, y);
    }

    /** The plus sign at (x, y). */
    public static void plus(WidgetHolder widgets, int x, int y) {
        widgets.addTexture(EmiTexture.PLUS, x, y);
    }

    /** A gray line centred in {@code width}, in the 10 px row at {@code y}. */
    public static void line(WidgetHolder widgets, Component text, int width, int y) {
        widgets.addText(text, width / 2, y + 1, TEXT_COLOR, false).horizontalAlign(TextWidget.Alignment.CENTER);
    }

    // ---- stacks

    public static EmiIngredient items(List<ItemStack> stacks) {
        return EmiIngredient.of(stacks.stream().filter(s -> !s.isEmpty()).map(EmiStack::of).toList());
    }

    public static EmiStack fluid(FluidStack fluid) {
        return EmiStack.of(fluid.getFluid(), Math.max(1, fluid.getAmount()));
    }

    /** One of {@code fluids}, {@code amount} mB. */
    public static EmiIngredient fluids(List<Fluid> fluids, int amount) {
        long shown = Math.max(1, amount);
        return EmiIngredient.of(fluids.stream().map(f -> EmiStack.of(f, shown)).toList(), shown);
    }

    /** The items that hold these fluids (cells, buckets), for lookups only. */
    public static List<EmiStack> containers(List<Fluid> fluids) {
        List<EmiStack> out = new ArrayList<>();
        for (Fluid fluid : fluids) for (ItemStack stack : RecipeViews.containers(fluid)) out.add(EmiStack.of(stack));
        return out;
    }

    /** The fluids inside these items (cells, buckets), for lookups only. */
    public static List<EmiStack> contents(List<ItemStack> stacks) {
        List<EmiStack> out = new ArrayList<>();
        RecipeViews.fluidsInside(stacks).forEach((fluid, amount) -> out.add(EmiStack.of(fluid, Math.max(1, amount))));
        return out;
    }
}
