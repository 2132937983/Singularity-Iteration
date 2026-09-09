package com.singularity_iteration.mio_icif.integration.jei.category;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.recipe.canner.empty_to_tank.EmptyToTankRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 装罐�?空罐注液模式 JEI 配方类别
 */
@SuppressWarnings("null")
public class mio_icif_EmptyToTankCategory implements IRecipeCategory<EmptyToTankRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "empty_to_tank");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
            "textures/gui/gui_canner_elc.png");
    public static final RecipeType<EmptyToTankRecipe> EMPTY_TO_TANK_TYPE =
            new RecipeType<>(UID, EmptyToTankRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progressBar;

    public mio_icif_EmptyToTankCategory(IGuiHelper helper) {
        // GUI 尺寸 176x104
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 104);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(mio_icif_blocks.CANNER_ELC.get()));
        // 进度条纹�?(233, 0), 尺寸 24x15
        this.progressBar = helper.createAnimatedDrawable(
                helper.createDrawable(TEXTURE, 233, 0, 24, 15),
                40, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<EmptyToTankRecipe> getRecipeType() {
        return EMPTY_TO_TANK_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mio_icif.empty_to_tank");
    }

    @Override
    public int getWidth() {
        return 176;
    }

    @Override
    public int getHeight() {
        return 104;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EmptyToTankRecipe recipe, IFocusGroup focuses) {
        // 充满流体的单元输入槽 (41, 17), 空单元输出槽 (119, 17) - �?GUI 一�
    builder.addSlot(RecipeIngredientRole.INPUT, 41, 17)
                .addIngredients(recipe.getIngredients().get(0));

        // 空单元输出槽
        builder.addSlot(RecipeIngredientRole.OUTPUT, 119, 17)
                .addItemStack(recipe.getEmptyCellResult());
    }

    @Override
    public void draw(EmptyToTankRecipe recipe, IRecipeSlotsView recipeSlotsView, net.minecraft.client.gui.GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.background.draw(guiGraphics, 0, 0);
        // 进度条位置?(74, 22) �?GUI 一�
    this.progressBar.draw(guiGraphics, 74, 22);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font != null) {
            String timeText = recipe.getProcessingTime() / 20 + "s";
            guiGraphics.drawString(minecraft.font, timeText, 86 - minecraft.font.width(timeText) / 2, 65, 0x808080, false);
        }
    }
}


