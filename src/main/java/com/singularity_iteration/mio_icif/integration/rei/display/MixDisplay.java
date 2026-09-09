package com.singularity_iteration.mio_icif.integration.rei.display;

import com.singularity_iteration.mio_icif.integration.rei.category.MixCategory;
import com.singularity_iteration.mio_icif.recipe.canner.mix.MixRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Collections;

@SuppressWarnings("null")
public class MixDisplay extends BasicDisplay {

    public MixDisplay(RecipeHolder<MixRecipe> recipe) {
        super(
            EntryIngredients.ofIngredients(recipe.value().getIngredients()),
            Collections.singletonList(EntryIngredients.of(recipe.value().getDynamicResultCell()))
        );
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return MixCategory.TYPE;
    }
}