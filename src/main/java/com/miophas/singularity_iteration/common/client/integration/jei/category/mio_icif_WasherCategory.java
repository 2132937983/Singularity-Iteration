package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.client.integration.MachineRecipeViews;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.recipe.washer.mio_icif_WasherRecipe;
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
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * Ore Washing Plant: the ore and the water it takes, up to three outputs, the machine's time and energy.
 * <pre>
 *   [ore][water] ==> [o1][o2][o3]
 *    20 s · 16 EU/t · 6,400 EU
 *        Water: 1,000 mB
 * </pre>
 */
@SuppressWarnings("null")
public class mio_icif_WasherCategory extends AbstractJeiRecipeCategory<mio_icif_WasherRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "washer");
    public static final RecipeType<RecipeHolder<mio_icif_WasherRecipe>> WASHER_TYPE = RecipeType.createRecipeHolderType(UID);

    private static final int WIDTH = 150, HEIGHT = 53;
    private static final int ORE_X = 13, WATER_X = 31, ARROW_X = 55, OUT_X = 85, ROW_Y = 6, LINES_Y = 31;

    private final IDrawable icon;

    public mio_icif_WasherCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemLike(mio_icif_blocks.WASHER_ELC.get());
    }

    @Override public RecipeType<RecipeHolder<mio_icif_WasherRecipe>> getRecipeType() { return WASHER_TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.mio_icif.washer"); }
    @Override public @Nullable IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }

    @Override
    protected void setRecipeFor(IRecipeLayoutBuilder builder, mio_icif_WasherRecipe recipe, IFocusGroup focuses) {
        MachineRecipeViews.Separation view = MachineRecipeViews.washer(recipe);
        SiJei.input(builder, ORE_X, ROW_Y).addIngredients(view.input());
        if (view.water() > 0) {
            SiJei.fluid(builder, RecipeIngredientRole.INPUT, WATER_X, ROW_Y, new FluidStack(Fluids.WATER, view.water()));
        }
        for (int i = 0; i < view.results().size(); i++) {
            if (!view.results().get(i).isEmpty()) SiJei.output(builder, OUT_X + i * 18, ROW_Y).addItemStack(view.results().get(i));
        }
    }

    @Override
    protected void createExtrasFor(IRecipeExtrasBuilder builder, mio_icif_WasherRecipe recipe, IFocusGroup focuses) {
        MachineRecipeViews.Separation view = MachineRecipeViews.washer(recipe);
        SiJei.arrow(builder, ARROW_X, ROW_Y, view.ticks());
        for (int i = 0; i < view.lines().size(); i++) SiJei.line(builder, view.lines().get(i), WIDTH, LINES_Y + i * SiJei.LINE_HEIGHT);
    }
}
