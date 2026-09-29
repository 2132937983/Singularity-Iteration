package com.miophas.singularity_iteration.common.client.integration.rei.display;

import com.miophas.singularity_iteration.common.client.integration.rei.category.MixCategory;
import com.miophas.singularity_iteration.common.recipe.canner.mix.MixRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("null")
public class MixDisplay extends BasicDisplay {

    public MixDisplay(RecipeHolder<MixRecipe> recipe) {
        super(
            List.of(EntryIngredients.ofItemStacks(
                Arrays.stream(recipe.value().getMaterialIngredient().getItems())
                    .map(stack -> stack.copyWithCount(recipe.value().getMaterialCount()))
                    .toList())),
            Collections.singletonList(EntryIngredients.of(recipe.value().getDynamicResultCell()))
        );
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return MixCategory.TYPE;
    }
}
