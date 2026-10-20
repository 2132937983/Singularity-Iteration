package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.recipe.block_cutter.mio_icif_BlockCutterRecipe;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/** Block Cutter: the machine's time and energy, and the blade hardness the recipe needs. */
@SuppressWarnings("null")
public class mio_icif_BlockCutterCategory extends SiItemMachineCategory<mio_icif_BlockCutterRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "block_cutter");
    public static final RecipeType<RecipeHolder<mio_icif_BlockCutterRecipe>> BLOCK_CUTTER_TYPE = RecipeType.createRecipeHolderType(UID);

    public mio_icif_BlockCutterCategory(IGuiHelper helper) {
        super(helper, BLOCK_CUTTER_TYPE, "jei.mio_icif.block_cutter", mio_icif_blocks.BLOCK_CUTTER.get(), 2);
    }

    @Override
    protected List<ItemStack> inputs(mio_icif_BlockCutterRecipe recipe) {
        return SiJei.withCount(recipe.getIngredient(), recipe.getIngredientCount());
    }

    @Override
    protected ItemStack result(mio_icif_BlockCutterRecipe recipe) {
        return recipe.getResult();
    }

    @Override
    protected int ticks(mio_icif_BlockCutterRecipe recipe) {
        return (int) com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_block_cutter.MAX_PROGRESS;
    }

    @Override
    protected long euPerTick(mio_icif_BlockCutterRecipe recipe) {
        return com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_block_cutter.ENERGY_PER_TICK;
    }

    @Override
    protected List<Component> lines(mio_icif_BlockCutterRecipe recipe) {
        return List.of(SiJei.cost(ticks(recipe), euPerTick(recipe)), Component.translatable("jei.mio_icif.block_cutter.blade", recipe.getHardness()));
    }
}
