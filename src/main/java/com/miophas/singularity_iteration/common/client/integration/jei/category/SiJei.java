package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The shared look of the SI JEI recipe categories: JEI's own slot backgrounds (18 px inputs, 26 px
 * main output), JEI's arrow animated over the processing time, and gray info lines under the recipe
 * (time, EU/t and total EU, heat, fluid amounts). No machine GUI crops: their battery slots, gauges
 * and upgrade marks have no meaning in a recipe view.
 */
public final class SiJei {
    private SiJei() {}

    public static final int TEXT_COLOR = 0xFF808080;
    public static final int LINE_HEIGHT = 10;
    /** The JEI arrow (24 x 17). */
    public static final int ARROW_W = 24, ARROW_H = 17;

    /** Ticks as seconds: "15 s", "0.5 s". */
    public static String seconds(int ticks) {
        return ticks % 20 == 0 ? ticks / 20 + " s" : String.format(Locale.ROOT, "%.1f s", ticks / 20f);
    }

    /** A number with thousands separators: 2,304,000. */
    public static String number(long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }

    /** "15 s · 2 EU/t · 600 EU"; without energy "15 s". Too long for big machines: use {@link #rate} and {@link #total}. */
    public static Component cost(int ticks, long euPerTick) {
        if (euPerTick <= 0) return Component.literal(seconds(ticks));
        return Component.translatable("jei.mio_icif.cost", seconds(ticks), number(euPerTick), number(euPerTick * ticks));
    }

    /** "75 s · 1,536 EU/t". */
    public static Component rate(int ticks, long euPerTick) {
        return Component.translatable("jei.mio_icif.rate", seconds(ticks), number(euPerTick));
    }

    /** "2,304,000 EU in total". */
    public static Component total(long eu) {
        return Component.translatable("jei.mio_icif.total", number(eu));
    }

    /** An item input slot with JEI's slot background. */
    public static IRecipeSlotBuilder input(IRecipeLayoutBuilder builder, int x, int y) {
        return builder.addSlot(RecipeIngredientRole.INPUT, x, y).setStandardSlotBackground();
    }

    /** A secondary output slot (18 px background). */
    public static IRecipeSlotBuilder output(IRecipeLayoutBuilder builder, int x, int y) {
        return builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).setStandardSlotBackground();
    }

    /** The main output slot (26 px background around the 16 px item at x, y). */
    public static IRecipeSlotBuilder mainOutput(IRecipeLayoutBuilder builder, int x, int y) {
        return builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).setOutputSlotBackground();
    }

    /** A fluid in a slot-sized square, drawn full; the amount is in its tooltip. See {@link #fluids}. */
    public static IRecipeSlotBuilder fluid(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, FluidStack fluid) {
        return fluids(builder, role, x, y, List.of(fluid.getFluid()), fluid.getAmount());
    }

    /**
     * A fluid slot (one of {@code fluids}) in a slot-sized square, drawn full. The items that hold these
     * fluids ({@link #containers}) are invisible ingredients of the same role, so looking up a Biogas Cell
     * finds the recipe that makes biogas.
     */
    public static IRecipeSlotBuilder fluids(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, List<Fluid> fluids, int amount) {
        int shown = Math.max(1, amount);
        IRecipeSlotBuilder slot = builder.addSlot(role, x, y).setStandardSlotBackground();
        List<ItemStack> containers = new ArrayList<>();
        for (Fluid fluid : fluids) {
            slot.addFluidStack(fluid, shown);
            containers.addAll(containers(fluid));
        }
        slot.setFluidRenderer(shown, false, 16, 16);
        if (!containers.isEmpty()) builder.addInvisibleIngredients(role).addItemStacks(containers);
        return slot;
    }

    /** The items that hold {@code fluid}: its own cell, the universal cell filled with it, its bucket. */
    public static List<ItemStack> containers(Fluid fluid) {
        List<ItemStack> containers = new ArrayList<>();
        if (fluid == Fluids.EMPTY) return containers;
        Item cell = mio_icif_cells.getFilledCellForFluid(fluid);
        if (cell != null) containers.add(new ItemStack(cell));
        ItemStack universal = mio_icif_cells.createDynamicFilledCell(fluid, 1000);
        if (!universal.isEmpty()) containers.add(universal);
        Item bucket = fluid.getBucket();
        if (bucket != Items.AIR) containers.add(new ItemStack(bucket));
        return containers;
    }

    /**
     * The fluids inside these items (cells, buckets, other fluid containers) as invisible ingredients of
     * {@code role}, so looking up the fluid finds recipes that make or use its containers.
     */
    public static void contents(IRecipeLayoutBuilder builder, RecipeIngredientRole role, List<ItemStack> stacks) {
        java.util.Map<Fluid, Integer> fluids = new java.util.LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            FluidStack inside = mio_icif_cells.getCellFluid(stack);
            if (inside.isEmpty()) inside = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
            if (!inside.isEmpty()) fluids.putIfAbsent(inside.getFluid(), inside.getAmount());
        }
        if (fluids.isEmpty()) return;
        var acceptor = builder.addInvisibleIngredients(role);
        fluids.forEach(acceptor::addFluidStack);
    }

    /** The items of an ingredient with the recipe's count. */
    public static List<ItemStack> withCount(Ingredient ingredient, int count) {
        return Arrays.stream(ingredient.getItems()).map(stack -> stack.copyWithCount(Math.max(1, count))).toList();
    }

    /** JEI's arrow at (x, y), filling over {@code ticks}; static when there is no time. */
    public static void arrow(IRecipeExtrasBuilder builder, int x, int y, int ticks) {
        if (ticks > 0) builder.addAnimatedRecipeArrow(ticks).setPosition(x, y);
        else builder.addRecipeArrow().setPosition(x, y);
    }

    /** A gray line centred in {@code width} at {@code y}. */
    public static void line(IRecipeExtrasBuilder builder, Component text, int width, int y) {
        builder.addText(text, 0, y, width, LINE_HEIGHT)
            .alignHorizontalCenter()
            .alignVerticalCenter()
            .setColor(TEXT_COLOR)
            .setShadow(false);
    }
}
