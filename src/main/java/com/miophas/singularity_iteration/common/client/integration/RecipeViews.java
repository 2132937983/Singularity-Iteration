// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration;

import com.miophas.singularity_iteration.common.blockentity.generator.mio_icif_steam_turbine;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.item.lathe.LatheTurningBlanksItem;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.reactor.ReactorLayoutAnalysis;
import com.miophas.singularity_iteration.common.reactor.ReactorLayoutAnalysis.Verdict;
import com.miophas.singularity_iteration.common.recipe.fluid_refining.FluidRefiningRecipe;
import com.miophas.singularity_iteration.core.api.item.ILatheItem;
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprint;
import com.miophas.singularity_iteration.core.api.reactor.ReactorLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * 0.1.11.209: what the JEI and the EMI recipe views share (the texts under a recipe, the items that hold a fluid,
 * the materials of a structure, the lathe tools, the canner's cell pages, the reactor layout report). It references
 * neither viewer, so each integration can load it whether or not the other viewer is installed.
 */
public final class RecipeViews {
    private RecipeViews() {}

    // ---- texts

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

    // ---- fluids

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

    /** The fluids inside these items (cells, buckets, other fluid containers), each with the amount of the first holder. */
    public static Map<Fluid, Integer> fluidsInside(List<ItemStack> stacks) {
        Map<Fluid, Integer> fluids = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            FluidStack inside = mio_icif_cells.getCellFluid(stack);
            if (inside.isEmpty()) inside = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
            if (!inside.isEmpty()) fluids.putIfAbsent(inside.getFluid(), inside.getAmount());
        }
        return fluids;
    }

    /** The fluids an Oil Refinery recipe takes: its fluid, or the source fluids of its tag. */
    public static List<Fluid> refiningInputs(FluidRefiningRecipe recipe) {
        List<Fluid> fluids = new ArrayList<>();
        if (recipe.getInputFluidTag() != null) {
            for (var holder : BuiltInRegistries.FLUID.getTagOrEmpty(recipe.getInputFluidTag())) {
                Fluid fluid = holder.value();
                if (fluid != Fluids.EMPTY && fluid.isSource(fluid.defaultFluidState())) fluids.add(fluid);
            }
        } else if (recipe.getInputFluid() != Fluids.EMPTY) {
            fluids.add(recipe.getInputFluid());
        }
        return fluids;
    }

    // ---- canner, Tank → Cell and Cell → Tank

    /** An empty cell and {@code fluid} make the {@code filled} cell; Cell → Tank runs it backwards. */
    public record CellFill(ItemStack empty, FluidStack fluid, ItemStack filled) {}

    /** Every source fluid a cell can hold, with the cell the canner fills it into. */
    public static List<CellFill> cellFills() {
        List<CellFill> recipes = new ArrayList<>();
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid == Fluids.EMPTY || !fluid.isSource(fluid.defaultFluidState())) continue;
            ItemStack filled = mio_icif_cells.getFilledCellForFluidStack(fluid);
            FluidStack inside = mio_icif_cells.getCellFluid(filled);
            if (filled.isEmpty() || inside.isEmpty()) continue;
            recipes.add(new CellFill(mio_icif_cells.getEmptyCellForStack(filled), inside, filled));
        }
        return recipes;
    }

    // ---- lathe

    /** The uncut blank of a lathe material. */
    public static ItemStack latheBlank(LatheTurningBlanksItem.LatheMaterial material) {
        return new ItemStack(material == LatheTurningBlanksItem.LatheMaterial.WOOD
            ? mio_icif_normal.WOODEN_TURNING_BLANKS.get() : mio_icif_normal.IRON_TURNING_BLANKS.get());
    }

    /** Every lathe tool that can cut the blank (harder than it). */
    public static List<ItemStack> latheTools(ItemStack blank) {
        int hardness = ((ILatheItem) blank.getItem()).getHardness(blank);
        List<ItemStack> tools = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ILatheItem.ILatheTool tool)) continue;
            ItemStack stack = new ItemStack(item);
            if (tool.getHardness(stack) > hardness) tools.add(stack);
        }
        return tools;
    }

    // ---- structures

    /** One entry per key of the blueprint, in its order: the items of the permitted blocks, with the count of the key. */
    public static List<List<ItemStack>> structureMaterials(StructureBlueprint blueprint) {
        List<List<ItemStack>> out = new ArrayList<>();
        for (Map.Entry<StructureBlueprint.Key, Integer> entry : blueprint.counts().entrySet()) {
            List<ItemStack> stacks = new ArrayList<>();
            for (StructureBlueprint.BlockRef ref : entry.getKey().blocks()) {
                BlockState state = ref.resolve();
                if (state == null) continue;
                ItemStack stack = new ItemStack(state.getBlock().asItem(), entry.getValue());
                if (!stack.isEmpty() && stacks.stream().noneMatch(s -> s.is(stack.getItem()))) stacks.add(stack);
            }
            out.add(stacks);
        }
        return out;
    }

    // ---- reactor layouts

    private static final String LAYOUT = "jei.mio_icif.reactor_layout.";

    /** The report under a reactor layout: EU/t (or steam), heat, the risk, the explosion, the fuel life, the notes. */
    public static List<Component> reactorLayoutLines(ReactorLayoutAnalysis layout) {
        List<Component> out = new ArrayList<>();
        ReactorLayout plan = layout.layout();
        ReactorLayoutAnalysis.Result result = layout.result();
        if (result == null) {
            out.add(Component.translatable(LAYOUT + "unknown", String.join(", ", layout.unknown())).withStyle(ChatFormatting.RED));
        } else {
            var report = result.report();
            // 0.1.10.37: a steam layout shows its steam and the EU/t of the turbines it feeds (2 mB of steam per EU)
            if (plan.steam()) out.add(Component.translatable(LAYOUT + "steam_output", number(report.steamPerCycle()),
                number(report.steamPerCycle() / 20 / mio_icif_steam_turbine.STEAM_PER_EU), plan.chambers()).withStyle(ChatFormatting.DARK_GRAY));
            else out.add(Component.translatable(LAYOUT + "output", number(Math.round(report.euPerTick())), plan.chambers())
                .withStyle(ChatFormatting.DARK_GRAY));
            out.add(Component.translatable(LAYOUT + "heat", number(report.heatPerCycle()), number(report.ventPerCycle()))
                .withStyle(ChatFormatting.DARK_GRAY));
            out.add(reactorRisk(result));
            var blast = result.blast();
            out.add(blast.disabled()
                ? Component.translatable(LAYOUT + "blast_off").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable(LAYOUT + "blast", String.format(Locale.ROOT, "%.1f", blast.power()), blast.radius())
                    .withStyle(result.verdict() == Verdict.STABLE ? ChatFormatting.DARK_GRAY : ChatFormatting.DARK_RED));
            if (result.verdict() == Verdict.STABLE && result.fuelCycles() > 0) {
                out.add(plan.steam()
                    ? Component.translatable(LAYOUT + "steam_fuel", duration(result.fuelCycles()), number(report.steamPerCycle() * result.fuelCycles()))
                        .withStyle(ChatFormatting.DARK_GRAY)
                    : Component.translatable(LAYOUT + "fuel", duration(result.fuelCycles()), number(Math.round(result.euPerLoad())))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        if (plan.noteKey() != null) out.add(Component.translatable(plan.noteKey()).withStyle(ChatFormatting.GRAY));
        out.add(Component.translatable(LAYOUT + "model").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (plan.steam()) out.add(Component.translatable(LAYOUT + "steam_model").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        return out;
    }

    private static Component reactorRisk(ReactorLayoutAnalysis.Result result) {
        return switch (result.verdict()) {
            case STABLE -> Component.translatable(LAYOUT + "stable").withStyle(ChatFormatting.DARK_GREEN);
            case HULL_RISING -> Component.translatable(LAYOUT + "hull_rising",
                String.format(Locale.ROOT, "%.1f", result.report().hullTrend()), duration(result.riskSeconds())).withStyle(ChatFormatting.GOLD);
            case COMPONENTS_MELT -> Component.translatable(LAYOUT + "melts", duration(result.riskSeconds())).withStyle(ChatFormatting.GOLD);
            case MELTDOWN -> result.report().firstMeltCycle() >= 0
                ? Component.translatable(LAYOUT + "meltdown_after_melt", duration(result.report().firstMeltCycle()), duration(result.riskSeconds()))
                    .withStyle(ChatFormatting.RED)
                : Component.translatable(LAYOUT + "meltdown", duration(result.riskSeconds())).withStyle(ChatFormatting.RED);
        };
    }

    /** "45 s", "4 min 40 s", "5 h 33 min". */
    public static String duration(int seconds) {
        if (seconds < 0) return "-";
        if (seconds < 60) return seconds + " s";
        if (seconds < 3600) return seconds % 60 == 0 ? seconds / 60 + " min" : seconds / 60 + " min " + seconds % 60 + " s";
        int minutes = seconds / 60;
        return minutes % 60 == 0 ? minutes / 60 + " h" : minutes / 60 + " h " + minutes % 60 + " min";
    }
}
