// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_block_cutter;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_centrifuge_elc;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_extrator_elc;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_neutron_polymerizer;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_powder_elc;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_washer_elc;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.processing.Comminution;
import com.miophas.singularity_iteration.common.processing.Pressing;
import com.miophas.singularity_iteration.common.recipe.blast_furnace.mio_icif_BlastFurnaceRecipe;
import com.miophas.singularity_iteration.common.recipe.block_cutter.mio_icif_BlockCutterRecipe;
import com.miophas.singularity_iteration.common.recipe.canner.canning.CanningRecipe;
import com.miophas.singularity_iteration.common.recipe.canner.mix.MixRecipe;
import com.miophas.singularity_iteration.common.recipe.centrifuge.mio_icif_CentrifugeRecipe;
import com.miophas.singularity_iteration.common.recipe.compressor.mio_icif_CompressorRecipe;
import com.miophas.singularity_iteration.common.recipe.extractor.mio_icif_ExtractorRecipe;
import com.miophas.singularity_iteration.common.recipe.generic.NeutronPolymerizerRecipe;
import com.miophas.singularity_iteration.common.recipe.metal_former.cutting.mio_icif_CuttingRecipe;
import com.miophas.singularity_iteration.common.recipe.metal_former.extruding.mio_icif_ExtrudingRecipe;
import com.miophas.singularity_iteration.common.recipe.metal_former.rolling.mio_icif_RollingRecipe;
import com.miophas.singularity_iteration.common.recipe.mio_icif_PowderRecipe;
import com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipe;
import com.miophas.singularity_iteration.common.recipe.washer.mio_icif_WasherRecipe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 0.1.11.217: what the processing machine pages show, for both recipe viewers. The JEI categories and the native EMI
 * pages take their stacks, counts, times, energy and lines from here, so the two viewers cannot drift apart again.
 * Until 0.1.11.216 the EMI pages of these machines had a generic layout of their own: no input count except for the
 * macerator and the canner's mixing (a player saw "1 Tin Plate" for the Dense Tin Plate, which takes 9), no arrow,
 * no time or energy, no air cells for the blast furnace, no water for the ore washer.
 * <p>Like {@link RecipeViews} it references neither viewer.
 */
public final class MachineRecipeViews {
    private MachineRecipeViews() {}

    /** The items of an ingredient with the recipe's count (at least 1). */
    public static List<ItemStack> withCount(Ingredient ingredient, int count) {
        return Arrays.stream(ingredient.getItems()).map(stack -> stack.copyWithCount(Math.max(1, count))).toList();
    }

    // ---- one item in, one item out

    /**
     * One item in, one item out: the input stacks with the recipe's count (empty for no input slot), the result, the
     * time (0 when it is not fixed: the arrow does not fill) and the power as the machine runs the recipe, the lines
     * under the slots.
     */
    public record ItemMachine(List<ItemStack> inputs, ItemStack result, int ticks, long euPerTick, List<Component> lines) {
        /** With the default line: time and energy. */
        static ItemMachine withCost(List<ItemStack> inputs, ItemStack result, int ticks, long euPerTick) {
            return new ItemMachine(inputs, result, ticks, euPerTick, List.of(RecipeViews.cost(ticks, euPerTick)));
        }
    }

    public static ItemMachine powder(mio_icif_PowderRecipe recipe) {
        ItemStack[] items = recipe.getIngredient().getItems();
        ItemStack input = items.length > 0 ? items[0] : ItemStack.EMPTY;
        int ticks = Comminution.maceratorCost(input, recipe, mio_icif_powder_elc.DEFAULT_ENERGY_PER_TICK, 1.0).ticks();
        return ItemMachine.withCost(withCount(recipe.getIngredient(), recipe.getIngredientCount()), recipe.getResult(),
            ticks, mio_icif_powder_elc.DEFAULT_ENERGY_PER_TICK);
    }

    public static ItemMachine extractor(mio_icif_ExtractorRecipe recipe) {
        var cost = mio_icif_extrator_elc.costOf(recipe);   // 0.1.11.116: as the extractor runs it
        return ItemMachine.withCost(withCount(recipe.getIngredient(), 1), recipe.getResult(), cost.ticks(), cost.power());
    }

    public static ItemMachine compressor(mio_icif_CompressorRecipe recipe) {
        var cost = Pressing.cost(recipe);   // 0.1.11.112: as the compressor runs it
        return ItemMachine.withCost(withCount(recipe.getIngredient(), recipe.getIngredientCount()), recipe.getResult(), cost.ticks(), cost.power());
    }

    public static ItemMachine blockCutter(mio_icif_BlockCutterRecipe recipe) {
        // 0.1.11.115: as the cutter runs it: the recipe's own time and power, else 900 t at the stock power
        int ticks = recipe.processingTimeOverride().orElse((int) mio_icif_block_cutter.MAX_PROGRESS);
        long power = recipe.energyPerTickOverride().isPresent() ? recipe.energyPerTickOverride().getAsInt() : mio_icif_block_cutter.ENERGY_PER_TICK;
        return new ItemMachine(withCount(recipe.getIngredient(), recipe.getIngredientCount()), recipe.getResult(), ticks, power,
            List.of(RecipeViews.cost(ticks, power), Component.translatable("jei.mio_icif.block_cutter.blade", recipe.getHardness())));
    }

    public static ItemMachine rolling(mio_icif_RollingRecipe recipe) {
        return ItemMachine.withCost(withCount(recipe.getIngredient(), recipe.getIngredientCount()), recipe.getResult(),
            (int) recipe.getProcessingTime(), recipe.getEnergyPerTick());
    }

    public static ItemMachine cutting(mio_icif_CuttingRecipe recipe) {
        return ItemMachine.withCost(withCount(recipe.getIngredient(), recipe.getIngredientCount()), recipe.getResult(),
            (int) recipe.getProcessingTime(), recipe.getEnergyPerTick());
    }

    public static ItemMachine extruding(mio_icif_ExtrudingRecipe recipe) {
        return ItemMachine.withCost(withCount(recipe.getIngredient(), recipe.getIngredientCount()), recipe.getResult(),
            (int) recipe.getProcessingTime(), recipe.getEnergyPerTick());
    }

    /** The molecular transformer has no fixed time: it runs as fast as the power comes in; the line is the energy. */
    public static ItemMachine molecularTransformer(mio_icif_MolecularTransformerRecipe recipe) {
        List<ItemStack> inputs = recipe.getIngredients().isEmpty() ? List.of() : List.of(recipe.getIngredients().get(0).getItems());
        return new ItemMachine(inputs, recipe.getResult(), 0, 0, List.of(Component.literal(RecipeViews.number(recipe.getEuCost()) + " EU")));
    }

    /** The total does not fit on the time and energy line. */
    public static ItemMachine neutronPolymerizer(NeutronPolymerizerRecipe recipe) {
        int ticks = mio_icif_neutron_polymerizer.DEFAULT_WORK_TIME;
        long power = mio_icif_neutron_polymerizer.DEFAULT_ENERGY_PER_TICK;
        return new ItemMachine(withCount(recipe.getIngredient(), 1), recipe.getResult(), ticks, power,
            List.of(RecipeViews.rate(ticks, power), RecipeViews.total((long) ticks * power)));
    }

    // ---- ore washer and thermal centrifuge: one input, up to three outputs

    /**
     * One input, up to three outputs. {@code results} always has three entries, an empty stack for an output the recipe
     * does not have, so each output keeps its place. {@code water} is 0 when no water is used.
     */
    public record Separation(Ingredient input, int water, List<ItemStack> results, int ticks, List<Component> lines) {}

    private static List<ItemStack> results(ItemStack primary, int primaryCount, ItemStack secondary, int secondaryCount,
                                           ItemStack tertiary, int tertiaryCount) {
        return List.of(primary.copyWithCount(primaryCount),
            secondaryCount > 0 ? secondary.copyWithCount(secondaryCount) : ItemStack.EMPTY,
            tertiaryCount > 0 ? tertiary.copyWithCount(tertiaryCount) : ItemStack.EMPTY);
    }

    public static Separation washer(mio_icif_WasherRecipe recipe) {
        int ticks = mio_icif_washer_elc.ticksOf(recipe);   // 0.1.11.117: as the machine runs it
        List<Component> lines = new ArrayList<>();
        lines.add(RecipeViews.cost(ticks, mio_icif_washer_elc.powerOf(recipe)));
        if (recipe.getWaterAmount() > 0) {
            lines.add(Component.translatable("jei.mio_icif.washer.water", RecipeViews.number(recipe.getWaterAmount())));
        }
        return new Separation(recipe.getIngredient(), Math.max(0, recipe.getWaterAmount()),
            results(recipe.getPrimaryResult(), recipe.getPrimaryCount(), recipe.getSecondaryResult(), recipe.getSecondaryCount(),
                recipe.getTertiaryResult(), recipe.getTertiaryCount()), ticks, List.copyOf(lines));
    }

    public static Separation centrifuge(mio_icif_CentrifugeRecipe recipe) {
        int ticks = mio_icif_centrifuge_elc.DEFAULT_WORK_TIME;
        return new Separation(recipe.getIngredient(), 0,
            results(recipe.getPrimaryResult(), recipe.getPrimaryCount(), recipe.getSecondaryResult(), recipe.getSecondaryCount(),
                recipe.getTertiaryResult(), recipe.getTertiaryCount()), ticks,
            List.of(Component.translatable("jei.mio_icif.centrifuge.heat", RecipeViews.number(recipe.getMinHeatRequired())),
                RecipeViews.cost(ticks, mio_icif_centrifuge_elc.DEFAULT_ENERGY_PER_TICK)));
    }

    // ---- canner: canning and mixing

    /** A can plus a material into a filled can; {@code material} is empty when the recipe has none. */
    public record Canning(Ingredient can, Ingredient material, ItemStack result, int ticks, List<Component> lines) {}

    public static Canning canning(CanningRecipe recipe) {
        var ingredients = recipe.getIngredients();
        return new Canning(ingredients.size() > 0 ? ingredients.get(0) : Ingredient.EMPTY, ingredients.size() > 1 ? ingredients.get(1) : Ingredient.EMPTY,
            recipe.getResult(), recipe.getProcessingTime(), List.of(RecipeViews.cost(recipe.getProcessingTime(), recipe.getEnergyPerTick())));
    }

    /** A fluid plus a material (with its count) into a fluid and/or a filled cell; empty stacks for what is not there. */
    public record Mixing(FluidStack fluid, List<ItemStack> material, FluidStack resultFluid, ItemStack cell, int ticks, List<Component> lines) {}

    public static Mixing mixing(MixRecipe recipe) {
        return new Mixing(recipe.getInputFluid(), withCount(recipe.getMaterialIngredient(), recipe.getMaterialCount()),
            recipe.getResultFluid(), recipe.getDynamicResultCell(), recipe.getProcessingTime(),
            List.of(RecipeViews.cost(recipe.getProcessingTime(), recipe.getEnergyPerTick())));
    }

    // ---- blast furnace

    /**
     * Ore (with its count) plus the compressed air cells the run takes (empty without air) into steel and slag (empty
     * without slag). {@code usesAir} decides the plus sign.
     */
    public record Blasting(List<ItemStack> ore, boolean usesAir, ItemStack airCells, ItemStack result, ItemStack slag, int ticks, List<Component> lines) {}

    public static Blasting blasting(mio_icif_BlastFurnaceRecipe recipe) {
        long air = (long) recipe.getAirCostPerTick() * recipe.getDuration();
        ItemStack cells = ItemStack.EMPTY;
        if (air > 0) {
            // the furnace fills its air tank from compressed air cells; the fluid itself is nearly invisible
            ItemStack cell = new ItemStack(mio_icif_cells.CELL_AIR.get());
            int perCell = Math.max(1, mio_icif_cells.getCellCapacity(cell));
            cells = cell.copyWithCount((int) Math.min(64, (air + perCell - 1) / perCell));
        }
        boolean usesAir = recipe.getAirCostPerTick() > 0;
        Component line = usesAir
            ? Component.translatable("jei.mio_icif.blast_furnace.air", RecipeViews.seconds(recipe.getDuration()), RecipeViews.number(recipe.getAirCostPerTick()))
            : Component.literal(RecipeViews.seconds(recipe.getDuration()));
        return new Blasting(withCount(recipe.getIngredient(), recipe.getIngredientCount()), usesAir, cells, recipe.getResult(),
            recipe.getSecondaryResult(), recipe.getDuration(), List.of(line));
    }
}
