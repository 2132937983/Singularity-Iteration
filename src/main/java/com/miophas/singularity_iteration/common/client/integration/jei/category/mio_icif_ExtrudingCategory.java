package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.recipe.metal_former.extruding.mio_icif_ExtrudingRecipe;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/** Metal Former, extruding mode: the recipe sets the time and the energy. */
@SuppressWarnings("null")
public class mio_icif_ExtrudingCategory extends SiItemMachineCategory<mio_icif_ExtrudingRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "extruding");
    public static final RecipeType<RecipeHolder<mio_icif_ExtrudingRecipe>> EXTRUDING_TYPE = RecipeType.createRecipeHolderType(UID);

    public mio_icif_ExtrudingCategory(IGuiHelper helper) {
        super(helper, EXTRUDING_TYPE, "jei.mio_icif.extruding", mio_icif_blocks.METAL_FORMER.get(), 1);
    }

    @Override
    protected List<ItemStack> inputs(mio_icif_ExtrudingRecipe recipe) {
        return SiJei.withCount(recipe.getIngredient(), recipe.getIngredientCount());
    }

    @Override
    protected ItemStack result(mio_icif_ExtrudingRecipe recipe) {
        return recipe.getResult();
    }

    @Override
    protected int ticks(mio_icif_ExtrudingRecipe recipe) {
        return (int) recipe.getProcessingTime();
    }

    @Override
    protected long euPerTick(mio_icif_ExtrudingRecipe recipe) {
        return recipe.getEnergyPerTick();
    }
}
