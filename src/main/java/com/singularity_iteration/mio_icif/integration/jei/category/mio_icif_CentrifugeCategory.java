package com.singularity_iteration.mio_icif.integration.jei.category;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.recipe.centrifuge.mio_icif_CentrifugeRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
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

@SuppressWarnings("null")
public class mio_icif_CentrifugeCategory implements IRecipeCategory<mio_icif_CentrifugeRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "centrifuge");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
            "textures/gui/gui_centrifuge_elc.png");
    public static final RecipeType<mio_icif_CentrifugeRecipe> CENTRIFUGE_TYPE =
            new RecipeType<>(UID, mio_icif_CentrifugeRecipe.class);

    private static final int BG_X = 5;
    private static final int BG_Y = 5;
    private static final int BG_WIDTH = 166;
    private static final int BG_HEIGHT = 78;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progressBar;
    private final IDrawableAnimated energyBar;
    private final IDrawableAnimated heatBar;
    private final IDrawableStatic greenLight;

    public mio_icif_CentrifugeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, BG_X, BG_Y, BG_WIDTH, BG_HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(mio_icif_blocks.CENTRIFUGE_ELC.get()));

        IDrawableStatic progressBg = helper.createDrawable(TEXTURE, 176, 50, 5, 30);
        this.progressBar = helper.createAnimatedDrawable(progressBg,
                40, IDrawableAnimated.StartDirection.BOTTOM, false);

        IDrawableStatic energyBg = helper.createDrawable(TEXTURE, 176, 0, 14, 15);
        this.energyBar = helper.createAnimatedDrawable(energyBg,
                300, IDrawableAnimated.StartDirection.BOTTOM, true);

        IDrawableStatic heatBg = helper.createDrawable(TEXTURE, 176, 28, 23, 8);
        this.heatBar = helper.createAnimatedDrawable(heatBg,
                200, IDrawableAnimated.StartDirection.LEFT, false);

        this.greenLight = helper.createDrawable(TEXTURE, 176, 36, 14, 14);
    }

    @Override
    public RecipeType<mio_icif_CentrifugeRecipe> getRecipeType() {
        return CENTRIFUGE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mio_icif.centrifuge");
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
    public void setRecipe(IRecipeLayoutBuilder builder, mio_icif_CentrifugeRecipe recipe, IFocusGroup focuses) {
        int ox = -BG_X;
        int oy = -BG_Y;

        builder.addSlot(RecipeIngredientRole.INPUT, 11 + ox, 21 + oy)
                .addIngredients(recipe.getIngredients().get(0));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 124 + ox, 18 + oy)
                .addItemStack(new ItemStack(recipe.getPrimaryResult().getItem(), recipe.getPrimaryCount()));

        if (recipe.getSecondaryCount() > 0) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 124 + ox, 36 + oy)
                    .addItemStack(new ItemStack(recipe.getSecondaryResult().getItem(), recipe.getSecondaryCount()));
        }

        if (recipe.getTertiaryCount() > 0) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 124 + ox, 54 + oy)
                    .addItemStack(new ItemStack(recipe.getTertiaryResult().getItem(), recipe.getTertiaryCount()));
        }
    }

    @Override
    public void draw(mio_icif_CentrifugeRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.background.draw(guiGraphics, 0, 0);

        int ox = -BG_X;
        int oy = -BG_Y;

        this.energyBar.draw(guiGraphics, 12 + ox, 41 + oy);

        this.heatBar.draw(guiGraphics, 67 + ox, 65 + oy);

        this.greenLight.draw(guiGraphics, 92 + ox, 62 + oy);

        this.progressBar.draw(guiGraphics, 83 + ox, 24 + oy);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font != null) {
            String timeText = recipe.getProcessingTime() / 20 + "s";
            guiGraphics.drawString(minecraft.font, timeText,
                    (BG_WIDTH - minecraft.font.width(timeText)) / 2, BG_HEIGHT - 10, 0x808080, false);

            String euText = recipe.getEnergyPerTick() + " EU/t";
            guiGraphics.drawString(minecraft.font, euText, 2, 35, 0x808080, false);

            if (recipe.getMinHeatRequired() > 0) {
                String heatText = recipe.getMinHeatRequired() + " HU";
                guiGraphics.drawString(minecraft.font, heatText, 62, 70, 0xFF6600, false);
            }
        }
    }
}

