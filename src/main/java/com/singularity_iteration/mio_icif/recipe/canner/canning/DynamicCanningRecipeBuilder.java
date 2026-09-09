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
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 动态装罐配方构建器
 * 用于数据生成时创建动态装罐配方? */
@SuppressWarnings("null")
public class DynamicCanningRecipeBuilder implements RecipeBuilder {

    private final Ingredient canIngredient;
    private final Ingredient foodIngredient;
    private int processingTime = 200;
    private int energyPerTick = 10;
    private int nutritionMultiplier = 1;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    public DynamicCanningRecipeBuilder(Ingredient canIngredient, Ingredient foodIngredient) {
        this.canIngredient = canIngredient;
        this.foodIngredient = foodIngredient;
    }

    /**
     * 创建一个新的动态装罐配方构建器
     * @param canIngredient 空锡罐原�
 * @param foodIngredient 食物原料
     * @return 配方构建�
 */
    public static DynamicCanningRecipeBuilder canning(Ingredient canIngredient, Ingredient foodIngredient) {
        return new DynamicCanningRecipeBuilder(canIngredient, foodIngredient);
    }

    /**
     * 设置处理时间
     * @param processingTime 处理时间（tick�
 * @return 配方构建�
 */
    public DynamicCanningRecipeBuilder processingTime(int processingTime) {
        this.processingTime = processingTime;
        return this;
    }

    /**
     * 设置每tick能量消耗
 * @param energyPerTick 能量消耗
 * @return 配方构建�
 */
    public DynamicCanningRecipeBuilder energyPerTick(int energyPerTick) {
        this.energyPerTick = energyPerTick;
        return this;
    }

    /**
     * 设置营养值倍数
     * @param multiplier 倍数（默�?�
 * @return 配方构建�
 */
    public DynamicCanningRecipeBuilder multiplier(int multiplier) {
        this.nutritionMultiplier = multiplier;
        return this;
    }

    @Override
    public RecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public RecipeBuilder group(@Nullable String groupName) {
        // 动态配方不需要分配
    return this;
    }

    @Override
    public Item getResult() {
        // 返回空物品，因为结果是动态的
        return net.minecraft.world.item.Items.AIR;
    }

    @Override
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        // 创建进度条件
        Advancement.Builder advancement = recipeOutput.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id))
            .requirements(AdvancementRequirements.Strategy.OR);
        
        this.criteria.forEach(advancement::addCriterion);

        // 创建配方
        DynamicCanningRecipe recipe = new DynamicCanningRecipe(
            "",
            this.canIngredient,
            this.foodIngredient,
            this.processingTime,
            this.energyPerTick,
            this.nutritionMultiplier
        );

        // 输出配方
        recipeOutput.accept(id, recipe, advancement.build(id.withPrefix("recipes/")));
    }
}


