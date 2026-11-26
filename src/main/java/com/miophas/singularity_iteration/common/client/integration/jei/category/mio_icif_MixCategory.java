package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.client.integration.MachineRecipeViews;
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
import net.minecraft.world.item.crafting.RecipeHolder;
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
        MachineRecipeViews.Mixing view = MachineRecipeViews.mixing(recipe);
        if (!view.fluid().isEmpty()) SiJei.fluid(builder, RecipeIngredientRole.INPUT, FLUID_X, ROW_Y, view.fluid());
        SiJei.input(builder, MAT_X, ROW_Y).addItemStacks(view.material());
        int x = OUT_X;
        if (!view.resultFluid().isEmpty()) {
            SiJei.fluid(builder, RecipeIngredientRole.OUTPUT, x, ROW_Y, view.resultFluid());
            x += 18;
        }
        if (!view.cell().isEmpty()) SiJei.output(builder, x, ROW_Y).addItemStack(view.cell());
        SiJei.contents(builder, RecipeIngredientRole.INPUT, java.util.List.of(recipe.getMaterialIngredient().getItems()));
    }

    @Override
    protected void createExtrasFor(IRecipeExtrasBuilder builder, MixRecipe recipe, IFocusGroup focuses) {
        MachineRecipeViews.Mixing view = MachineRecipeViews.mixing(recipe);
        builder.addRecipePlusSign().setPosition(PLUS_X, ROW_Y + 2);
        SiJei.arrow(builder, ARROW_X, ROW_Y, view.ticks());
        for (int i = 0; i < view.lines().size(); i++) SiJei.line(builder, view.lines().get(i), WIDTH, LINES_Y + i * SiJei.LINE_HEIGHT);
    }
}
