package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.recipe.generic.NeutronPolymerizerRecipe;
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

@SuppressWarnings("null")
public class mio_icif_NeutronPolymerizerCategory extends AbstractJeiRecipeCategory<NeutronPolymerizerRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "neutron_polymerizer");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID,
            "textures/gui/gui_neutron_polymerizer.png");
    public static final ResourceLocation ATLAS_TEXTURE = mio_icif_gui_global_variables.ATLAS_TEXTURE;
    public static final RecipeType<RecipeHolder<NeutronPolymerizerRecipe>> NEUTRON_POLYMERIZER_TYPE =
            RecipeType.createRecipeHolderType(UID);

    private final IDrawable background;
    private final IDrawable icon;

    public mio_icif_NeutronPolymerizerCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 7, 7, 162, 72);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(mio_icif_blocks.NEUTRON_POLYMERIZER.get()));
    }

    @Override
    public RecipeType<RecipeHolder<NeutronPolymerizerRecipe>> getRecipeType() {
        return NEUTRON_POLYMERIZER_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mio_icif.neutron_polymerizer");
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
    protected void setRecipeFor(IRecipeLayoutBuilder builder, NeutronPolymerizerRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 49, 10)
                .addIngredients(recipe.getIngredient());

        builder.addSlot(RecipeIngredientRole.OUTPUT, 109, 28)
                .addItemStack(recipe.getResult());
    }

    @Override
    protected void drawFor(NeutronPolymerizerRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.background.draw(guiGraphics, 0, 0);

        int progressX = 72;
        int progressY = 27;
        int progressPixels = mio_icif_gui_global_variables.COMPRESSOR_WORK_PROGRESS_U;

        guiGraphics.blit(ATLAS_TEXTURE, progressX, progressY, 0,
            (float) mio_icif_gui_global_variables.COMPRESSOR_WORK_PROGRESS_BG_X,
            (float) mio_icif_gui_global_variables.COMPRESSOR_WORK_PROGRESS_BG_Y,
            mio_icif_gui_global_variables.COMPRESSOR_WORK_PROGRESS_BG_U,
            mio_icif_gui_global_variables.COMPRESSOR_WORK_PROGRESS_BG_V,
            mio_icif_gui_global_variables.ATLAS_WIDTH,
            mio_icif_gui_global_variables.ATLAS_HEIGHT);

        guiGraphics.blit(ATLAS_TEXTURE, progressX, progressY, 0,
            (float) mio_icif_gui_global_variables.COMPRESSOR_WORK_PROGRESS_X,
            (float) mio_icif_gui_global_variables.COMPRESSOR_WORK_PROGRESS_Y,
            progressPixels,
            mio_icif_gui_global_variables.COMPRESSOR_WORK_PROGRESS_V,
            mio_icif_gui_global_variables.ATLAS_WIDTH,
            mio_icif_gui_global_variables.ATLAS_HEIGHT);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font != null) {
            NumberFormat formatter = NumberFormat.getInstance(Locale.CHINA);
            String euText = formatter.format((long) recipe.getEnergyPerTick() * recipe.getProcessingTime()) + " EU";
            guiGraphics.drawString(minecraft.font, euText, 86 - minecraft.font.width(euText) / 2, 65, 0x808080, false);
        }
    }
}