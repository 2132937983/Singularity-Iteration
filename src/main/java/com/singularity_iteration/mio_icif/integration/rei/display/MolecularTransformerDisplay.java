package com.singularity_iteration.mio_icif.integration.rei.display;

import com.singularity_iteration.mio_icif.integration.rei.category.MolecularTransformerCategory;
import com.singularity_iteration.mio_icif.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Collections;

@SuppressWarnings("null")
public class MolecularTransformerDisplay extends BasicDisplay {

    public MolecularTransformerDisplay(RecipeHolder<mio_icif_MolecularTransformerRecipe> recipe) {
        super(
            EntryIngredients.ofIngredients(recipe.value().getIngredients()),
            Collections.singletonList(EntryIngredients.of(recipe.value().getResultItem(registryAccess())))
        );
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return MolecularTransformerCategory.TYPE;
    }
}