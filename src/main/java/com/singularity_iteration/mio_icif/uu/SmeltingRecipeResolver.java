package com.singularity_iteration.mio_icif.uu;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 熔配方解析器
 * 解析所有 SmeltingRecipe（包括熔炼、烟熏、高）
 */
public class SmeltingRecipeResolver implements IRecipeResolver {
    private static final double TRANSFORM_COST = 14.0D;
    private final Level level;

    public SmeltingRecipeResolver(Level level) {
        this.level = level;
    }

    @Override
    public List<RecipeTransformation> getTransformations() {
        List<RecipeTransformation> ret = new ArrayList<>();
        RecipeManager recipeManager = level.getRecipeManager();

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (recipe instanceof AbstractCookingRecipe cookingRecipe) {
                if (!UuRecipeWhitelist.isAllowed(recipe.getType())) continue;
                try {
                    ItemStack output = cookingRecipe.getResultItem(level.registryAccess());
                    if (output.isEmpty()) continue;

                    Ingredient input = cookingRecipe.getIngredients().get(0);
                    if (input.isEmpty()) continue;

                    List<List<LeanItemStack>> inputs = convertIngredient(input);
                    if (inputs.isEmpty()) continue;

                    ret.add(new RecipeTransformation(TRANSFORM_COST, inputs, new LeanItemStack(output)));
                } catch (Exception e) {
                    Singularity_Iteration.LOGGER.warn("[UU] Invalid smelting recipe: {}", e.getMessage());
                }
            }
        }

        return ret;
    }

    private List<List<LeanItemStack>> convertIngredient(Ingredient ingredient) {
        ItemStack[] matchingStacks = ingredient.getItems();
        if (matchingStacks.length == 0) return Collections.emptyList();

        List<LeanItemStack> options = new ArrayList<>();
        for (ItemStack stack : matchingStacks) {
            if (!stack.isEmpty()) {
                options.add(new LeanItemStack(stack, 1));
            }
        }
        return options.isEmpty() ? Collections.emptyList() : Collections.singletonList(options);
    }
}