package com.singularity_iteration.mio_icif.recipe.centrifuge;

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
 * 热能离心机配方构建器
 * 用于数据生成时创建热能离心机配方
 * 支持多输出（3个输出槽位? */
@SuppressWarnings("null")
public class mio_icif_CentrifugeRecipeBuilder implements RecipeBuilder {

    private final Ingredient ingredient;
    // 主输出（输出�?�
private final ItemStack primaryResult;
    private int primaryCount = 1;
    // 副输�?（输出槽2�
private ItemStack secondaryResult = ItemStack.EMPTY;
    private int secondaryCount = 0;
    // 副输�?（输出槽3�
private ItemStack tertiaryResult = ItemStack.EMPTY;
    private int tertiaryCount = 0;
    
    private int processingTime = 500;
    private int energyPerTick = 40;
    private int minHeatRequired = 5000;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    private mio_icif_CentrifugeRecipeBuilder(Ingredient ingredient, ItemStack primaryResult) {
        this.ingredient = ingredient;
        this.primaryResult = primaryResult;
    }

    /**
     * 创建热能离心机配方构建器
     * @param ingredient 输入原料
     * @param primaryResult 主输出物�
 * @return 配方构建�
 */
    public static mio_icif_CentrifugeRecipeBuilder centrifuging(Ingredient ingredient, Item primaryResult) {
        return new mio_icif_CentrifugeRecipeBuilder(ingredient, new ItemStack(primaryResult));
    }

    /**
     * 创建热能离心机配方构建器（带主输出数量）
     * @param ingredient 输入原料
     * @param primaryResult 主输出物�
 * @param primaryCount 主输出数据
 * @return 配方构建�
 */
    public static mio_icif_CentrifugeRecipeBuilder centrifuging(Ingredient ingredient, Item primaryResult, int primaryCount) {
        mio_icif_CentrifugeRecipeBuilder builder = new mio_icif_CentrifugeRecipeBuilder(ingredient, new ItemStack(primaryResult));
        builder.primaryCount = primaryCount;
        return builder;
    }

    /**
     * 设置主输出数据
 * @param count 数量
     * @return 配方构建�
 */
    public mio_icif_CentrifugeRecipeBuilder primaryCount(int count) {
        this.primaryCount = count;
        return this;
    }

    /**
     * 设置副输�?
     * @param item 物品
     * @param count 数量
     * @return 配方构建�
 */
    public mio_icif_CentrifugeRecipeBuilder secondaryOutput(Item item, int count) {
        this.secondaryResult = new ItemStack(item);
        this.secondaryCount = count;
        return this;
    }

    /**
     * 设置副输�?
     * @param item 物品
     * @param count 数量
     * @return 配方构建�
 */
    public mio_icif_CentrifugeRecipeBuilder tertiaryOutput(Item item, int count) {
        this.tertiaryResult = new ItemStack(item);
        this.tertiaryCount = count;
        return this;
    }

    /**
     * 设置处理时间
     * @param processingTime 处理时间（ticks�
 * @return 配方构建�
 */
    public mio_icif_CentrifugeRecipeBuilder processingTime(int processingTime) {
        this.processingTime = processingTime;
        return this;
    }

    /**
     * 设置每tick能量消耗
 * @param energyPerTick 能量消耗（EU/tick）
 * @return 配方构建�
 */
    public mio_icif_CentrifugeRecipeBuilder energyPerTick(int energyPerTick) {
        this.energyPerTick = energyPerTick;
        return this;
    }

    /**
     * 设置所需最小热�
 * @param minHeatRequired 最小热量（HU�
 * @return 配方构建�
 */
    public mio_icif_CentrifugeRecipeBuilder minHeatRequired(int minHeatRequired) {
        this.minHeatRequired = minHeatRequired;
        return this;
    }

    @Override
    public mio_icif_CentrifugeRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public mio_icif_CentrifugeRecipeBuilder group(@Nullable String groupName) {
        // 热能离心机配方不支持分组，忽�
    return this;
    }

    @Override
    public Item getResult() {
        return this.primaryResult.getItem();
    }

    @Override
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        // 创建配方
        mio_icif_CentrifugeRecipe recipe = new mio_icif_CentrifugeRecipe(
            "",
            this.ingredient,
            this.primaryResult, this.primaryCount,
            this.secondaryResult, this.secondaryCount,
            this.tertiaryResult, this.tertiaryCount,
            this.processingTime,
            this.energyPerTick,
            this.minHeatRequired
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


