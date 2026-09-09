package com.singularity_iteration.mio_icif.client;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.recipe.generic.NeutronPolymerizerRecipes;
import com.singularity_iteration.mio_icif.recipe.mio_icif_ModRecipes;
import net.minecraft.client.RecipeBookCategories;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRecipeBookCategoriesEvent;

@EventBusSubscriber(modid = Singularity_Iteration.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings("null")
public class mio_icif_RecipeBookCategories {

    @SubscribeEvent
    public static void onRegisterRecipeBookCategories(RegisterRecipeBookCategoriesEvent event) {
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.POWDER_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.WASHER_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.CENTRIFUGE_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.EXTRACTOR_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.COMPRESSOR_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.CANNING_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.EMPTY_TO_TANK_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.FILL_FROM_TANK_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.MIX_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.DYNAMIC_CANNING_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.CONDENSATOR_REPAIR_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.BLOCK_CUTTER_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.ROLLING_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.CUTTING_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.EXTRUDING_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.BLAST_FURNACE_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(mio_icif_ModRecipes.MOLECULAR_TRANSFORMER_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
        event.registerRecipeCategoryFinder(NeutronPolymerizerRecipes.NEUTRON_POLYMERIZER_TYPE.get(), holder -> RecipeBookCategories.CRAFTING_MISC);
    }
}