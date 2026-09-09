package com.singularity_iteration.mio_icif.integration.jei.category;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.recipe.blast_furnace.mio_icif_BlastFurnaceRecipe;
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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null", "deprecation"})
public class mio_icif_BlastFurnaceCategory implements IRecipeCategory<mio_icif_BlastFurnaceRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "blast_furnace");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
            "textures/gui/gui_blast_furnace.png");
    public static final RecipeType<mio_icif_BlastFurnaceRecipe> BLAST_FURNACE_TYPE =
            new RecipeType<>(UID, mio_icif_BlastFurnaceRecipe.class);

    private static final int BG_X = 5;
    private static final int BG_Y = 5;
    private static final int BG_WIDTH = 166;
    private static final int BG_HEIGHT = 78;

    private static final int AIR_BAR_X = 73;
    private static final int AIR_BAR_Y = 32;
    private static final int AIR_BAR_WIDTH = 21;
    private static final int AIR_BAR_HEIGHT = 21;
    private static final int AIR_CAPACITY = 8000;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progressBar;
    private final IDrawableAnimated heatBar;
    private final IDrawableStatic heatOkIcon;
    private final IDrawableStatic airOkIcon;

    public mio_icif_BlastFurnaceCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, BG_X, BG_Y, BG_WIDTH, BG_HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(mio_icif_blocks.BLAST_FURNACE.get()));

        IDrawableStatic progressBg = helper.createDrawable(TEXTURE, 176, 51, 27, 27);
        this.progressBar = helper.createAnimatedDrawable(progressBg,
                100, IDrawableAnimated.StartDirection.BOTTOM, false);

        IDrawableStatic heatBg = helper.createDrawable(TEXTURE, 176, 0, 23, 8);
        this.heatBar = helper.createAnimatedDrawable(heatBg,
                200, IDrawableAnimated.StartDirection.LEFT, false);

        this.heatOkIcon = helper.createDrawable(TEXTURE, 176, 8, 14, 14);
        this.airOkIcon = helper.createDrawable(TEXTURE, 176, 23, 27, 27);
    }

    @Override
    public RecipeType<mio_icif_BlastFurnaceRecipe> getRecipeType() {
        return BLAST_FURNACE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mio_icif.blast_furnace");
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
    public void setRecipe(IRecipeLayoutBuilder builder, mio_icif_BlastFurnaceRecipe recipe, IFocusGroup focuses) {
        int ox = -BG_X;
        int oy = -BG_Y;

        builder.addSlot(RecipeIngredientRole.INPUT, 35 + ox, 33 + oy)
                .addIngredients(recipe.getIngredient());

        builder.addSlot(RecipeIngredientRole.INPUT, 26 + ox, 56 + oy)
                .addItemStack(new ItemStack(mio_icif_cells.CELL_AIR.get()));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 44 + ox, 56 + oy)
                .addItemStack(new ItemStack(mio_icif_cells.CELL_EMPTY.get()));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 134 + ox, 56 + oy)
                .addItemStack(recipe.getResult());

        if (!recipe.getSecondaryResult().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 152 + ox, 56 + oy)
                    .addItemStack(recipe.getSecondaryResult());
        }
    }

    private void renderFluidBar(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                FluidStack fluid, int capacity) {
        if (fluid.isEmpty() || capacity <= 0) return;

        int fillHeight = (int) ((float) fluid.getAmount() / capacity * height);
        if (fillHeight <= 0) return;

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluidType());
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                .apply(extensions.getStillTexture());

        int color = extensions.getTintColor(fluid);
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        int tileSize = sprite.contents().width();
        int fillTop = y + height - fillHeight;

        guiGraphics.setColor(r, g, b, a);
        guiGraphics.enableScissor(x, fillTop, x + width, y + height);

        for (int tileY = fillTop; tileY < y + height; tileY += tileSize) {
            for (int tileX = x; tileX < x + width; tileX += tileSize) {
                guiGraphics.blit(tileX, tileY, 0, tileSize, tileSize, sprite);
            }
        }

        guiGraphics.disableScissor();
        guiGraphics.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public void draw(mio_icif_BlastFurnaceRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.background.draw(guiGraphics, 0, 0);

        int ox = -BG_X;
        int oy = -BG_Y;

        this.heatBar.draw(guiGraphics, 70 + ox, 69 + oy);
        this.heatOkIcon.draw(guiGraphics, 95 + ox, 66 + oy);

        renderFluidBar(guiGraphics, AIR_BAR_X, AIR_BAR_Y, AIR_BAR_WIDTH, AIR_BAR_HEIGHT,
                new FluidStack(com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids.AIR.get(), AIR_CAPACITY),
                AIR_CAPACITY);

        this.airOkIcon.draw(guiGraphics, 75 + ox, 34 + oy);

        this.progressBar.draw(guiGraphics, 75 + ox, 34 + oy);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font != null) {
            String timeText = recipe.getDuration() / 20 + "s";
            guiGraphics.drawString(minecraft.font, timeText,
                    (BG_WIDTH - minecraft.font.width(timeText)) / 2, BG_HEIGHT - 10, 0x808080, false);

            if (recipe.getAirCostPerTick() > 0) {
                String airText = recipe.getAirCostPerTick() + " mB/t";
                guiGraphics.drawString(minecraft.font, airText, 70 + ox, 74 + oy, 0x808080, false);
            }

            if (recipe.getHeatCostPerTick() > 0) {
                String heatText = recipe.getHeatCostPerTick() + " HU/t";
                guiGraphics.drawString(minecraft.font, heatText, 7 + ox, 40 + oy, 0xFF6600, false);
            }
        }
    }
}

