package com.miophas.singularity_iteration.common.client.integration.rei.display;

import com.miophas.singularity_iteration.common.client.integration.rei.category.PowderCategory;
import com.miophas.singularity_iteration.common.recipe.mio_icif_PowderRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("null")
public class PowderDisplay extends BasicDisplay {

    public PowderDisplay(RecipeHolder<mio_icif_PowderRecipe> recipe) {
        super(
            List.of(EntryIngredients.ofItemStacks(
                Arrays.stream(recipe.value().getIngredient().getItems())
                    .map(stack -> stack.copyWithCount(recipe.value().getIngredientCount()))
                    .toList())),
            Collections.singletonList(EntryIngredients.of(recipe.value().getResultItem(registryAccess())))
        );
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return PowderCategory.TYPE;
    }
}
