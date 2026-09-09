package com.singularity_iteration.mio_icif.integration.rei.display;

import com.singularity_iteration.mio_icif.integration.rei.category.BlastFurnaceCategory;
import com.singularity_iteration.mio_icif.recipe.blast_furnace.mio_icif_BlastFurnaceRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Collections;

@SuppressWarnings("null")
public class BlastFurnaceDisplay extends BasicDisplay {

    public BlastFurnaceDisplay(RecipeHolder<mio_icif_BlastFurnaceRecipe> recipe) {
        super(
            EntryIngredients.ofIngredients(recipe.value().getIngredients()),
            Collections.singletonList(EntryIngredients.of(recipe.value().getResultItem(registryAccess())))
        );
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return BlastFurnaceCategory.TYPE;
    }
}