package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.recipe.compressor.mio_icif_CompressorRecipe;
import com.miophas.singularity_iteration.common.client.integration.MachineRecipeViews;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Compressor: the recipe sets the time and the energy. */
@SuppressWarnings("null")
public class mio_icif_CompressorCategory extends SiItemMachineCategory<mio_icif_CompressorRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "compressor");
    public static final RecipeType<RecipeHolder<mio_icif_CompressorRecipe>> COMPRESSOR_TYPE = RecipeType.createRecipeHolderType(UID);

    public mio_icif_CompressorCategory(IGuiHelper helper) {
        super(helper, COMPRESSOR_TYPE, "jei.mio_icif.compressor", mio_icif_blocks.COMPRESSOR_ELC.get(), 1);
    }

    @Override
    protected MachineRecipeViews.ItemMachine view(mio_icif_CompressorRecipe recipe) {
        return MachineRecipeViews.compressor(recipe);
    }
}
