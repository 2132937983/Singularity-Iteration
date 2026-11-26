// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration.emi;

import com.miophas.singularity_iteration.common.client.integration.MachineRecipeViews;
import com.miophas.singularity_iteration.common.recipe.block_cutter.mio_icif_BlockCutterRecipe;
import com.miophas.singularity_iteration.common.recipe.mio_icif_ModRecipes;
import com.miophas.singularity_iteration.core.runtime.diagnostics.QuietFailures;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluids;

/**
 * 0.1.11.217: the native EMI pages of the processing machines (macerator, ore washer, thermal centrifuge, extractor,
 * compressor, block cutter, the three metal former modes, canning, mixing, blast furnace, molecular transformer). Each
 * has the layout of its JEI twin and the data of {@link MachineRecipeViews}, as the JEI category. Until 0.1.11.216
 * they shared one generic page (MioIcifEmiRecipe): input counts only for the macerator and mixing, so EMI showed one
 * Tin Plate for the Dense Tin Plate (it takes 9), and no arrow, time, energy, air cells or water.
 */
public final class SiEmiMachines {
    private SiEmiMachines() {}

    private static final int WIDTH = 150;

    /** The id of a machine page: "/emi/" + the recipe's path, as before 0.1.11.217 (EMI favourites keep pointing at it). */
    static ResourceLocation id(RecipeHolder<?> holder) {
        return ResourceLocation.fromNamespaceAndPath(holder.id().getNamespace(), "/emi/" + holder.id().getPath());
    }

    /** Registers the machine pages; a recipe whose page fails is logged and skipped, the others still register. */
    public static void register(EmiRegistry registry, RecipeManager recipes) {
        each(registry, "powder", recipes.getAllRecipesFor(mio_icif_ModRecipes.POWDER_TYPE.get()),
            h -> itemMachine(MioIcifEmiPlugin.POWDER, id(h), MachineRecipeViews.powder(h.value())));
        each(registry, "washer", recipes.getAllRecipesFor(mio_icif_ModRecipes.WASHER_TYPE.get()),
            h -> separation(MioIcifEmiPlugin.WASHER, h, MachineRecipeViews.washer(h.value()), 13, 31, 55, 85));
        each(registry, "centrifuge", recipes.getAllRecipesFor(mio_icif_ModRecipes.CENTRIFUGE_TYPE.get()),
            h -> separation(MioIcifEmiPlugin.CENTRIFUGE, h, MachineRecipeViews.centrifuge(h.value()), 20, 0, 44, 76));
        each(registry, "extractor", recipes.getAllRecipesFor(mio_icif_ModRecipes.EXTRACTOR_TYPE.get()),
            h -> itemMachine(MioIcifEmiPlugin.EXTRACTOR, id(h), MachineRecipeViews.extractor(h.value())));
        each(registry, "compressor", recipes.getAllRecipesFor(mio_icif_ModRecipes.COMPRESSOR_TYPE.get()),
            h -> itemMachine(MioIcifEmiPlugin.COMPRESSOR, id(h), MachineRecipeViews.compressor(h.value())));
        // 0.1.10.29: the wood recipe is shown once per planks recipe
        var level = net.minecraft.client.Minecraft.getInstance().level;
        each(registry, "blockCutter", mio_icif_BlockCutterRecipe.displays(recipes, level == null ? null : level.registryAccess(),
                recipes.getAllRecipesFor(mio_icif_ModRecipes.BLOCK_CUTTER_TYPE.get())),
            h -> itemMachine(MioIcifEmiPlugin.BLOCK_CUTTER, id(h), MachineRecipeViews.blockCutter(h.value())));
        each(registry, "rolling", recipes.getAllRecipesFor(mio_icif_ModRecipes.ROLLING_TYPE.get()),
            h -> itemMachine(MioIcifEmiPlugin.ROLLING, id(h), MachineRecipeViews.rolling(h.value())));
        each(registry, "cutting", recipes.getAllRecipesFor(mio_icif_ModRecipes.CUTTING_TYPE.get()),
            h -> itemMachine(MioIcifEmiPlugin.CUTTING, id(h), MachineRecipeViews.cutting(h.value())));
        each(registry, "extruding", recipes.getAllRecipesFor(mio_icif_ModRecipes.EXTRUDING_TYPE.get()),
            h -> itemMachine(MioIcifEmiPlugin.EXTRUDING, id(h), MachineRecipeViews.extruding(h.value())));
        each(registry, "canning", recipes.getAllRecipesFor(mio_icif_ModRecipes.CANNING_TYPE.get()),
            h -> canning(MioIcifEmiPlugin.CANNING, h, MachineRecipeViews.canning(h.value())));
        each(registry, "mix", recipes.getAllRecipesFor(mio_icif_ModRecipes.MIX_TYPE.get()),
            h -> mixing(MioIcifEmiPlugin.MIX, h, MachineRecipeViews.mixing(h.value())));
        each(registry, "blastFurnace", recipes.getAllRecipesFor(mio_icif_ModRecipes.BLAST_FURNACE_TYPE.get()),
            h -> blasting(MioIcifEmiPlugin.BLAST_FURNACE, h, MachineRecipeViews.blasting(h.value())));
        each(registry, "molecularTransformer", recipes.getAllRecipesFor(mio_icif_ModRecipes.MOLECULAR_TRANSFORMER_TYPE.get()),
            h -> itemMachine(MioIcifEmiPlugin.MOLECULAR_TRANSFORMER, id(h), MachineRecipeViews.molecularTransformer(h.value())));
    }

    private static <R extends Recipe<?>> void each(EmiRegistry registry, String name, List<RecipeHolder<R>> holders,
                                                   Function<RecipeHolder<R>, EmiRecipe> page) {
        for (RecipeHolder<R> holder : holders) {
            try {
                registry.addRecipe(page.apply(holder));
            } catch (RuntimeException | LinkageError e) {
                QuietFailures.report("emi-machine-" + name, "EMI page of " + holder.id(), e);
            }
        }
    }

    private static void lines(WidgetHolder widgets, List<Component> lines, int y) {
        for (int i = 0; i < lines.size(); i++) SiEmi.line(widgets, lines.get(i), WIDTH, y + i * SiEmi.LINE_HEIGHT);
    }

    /** A recipe found through lookup-only stacks (the fluid in a cell) stays out of EMI's recipe tree. */
    private static SiEmiRecipe treeUnless(SiEmiRecipe recipe, boolean lookupOnly) {
        return lookupOnly ? recipe.noTree() : recipe;
    }

    // ---- one item in, one item out (JEI SiItemMachineCategory): [in] ==> [ out ], the lines under it

    private static final int IN_X = 36, IN_Y = 9, ARROW_X = 60, OUT_X = 94, LINES_Y = 34;

    public static SiEmiRecipe itemMachine(EmiRecipeCategory category, ResourceLocation id, MachineRecipeViews.ItemMachine view) {
        boolean hasInput = !view.inputs().isEmpty();
        EmiIngredient input = hasInput ? SiEmi.items(view.inputs()) : EmiStack.EMPTY;
        EmiStack result = EmiStack.of(view.result());
        // a cell in or out (e.g. an Empty Cell into a Compressed Air Cell) is also found through its fluid
        List<EmiStack> inContents = SiEmi.contents(view.inputs());
        List<EmiStack> outContents = SiEmi.contents(List.of(view.result()));
        List<EmiIngredient> inputs = new ArrayList<>();
        if (hasInput) inputs.add(input);
        inputs.addAll(inContents);
        List<EmiStack> outputs = new ArrayList<>(List.of(result));
        outputs.addAll(outContents);
        SiEmiRecipe recipe = new SiEmiRecipe(category, id, WIDTH, LINES_Y + view.lines().size() * SiEmi.LINE_HEIGHT + 1, inputs, outputs, (w, r) -> {
            if (hasInput) SiEmi.input(w, input, IN_X, IN_Y);
            SiEmi.arrow(w, ARROW_X, IN_Y, view.ticks());
            SiEmi.mainOutput(w, r, result, OUT_X, IN_Y);
            lines(w, view.lines(), LINES_Y);
        });
        return treeUnless(recipe, !inContents.isEmpty() || !outContents.isEmpty());
    }

    // ---- ore washer, thermal centrifuge: [in][water] ==> [a][b][c]

    private static SiEmiRecipe separation(EmiRecipeCategory category, RecipeHolder<?> holder, MachineRecipeViews.Separation view,
                                          int inX, int waterX, int arrowX, int outX) {
        final int rowY = 6, linesY = 31;
        EmiIngredient input = EmiIngredient.of(view.input());
        EmiStack water = view.water() > 0 ? EmiStack.of(Fluids.WATER, view.water()) : EmiStack.EMPTY;
        List<EmiIngredient> inputs = new ArrayList<>(List.of(input));
        if (!water.isEmpty()) inputs.add(water);
        List<EmiStack> outputs = view.results().stream().filter(s -> !s.isEmpty()).map(EmiStack::of).toList();
        return new SiEmiRecipe(category, id(holder), WIDTH, linesY + view.lines().size() * SiEmi.LINE_HEIGHT + 2, inputs, outputs, (w, r) -> {
            SiEmi.input(w, input, inX, rowY);
            if (!water.isEmpty()) SiEmi.input(w, water, waterX, rowY);
            SiEmi.arrow(w, arrowX, rowY, view.ticks());
            for (int i = 0; i < view.results().size(); i++) {
                ItemStack out = view.results().get(i);
                if (!out.isEmpty()) SiEmi.output(w, r, EmiStack.of(out), outX + i * 18, rowY);
            }
            lines(w, view.lines(), linesY);
        });
    }

    // ---- canning: [can] + [material] ==> [ out ]

    private static SiEmiRecipe canning(EmiRecipeCategory category, RecipeHolder<?> holder, MachineRecipeViews.Canning view) {
        final int canX = 17, plusX = 38, materialX = 56, arrowX = 79, outX = 113, rowY = 9, linesY = 34;
        EmiIngredient can = view.can().isEmpty() ? EmiStack.EMPTY : EmiIngredient.of(view.can());
        EmiIngredient material = view.material().isEmpty() ? EmiStack.EMPTY : EmiIngredient.of(view.material());
        EmiStack result = EmiStack.of(view.result());
        List<ItemStack> inStacks = new ArrayList<>(List.of(view.can().getItems()));
        inStacks.addAll(List.of(view.material().getItems()));
        List<EmiStack> inContents = SiEmi.contents(inStacks);
        List<EmiStack> outContents = SiEmi.contents(List.of(view.result()));
        List<EmiIngredient> inputs = new ArrayList<>();
        if (!can.isEmpty()) inputs.add(can);
        if (!material.isEmpty()) inputs.add(material);
        inputs.addAll(inContents);
        List<EmiStack> outputs = new ArrayList<>(List.of(result));
        outputs.addAll(outContents);
        SiEmiRecipe recipe = new SiEmiRecipe(category, id(holder), WIDTH, linesY + view.lines().size() * SiEmi.LINE_HEIGHT + 1, inputs, outputs, (w, r) -> {
            if (!can.isEmpty()) SiEmi.input(w, can, canX, rowY);
            SiEmi.plus(w, plusX, rowY + 2);
            if (!material.isEmpty()) SiEmi.input(w, material, materialX, rowY);
            SiEmi.arrow(w, arrowX, rowY, view.ticks());
            SiEmi.mainOutput(w, r, result, outX, rowY);
            lines(w, view.lines(), linesY);
        });
        return treeUnless(recipe, !inContents.isEmpty() || !outContents.isEmpty());
    }

    // ---- mixing: [fluid] + [material] ==> [fluid][cell]

    private static SiEmiRecipe mixing(EmiRecipeCategory category, RecipeHolder<?> holder, MachineRecipeViews.Mixing view) {
        final int fluidX = 13, plusX = 34, materialX = 52, arrowX = 75, outX = 105, rowY = 6, linesY = 30;
        EmiStack fluid = view.fluid().isEmpty() ? EmiStack.EMPTY : SiEmi.fluid(view.fluid());
        EmiIngredient material = SiEmi.items(view.material());
        EmiStack resultFluid = view.resultFluid().isEmpty() ? EmiStack.EMPTY : SiEmi.fluid(view.resultFluid());
        EmiStack cell = view.cell().isEmpty() ? EmiStack.EMPTY : EmiStack.of(view.cell());
        List<EmiStack> inContents = SiEmi.contents(view.material().stream().map(s -> s.copyWithCount(1)).toList());
        List<EmiIngredient> inputs = new ArrayList<>();
        if (!fluid.isEmpty()) inputs.add(fluid);
        if (!material.isEmpty()) inputs.add(material);
        inputs.addAll(inContents);
        List<EmiStack> outputs = new ArrayList<>();
        if (!resultFluid.isEmpty()) outputs.add(resultFluid);
        if (!cell.isEmpty()) outputs.add(cell);
        SiEmiRecipe recipe = new SiEmiRecipe(category, id(holder), WIDTH, linesY + view.lines().size() * SiEmi.LINE_HEIGHT + 1, inputs, outputs, (w, r) -> {
            if (!fluid.isEmpty()) SiEmi.input(w, fluid, fluidX, rowY);
            SiEmi.plus(w, plusX, rowY + 2);
            if (!material.isEmpty()) SiEmi.input(w, material, materialX, rowY);
            SiEmi.arrow(w, arrowX, rowY, view.ticks());
            int x = outX;
            if (!resultFluid.isEmpty()) {
                SiEmi.output(w, r, resultFluid, x, rowY);
                x += 18;
            }
            if (!cell.isEmpty()) SiEmi.output(w, r, cell, x, rowY);
            lines(w, view.lines(), linesY);
        });
        return treeUnless(recipe, !inContents.isEmpty());
    }

    // ---- blast furnace: [ore] + [air cells] ==> [ out ][slag]

    private static SiEmiRecipe blasting(EmiRecipeCategory category, RecipeHolder<?> holder, MachineRecipeViews.Blasting view) {
        final int oreX = 9, plusX = 30, airX = 48, arrowX = 71, outX = 105, slagX = 127, rowY = 9, linesY = 34;
        EmiIngredient ore = SiEmi.items(view.ore());
        EmiStack air = view.airCells().isEmpty() ? EmiStack.EMPTY : EmiStack.of(view.airCells());
        EmiStack result = EmiStack.of(view.result());
        EmiStack slag = view.slag().isEmpty() ? EmiStack.EMPTY : EmiStack.of(view.slag());
        List<EmiStack> airContents = air.isEmpty() ? List.of() : SiEmi.contents(List.of(view.airCells().copyWithCount(1)));
        List<EmiIngredient> inputs = new ArrayList<>(List.of(ore));
        if (!air.isEmpty()) inputs.add(air);
        inputs.addAll(airContents);
        List<EmiStack> outputs = new ArrayList<>(List.of(result));
        if (!slag.isEmpty()) outputs.add(slag);
        SiEmiRecipe recipe = new SiEmiRecipe(category, id(holder), WIDTH, linesY + view.lines().size() * SiEmi.LINE_HEIGHT + 1, inputs, outputs, (w, r) -> {
            SiEmi.input(w, ore, oreX, rowY);
            if (view.usesAir()) SiEmi.plus(w, plusX, rowY + 2);
            if (!air.isEmpty()) SiEmi.input(w, air, airX, rowY);
            SiEmi.arrow(w, arrowX, rowY, view.ticks());
            SiEmi.mainOutput(w, r, result, outX, rowY);
            if (!slag.isEmpty()) SiEmi.output(w, r, slag, slagX, rowY);
            lines(w, view.lines(), linesY);
        });
        return treeUnless(recipe, !airContents.isEmpty());
    }
}
