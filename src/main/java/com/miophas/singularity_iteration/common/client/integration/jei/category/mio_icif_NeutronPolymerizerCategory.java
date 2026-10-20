package com.miophas.singularity_iteration.common.client.integration.jei.category;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;
import com.miophas.singularity_iteration.common.recipe.generic.NeutronPolymerizerRecipe;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/** Neutron Polymerizer: the machine runs every recipe for its own time and energy. */
@SuppressWarnings("null")
public class mio_icif_NeutronPolymerizerCategory extends SiItemMachineCategory<NeutronPolymerizerRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "neutron_polymerizer");
    public static final RecipeType<RecipeHolder<NeutronPolymerizerRecipe>> NEUTRON_POLYMERIZER_TYPE = RecipeType.createRecipeHolderType(UID);

    public mio_icif_NeutronPolymerizerCategory(IGuiHelper helper) {
        super(helper, NEUTRON_POLYMERIZER_TYPE, "jei.mio_icif.neutron_polymerizer", mio_icif_blocks.NEUTRON_POLYMERIZER.get(), 2);
    }

    @Override
    protected List<ItemStack> inputs(NeutronPolymerizerRecipe recipe) {
        return SiJei.withCount(recipe.getIngredient(), 1);
    }

    @Override
    protected ItemStack result(NeutronPolymerizerRecipe recipe) {
        return recipe.getResult();
    }

    @Override
    protected int ticks(NeutronPolymerizerRecipe recipe) {
        return (int) com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_neutron_polymerizer.DEFAULT_WORK_TIME;
    }

    @Override
    protected long euPerTick(NeutronPolymerizerRecipe recipe) {
        return com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_neutron_polymerizer.DEFAULT_ENERGY_PER_TICK;
    }

    /** The total does not fit on the time and energy line. */
    @Override
    protected List<Component> lines(NeutronPolymerizerRecipe recipe) {
        return List.of(SiJei.rate(ticks(recipe), euPerTick(recipe)), SiJei.total((long) ticks(recipe) * euPerTick(recipe)));
    }
}
