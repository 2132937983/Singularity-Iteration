package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipe;
import com.miophas.singularity_iteration.common.util.mio_icif_gui_global_variables;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * 分子重组仪 JEI 配方类别
 */
@SuppressWarnings("null")
public class mio_icif_MolecularTransformerCategory extends AbstractJeiRecipeCategory<mio_icif_MolecularTransformerRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "molecular_transformer");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
            "textures/gui/gui_molecular_transformer.png");
    public static final ResourceLocation ATLAS_TEXTURE = mio_icif_gui_global_variables.ATLAS_TEXTURE;
    public static final RecipeType<RecipeHolder<mio_icif_MolecularTransformerRecipe>> MOLECULAR_TRANSFORMER_TYPE =
            RecipeType.createRecipeHolderType(UID);

    private final IDrawable background;
    private final IDrawable icon;

    public mio_icif_MolecularTransformerCategory(IGuiHelper helper) {
        // 背景只绘制机器内容区域，去掉边框
        this.background = helper.createDrawable(TEXTURE, 7, 7, 162, 72);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(mio_icif_blocks.MOLECULAR_TRANSFORMER.get()));
    }

    @Override
    public RecipeType<RecipeHolder<mio_icif_MolecularTransformerRecipe>> getRecipeType() {
        return MOLECULAR_TRANSFORMER_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mio_icif.molecular_transformer");
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
    protected void setRecipeFor(IRecipeLayoutBuilder builder, mio_icif_MolecularTransformerRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 48, 28)
                .addIngredients(recipe.getIngredients().get(0));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 108, 28)
                .addItemStack(recipe.getResultItem(null));
    }

    @Override
    protected void drawFor(mio_icif_MolecularTransformerRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.background.draw(guiGraphics, 0, 0);
        
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

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font != null) {
            NumberFormat formatter = NumberFormat.getInstance(Locale.CHINA);
            String euText = formatter.format(recipe.getEuCost()) + " EU";
            guiGraphics.drawString(minecraft.font, euText, 86 - minecraft.font.width(euText) / 2, 65, 0x808080, false);
        }
    }
}