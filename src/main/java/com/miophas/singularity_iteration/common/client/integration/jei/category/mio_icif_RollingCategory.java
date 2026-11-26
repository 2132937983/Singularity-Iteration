package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.recipe.metal_former.rolling.mio_icif_RollingRecipe;
import com.miophas.singularity_iteration.common.client.integration.MachineRecipeViews;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Metal Former, rolling mode: the recipe sets the time and the energy. */
@SuppressWarnings("null")
public class mio_icif_RollingCategory extends SiItemMachineCategory<mio_icif_RollingRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "rolling");
    public static final RecipeType<RecipeHolder<mio_icif_RollingRecipe>> ROLLING_TYPE = RecipeType.createRecipeHolderType(UID);

    public mio_icif_RollingCategory(IGuiHelper helper) {
        super(helper, ROLLING_TYPE, "jei.mio_icif.rolling", mio_icif_blocks.METAL_FORMER.get(), 1);
    }

    @Override
    protected MachineRecipeViews.ItemMachine view(mio_icif_RollingRecipe recipe) {
        return MachineRecipeViews.rolling(recipe);
    }
}
