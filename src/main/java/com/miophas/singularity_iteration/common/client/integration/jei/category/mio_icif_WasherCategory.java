package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_washer_elc;
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
import net.minecraft.world.item.ItemStack;
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
        SiJei.input(builder, ORE_X, ROW_Y).addIngredients(recipe.getIngredient());
        if (recipe.getWaterAmount() > 0) {
            SiJei.fluid(builder, RecipeIngredientRole.INPUT, WATER_X, ROW_Y, new FluidStack(Fluids.WATER, recipe.getWaterAmount()));
        }
        ItemStack[] results = {
            recipe.getPrimaryResult().copyWithCount(recipe.getPrimaryCount()),
            recipe.getSecondaryCount() > 0 ? recipe.getSecondaryResult().copyWithCount(recipe.getSecondaryCount()) : ItemStack.EMPTY,
            recipe.getTertiaryCount() > 0 ? recipe.getTertiaryResult().copyWithCount(recipe.getTertiaryCount()) : ItemStack.EMPTY};
        for (int i = 0; i < results.length; i++) {
            if (!results[i].isEmpty()) SiJei.output(builder, OUT_X + i * 18, ROW_Y).addItemStack(results[i]);
        }
    }

    @Override
    protected void createExtrasFor(IRecipeExtrasBuilder builder, mio_icif_WasherRecipe recipe, IFocusGroup focuses) {
        int ticks = mio_icif_washer_elc.DEFAULT_WORK_TIME;
        SiJei.arrow(builder, ARROW_X, ROW_Y, ticks);
        SiJei.line(builder, SiJei.cost(ticks, mio_icif_washer_elc.DEFAULT_ENERGY_PER_TICK), WIDTH, LINES_Y);
        if (recipe.getWaterAmount() > 0) {
            SiJei.line(builder, Component.translatable("jei.mio_icif.washer.water", SiJei.number(recipe.getWaterAmount())), WIDTH, LINES_Y + SiJei.LINE_HEIGHT);
        }
    }
}
