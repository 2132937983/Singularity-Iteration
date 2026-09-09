package com.singularity_iteration.mio_icif.integration.jei.category;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.recipe.fluid_refining.FluidRefiningRecipe;
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
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_FluidRefiningCategory implements IRecipeCategory<FluidRefiningRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "fluid_refining");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
            "textures/gui/gui_oil_refinery_elc.png");
    public static final RecipeType<FluidRefiningRecipe> FLUID_REFINING_TYPE =
            new RecipeType<>(UID, FluidRefiningRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public mio_icif_FluidRefiningCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 7, 7, 162, 72);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(mio_icif_blocks.OIL_REFINERY_ELC.get()));
    }

    @Override
    public RecipeType<FluidRefiningRecipe> getRecipeType() {
        return FLUID_REFINING_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mio_icif.fluid_refining");
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
    public void setRecipe(IRecipeLayoutBuilder builder, FluidRefiningRecipe recipe, IFocusGroup focuses) {
        int offsetX = -7;
        int offsetY = -7;

        FluidStack inputFluid = new FluidStack(recipe.getInputFluid(), 1000);
        builder.addSlot(RecipeIngredientRole.INPUT, 45 + offsetX, 13 + offsetY)
                .addFluidStack(inputFluid.getFluid(), inputFluid.getAmount())
                .setFluidRenderer(10000, false, 20, 55);

        FluidStack outputFluid = recipe.getOutputFluid();
        builder.addSlot(RecipeIngredientRole.OUTPUT, 103 + offsetX, 13 + offsetY)
                .addFluidStack(outputFluid.getFluid(), Math.max(outputFluid.getAmount(), 1000))
                .setFluidRenderer(10000, false, 20, 55);
    }

    @Override
    public void draw(FluidRefiningRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.background.draw(guiGraphics, 0, 0);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font != null) {
            String timeText = recipe.getProcessingTime() / 20 + "s";
            guiGraphics.drawString(minecraft.font, timeText, 86 - minecraft.font.width(timeText) / 2, 65, 0x808080, false);
        }
    }
}