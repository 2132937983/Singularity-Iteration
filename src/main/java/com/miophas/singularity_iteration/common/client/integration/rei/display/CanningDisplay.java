package com.miophas.singularity_iteration.common.client.integration.rei.display;

import com.miophas.singularity_iteration.common.client.integration.rei.category.CanningCategory;
import com.miophas.singularity_iteration.common.recipe.canner.canning.CanningRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Collections;

@SuppressWarnings("null")
public class CanningDisplay extends BasicDisplay {

    public CanningDisplay(RecipeHolder<CanningRecipe> recipe) {
        super(
            EntryIngredients.ofIngredients(recipe.value().getIngredients()),
            Collections.singletonList(EntryIngredients.of(recipe.value().getResultItem(registryAccess())))
        );
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CanningCategory.TYPE;
    }
}