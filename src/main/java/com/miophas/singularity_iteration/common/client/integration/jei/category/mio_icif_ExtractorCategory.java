package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.recipe.extractor.mio_icif_ExtractorRecipe;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/** Extractor: the machine runs every recipe for its own time and energy (IC2: 300 ticks, 2 EU/t). */
@SuppressWarnings("null")
public class mio_icif_ExtractorCategory extends SiItemMachineCategory<mio_icif_ExtractorRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "extractor");
    public static final RecipeType<RecipeHolder<mio_icif_ExtractorRecipe>> EXTRACTOR_TYPE = RecipeType.createRecipeHolderType(UID);

    public mio_icif_ExtractorCategory(IGuiHelper helper) {
        super(helper, EXTRACTOR_TYPE, "jei.mio_icif.extractor", mio_icif_blocks.EXTRACTOR_ELC.get(), 1);
    }

    @Override
    protected List<ItemStack> inputs(mio_icif_ExtractorRecipe recipe) {
        return SiJei.withCount(recipe.getIngredient(), 1);
    }

    @Override
    protected ItemStack result(mio_icif_ExtractorRecipe recipe) {
        return recipe.getResult();
    }

    @Override
    protected int ticks(mio_icif_ExtractorRecipe recipe) {
        return (int) com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_extrator_elc.DEFAULT_WORK_TIME;
    }

    @Override
    protected long euPerTick(mio_icif_ExtractorRecipe recipe) {
        return com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_extrator_elc.DEFAULT_ENERGY_PER_TICK;
    }
}
