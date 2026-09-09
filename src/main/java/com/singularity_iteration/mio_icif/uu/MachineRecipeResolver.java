package com.singularity_iteration.mio_icif.uu;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.recipe.blast_furnace.mio_icif_BlastFurnaceRecipe;
import com.singularity_iteration.mio_icif.recipe.centrifuge.mio_icif_CentrifugeRecipe;
import com.singularity_iteration.mio_icif.recipe.compressor.mio_icif_CompressorRecipe;
import com.singularity_iteration.mio_icif.recipe.extractor.mio_icif_ExtractorRecipe;
import com.singularity_iteration.mio_icif.recipe.generic.AbstractSingleInputRecipe;
import com.singularity_iteration.mio_icif.recipe.metal_former.cutting.mio_icif_CuttingRecipe;
import com.singularity_iteration.mio_icif.recipe.metal_former.extruding.mio_icif_ExtrudingRecipe;
import com.singularity_iteration.mio_icif.recipe.metal_former.rolling.mio_icif_RollingRecipe;
import com.singularity_iteration.mio_icif.recipe.mio_icif_PowderRecipe;
import com.singularity_iteration.mio_icif.recipe.molecular_transformer.mio_icif_MolecularTransformerRecipe;
import com.singularity_iteration.mio_icif.recipe.washer.mio_icif_WasherRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MachineRecipeResolver implements IRecipeResolver {
    private static final double TRANSFORM_COST = 14.0D;
    private final Level level;

    public MachineRecipeResolver(Level level) {
        this.level = level;
    }

    @Override
    public List<RecipeTransformation> getTransformations() {
        List<RecipeTransformation> ret = new ArrayList<>();
        RecipeManager recipeManager = level.getRecipeManager();

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            Recipe<?> recipe = holder.value();

            if (!UuRecipeWhitelist.isAllowed(recipe.getType())) continue;

            try {
                if (recipe instanceof mio_icif_PowderRecipe r) {
                    addSingleInputSingleOutput(r.getIngredients(), r.getResultItem(level.registryAccess()), ret);
                } else if (recipe instanceof mio_icif_WasherRecipe r) {
                    addWasherRecipe(r, ret);
                } else if (recipe instanceof mio_icif_CentrifugeRecipe r) {
                    addCentrifugeRecipe(r, ret);
                } else if (recipe instanceof mio_icif_CompressorRecipe r) {
                    addSingleInputSingleOutput(r.getIngredients(), r.getResultItem(level.registryAccess()), ret);
                } else if (recipe instanceof mio_icif_ExtractorRecipe r) {
                    addSingleInputSingleOutput(r.getIngredients(), r.getResultItem(level.registryAccess()), ret);
                } else if (recipe instanceof mio_icif_BlastFurnaceRecipe r) {
                    addBlastFurnaceRecipe(r, ret);
                } else if (recipe instanceof mio_icif_ExtrudingRecipe r) {
                    addSingleInputSingleOutput(r.getIngredients(), r.getResultItem(level.registryAccess()), ret);
                } else if (recipe instanceof mio_icif_RollingRecipe r) {
                    addSingleInputSingleOutput(r.getIngredients(), r.getResultItem(level.registryAccess()), ret);
                } else if (recipe instanceof mio_icif_CuttingRecipe r) {
                    addSingleInputSingleOutput(r.getIngredients(), r.getResultItem(level.registryAccess()), ret);
                } else if (recipe instanceof mio_icif_MolecularTransformerRecipe r) {
                    addSingleInputSingleOutput(r.getIngredients(), r.getResultItem(level.registryAccess()), ret);
                } else if (recipe instanceof AbstractSingleInputRecipe<?> r) {
                    addSingleInputSingleOutput(r.getIngredients(), r.getResultItem(level.registryAccess()), ret);
                }
            } catch (Exception e) {
                Singularity_Iteration.LOGGER.warn("[UU] Invalid machine recipe: {}", e.getMessage());
            }
        }

        Singularity_Iteration.LOGGER.debug("[UU] MachineRecipeResolver found {} transformations.", ret.size());
        return ret;
    }

    private void addSingleInputSingleOutput(NonNullList<Ingredient> ingredients, ItemStack output, List<RecipeTransformation> ret) {
        if (output.isEmpty()) return;
        if (ingredients.isEmpty() || ingredients.get(0).isEmpty()) return;

        List<List<LeanItemStack>> inputs = convertIngredient(ingredients.get(0));
        if (inputs.isEmpty()) return;

        ret.add(new RecipeTransformation(TRANSFORM_COST, inputs, new LeanItemStack(output)));
    }

    private void addWasherRecipe(mio_icif_WasherRecipe r, List<RecipeTransformation> ret) {
        Ingredient input = r.getIngredients().get(0);
        if (input.isEmpty()) return;

        List<List<LeanItemStack>> inputs = convertIngredient(input);
        if (inputs.isEmpty()) return;

        List<LeanItemStack> outputs = new ArrayList<>();
        ItemStack primary = r.getPrimaryResult().copy();
        primary.setCount(r.getPrimaryCount());
        if (!primary.isEmpty()) outputs.add(new LeanItemStack(primary));
        ItemStack secondary = r.getSecondaryResult().copy();
        secondary.setCount(r.getSecondaryCount());
        if (!secondary.isEmpty()) outputs.add(new LeanItemStack(secondary));
        ItemStack tertiary = r.getTertiaryResult().copy();
        tertiary.setCount(r.getTertiaryCount());
        if (!tertiary.isEmpty()) outputs.add(new LeanItemStack(tertiary));
        if (outputs.isEmpty()) return;

        ret.add(new RecipeTransformation(TRANSFORM_COST, inputs, outputs));
    }

    private void addCentrifugeRecipe(mio_icif_CentrifugeRecipe r, List<RecipeTransformation> ret) {
        Ingredient input = r.getIngredients().get(0);
        if (input.isEmpty()) return;

        List<List<LeanItemStack>> inputs = convertIngredient(input);
        if (inputs.isEmpty()) return;

        List<LeanItemStack> outputs = new ArrayList<>();
        ItemStack primary = r.getPrimaryResult().copy();
        primary.setCount(r.getPrimaryCount());
        if (!primary.isEmpty()) outputs.add(new LeanItemStack(primary));
        ItemStack secondary = r.getSecondaryResult().copy();
        secondary.setCount(r.getSecondaryCount());
        if (!secondary.isEmpty()) outputs.add(new LeanItemStack(secondary));
        ItemStack tertiary = r.getTertiaryResult().copy();
        tertiary.setCount(r.getTertiaryCount());
        if (!tertiary.isEmpty()) outputs.add(new LeanItemStack(tertiary));
        if (outputs.isEmpty()) return;

        ret.add(new RecipeTransformation(TRANSFORM_COST, inputs, outputs));
    }

    private void addBlastFurnaceRecipe(mio_icif_BlastFurnaceRecipe r, List<RecipeTransformation> ret) {
        Ingredient input = r.getIngredients().get(0);
        if (input.isEmpty()) return;

        List<List<LeanItemStack>> inputs = convertIngredient(input);
        if (inputs.isEmpty()) return;

        List<LeanItemStack> outputs = new ArrayList<>();
        ItemStack primary = r.getResultItem(level.registryAccess());
        if (!primary.isEmpty()) outputs.add(new LeanItemStack(primary));
        ItemStack secondary = r.getSecondaryResultItem();
        if (!secondary.isEmpty()) outputs.add(new LeanItemStack(secondary));
        if (outputs.isEmpty()) return;

        ret.add(new RecipeTransformation(TRANSFORM_COST, inputs, outputs));
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