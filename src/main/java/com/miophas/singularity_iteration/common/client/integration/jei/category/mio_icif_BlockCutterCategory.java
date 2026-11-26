package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.recipe.block_cutter.mio_icif_BlockCutterRecipe;
import com.miophas.singularity_iteration.common.client.integration.MachineRecipeViews;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Block Cutter: the machine's time and energy, and the blade hardness the recipe needs. */
@SuppressWarnings("null")
public class mio_icif_BlockCutterCategory extends SiItemMachineCategory<mio_icif_BlockCutterRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block_cutter");
    public static final RecipeType<RecipeHolder<mio_icif_BlockCutterRecipe>> BLOCK_CUTTER_TYPE = RecipeType.createRecipeHolderType(UID);

    public mio_icif_BlockCutterCategory(IGuiHelper helper) {
        super(helper, BLOCK_CUTTER_TYPE, "jei.mio_icif.block_cutter", mio_icif_blocks.BLOCK_CUTTER.get(), 2);
    }

    @Override
    protected MachineRecipeViews.ItemMachine view(mio_icif_BlockCutterRecipe recipe) {
        return MachineRecipeViews.blockCutter(recipe);
    }
}
