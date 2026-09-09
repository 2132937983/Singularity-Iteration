package com.singularity_iteration.mio_icif.recipe.block_cutter;

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
 * 方块切割机配方构建器
 * 用于数据生成时创建方块切割机配方
 */
@SuppressWarnings("null")
public class mio_icif_BlockCutterRecipeBuilder implements RecipeBuilder {

    private final Ingredient ingredient;
    private final ItemStack result;
    private int processingTime = 450;
    private int energyPerTick = 4;
    private int ingredientCount = 1;
    private int hardness = 2;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    private mio_icif_BlockCutterRecipeBuilder(Ingredient ingredient, ItemStack result) {
        this.ingredient = ingredient;
        this.result = result;
    }

    public static mio_icif_BlockCutterRecipeBuilder cut(Ingredient ingredient, Item result, int count) {
        return new mio_icif_BlockCutterRecipeBuilder(ingredient, new ItemStack(result, count));
    }

    public static mio_icif_BlockCutterRecipeBuilder cut(Ingredient ingredient, ItemStack result) {
        return new mio_icif_BlockCutterRecipeBuilder(ingredient, result);
    }

    public mio_icif_BlockCutterRecipeBuilder processingTime(int processingTime) {
        this.processingTime = processingTime;
        return this;
    }

    public mio_icif_BlockCutterRecipeBuilder energyPerTick(int energyPerTick) {
        this.energyPerTick = energyPerTick;
        return this;
    }

    public mio_icif_BlockCutterRecipeBuilder ingredientCount(int count) {
        this.ingredientCount = count;
        return this;
    }

    public mio_icif_BlockCutterRecipeBuilder hardness(int hardness) {
        this.hardness = hardness;
        return this;
    }

    @Override
    public mio_icif_BlockCutterRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public mio_icif_BlockCutterRecipeBuilder group(@Nullable String groupName) {
        return this;
    }

    @Override
    public Item getResult() {
        return this.result.getItem();
    }

    @Override
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        mio_icif_BlockCutterRecipe recipe = new mio_icif_BlockCutterRecipe(
            "",
            this.ingredient,
            this.result,
            this.processingTime,
            this.energyPerTick,
            this.ingredientCount,
            this.hardness
        );

        Advancement.Builder advancementBuilder = recipeOutput.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id));
        this.criteria.forEach(advancementBuilder::addCriterion);

        recipeOutput.accept(id, recipe, advancementBuilder.build(id.withPrefix("recipes/block_cutter/")));
    }
}


