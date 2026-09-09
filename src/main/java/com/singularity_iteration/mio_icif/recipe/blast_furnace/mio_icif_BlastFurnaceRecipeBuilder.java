package com.singularity_iteration.mio_icif.recipe.blast_furnace;

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

/**
 * 高配方构建? * 用于数据生成时创建高配? */
@SuppressWarnings("null")
public class mio_icif_BlastFurnaceRecipeBuilder implements RecipeBuilder {

    private final Ingredient ingredient;
    private final ItemStack result;
    private ItemStack secondaryResult = ItemStack.EMPTY;
    private int duration = 1200;
    private int airCostPerTick = 1;
    private int heatCostPerTick = 128;
    private int ingredientCount = 1;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    private mio_icif_BlastFurnaceRecipeBuilder(Ingredient ingredient, ItemStack result) {
        this.ingredient = ingredient;
        this.result = result;
    }

    public static mio_icif_BlastFurnaceRecipeBuilder blast(Ingredient ingredient, Item result, int count) {
        return new mio_icif_BlastFurnaceRecipeBuilder(ingredient, new ItemStack(result, count));
    }

    public static mio_icif_BlastFurnaceRecipeBuilder blast(Ingredient ingredient, ItemStack result) {
        return new mio_icif_BlastFurnaceRecipeBuilder(ingredient, result);
    }

    public mio_icif_BlastFurnaceRecipeBuilder secondaryResult(Item result, int count) {
        this.secondaryResult = new ItemStack(result, count);
        return this;
    }

    public mio_icif_BlastFurnaceRecipeBuilder secondaryResult(ItemStack stack) {
        this.secondaryResult = stack.copy();
        return this;
    }

    public mio_icif_BlastFurnaceRecipeBuilder duration(int duration) {
        this.duration = duration;
        return this;
    }

    public mio_icif_BlastFurnaceRecipeBuilder airCostPerTick(int airCostPerTick) {
        this.airCostPerTick = airCostPerTick;
        return this;
    }

    public mio_icif_BlastFurnaceRecipeBuilder heatCostPerTick(int heatCostPerTick) {
        this.heatCostPerTick = heatCostPerTick;
        return this;
    }

    public mio_icif_BlastFurnaceRecipeBuilder ingredientCount(int ingredientCount) {
        this.ingredientCount = ingredientCount;
        return this;
    }

    @Override
    public mio_icif_BlastFurnaceRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public mio_icif_BlastFurnaceRecipeBuilder group(@Nullable String groupName) {
        return this;
    }

    @Override
    public Item getResult() {
        return this.result.getItem();
    }

    @Override
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        mio_icif_BlastFurnaceRecipe recipe = new mio_icif_BlastFurnaceRecipe(
            "",
            this.ingredient,
            this.result,
            this.secondaryResult,
            this.duration,
            this.airCostPerTick,
            this.heatCostPerTick,
            this.ingredientCount
        );

        Advancement.Builder advancementBuilder = recipeOutput.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id));
        this.criteria.forEach(advancementBuilder::addCriterion);

        recipeOutput.accept(id, recipe, advancementBuilder.build(id.withPrefix("recipes/blast_furnace/")));
    }
}