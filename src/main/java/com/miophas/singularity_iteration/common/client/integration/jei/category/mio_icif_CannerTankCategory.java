package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_canner_elc;
import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Canner, Tank → Cell and Cell → Tank modes: no JSON recipes, the canner moves any fluid between its
 * tank and a cell. One page per fluid, so a cell's recipes show how to fill it and its uses how to
 * empty it.
 * <pre>
 *   Tank → Cell:  [fluid] + [cell] ==> [filled]        Cell → Tank:  [filled] ==> [fluid][cell]
 *                 10 s · 4 EU/t · 800 EU                             10 s · 4 EU/t · 800 EU
 * </pre>
 */
@SuppressWarnings("null")
public class mio_icif_CannerTankCategory implements IRecipeCategory<mio_icif_CannerTankCategory.CellFill> {

    public static final RecipeType<CellFill> FILL_FROM_TANK_TYPE = new RecipeType<>(
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "fill_from_tank"), CellFill.class);
    public static final RecipeType<CellFill> EMPTY_TO_TANK_TYPE = new RecipeType<>(
        ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "empty_to_tank"), CellFill.class);

    private static final int WIDTH = 150, HEIGHT = 41, ROW_Y = 6, LINES_Y = 30;
    // Tank → Cell keeps the Mix layout; Cell → Tank the Fermenter one (one input, two outputs).
    private static final int FLUID_X = 13, PLUS_X = 34, EMPTY_X = 52, FILL_ARROW_X = 75, FILLED_X = 105;
    private static final int FULL_X = 31, DRAIN_ARROW_X = 55, OUT_X = 85;

    /** An empty cell and {@code fluid} make the {@code filled} cell; Cell → Tank runs it backwards. */
    public record CellFill(ItemStack empty, FluidStack fluid, ItemStack filled) {}

    /** Every source fluid a cell can hold, with the cell the canner fills it into. */
    public static List<CellFill> recipes() {
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

    private final boolean toTank;
    private final IDrawable icon;

    /** {@code toTank}: the Cell → Tank page; otherwise Tank → Cell. */
    public mio_icif_CannerTankCategory(IGuiHelper helper, boolean toTank) {
        this.toTank = toTank;
        this.icon = helper.createDrawableItemLike(mio_icif_blocks.CANNER_ELC.get());
    }

    @Override public RecipeType<CellFill> getRecipeType() { return toTank ? EMPTY_TO_TANK_TYPE : FILL_FROM_TANK_TYPE; }
    @Override public Component getTitle() { return Component.translatable(toTank ? "jei.mio_icif.empty_to_tank" : "jei.mio_icif.fill_from_tank"); }
    @Override public @Nullable IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CellFill recipe, IFocusGroup focuses) {
        // Plain fluid slots, not SiJei.fluid: the cell is already a visible ingredient here, and listing
        // it again on the other side would make draining a cell a "recipe" for that cell.
        if (toTank) {
            SiJei.input(builder, FULL_X, ROW_Y).addItemStack(recipe.filled());
            fluid(builder, RecipeIngredientRole.OUTPUT, OUT_X, recipe.fluid());
            SiJei.output(builder, OUT_X + 18, ROW_Y).addItemStack(recipe.empty());
        } else {
            fluid(builder, RecipeIngredientRole.INPUT, FLUID_X, recipe.fluid());
            SiJei.input(builder, EMPTY_X, ROW_Y).addItemStack(recipe.empty());
            SiJei.output(builder, FILLED_X, ROW_Y).addItemStack(recipe.filled());
        }
    }

    private static void fluid(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, FluidStack fluid) {
        builder.addSlot(role, x, ROW_Y).setStandardSlotBackground()
            .addFluidStack(fluid.getFluid(), fluid.getAmount())
            .setFluidRenderer(fluid.getAmount(), false, 16, 16);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CellFill recipe, IFocusGroup focuses) {
        int ticks = mio_icif_canner_elc.DEFAULT_WORK_TIME;
        if (toTank) {
            SiJei.arrow(builder, DRAIN_ARROW_X, ROW_Y, ticks);
        } else {
            builder.addRecipePlusSign().setPosition(PLUS_X, ROW_Y + 2);
            SiJei.arrow(builder, FILL_ARROW_X, ROW_Y, ticks);
        }
        SiJei.line(builder, SiJei.cost(ticks, mio_icif_canner_elc.DEFAULT_ENERGY_PER_TICK), WIDTH, LINES_Y);
    }
}
