package com.singularity_iteration.mio_icif.integration.jei.category;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.recipe.mio_icif_PowderRecipe;
import com.singularity_iteration.mio_icif.util.mio_icif_gui_global_variables;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 打粉�?JEI 配方类别
 * 支持进度条动画显示处理进�?
 */
@SuppressWarnings("null")
public class mio_icif_PowderCategory implements IRecipeCategory<mio_icif_PowderRecipe> {

    // 配方类型 UID
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "powder");

    // GUI 纹理路径
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
            "textures/gui/gui_powder_elc.png");
    public static final ResourceLocation ATLAS_TEXTURE = mio_icif_gui_global_variables.ATLAS_TEXTURE;

    // 配方类型定义
    public static final RecipeType<mio_icif_PowderRecipe> POWDER_TYPE =
            new RecipeType<>(UID, mio_icif_PowderRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public mio_icif_PowderCategory(IGuiHelper helper) {
        // 背景只绘制机器内容区域，去掉边框
        // 从纹�?7,7)开始，尺寸(162, 72) - 裁剪掉边�?
        this.background = helper.createDrawable(TEXTURE, 7, 7, 162, 72);

        // 创建图标（使用打粉机方方块?
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(mio_icif_blocks.POWDER_ELC));
    }

    @Override
    public RecipeType<mio_icif_PowderRecipe> getRecipeType() {
        return POWDER_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mio_icif.powder");
    }

    @Override
    public int getWidth() {
        return 162;
    }

    @Override
    public int getHeight() {
        return 72;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, mio_icif_PowderRecipe recipe, IFocusGroup focuses) {
        // 槽位坐标已根据新的背景区域?7,7)调整
        // 输入�? (49, 10), 输出�? (109, 28)
        builder.addSlot(RecipeIngredientRole.INPUT, 49, 10)
                .addIngredients(recipe.getIngredients().get(0));

        // 输出�?
        builder.addSlot(RecipeIngredientRole.OUTPUT, 109, 28)
                .addItemStack(recipe.getResultItem(null));
    }

    @Override
    public void draw(mio_icif_PowderRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 绘制背景
        if (this.background != null) {
            this.background.draw(guiGraphics, 0, 0);
        }

        // 绘制标准箭头进度条 - 使用与 Screen 基类相同的方法
        int progressX = 72;
        int progressY = 28;
        int progressPixels = mio_icif_gui_global_variables.ARROW_WIDTH; // 满进度
        
        // 先渲染背景
        guiGraphics.blit(ATLAS_TEXTURE, progressX, progressY, 0,
            (float) mio_icif_gui_global_variables.PROGRESS_BAR_BG_TEXTURE_X,
            (float) mio_icif_gui_global_variables.PROGRESS_BAR_BG_TEXTURE_Y,
            mio_icif_gui_global_variables.PROGRESS_BAR_BG_WIDTH,
            mio_icif_gui_global_variables.PROGRESS_BAR_BG_HEIGHT,
            mio_icif_gui_global_variables.ATLAS_WIDTH,
            mio_icif_gui_global_variables.ATLAS_HEIGHT);
        
        // 再渲染进度条
        guiGraphics.blit(ATLAS_TEXTURE, progressX, progressY, 0,
            (float) mio_icif_gui_global_variables.ARROW_U,
            (float) mio_icif_gui_global_variables.ARROW_V,
            progressPixels,
            mio_icif_gui_global_variables.ARROW_HEIGHT,
            mio_icif_gui_global_variables.ATLAS_WIDTH,
            mio_icif_gui_global_variables.ATLAS_HEIGHT);

        // 绘制处理时间信息
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font != null) {
            String timeText = recipe.getProcessingTime() / 20 + "s";
            guiGraphics.drawString(minecraft.font, timeText, 86 - minecraft.font.width(timeText) / 2, 65, 0x808080, false);
        }
    }
}