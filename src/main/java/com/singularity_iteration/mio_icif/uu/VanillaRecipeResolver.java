package com.singularity_iteration.mio_icif.uu;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 原版工作台配方解析器
 * 解析所有 CraftingRecipe（包括 shaped 和 shapeless）
 */
public class VanillaRecipeResolver implements IRecipeResolver {
    private static final double TRANSFORM_COST = 1.0D;
    private final Level level;

    public VanillaRecipeResolver(Level level) {
        this.level = level;
    }

    @Override
    public List<RecipeTransformation> getTransformations() {
        List<RecipeTransformation> ret = new ArrayList<>();
        RecipeManager recipeManager = level.getRecipeManager();

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (recipe instanceof CraftingRecipe craftingRecipe) {
                if (!UuRecipeWhitelist.isAllowed(recipe.getType())) continue;
                try {
                    ItemStack output = craftingRecipe.getResultItem(level.registryAccess());
                    if (output.isEmpty()) continue;

                    List<List<LeanItemStack>> inputs = convertIngredients(craftingRecipe.getIngredients());
                    if (inputs.isEmpty()) continue;

                    ret.add(new RecipeTransformation(TRANSFORM_COST, inputs, new LeanItemStack(output)));
                } catch (Exception e) {
                    Singularity_Iteration.LOGGER.warn("[UU] Invalid crafting recipe: {}", e.getMessage());
                }
            }
        }

        return ret;
    }

    private List<List<LeanItemStack>> convertIngredients(List<Ingredient> ingredients) {
        List<List<LeanItemStack>> ret = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) continue;
            ItemStack[] matchingStacks = ingredient.getItems();
            if (matchingStacks.length == 0) continue;

            List<LeanItemStack> options = new ArrayList<>();
            for (ItemStack stack : matchingStacks) {
                if (!stack.isEmpty()) {
                    options.add(new LeanItemStack(stack, 1));
                }
            }
            if (!options.isEmpty()) {
                ret.add(options);
            }
        }
        return ret.isEmpty() ? Collections.emptyList() : ret;
    }
}