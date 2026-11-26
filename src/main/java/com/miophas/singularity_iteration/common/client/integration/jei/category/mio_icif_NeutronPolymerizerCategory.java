package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.recipe.generic.NeutronPolymerizerRecipe;
import com.miophas.singularity_iteration.common.client.integration.MachineRecipeViews;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Neutron Polymerizer: the machine runs every recipe for its own time and energy. */
@SuppressWarnings("null")
public class mio_icif_NeutronPolymerizerCategory extends SiItemMachineCategory<NeutronPolymerizerRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "neutron_polymerizer");
    public static final RecipeType<RecipeHolder<NeutronPolymerizerRecipe>> NEUTRON_POLYMERIZER_TYPE = RecipeType.createRecipeHolderType(UID);

    public mio_icif_NeutronPolymerizerCategory(IGuiHelper helper) {
        super(helper, NEUTRON_POLYMERIZER_TYPE, "jei.mio_icif.neutron_polymerizer", mio_icif_blocks.NEUTRON_POLYMERIZER.get(), 2);
    }

    @Override
    protected MachineRecipeViews.ItemMachine view(NeutronPolymerizerRecipe recipe) {
        return MachineRecipeViews.neutronPolymerizer(recipe);
    }
}
