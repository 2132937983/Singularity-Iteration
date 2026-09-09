package com.singularity_iteration.mio_icif.recipe.canner.canning;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
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
 * 固定输出装罐配方构建�? * 用于将原料装入容器（如空燃料�?+ 铀 -> 铀燃料棒）
 */
@SuppressWarnings("null")
public class CanningRecipeBuilder implements RecipeBuilder {

    private final Ingredient canIngredient;
    private final Ingredient foodIngredient;
    private final ItemStack result;
    private int processingTime = 200;
    private int energyPerTick = 10;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    public CanningRecipeBuilder(Ingredient canIngredient, Ingredient foodIngredient, ItemStack result) {
        this.canIngredient = canIngredient;
        this.foodIngredient = foodIngredient;
        this.result = result;
    }

    /**
     * 创建一个新的固定输出装罐配方构建器
     * @param canIngredient 容器原料（如空燃料棒�?     * @param foodIngredient 原料（如铀-238�?     * @param result 结果物品
     * @return 配方构建�?     */
    public static CanningRecipeBuilder canning(Ingredient canIngredient, Ingredient foodIngredient, ItemStack result) {
        return new CanningRecipeBuilder(canIngredient, foodIngredient, result);
    }

    /**
     * 设置处理时间
     * @param processingTime 处理时间（tick�?     * @return 配方构建�?     */
    public CanningRecipeBuilder processingTime(int processingTime) {
        this.processingTime = processingTime;
        return this;
    }

    /**
     * 设置每tick能量消耗?     * @param energyPerTick 能量消耗?     * @return 配方构建�?     */
    public CanningRecipeBuilder energyPerTick(int energyPerTick) {
        this.energyPerTick = energyPerTick;
        return this;
    }

    @Override
    public RecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public RecipeBuilder group(@Nullable String groupName) {
        return this;
    }

    @Override
    public Item getResult() {
        return result.getItem();
    }

    @Override
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        Advancement.Builder advancement = recipeOutput.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id))
            .requirements(AdvancementRequirements.Strategy.OR);

        this.criteria.forEach(advancement::addCriterion);

        CanningRecipe recipe = new CanningRecipe(
            "",
            this.canIngredient,
            this.foodIngredient,
            this.result,
            this.processingTime,
            this.energyPerTick
        );

        recipeOutput.accept(id, recipe, advancement.build(id.withPrefix("recipes/")));
    }
}


