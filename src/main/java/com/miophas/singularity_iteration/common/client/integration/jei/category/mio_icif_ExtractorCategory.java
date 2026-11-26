package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.recipe.extractor.mio_icif_ExtractorRecipe;
import com.miophas.singularity_iteration.common.client.integration.MachineRecipeViews;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Extractor: the machine runs every recipe for its own time and energy (IC2: 300 ticks, 2 EU/t). */
@SuppressWarnings("null")
public class mio_icif_ExtractorCategory extends SiItemMachineCategory<mio_icif_ExtractorRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "extractor");
    public static final RecipeType<RecipeHolder<mio_icif_ExtractorRecipe>> EXTRACTOR_TYPE = RecipeType.createRecipeHolderType(UID);

    public mio_icif_ExtractorCategory(IGuiHelper helper) {
        super(helper, EXTRACTOR_TYPE, "jei.mio_icif.extractor", mio_icif_blocks.EXTRACTOR_ELC.get(), 1);
    }

    @Override
    protected MachineRecipeViews.ItemMachine view(mio_icif_ExtractorRecipe recipe) {
        return MachineRecipeViews.extractor(recipe);
    }
}
