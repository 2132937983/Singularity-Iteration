package com.singularity_iteration.mio_icif.recipe.metal_former.extruding;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("null")
public class mio_icif_ExtrudingRecipeBuilder implements RecipeBuilder {

    private final Ingredient ingredient;
    private final ItemStack result;
    private int processingTime = 100;
    private int energyPerTick = 10;
    private int ingredientCount = 1;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    private mio_icif_ExtrudingRecipeBuilder(Ingredient ingredient, ItemStack result) {
        this.ingredient = ingredient;
        this.result = result;
    }

    public static mio_icif_ExtrudingRecipeBuilder extrude(Ingredient ingredient, Item result, int count) {
        return new mio_icif_ExtrudingRecipeBuilder(ingredient, new ItemStack(result, count));
    }

    public static mio_icif_ExtrudingRecipeBuilder extrude(Ingredient ingredient, ItemStack result) {
        return new mio_icif_ExtrudingRecipeBuilder(ingredient, result);
    }

    public mio_icif_ExtrudingRecipeBuilder processingTime(int processingTime) {
        this.processingTime = processingTime;
        return this;
    }

    public mio_icif_ExtrudingRecipeBuilder energyPerTick(int energyPerTick) {
        this.energyPerTick = energyPerTick;
        return this;
    }

    public mio_icif_ExtrudingRecipeBuilder ingredientCount(int count) {
        this.ingredientCount = count;
        return this;
    }

    @Override
    public mio_icif_ExtrudingRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public mio_icif_ExtrudingRecipeBuilder group(@Nullable String groupName) {
        return this;
    }

    @Override
    public Item getResult() {
        return this.result.getItem();
    }

    @Override
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        mio_icif_ExtrudingRecipe recipe = new mio_icif_ExtrudingRecipe(
            "",
            this.ingredient,
            this.result,
            this.processingTime,
            this.energyPerTick,
            this.ingredientCount
        );

        Advancement.Builder advancementBuilder = recipeOutput.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id));
        this.criteria.forEach(advancementBuilder::addCriterion);

        recipeOutput.accept(id, recipe, advancementBuilder.build(id.withPrefix("recipes/metal_former_extruding/")));
    }
}


