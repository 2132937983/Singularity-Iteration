package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.recipe.mio_icif_PowderRecipe;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/** Macerator: the machine runs every recipe for its own time and energy (IC2: 300 ticks, 2 EU/t). */
@SuppressWarnings("null")
public class mio_icif_PowderCategory extends SiItemMachineCategory<mio_icif_PowderRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "powder");
    public static final RecipeType<RecipeHolder<mio_icif_PowderRecipe>> POWDER_TYPE = RecipeType.createRecipeHolderType(UID);

    public mio_icif_PowderCategory(IGuiHelper helper) {
        super(helper, POWDER_TYPE, "jei.mio_icif.powder", mio_icif_blocks.POWDER_ELC.get(), 1);
    }

    @Override
    protected List<ItemStack> inputs(mio_icif_PowderRecipe recipe) {
        return SiJei.withCount(recipe.getIngredient(), recipe.getIngredientCount());
    }

    @Override
    protected ItemStack result(mio_icif_PowderRecipe recipe) {
        return recipe.getResult();
    }

    @Override
    protected int ticks(mio_icif_PowderRecipe recipe) {
        return (int) com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_powder_elc.DEFAULT_WORK_TIME;
    }

    @Override
    protected long euPerTick(mio_icif_PowderRecipe recipe) {
        return com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_powder_elc.DEFAULT_ENERGY_PER_TICK;
    }
}
