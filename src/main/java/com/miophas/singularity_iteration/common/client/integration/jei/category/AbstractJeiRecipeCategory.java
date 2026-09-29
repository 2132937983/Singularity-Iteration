package com.miophas.singularity_iteration.common.client.integration.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * JEI 配方类别基类。
 *
 * <p>JEI 的“复制配方 ID”（默认按键 Ctrl+O）、高级提示中的配方来源、以及配方书签，
 * 都依赖 {@link IRecipeCategory#getRegistryName(Object)} 返回的 {@link ResourceLocation}。
 * 而 JEI <b>只有当配方对象本身是 {@link RecipeHolder} 时</b>才能取到 ID，
 * 否则会提示 “复制配方 ID 失败”。</p>
 *
 * <p>因此本基类统一以 {@code RecipeHolder<T>} 作为 JEI 的配方类型，
 * 保证 ID 始终可用；子类依旧只需要针对原始配方类型 {@code T} 实现
 * {@link #setRecipeFor(IRecipeLayoutBuilder, Object, IFocusGroup)} 与
 * {@link #drawFor(Object, IRecipeSlotsView, GuiGraphics, double, double)}。</p>
 *
 * @param <T> 原始配方类型（非 RecipeHolder）
 */
public abstract class AbstractJeiRecipeCategory<T extends Recipe<?>> implements IRecipeCategory<RecipeHolder<T>> {

    @Override
    public final void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<T> recipe, IFocusGroup focuses) {
        setRecipeFor(builder, recipe.value(), focuses);
    }

    /**
     * 子类实现：针对原始配方类型布局槽位。
     * 由 {@link #setRecipe(IRecipeLayoutBuilder, RecipeHolder, IFocusGroup)} 自动解包 RecipeHolder 后调用。
     */
    protected abstract void setRecipeFor(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses);

    @Override
    public void draw(RecipeHolder<T> recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        drawFor(recipe.value(), recipeSlotsView, guiGraphics, mouseX, mouseY);
    }

    /**
     * 子类实现：绘制额外内容。默认空实现，子类可选覆写。
     */
    protected void drawFor(T recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
    }

    /**
     * 显式返回配方注册 ID，使 JEI 的“复制配方 ID”、高级提示与书签功能可用。
     */
    @Override
    public @Nullable ResourceLocation getRegistryName(RecipeHolder<T> recipe) {
        return recipe.id();
    }
}
