package com.singularity_iteration.mio_icif.recipe.extractor;

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
 * 提取机配方构建器
 * 用于数据生成时创建提取机配方
 */
@SuppressWarnings("null")
public class mio_icif_ExtractorRecipeBuilder implements RecipeBuilder {

    private final Ingredient ingredient;
    private final ItemStack result;
    private int processingTime = 200;
    private int energyPerTick = 15;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    private mio_icif_ExtractorRecipeBuilder(Ingredient ingredient, ItemStack result) {
        this.ingredient = ingredient;
        this.result = result;
    }

    /**
     * 创建提取机配方构建器
     * @param ingredient 输入原料
     * @param result 输出物品
     * @param count 输出数量
     * @return 配方构建�
 */
    public static mio_icif_ExtractorRecipeBuilder extract(Ingredient ingredient, Item result, int count) {
        return new mio_icif_ExtractorRecipeBuilder(ingredient, new ItemStack(result, count));
    }

    /**
     * 设置处理时间
     * @param processingTime 处理时间（ticks�
 * @return 配方构建�
 */
    public mio_icif_ExtractorRecipeBuilder processingTime(int processingTime) {
        this.processingTime = processingTime;
        return this;
    }

    /**
     * 设置每tick能量消耗
 * @param energyPerTick 能量消耗（FE/tick�
 * @return 配方构建�
 */
    public mio_icif_ExtractorRecipeBuilder energyPerTick(int energyPerTick) {
        this.energyPerTick = energyPerTick;
        return this;
    }

    @Override
    public mio_icif_ExtractorRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public mio_icif_ExtractorRecipeBuilder group(@Nullable String groupName) {
        // 提取机配方不支持分组，忽�
    return this;
    }

    @Override
    public Item getResult() {
        return this.result.getItem();
    }

    @Override
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        // 创建配方
        mio_icif_ExtractorRecipe recipe = new mio_icif_ExtractorRecipe(
            "",
            this.ingredient,
            this.result,
            this.processingTime,
            this.energyPerTick
        );

        // 创建进度
        Advancement.Builder advancementBuilder = recipeOutput.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id));
        this.criteria.forEach(advancementBuilder::addCriterion);

        // 保存配方
        recipeOutput.accept(id, recipe, advancementBuilder.build(id.withPrefix("recipes/")));
    }
}


