package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.recipe.mio_icif_PowderRecipe;
import com.miophas.singularity_iteration.common.client.integration.MachineRecipeViews;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Macerator: 2 EU/t; 0.1.11.108: the time follows the input's Bond work index (synced material table), or the recipe's
 * own time when it sets one.
 */
@SuppressWarnings("null")
public class mio_icif_PowderCategory extends SiItemMachineCategory<mio_icif_PowderRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "powder");
    public static final RecipeType<RecipeHolder<mio_icif_PowderRecipe>> POWDER_TYPE = RecipeType.createRecipeHolderType(UID);

    public mio_icif_PowderCategory(IGuiHelper helper) {
        super(helper, POWDER_TYPE, "jei.mio_icif.powder", mio_icif_blocks.POWDER_ELC.get(), 1);
    }

    @Override
    protected MachineRecipeViews.ItemMachine view(mio_icif_PowderRecipe recipe) {
        return MachineRecipeViews.powder(recipe);
    }
}
