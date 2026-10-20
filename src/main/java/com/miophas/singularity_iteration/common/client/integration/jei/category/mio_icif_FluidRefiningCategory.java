package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_oil_refinery_elc;
import com.miophas.singularity_iteration.common.recipe.fluid_refining.FluidRefiningRecipe;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Oil Refinery: one fluid into another, 1:1 by volume, per cycle of the machine (optionally with an item).
 * <pre>
 *   [item][fluid] ==> [fluid]
 *     0.5 s per cycle · 8 EU/t
 * </pre>
 */
@SuppressWarnings("null")
public class mio_icif_FluidRefiningCategory extends AbstractJeiRecipeCategory<FluidRefiningRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "fluid_refining");
    public static final RecipeType<RecipeHolder<FluidRefiningRecipe>> FLUID_REFINING_TYPE = RecipeType.createRecipeHolderType(UID);

    private static final int WIDTH = 150, HEIGHT = 41;
    private static final int ITEM_X = 31, FLUID_X = 49, ARROW_X = 73, OUT_X = 103, ROW_Y = 6, LINES_Y = 30;

    private final IDrawable icon;

    public mio_icif_FluidRefiningCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemLike(mio_icif_blocks.OIL_REFINERY_ELC.get());
    }

    @Override public RecipeType<RecipeHolder<FluidRefiningRecipe>> getRecipeType() { return FLUID_REFINING_TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.mio_icif.fluid_refining"); }
    @Override public @Nullable IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }

    /** The fluids the recipe takes: its fluid, or the source fluids of its tag. */
    private static List<Fluid> inputFluids(FluidRefiningRecipe recipe) {
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

    @Override
    protected void setRecipeFor(IRecipeLayoutBuilder builder, FluidRefiningRecipe recipe, IFocusGroup focuses) {
        FluidStack output = recipe.getOutputFluid();
        int amount = Math.max(1, output.getAmount());
        if (!recipe.getInputIngredient().isEmpty()) SiJei.input(builder, ITEM_X, ROW_Y).addIngredients(recipe.getInputIngredient());
        List<Fluid> inputs = inputFluids(recipe);
        if (!inputs.isEmpty()) SiJei.fluids(builder, RecipeIngredientRole.INPUT, FLUID_X, ROW_Y, inputs, amount);
        SiJei.fluid(builder, RecipeIngredientRole.OUTPUT, OUT_X, ROW_Y, output);
    }

    @Override
    protected void createExtrasFor(IRecipeExtrasBuilder builder, FluidRefiningRecipe recipe, IFocusGroup focuses) {
        int ticks = mio_icif_oil_refinery_elc.DEFAULT_WORK_TIME;
        SiJei.arrow(builder, ARROW_X, ROW_Y, ticks);
        SiJei.line(builder, Component.translatable("jei.mio_icif.fluid_refining.cycle", SiJei.seconds(ticks),
            SiJei.number(mio_icif_oil_refinery_elc.DEFAULT_ENERGY_PER_TICK)), WIDTH, LINES_Y);
    }
}
