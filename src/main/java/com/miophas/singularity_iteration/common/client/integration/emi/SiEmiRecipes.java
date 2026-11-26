// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration.emi;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.blockentity.MachineConversion;
import com.miophas.singularity_iteration.common.blockentity.MachineConversions;
import com.miophas.singularity_iteration.common.blockentity.kuentity.mio_icif_lathe;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_canner_elc;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_fermenter_elc;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_oil_refinery_elc;
import com.miophas.singularity_iteration.common.client.integration.RecipeViews;
import com.miophas.singularity_iteration.common.item.lathe.LatheShapes;
import com.miophas.singularity_iteration.common.item.lathe.LatheTurningBlanksItem;
import com.miophas.singularity_iteration.common.reactor.ReactorLayoutAnalysis;
import com.miophas.singularity_iteration.common.recipe.fermenter.FermenterRecipe;
import com.miophas.singularity_iteration.common.recipe.fermenter.FermenterRecipes;
import com.miophas.singularity_iteration.common.recipe.fluid_refining.FluidRefiningRecipe;
import com.miophas.singularity_iteration.common.recipe.fluid_refining.FluidRefiningRecipes;
import com.miophas.singularity_iteration.common.recipe.generic.NeutronPolymerizerRecipe;
import com.miophas.singularity_iteration.common.recipe.generic.NeutronPolymerizerRecipes;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprint;
import com.miophas.singularity_iteration.core.api.multiblock.StructureBlueprintRegistry;
import com.miophas.singularity_iteration.core.api.reactor.ReactorLayout;
import com.miophas.singularity_iteration.core.api.reactor.ReactorLayoutRegistry;
import com.miophas.singularity_iteration.core.api.reactor.ReactorTransformation;
import com.miophas.singularity_iteration.core.api.reactor.ReactorTransformationRegistry;
import com.miophas.singularity_iteration.core.runtime.diagnostics.QuietFailures;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.TextWidget;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 0.1.11.209: the native EMI pages that only the JEI plugin had. EMI does not call the JEI plugin of a mod that has an
 * EMI plugin (its PluginCallerMixin skips it whole), so with EMI installed (with or without JEI) every page must be
 * here: the Neutron Polymerizer (the only source of the Neutron Plate), the Oil Refinery, the Fermenter, the Lathe,
 * the canner's tank modes, the machines' own conversions, the fuel breeding and decay, the reactor layouts, the
 * multiblock structures, the cable and fusion information pages. Each page has the layout of its JEI twin.
 */
public final class SiEmiRecipes {
    private SiEmiRecipes() {}

    private static final int WIDTH = 150;

    public static final EmiRecipeCategory NEUTRON_POLYMERIZER = new SiEmiCategory("neutron_polymerizer", mio_icif_blocks.NEUTRON_POLYMERIZER.get(), "jei.mio_icif.neutron_polymerizer");
    public static final EmiRecipeCategory FLUID_REFINING = new SiEmiCategory("fluid_refining", mio_icif_blocks.OIL_REFINERY_ELC.get(), "jei.mio_icif.fluid_refining");
    public static final EmiRecipeCategory FERMENTER = new SiEmiCategory("fermenter", mio_icif_blocks.FERMENTER_ELC.get(), "jei.mio_icif.fermenter");
    public static final EmiRecipeCategory LATHE = new SiEmiCategory("lathe", mio_icif_blocks.LATHE.get(), "jei.mio_icif.lathe");
    public static final EmiRecipeCategory FILL_FROM_TANK = new SiEmiCategory("fill_from_tank", mio_icif_blocks.CANNER_ELC.get(), "jei.mio_icif.fill_from_tank");
    public static final EmiRecipeCategory EMPTY_TO_TANK = new SiEmiCategory("empty_to_tank", mio_icif_blocks.CANNER_ELC.get(), "jei.mio_icif.empty_to_tank");
    public static final EmiRecipeCategory REACTOR_TRANSFORMATION = new SiEmiCategory("reactor_transformation",
        item("normal/item_tritium_cell"), Component.translatable("jei.mio_icif.reactor_transformation"));
    public static final EmiRecipeCategory REACTOR_LAYOUT = new SiEmiCategory("reactor_layout",
        item("reactor/item_reactor_uranium_quad"), Component.translatable("jei.mio_icif.category.reactor_layouts"));
    public static final EmiRecipeCategory STRUCTURE = new SiEmiCategory("structure", mio_icif_blocks.LARGE_FABRICATOR_CORE.get(), "jei.mio_icif.category.structures");

    private static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, path)));
    }

    /** A synthetic EMI recipe id (EMI wants a "/" before the path of an id that is not a recipe of the recipe manager). */
    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, "/" + path);
    }

    private static ResourceLocation id(String category, RecipeHolder<?> holder) {
        return id(holder.id().getNamespace(), category + "/" + holder.id().getPath());
    }

    /** Registers the pages, each step on its own: a failing one is logged and the others still register. */
    public static void register(EmiRegistry registry) {
        for (EmiRecipeCategory category : List.of(NEUTRON_POLYMERIZER, FLUID_REFINING, FERMENTER, LATHE, FILL_FROM_TANK,
                EMPTY_TO_TANK, REACTOR_TRANSFORMATION, REACTOR_LAYOUT, STRUCTURE)) {
            registry.addCategory(category);
        }
        registry.addWorkstation(NEUTRON_POLYMERIZER, EmiStack.of(mio_icif_blocks.NEUTRON_POLYMERIZER.get()));
        registry.addWorkstation(FLUID_REFINING, EmiStack.of(mio_icif_blocks.OIL_REFINERY_ELC.get()));
        registry.addWorkstation(FERMENTER, EmiStack.of(mio_icif_blocks.FERMENTER_ELC.get()));
        registry.addWorkstation(LATHE, EmiStack.of(mio_icif_blocks.LATHE.get()));
        registry.addWorkstation(FILL_FROM_TANK, EmiStack.of(mio_icif_blocks.CANNER_ELC.get()));
        registry.addWorkstation(EMPTY_TO_TANK, EmiStack.of(mio_icif_blocks.CANNER_ELC.get()));
        // the reactors run the layouts (a chamber adds one column) and both reactors the component changes
        registry.addWorkstation(REACTOR_LAYOUT, EmiStack.of(mio_icif_blocks.NUCLEAR_REACTOR_GENERATOR.get()));
        registry.addWorkstation(REACTOR_LAYOUT, EmiStack.of(mio_icif_blocks.REACTOR_CHAMBER.get()));
        registry.addWorkstation(REACTOR_LAYOUT, EmiStack.of(mio_icif_blocks.STEAM_REACTOR.get()));
        registry.addWorkstation(REACTOR_LAYOUT, EmiStack.of(mio_icif_blocks.STEAM_REACTOR_CHAMBER.get()));
        registry.addWorkstation(REACTOR_TRANSFORMATION, EmiStack.of(mio_icif_blocks.NUCLEAR_REACTOR_GENERATOR.get()));
        registry.addWorkstation(REACTOR_TRANSFORMATION, EmiStack.of(mio_icif_blocks.STEAM_REACTOR.get()));

        RecipeManager recipes = registry.getRecipeManager();
        if (recipes != null) {
            guarded("neutronPolymerizer", () -> recipes.getAllRecipesFor(NeutronPolymerizerRecipes.NEUTRON_POLYMERIZER_TYPE.get())
                .forEach(h -> registry.addRecipe(neutronPolymerizer(h))));
            guarded("fluidRefining", () -> recipes.getAllRecipesFor(FluidRefiningRecipes.FLUID_REFINING_TYPE.get())
                .forEach(h -> registry.addRecipe(fluidRefining(h))));
            guarded("fermenter", () -> recipes.getAllRecipesFor(FermenterRecipes.FERMENTER_TYPE.get())
                .forEach(h -> registry.addRecipe(fermenter(h))));
        }
        guarded("lathe", () -> LatheShapes.all().forEach(shape -> registry.addRecipe(lathe(shape))));
        guarded("cannerTank", () -> {
            for (RecipeViews.CellFill fill : RecipeViews.cellFills()) {
                registry.addRecipe(cannerTank(fill, false));
                registry.addRecipe(cannerTank(fill, true));
            }
        });
        guarded("conversions", () -> conversions(registry, recipes));
        guarded("reactorTransformations", () -> {
            for (ReactorTransformation change : ReactorTransformationRegistry.available()) registry.addRecipe(reactorTransformation(change));
        });
        guarded("reactorLayouts", () -> {
            for (ReactorLayout layout : ReactorLayoutRegistry.all()) registry.addRecipe(reactorLayout(ReactorLayoutAnalysis.of(layout)));
        });
        guarded("structures", () -> {
            for (StructureBlueprint blueprint : StructureBlueprintRegistry.all()) registry.addRecipe(structure(blueprint));
        });
        guarded("info", () -> info(registry));
    }

    private static void guarded(String name, Runnable step) {
        try {
            step.run();
        } catch (RuntimeException | LinkageError e) {
            QuietFailures.report("emi-" + name, "EMI registration step " + name, e);
        }
    }

    // ---- one item in, one item out (the JEI SiItemMachineCategory layout, built by SiEmiMachines)

    private static SiEmiRecipe neutronPolymerizer(RecipeHolder<NeutronPolymerizerRecipe> holder) {
        // 0.1.11.217: the shared one-in-one-out page (SiEmiMachines) with the shared data
        return SiEmiMachines.itemMachine(NEUTRON_POLYMERIZER, id("neutron_polymerizer", holder),
            com.miophas.singularity_iteration.common.client.integration.MachineRecipeViews.neutronPolymerizer(holder.value()));
    }

    // ---- Oil Refinery: [item][fluid] ==> [fluid], 1:1 by volume per cycle

    private static SiEmiRecipe fluidRefining(RecipeHolder<FluidRefiningRecipe> holder) {
        FluidRefiningRecipe recipe = holder.value();
        FluidStack out = recipe.getOutputFluid();
        int amount = Math.max(1, out.getAmount());
        int ticks = mio_icif_oil_refinery_elc.DEFAULT_WORK_TIME;
        boolean hasItem = !recipe.getInputIngredient().isEmpty();
        EmiIngredient item = hasItem ? EmiIngredient.of(recipe.getInputIngredient()) : EmiStack.EMPTY;
        List<Fluid> inFluids = RecipeViews.refiningInputs(recipe);
        EmiIngredient fluid = inFluids.isEmpty() ? EmiStack.EMPTY : SiEmi.fluids(inFluids, amount);
        EmiStack output = SiEmi.fluid(out);
        List<EmiIngredient> inputs = new ArrayList<>();
        if (hasItem) inputs.add(item);
        if (!inFluids.isEmpty()) inputs.add(fluid);
        inputs.addAll(SiEmi.containers(inFluids));
        List<EmiStack> outputs = new ArrayList<>(List.of(output));
        outputs.addAll(SiEmi.containers(List.of(out.getFluid())));
        return new SiEmiRecipe(FLUID_REFINING, id("fluid_refining", holder), WIDTH, 41, inputs, outputs, (w, r) -> {
            if (hasItem) SiEmi.input(w, item, 31, 6);
            if (!inFluids.isEmpty()) SiEmi.input(w, fluid, 49, 6);
            SiEmi.arrow(w, 73, 6, ticks);
            SiEmi.output(w, r, output, 103, 6);
            SiEmi.line(w, Component.translatable("jei.mio_icif.fluid_refining.cycle", RecipeViews.seconds(ticks),
                RecipeViews.number(mio_icif_oil_refinery_elc.DEFAULT_ENERGY_PER_TICK)), WIDTH, 30);
        }).noTree();
    }

    // ---- Fermenter: [fluid] ==> [fluid][by-product]

    private static SiEmiRecipe fermenter(RecipeHolder<FermenterRecipe> holder) {
        FermenterRecipe recipe = holder.value();
        List<Fluid> inFluids = Arrays.stream(recipe.getInput().getFluids()).map(FluidStack::getFluid).distinct().toList();
        EmiIngredient input = SiEmi.fluids(inFluids, recipe.getInput().amount());
        EmiStack output = SiEmi.fluid(recipe.getOutput());
        ItemStack byproduct = recipe.getByproduct();
        int ticks = Math.max(1, (recipe.getHeat() + mio_icif_fermenter_elc.MAX_HEAT_RECEIVE - 1) / mio_icif_fermenter_elc.MAX_HEAT_RECEIVE);
        List<EmiIngredient> inputs = new ArrayList<>(List.of(input));
        inputs.addAll(SiEmi.containers(inFluids));
        List<EmiStack> outputs = new ArrayList<>(List.of(output));
        if (!byproduct.isEmpty()) outputs.add(EmiStack.of(byproduct));
        outputs.addAll(SiEmi.containers(List.of(recipe.getOutput().getFluid())));
        return new SiEmiRecipe(FERMENTER, id("fermenter", holder), WIDTH, 51, inputs, outputs, (w, r) -> {
            SiEmi.input(w, input, 31, 6);
            SiEmi.arrow(w, 55, 6, ticks);
            SiEmi.output(w, r, output, 85, 6);
            if (!byproduct.isEmpty()) SiEmi.output(w, r, EmiStack.of(byproduct), 103, 6);
            SiEmi.line(w, Component.translatable("jei.mio_icif.fermenter.heat", RecipeViews.number(recipe.getHeat())), WIDTH, 30);
            if (!byproduct.isEmpty()) SiEmi.line(w, Component.translatable("jei.mio_icif.fermenter.byproduct", byproduct.getCount(),
                byproduct.getHoverName(), RecipeViews.number(recipe.getByproductInterval())), WIDTH, 30 + SiEmi.LINE_HEIGHT);
        }).noTree();
    }

    // ---- Lathe: [blank] + [tool] ==> [part]

    private static SiEmiRecipe lathe(LatheShapes.Shape shape) {
        ItemStack blankStack = RecipeViews.latheBlank(shape.material());
        EmiStack blank = EmiStack.of(blankStack);
        EmiIngredient tools = SiEmi.items(RecipeViews.latheTools(blankStack));
        EmiStack part = EmiStack.of(LatheTurningBlanksItem.getStackForState(blankStack, shape.state()));
        int cuts = 0;
        for (int s : shape.state()) cuts += shape.material().width - s;
        int totalCuts = cuts;
        ResourceLocation id = shape.id() != null ? id(shape.id().getNamespace(), "lathe/" + shape.id().getPath())
            : id(Singularity_Iteration.MOD_ID, "lathe/" + Integer.toHexString(LatheTurningBlanksItem.getNumericState(shape.state())));
        // the tool is not used up: a catalyst-like input, kept out of the recipe tree's cost
        return new SiEmiRecipe(LATHE, id, WIDTH, 65, List.of(blank, tools), List.of(part), (w, r) -> {
            SiEmi.input(w, blank, 17, 9);
            SiEmi.plus(w, 38, 11);
            SiEmi.input(w, tools, 56, 9).catalyst(true);
            SiEmi.arrow(w, 79, 9, 0);
            SiEmi.mainOutput(w, r, part, 113, 9);
            SiEmi.line(w, Component.translatable("jei.mio_icif.lathe.profile", shape.profile()), WIDTH, 34);
            SiEmi.line(w, Component.translatable("jei.mio_icif.lathe.cost", totalCuts,
                RecipeViews.number((long) totalCuts * mio_icif_lathe.KU_COST_PER_OPERATION)), WIDTH, 44);
            SiEmi.line(w, Component.translatable("jei.mio_icif.lathe.part", shape.usedInItem().getDescription()), WIDTH, 54);
        }).noTree();
    }

    // ---- Canner, Tank → Cell ([fluid] + [cell] ==> [filled]) and Cell → Tank ([filled] ==> [fluid][cell])

    private static SiEmiRecipe cannerTank(RecipeViews.CellFill fill, boolean toTank) {
        EmiStack fluid = SiEmi.fluid(fill.fluid());
        EmiStack empty = EmiStack.of(fill.empty());
        EmiStack filled = EmiStack.of(fill.filled());
        int ticks = mio_icif_canner_elc.DEFAULT_WORK_TIME;
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fill.fluid().getFluid());
        String path = (toTank ? "empty_to_tank/" : "fill_from_tank/") + fluidId.getNamespace() + "/" + fluidId.getPath();
        Component cost = RecipeViews.cost(ticks, mio_icif_canner_elc.DEFAULT_ENERGY_PER_TICK);
        // plain fluid slots: the cell is already a visible ingredient here (see the JEI page)
        if (toTank) return new SiEmiRecipe(EMPTY_TO_TANK, id(Singularity_Iteration.MOD_ID, path), WIDTH, 41,
            List.of(filled), List.of(fluid, empty), (w, r) -> {
                SiEmi.input(w, filled, 31, 6);
                SiEmi.arrow(w, 55, 6, ticks);
                SiEmi.output(w, r, fluid, 85, 6);
                SiEmi.output(w, r, empty, 103, 6);
                SiEmi.line(w, cost, WIDTH, 30);
            });
        return new SiEmiRecipe(FILL_FROM_TANK, id(Singularity_Iteration.MOD_ID, path), WIDTH, 41,
            List.of(fluid, empty), List.of(filled), (w, r) -> {
                SiEmi.input(w, fluid, 13, 6);
                SiEmi.plus(w, 34, 8);
                SiEmi.input(w, empty, 52, 6);
                SiEmi.arrow(w, 75, 6, ticks);
                SiEmi.output(w, r, filled, 105, 6);
                SiEmi.line(w, cost, WIDTH, 30);
            });
    }

    // ---- the machines' own conversions: one category per machine, named after it

    private static final int CONV_WIDTH = 160, CONV_ROW = 6, CONV_LINES = 30, PITCH = 18, TO_ARROW = 24, TO_OUTPUT = 30;

    private static void conversions(EmiRegistry registry, RecipeManager recipes) {
        Map<ItemLike, List<MachineConversion>> byMachine = new LinkedHashMap<>();
        for (MachineConversion conversion : MachineConversions.all(recipes)) byMachine.computeIfAbsent(conversion.machine(), m -> new ArrayList<>()).add(conversion);
        byMachine.forEach((machine, list) -> {
            String path = BuiltInRegistries.ITEM.getKey(machine.asItem()).getPath();
            ItemStack icon = new ItemStack(machine);
            EmiRecipeCategory category = new SiEmiCategory("conversion/" + path, icon, icon.getHoverName());
            registry.addCategory(category);
            Set<ItemLike> blocks = new LinkedHashSet<>();
            for (MachineConversion conversion : list) blocks.addAll(conversion.machines());
            for (ItemLike block : blocks) registry.addWorkstation(category, EmiStack.of(block));
            for (int i = 0; i < list.size(); i++)
                registry.addRecipe(conversion(category, list.get(i), id(Singularity_Iteration.MOD_ID, "conversion/" + path + "/" + i)));
        });
    }

    private static SiEmiRecipe conversion(EmiRecipeCategory category, MachineConversion conversion, ResourceLocation id) {
        int inputCount = Math.max(1, conversion.fluidInputs().size() + conversion.itemInputs().size());
        int outputCount = Math.max(1, conversion.fluidOutputs().size() + conversion.itemOutputs().size());
        int span = (inputCount - 1) * PITCH + TO_ARROW + TO_OUTPUT + (outputCount - 1) * PITCH + 16;
        int firstX = (CONV_WIDTH - span) / 2, arrowX = firstX + (inputCount - 1) * PITCH + TO_ARROW;

        List<EmiIngredient> fluidIns = new ArrayList<>(), itemIns = new ArrayList<>();
        List<Fluid> allInFluids = new ArrayList<>();
        for (List<FluidStack> fluids : conversion.fluidInputs()) {
            List<Fluid> kinds = fluids.stream().map(FluidStack::getFluid).toList();
            allInFluids.addAll(kinds);
            fluidIns.add(SiEmi.fluids(kinds, fluids.get(0).getAmount()));
        }
        for (List<ItemStack> items : conversion.itemInputs()) itemIns.add(SiEmi.items(items));
        List<EmiStack> fluidOuts = conversion.fluidOutputs().stream().map(SiEmi::fluid).toList();
        List<EmiStack> itemOuts = conversion.itemOutputs().stream().map(EmiStack::of).toList();

        List<EmiIngredient> inputs = new ArrayList<>(fluidIns);
        inputs.addAll(itemIns);
        inputs.addAll(SiEmi.containers(allInFluids));
        List<EmiStack> outputs = new ArrayList<>(fluidOuts);
        outputs.addAll(itemOuts);
        outputs.addAll(SiEmi.containers(conversion.fluidOutputs().stream().map(FluidStack::getFluid).toList()));
        int height = CONV_LINES + Math.max(1, conversion.notes().size()) * SiEmi.LINE_HEIGHT + 1;
        return new SiEmiRecipe(category, id, CONV_WIDTH, height, inputs, outputs, (w, r) -> {
            int x = firstX;
            // without inputs the machine itself stands on the left
            if (fluidIns.isEmpty() && itemIns.isEmpty()) w.addSlot(EmiStack.of(conversion.machine()), x - 1, CONV_ROW - 1).drawBack(false);
            for (EmiIngredient in : fluidIns) { SiEmi.input(w, in, x, CONV_ROW); x += PITCH; }
            for (EmiIngredient in : itemIns) { SiEmi.input(w, in, x, CONV_ROW); x += PITCH; }
            SiEmi.arrow(w, arrowX, CONV_ROW, conversion.ticks());
            x = arrowX + TO_OUTPUT;
            for (EmiStack out : fluidOuts) { SiEmi.output(w, r, out, x, CONV_ROW); x += PITCH; }
            for (EmiStack out : itemOuts) { SiEmi.output(w, r, out, x, CONV_ROW); x += PITCH; }
            int y = CONV_LINES;
            for (Component note : conversion.notes()) { SiEmi.line(w, note, CONV_WIDTH, y); y += SiEmi.LINE_HEIGHT; }
        }).noTree();
    }

    // ---- Nuclear Fuel Breeding & Decay: [component] ==> [component]

    private static SiEmiRecipe reactorTransformation(ReactorTransformation change) {
        EmiStack input = EmiStack.of(BuiltInRegistries.ITEM.get(change.input()));
        EmiStack output = EmiStack.of(BuiltInRegistries.ITEM.get(change.output()));
        String key = "jei.mio_icif.reactor_transformation." + change.process().name().toLowerCase(Locale.ROOT);
        Component process = change.amount() > 0 ? Component.translatable(key, String.format(Locale.ROOT, "%,d", change.amount()))
            : Component.translatable(key + ".short");
        int height = 30 + (1 + change.notes().size()) * SiEmi.LINE_HEIGHT + 1;
        return new SiEmiRecipe(REACTOR_TRANSFORMATION, id(change.id().getNamespace(), "reactor_transformation/" + change.id().getPath()),
            176, height, List.of(input), List.of(output), (w, r) -> {
                SiEmi.input(w, input, 48, 6);
                SiEmi.arrow(w, 72, 6, 0);
                SiEmi.output(w, r, output, 112, 6);
                int y = 30;
                SiEmi.line(w, process, 176, y);
                for (Component note : change.notes()) { y += SiEmi.LINE_HEIGHT; SiEmi.line(w, note, 176, y); }
            });
    }

    // ---- Reactor Layouts: the component grid and the report of the reactor model

    private static final int PAGE_WIDTH = 168, PAGE_HEIGHT = 190, TITLE_COLOR = 0xFF404040;

    private static SiEmiRecipe reactorLayout(ReactorLayoutAnalysis layout) {
        ReactorLayout plan = layout.layout();
        int columns = layout.columns(), x0 = (PAGE_WIDTH - columns * 18) / 2, gridY = 11, linesY = gridY + 6 * 18 + 3;
        Map<ResourceLocation, EmiStack> components = new LinkedHashMap<>();
        for (int row = 0; row < ReactorLayout.MAX_ROWS; row++) {
            for (int column = 0; column < columns; column++) {
                ResourceLocation item = layout.item(row * 9 + column);
                if (item != null) components.computeIfAbsent(item, k -> EmiStack.of(BuiltInRegistries.ITEM.get(k)));
            }
        }
        return new SiEmiRecipe(REACTOR_LAYOUT, id(plan.id().getNamespace(), "reactor_layout/" + plan.id().getPath()),
            PAGE_WIDTH, PAGE_HEIGHT, List.copyOf(components.values()), List.of(), (w, r) -> {
                w.addText(Component.translatable(plan.effectiveTitleKey()), PAGE_WIDTH / 2, 1, TITLE_COLOR, false)
                    .horizontalAlign(TextWidget.Alignment.CENTER);
                for (int row = 0; row < ReactorLayout.MAX_ROWS; row++) {
                    for (int column = 0; column < columns; column++) {
                        ResourceLocation item = layout.item(row * 9 + column);
                        w.addSlot(item == null ? EmiStack.EMPTY : components.get(item), x0 + column * 18, gridY + row * 18);
                    }
                }
                // the model runs when the text is first drawn (see TextBlockWidget)
                w.add(new TextBlockWidget(0, linesY, PAGE_WIDTH, PAGE_HEIGHT - linesY, () -> RecipeViews.reactorLayoutLines(layout)));
            }).noTree();
    }

    // ---- Multiblock Structures: the 3D view, the materials with their counts, the notes

    private static final int VIEW_Y = 11, VIEW_H = 112, MATERIALS_Y = 125, PER_ROW = 9;

    private static SiEmiRecipe structure(StructureBlueprint blueprint) {
        List<List<ItemStack>> materials = RecipeViews.structureMaterials(blueprint);
        List<StructureBlueprint.Key> keys = new ArrayList<>(blueprint.counts().keySet());
        List<EmiIngredient> slots = new ArrayList<>();
        for (List<ItemStack> stacks : materials) slots.add(SiEmi.items(stacks));
        List<EmiIngredient> inputs = slots.stream().filter(s -> !s.isEmpty()).toList();
        int rows = Math.max(1, (materials.size() + PER_ROW - 1) / PER_ROW);
        int notesY = MATERIALS_Y + rows * 18 + 2;
        return new SiEmiRecipe(STRUCTURE, id(blueprint.id().getNamespace(), "structure/" + blueprint.id().getPath()),
            PAGE_WIDTH, PAGE_HEIGHT, inputs, List.of(), (w, r) -> {
                w.addText(Component.translatable(blueprint.titleKey()), PAGE_WIDTH / 2, 1, TITLE_COLOR, false)
                    .horizontalAlign(TextWidget.Alignment.CENTER);
                w.add(new StructurePreviewWidget(blueprint, 0, VIEW_Y, PAGE_WIDTH, VIEW_H));
                for (int i = 0; i < slots.size(); i++) {
                    if (slots.get(i).isEmpty()) continue;
                    int inRow = Math.min(PER_ROW, slots.size() - i / PER_ROW * PER_ROW);
                    int x = (PAGE_WIDTH - inRow * 18) / 2 + (i % PER_ROW) * 18, y = MATERIALS_Y + (i / PER_ROW) * 18;
                    var slot = w.addSlot(slots.get(i), x, y);
                    String note = keys.get(i).noteKey();
                    if (note != null) slot.appendTooltip(Component.translatable(note).withStyle(ChatFormatting.GRAY));
                }
                List<Component> notes = blueprint.noteKeys().stream().<Component>map(Component::translatable).toList();
                if (!notes.isEmpty() && PAGE_HEIGHT - notesY >= SiEmi.LINE_HEIGHT)
                    w.add(new TextBlockWidget(0, notesY, PAGE_WIDTH, PAGE_HEIGHT - notesY, () -> notes));
            }).noTree();
    }

    // ---- information pages: every cable, and the fusion reactor

    private static void info(EmiRegistry registry) {
        List<EmiIngredient> cables = new ArrayList<>();
        for (var block : BuiltInRegistries.BLOCK)
            if (block instanceof com.miophas.singularity_iteration.common.block.wire.mio_icif_block_wire && block.asItem() != net.minecraft.world.item.Items.AIR)
                cables.add(EmiStack.of(block));
        if (!cables.isEmpty()) registry.addRecipe(new EmiInfoRecipe(cables, List.of(
            Component.translatable("jei.mio_icif.cable_info.rating"),
            Component.translatable("jei.mio_icif.cable_info.loss"),
            Component.translatable("jei.mio_icif.cable_info.shock"),
            Component.translatable("jei.mio_icif.cable_info.colour"),
            Component.translatable("jei.mio_icif.cable_info.chunks")), id(Singularity_Iteration.MOD_ID, "info/cables")));
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(mio_icif_blocks.FUSION_CONTROLLER.get())), List.of(
            Component.translatable("jei.mio_icif.fusion_info.1"),
            Component.translatable("jei.mio_icif.fusion_info.2"),
            Component.translatable("jei.mio_icif.fusion_info.3"),
            Component.translatable("jei.mio_icif.fusion_info.4"),
            Component.translatable("jei.mio_icif.fusion_info.5")), id(Singularity_Iteration.MOD_ID, "info/fusion_controller")));
    }
}
