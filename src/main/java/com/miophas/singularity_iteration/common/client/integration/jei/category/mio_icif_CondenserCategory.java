package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
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

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_CondenserCategory implements IRecipeCategory<mio_icif_CondenserCategory.CondenserRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "condenser");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
            "textures/gui/gui_condenser_elc.png");
    public static final RecipeType<CondenserRecipe> CONDENSER_TYPE =
            new RecipeType<>(UID, CondenserRecipe.class);

    private static final int BG_X = 0;
    private static final int BG_Y = 0;
    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 95;

    private static final int STEAM_TANK_X = 46;
    private static final int STEAM_TANK_Y = 27;
    private static final int STEAM_TANK_W = 84;
    private static final int STEAM_TANK_H = 33;

    private static final int DISTILLED_TANK_X = 46;
    private static final int DISTILLED_TANK_Y = 74;
    private static final int DISTILLED_TANK_W = 84;
    private static final int DISTILLED_TANK_H = 15;

    private final IDrawable background;
    private final IDrawable icon;

    public record CondenserRecipe(FluidStack input, FluidStack output, int processingTime) {}

    public mio_icif_CondenserCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, BG_X, BG_Y, BG_WIDTH, BG_HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(mio_icif_blocks.CONDENSER.get()));
    }

    @Override
    public RecipeType<CondenserRecipe> getRecipeType() {
        return CONDENSER_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mio_icif.condenser");
    }

    @Override
    public int getWidth() {
        return BG_WIDTH;
    }

    @Override
    public int getHeight() {
        return BG_HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CondenserRecipe recipe, IFocusGroup focuses) {
        int offsetX = -BG_X;
        int offsetY = -BG_Y;

        builder.addSlot(RecipeIngredientRole.INPUT, STEAM_TANK_X + offsetX, STEAM_TANK_Y + offsetY)
                .addFluidStack(recipe.input().getFluid(), recipe.input().getAmount())
                .setFluidRenderer(10000, false, STEAM_TANK_W, STEAM_TANK_H);

        builder.addSlot(RecipeIngredientRole.OUTPUT, DISTILLED_TANK_X + offsetX, DISTILLED_TANK_Y + offsetY)
                .addFluidStack(recipe.output().getFluid(), recipe.output().getAmount())
                .setFluidRenderer(10000, false, DISTILLED_TANK_W, DISTILLED_TANK_H);
    }

    @Override
    public void draw(CondenserRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.background.draw(guiGraphics, 0, 0);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font != null) {
            String timeText = recipe.processingTime() / 20 + "s";
            guiGraphics.drawString(minecraft.font, timeText, 86 - minecraft.font.width(timeText) / 2, 62, 0x808080, false);
        }
    }

    public static List<CondenserRecipe> createRecipes() {
        return List.of(
                new CondenserRecipe(
                        new FluidStack(mio_icif_fluids.STEAM.get(), 1000),
                        new FluidStack(mio_icif_fluids.DISTILLEDWATER.get(), 1000),
                        1
                ),
                new CondenserRecipe(
                        new FluidStack(mio_icif_fluids.SUPERHEATEDSTEAM.get(), 1000),
                        new FluidStack(mio_icif_fluids.DISTILLEDWATER.get(), 1000),
                        1
                )
        );
    }
}