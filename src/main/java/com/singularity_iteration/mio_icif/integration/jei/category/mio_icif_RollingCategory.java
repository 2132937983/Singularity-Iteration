package com.singularity_iteration.mio_icif.integration.jei.category;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.recipe.metal_former.rolling.mio_icif_RollingRecipe;
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

import java.util.Arrays;
import java.util.List;

/**
 * 金属成型�?轧制 JEI 配方类别
 */
@SuppressWarnings("null")
public class mio_icif_RollingCategory implements IRecipeCategory<mio_icif_RollingRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "rolling");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
            "textures/gui/gui_metal_former.png");
    public static final ResourceLocation ATLAS_TEXTURE = mio_icif_gui_global_variables.ATLAS_TEXTURE;
    public static final RecipeType<mio_icif_RollingRecipe> ROLLING_TYPE =
            new RecipeType<>(UID, mio_icif_RollingRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public mio_icif_RollingCategory(IGuiHelper helper) {
        // 背景只绘制机器内容区域，去掉边框
        this.background = helper.createDrawable(TEXTURE, 7, 7, 162, 72);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(mio_icif_blocks.METAL_FORMER.get()));
    }

    @Override
    public RecipeType<mio_icif_RollingRecipe> getRecipeType() {
        return ROLLING_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mio_icif.rolling");
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
    public void setRecipe(IRecipeLayoutBuilder builder, mio_icif_RollingRecipe recipe, IFocusGroup focuses) {
        // 修复：使�?addItemStacks 并设置正确的数量
        List<ItemStack> inputs = Arrays.stream(recipe.getIngredient().getItems())
                .map(stack -> stack.copyWithCount(recipe.getIngredientCount()))
                .toList();
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 10)
                .addItemStacks(inputs);

        // 输出�
    builder.addSlot(RecipeIngredientRole.OUTPUT, 109, 28)
                .addItemStack(recipe.getResultItem(null));
    }

    @Override
    public void draw(mio_icif_RollingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.background.draw(guiGraphics, 0, 0);
        
        // 绘制金属成型机专属进度条 - 使用与 Screen 基类相同的方法
        int progressX = 43;
        int progressY = 30;
        int progressPixels = mio_icif_gui_global_variables.METAL_FORMER_WORK_PROGRESS_U; // 满进度
        
        // 先渲染背景
        guiGraphics.blit(ATLAS_TEXTURE, progressX, progressY, 0,
            (float) mio_icif_gui_global_variables.METAL_FORMER_WORK_PROGRESS_BG_X,
            (float) mio_icif_gui_global_variables.METAL_FORMER_WORK_PROGRESS_BG_Y,
            mio_icif_gui_global_variables.METAL_FORMER_WORK_PROGRESS_BG_U,
            mio_icif_gui_global_variables.METAL_FORMER_WORK_PROGRESS_BG_V,
            mio_icif_gui_global_variables.ATLAS_WIDTH,
            mio_icif_gui_global_variables.ATLAS_HEIGHT);
        
        // 再渲染进度条
        guiGraphics.blit(ATLAS_TEXTURE, progressX, progressY, 0,
            (float) mio_icif_gui_global_variables.METAL_FORMER_WORK_PROGRESS_X,
            (float) mio_icif_gui_global_variables.METAL_FORMER_WORK_PROGRESS_Y,
            progressPixels,
            mio_icif_gui_global_variables.METAL_FORMER_WORK_PROGRESS_V,
            mio_icif_gui_global_variables.ATLAS_WIDTH,
            mio_icif_gui_global_variables.ATLAS_HEIGHT);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font != null) {
            String timeText = recipe.getProcessingTime() / 20 + "s";
            guiGraphics.drawString(minecraft.font, timeText, 86 - minecraft.font.width(timeText) / 2, 65, 0x808080, false);
        }
    }
}