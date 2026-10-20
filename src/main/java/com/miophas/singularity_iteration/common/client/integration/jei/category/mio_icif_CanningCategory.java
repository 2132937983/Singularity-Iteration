package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.recipe.canner.canning.CanningRecipe;
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
 * Canner: the container and its content make the filled item; the recipe sets time and energy.
 * <pre>
 *   [can] + [food] ==> [ out ]
 *     10 s · 1 EU/t · 200 EU
 * </pre>
 */
@SuppressWarnings("null")
public class mio_icif_CanningCategory extends AbstractJeiRecipeCategory<CanningRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "canning");
    public static final RecipeType<RecipeHolder<CanningRecipe>> CANNING_TYPE = RecipeType.createRecipeHolderType(UID);

    private static final int WIDTH = 150, HEIGHT = 45;
    private static final int CAN_X = 17, PLUS_X = 38, FOOD_X = 56, ARROW_X = 79, OUT_X = 113, ROW_Y = 9, LINES_Y = 34;

    private final IDrawable icon;

    public mio_icif_CanningCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemLike(mio_icif_blocks.CANNER_ELC.get());
    }

    @Override public RecipeType<RecipeHolder<CanningRecipe>> getRecipeType() { return CANNING_TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.mio_icif.canning"); }
    @Override public @Nullable IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }

    @Override
    protected void setRecipeFor(IRecipeLayoutBuilder builder, CanningRecipe recipe, IFocusGroup focuses) {
        var ingredients = recipe.getIngredients();
        if (ingredients.size() > 0) SiJei.input(builder, CAN_X, ROW_Y).addIngredients(ingredients.get(0));
        if (ingredients.size() > 1) SiJei.input(builder, FOOD_X, ROW_Y).addIngredients(ingredients.get(1));
        SiJei.mainOutput(builder, OUT_X, ROW_Y).addItemStack(recipe.getResult());
        for (var ingredient : ingredients) SiJei.contents(builder, mezz.jei.api.recipe.RecipeIngredientRole.INPUT, java.util.List.of(ingredient.getItems()));
        SiJei.contents(builder, mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT, java.util.List.of(recipe.getResult()));
    }

    @Override
    protected void createExtrasFor(IRecipeExtrasBuilder builder, CanningRecipe recipe, IFocusGroup focuses) {
        builder.addRecipePlusSign().setPosition(PLUS_X, ROW_Y + 2);
        SiJei.arrow(builder, ARROW_X, ROW_Y, recipe.getProcessingTime());
        SiJei.line(builder, SiJei.cost(recipe.getProcessingTime(), recipe.getEnergyPerTick()), WIDTH, LINES_Y);
    }
}
