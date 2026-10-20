package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.recipe.canner.mix.MixRecipe;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * Canner, mix mode: a fluid and a material give a fluid (and/or a filled cell); the recipe sets time and energy.
 * <pre>
 *   [fluid] + [mat] ==> [fluid][cell]
 *       10 s · 1 EU/t · 200 EU
 * </pre>
 */
@SuppressWarnings("null")
public class mio_icif_MixCategory extends AbstractJeiRecipeCategory<MixRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "mix");
    public static final RecipeType<RecipeHolder<MixRecipe>> MIX_TYPE = RecipeType.createRecipeHolderType(UID);

    private static final int WIDTH = 150, HEIGHT = 41;
    private static final int FLUID_X = 13, PLUS_X = 34, MAT_X = 52, ARROW_X = 75, OUT_X = 105, ROW_Y = 6, LINES_Y = 30;

    private final IDrawable icon;

    public mio_icif_MixCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemLike(mio_icif_blocks.CANNER_ELC.get());
    }

    @Override public RecipeType<RecipeHolder<MixRecipe>> getRecipeType() { return MIX_TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.mio_icif.mix"); }
    @Override public @Nullable IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }

    @Override
    protected void setRecipeFor(IRecipeLayoutBuilder builder, MixRecipe recipe, IFocusGroup focuses) {
        if (!recipe.getInputFluid().isEmpty()) SiJei.fluid(builder, RecipeIngredientRole.INPUT, FLUID_X, ROW_Y, recipe.getInputFluid());
        SiJei.input(builder, MAT_X, ROW_Y).addItemStacks(SiJei.withCount(recipe.getMaterialIngredient(), recipe.getMaterialCount()));
        int x = OUT_X;
        FluidStack resultFluid = recipe.getResultFluid();
        if (!resultFluid.isEmpty()) {
            SiJei.fluid(builder, RecipeIngredientRole.OUTPUT, x, ROW_Y, resultFluid);
            x += 18;
        }
        ItemStack cell = recipe.getDynamicResultCell();
        if (!cell.isEmpty()) SiJei.output(builder, x, ROW_Y).addItemStack(cell);
        SiJei.contents(builder, RecipeIngredientRole.INPUT, java.util.List.of(recipe.getMaterialIngredient().getItems()));
    }

    @Override
    protected void createExtrasFor(IRecipeExtrasBuilder builder, MixRecipe recipe, IFocusGroup focuses) {
        builder.addRecipePlusSign().setPosition(PLUS_X, ROW_Y + 2);
        SiJei.arrow(builder, ARROW_X, ROW_Y, recipe.getProcessingTime());
        SiJei.line(builder, SiJei.cost(recipe.getProcessingTime(), recipe.getEnergyPerTick()), WIDTH, LINES_Y);
    }
}
