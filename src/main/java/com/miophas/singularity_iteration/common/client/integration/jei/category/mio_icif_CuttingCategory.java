package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.recipe.metal_former.cutting.mio_icif_CuttingRecipe;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/** Metal Former, cutting mode: the recipe sets the time and the energy. */
@SuppressWarnings("null")
public class mio_icif_CuttingCategory extends SiItemMachineCategory<mio_icif_CuttingRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "cutting");
    public static final RecipeType<RecipeHolder<mio_icif_CuttingRecipe>> CUTTING_TYPE = RecipeType.createRecipeHolderType(UID);

    public mio_icif_CuttingCategory(IGuiHelper helper) {
        super(helper, CUTTING_TYPE, "jei.mio_icif.cutting", mio_icif_blocks.METAL_FORMER.get(), 1);
    }

    @Override
    protected List<ItemStack> inputs(mio_icif_CuttingRecipe recipe) {
        return SiJei.withCount(recipe.getIngredient(), recipe.getIngredientCount());
    }

    @Override
    protected ItemStack result(mio_icif_CuttingRecipe recipe) {
        return recipe.getResult();
    }

    @Override
    protected int ticks(mio_icif_CuttingRecipe recipe) {
        return (int) recipe.getProcessingTime();
    }

    @Override
    protected long euPerTick(mio_icif_CuttingRecipe recipe) {
        return recipe.getEnergyPerTick();
    }
}
