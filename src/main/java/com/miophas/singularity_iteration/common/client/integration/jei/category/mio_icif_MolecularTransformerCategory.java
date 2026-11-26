package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipe;
import com.miophas.singularity_iteration.common.client.integration.MachineRecipeViews;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Molecular Transformer: the recipe's EU cost (the time depends on the power fed in). */
@SuppressWarnings("null")
public class mio_icif_MolecularTransformerCategory extends SiItemMachineCategory<mio_icif_MolecularTransformerRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "molecular_transformer");
    public static final RecipeType<RecipeHolder<mio_icif_MolecularTransformerRecipe>> MOLECULAR_TRANSFORMER_TYPE = RecipeType.createRecipeHolderType(UID);

    public mio_icif_MolecularTransformerCategory(IGuiHelper helper) {
        super(helper, MOLECULAR_TRANSFORMER_TYPE, "jei.mio_icif.molecular_transformer", mio_icif_blocks.MOLECULAR_TRANSFORMER.get(), 1);
    }

    @Override
    protected MachineRecipeViews.ItemMachine view(mio_icif_MolecularTransformerRecipe recipe) {
        return MachineRecipeViews.molecularTransformer(recipe);
    }
}
