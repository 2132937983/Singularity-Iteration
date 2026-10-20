package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_fermenter_elc;
import com.miophas.singularity_iteration.common.recipe.fermenter.FermenterRecipe;
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
 * Fermenter (mio_icif:fermenter JSON recipes): input fluid and heat give the output fluid, plus the
 * by-product every interval of fermented input. The arrow runs for the run at the full 100 HU/t.
 * <pre>
 *   [fluid] ==> [fluid][by-product]
 *         4,000 HU per run
 *    1 Fertilizer every 500 mB
 * </pre>
 */
@SuppressWarnings("null")
public class mio_icif_FermenterCategory extends AbstractJeiRecipeCategory<FermenterRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "fermenter");
    public static final RecipeType<RecipeHolder<FermenterRecipe>> FERMENTER_TYPE = RecipeType.createRecipeHolderType(UID);

    private static final int WIDTH = 150, HEIGHT = 51;
    private static final int IN_X = 31, ARROW_X = 55, OUT_X = 85, ROW_Y = 6, LINES_Y = 30;

    private final IDrawable icon;

    public mio_icif_FermenterCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemLike(mio_icif_blocks.FERMENTER_ELC.get());
    }

    @Override public RecipeType<RecipeHolder<FermenterRecipe>> getRecipeType() { return FERMENTER_TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.mio_icif.fermenter"); }
    @Override public @Nullable IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }

    @Override
    protected void setRecipeFor(IRecipeLayoutBuilder builder, FermenterRecipe recipe, IFocusGroup focuses) {
        SiJei.fluids(builder, RecipeIngredientRole.INPUT, IN_X, ROW_Y,
            java.util.Arrays.stream(recipe.getInput().getFluids()).map(FluidStack::getFluid).distinct().toList(), recipe.getInput().amount());
        SiJei.fluid(builder, RecipeIngredientRole.OUTPUT, OUT_X, ROW_Y, recipe.getOutput());
        ItemStack byproduct = recipe.getByproduct();
        if (!byproduct.isEmpty()) SiJei.output(builder, OUT_X + 18, ROW_Y).addItemStack(byproduct);
    }

    @Override
    protected void createExtrasFor(IRecipeExtrasBuilder builder, FermenterRecipe recipe, IFocusGroup focuses) {
        int ticks = Math.max(1, (recipe.getHeat() + mio_icif_fermenter_elc.MAX_HEAT_RECEIVE - 1) / mio_icif_fermenter_elc.MAX_HEAT_RECEIVE);
        SiJei.arrow(builder, ARROW_X, ROW_Y, ticks);
        SiJei.line(builder, Component.translatable("jei.mio_icif.fermenter.heat", SiJei.number(recipe.getHeat())), WIDTH, LINES_Y);
        ItemStack byproduct = recipe.getByproduct();
        if (!byproduct.isEmpty()) {
            SiJei.line(builder, Component.translatable("jei.mio_icif.fermenter.byproduct", byproduct.getCount(),
                byproduct.getHoverName(), SiJei.number(recipe.getByproductInterval())), WIDTH, LINES_Y + SiJei.LINE_HEIGHT);
        }
    }
}
