package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.client.integration.MachineRecipeViews;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.recipe.centrifuge.mio_icif_CentrifugeRecipe;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * Thermal Centrifuge: one input, up to three outputs, the minimum heat of the recipe and the machine's
 * time and energy.
 * <pre>
 *   [in] ==> [o1][o2][o3]
 *        Heat >= 500 HU
 *   25 s · 48 EU/t · 24,000 EU
 * </pre>
 */
@SuppressWarnings("null")
public class mio_icif_CentrifugeCategory extends AbstractJeiRecipeCategory<mio_icif_CentrifugeRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "centrifuge");
    public static final RecipeType<RecipeHolder<mio_icif_CentrifugeRecipe>> CENTRIFUGE_TYPE = RecipeType.createRecipeHolderType(UID);

    private static final int WIDTH = 150, HEIGHT = 53;
    private static final int IN_X = 20, ARROW_X = 44, OUT_X = 76, ROW_Y = 6, LINES_Y = 31;

    private final IDrawable icon;

    public mio_icif_CentrifugeCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemLike(mio_icif_blocks.CENTRIFUGE_ELC.get());
    }

    @Override public RecipeType<RecipeHolder<mio_icif_CentrifugeRecipe>> getRecipeType() { return CENTRIFUGE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.mio_icif.centrifuge"); }
    @Override public @Nullable IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }

    @Override
    protected void setRecipeFor(IRecipeLayoutBuilder builder, mio_icif_CentrifugeRecipe recipe, IFocusGroup focuses) {
        MachineRecipeViews.Separation view = MachineRecipeViews.centrifuge(recipe);
        SiJei.input(builder, IN_X, ROW_Y).addIngredients(view.input());
        for (int i = 0; i < view.results().size(); i++) {
            if (!view.results().get(i).isEmpty()) SiJei.output(builder, OUT_X + i * 18, ROW_Y).addItemStack(view.results().get(i));
        }
    }

    @Override
    protected void createExtrasFor(IRecipeExtrasBuilder builder, mio_icif_CentrifugeRecipe recipe, IFocusGroup focuses) {
        MachineRecipeViews.Separation view = MachineRecipeViews.centrifuge(recipe);
        SiJei.arrow(builder, ARROW_X, ROW_Y, view.ticks());
        for (int i = 0; i < view.lines().size(); i++) SiJei.line(builder, view.lines().get(i), WIDTH, LINES_Y + i * SiJei.LINE_HEIGHT);
    }
}
