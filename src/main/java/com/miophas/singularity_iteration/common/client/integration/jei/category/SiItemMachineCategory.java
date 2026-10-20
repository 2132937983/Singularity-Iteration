package com.miophas.singularity_iteration.common.client.integration.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One item in, one item out ({@link SiJei} look): input, arrow over the processing time, 26 px output,
 * then the info lines (by default the time and energy line).
 * <pre>
 *        [in]  ====>  [ out ]
 *       15 s · 2 EU/t · 600 EU
 * </pre>
 */
public abstract class SiItemMachineCategory<T extends Recipe<?>> extends AbstractJeiRecipeCategory<T> {

    protected static final int WIDTH = 150;
    protected static final int IN_X = 36, IN_Y = 9;
    protected static final int ARROW_X = 60, ARROW_Y = 9;
    protected static final int OUT_X = 94, OUT_Y = 9;
    protected static final int LINES_Y = 34;

    private final RecipeType<RecipeHolder<T>> type;
    private final Component title;
    private final IDrawable icon;
    private final int lineCount;

    protected SiItemMachineCategory(IGuiHelper helper, RecipeType<RecipeHolder<T>> type, String titleKey, ItemLike icon, int lineCount) {
        this.type = type;
        this.title = Component.translatable(titleKey);
        this.icon = helper.createDrawableItemLike(icon);
        this.lineCount = lineCount;
    }

    /** The input stacks (with the recipe's count); empty for no input slot. */
    protected abstract List<ItemStack> inputs(T recipe);

    protected abstract ItemStack result(T recipe);

    /** Ticks per operation; 0 when the time is not fixed (the arrow then does not animate). */
    protected abstract int ticks(T recipe);

    protected abstract long euPerTick(T recipe);

    /** The lines under the recipe, at most the category's line count. Default: time and energy. */
    protected List<Component> lines(T recipe) {
        return List.of(SiJei.cost(ticks(recipe), euPerTick(recipe)));
    }

    @Override
    public RecipeType<RecipeHolder<T>> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return LINES_Y + lineCount * SiJei.LINE_HEIGHT + 1;
    }

    @Override
    protected void setRecipeFor(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
        List<ItemStack> inputs = inputs(recipe);
        ItemStack result = result(recipe);
        if (!inputs.isEmpty()) SiJei.input(builder, IN_X, IN_Y).addItemStacks(inputs);
        SiJei.mainOutput(builder, OUT_X, OUT_Y).addItemStack(result);
        // a cell in or out (e.g. Compressor: Empty Cell -> Compressed Air Cell) also counts as its fluid
        SiJei.contents(builder, mezz.jei.api.recipe.RecipeIngredientRole.INPUT, inputs);
        SiJei.contents(builder, mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT, List.of(result));
    }

    @Override
    protected void createExtrasFor(IRecipeExtrasBuilder builder, T recipe, IFocusGroup focuses) {
        SiJei.arrow(builder, ARROW_X, ARROW_Y, ticks(recipe));
        List<Component> lines = lines(recipe);
        for (int i = 0; i < Math.min(lineCount, lines.size()); i++) {
            SiJei.line(builder, lines.get(i), WIDTH, LINES_Y + i * SiJei.LINE_HEIGHT);
        }
    }
}
