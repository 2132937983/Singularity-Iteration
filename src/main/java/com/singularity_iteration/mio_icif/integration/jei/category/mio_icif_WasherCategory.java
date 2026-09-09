package com.singularity_iteration.mio_icif.integration.jei.category;

import com.singularity_iteration.mio_icif.Blocks.entity.producer.mio_icif_washer_elc;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.recipe.washer.mio_icif_WasherRecipe;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"null", "deprecation"})
public class mio_icif_WasherCategory implements IRecipeCategory<mio_icif_WasherRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "washer");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
            "textures/gui/gui_washer_elc.png");
    public static final RecipeType<mio_icif_WasherRecipe> WASHER_TYPE =
            new RecipeType<>(UID, mio_icif_WasherRecipe.class);

    private static final int BG_X = 5;
    private static final int BG_Y = 5;
    private static final int BG_WIDTH = 166;
    private static final int BG_HEIGHT = 78;

    private static final int FLUID_BAR_X = 59;
    private static final int FLUID_BAR_Y = 19;
    private static final int FLUID_BAR_WIDTH = 12;
    private static final int FLUID_BAR_HEIGHT = 47;
    private static final int FLUID_CAPACITY = mio_icif_washer_elc.FLUID_CAPACITY;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progressBar;
    private final IDrawableAnimated energyBar;
    private final IDrawableStatic fluidTankBg;
    private final IDrawableStatic fluidTankScale;

    public mio_icif_WasherCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, BG_X, BG_Y, BG_WIDTH, BG_HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(mio_icif_blocks.WASHER_ELC.get()));

        this.progressBar = helper.createAnimatedDrawable(
                helper.createDrawable(TEXTURE, 176, 117, 20, 19),
                40, IDrawableAnimated.StartDirection.LEFT, false);

        IDrawableStatic energyBg = helper.createDrawable(TEXTURE, 176, 0, 14, 14);
        this.energyBar = helper.createAnimatedDrawable(energyBg,
                300, IDrawableAnimated.StartDirection.BOTTOM, true);

        this.fluidTankBg = helper.createDrawable(TEXTURE, 176, 15, 20, 55);
        this.fluidTankScale = helper.createDrawable(TEXTURE, 176, 70, 20, 47);
    }

    @Override
    public RecipeType<mio_icif_WasherRecipe> getRecipeType() {
        return WASHER_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mio_icif.washer");
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
    public void setRecipe(IRecipeLayoutBuilder builder, mio_icif_WasherRecipe recipe, IFocusGroup focuses) {
        int ox = -BG_X;
        int oy = -BG_Y;

        builder.addSlot(RecipeIngredientRole.INPUT, 104 + ox, 17 + oy)
                .addIngredients(recipe.getIngredients().get(0));

        builder.addSlot(RecipeIngredientRole.INPUT, 38 + ox, 17 + oy)
                .addItemStack(new ItemStack(Items.WATER_BUCKET));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 86 + ox, 62 + oy)
                .addItemStack(new ItemStack(recipe.getPrimaryResult().getItem(), recipe.getPrimaryCount()));

        if (recipe.getSecondaryCount() > 0) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 104 + ox, 62 + oy)
                    .addItemStack(new ItemStack(recipe.getSecondaryResult().getItem(), recipe.getSecondaryCount()));
        }

        if (recipe.getTertiaryCount() > 0) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 122 + ox, 62 + oy)
                    .addItemStack(new ItemStack(recipe.getTertiaryResult().getItem(), recipe.getTertiaryCount()));
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
    public void draw(mio_icif_WasherRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.background.draw(guiGraphics, 0, 0);

        int ox = -BG_X;
        int oy = -BG_Y;

        this.fluidTankBg.draw(guiGraphics, 55 + ox, 15 + oy);

        renderFluidBar(guiGraphics, FLUID_BAR_X, FLUID_BAR_Y, FLUID_BAR_WIDTH, FLUID_BAR_HEIGHT,
                new FluidStack(Fluids.WATER, recipe.getWaterAmount() > 0 ? FLUID_CAPACITY : 0),
                FLUID_CAPACITY);

        this.fluidTankScale.draw(guiGraphics, 59 + ox, 19 + oy);

        this.energyBar.draw(guiGraphics, 4 + ox, 38 + oy);

        this.progressBar.draw(guiGraphics, 97 + ox, 33 + oy);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font != null) {
            String timeText = recipe.getProcessingTime() / 20 + "s";
            guiGraphics.drawString(minecraft.font, timeText,
                    (BG_WIDTH - minecraft.font.width(timeText)) / 2, BG_HEIGHT - 10, 0x808080, false);

            String euText = recipe.getEnergyPerTick() + " EU/t";
            guiGraphics.drawString(minecraft.font, euText, 2, 35, 0x808080, false);

            if (recipe.getWaterAmount() > 0) {
                String waterText = recipe.getWaterAmount() + " mB";
                guiGraphics.drawString(minecraft.font, waterText, 50, 71, 0x4040FF, false);
            }
        }
    }
}

