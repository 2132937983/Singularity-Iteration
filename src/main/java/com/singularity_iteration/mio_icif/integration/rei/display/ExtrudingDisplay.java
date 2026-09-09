package com.singularity_iteration.mio_icif.integration.rei.display;

import com.singularity_iteration.mio_icif.integration.rei.category.ExtrudingCategory;
import com.singularity_iteration.mio_icif.recipe.metal_former.extruding.mio_icif_ExtrudingRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Collections;

@SuppressWarnings("null")
public class ExtrudingDisplay extends BasicDisplay {

    public ExtrudingDisplay(RecipeHolder<mio_icif_ExtrudingRecipe> recipe) {
        super(
            EntryIngredients.ofIngredients(recipe.value().getIngredients()),
            Collections.singletonList(EntryIngredients.of(recipe.value().getResultItem(registryAccess())))
        );
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return ExtrudingCategory.TYPE;
    }
}