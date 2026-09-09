package com.singularity_iteration.mio_icif.recipe.washer;

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
 * 洗矿配方构建�?
 * 用于数据生成时创建洗矿配方?
 * 支持多输出（3个输出槽位?
 */
@SuppressWarnings("null")
public class mio_icif_WasherRecipeBuilder implements RecipeBuilder {

    private final Ingredient ingredient;
    // 主输出（输出�?�?
    private final ItemStack primaryResult;
    private int primaryCount = 1;
    // 副输�?（输出槽2�?
    private ItemStack secondaryResult = ItemStack.EMPTY;
    private int secondaryCount = 0;
    // 副输�?（输出槽3�?
    private ItemStack tertiaryResult = ItemStack.EMPTY;
    private int tertiaryCount = 0;
    
    private int processingTime = 200;
    private int energyPerTick = 10;
    private int waterAmount = 200;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    private mio_icif_WasherRecipeBuilder(Ingredient ingredient, ItemStack primaryResult) {
        this.ingredient = ingredient;
        this.primaryResult = primaryResult;
    }

    /**
     * 创建洗矿配方构建�?
     * @param ingredient 输入原料
     * @param primaryResult 主输出物�?
     * @return 配方构建�?
     */
    public static mio_icif_WasherRecipeBuilder washing(Ingredient ingredient, Item primaryResult) {
        return new mio_icif_WasherRecipeBuilder(ingredient, new ItemStack(primaryResult));
    }

    /**
     * 创建洗矿配方构建器（带主输出数量�?
     * @param ingredient 输入原料
     * @param primaryResult 主输出物�?
     * @param primaryCount 主输出数据?
     * @return 配方构建�?
     */
    public static mio_icif_WasherRecipeBuilder washing(Ingredient ingredient, Item primaryResult, int primaryCount) {
        mio_icif_WasherRecipeBuilder builder = new mio_icif_WasherRecipeBuilder(ingredient, new ItemStack(primaryResult));
        builder.primaryCount = primaryCount;
        return builder;
    }

    /**
     * 设置主输出数据?
     * @param count 数量
     * @return 配方构建�?
     */
    public mio_icif_WasherRecipeBuilder primaryCount(int count) {
        this.primaryCount = count;
        return this;
    }

    /**
     * 设置副输�?
     * @param item 物品
     * @param count 数量
     * @return 配方构建�?
     */
    public mio_icif_WasherRecipeBuilder secondaryOutput(Item item, int count) {
        this.secondaryResult = new ItemStack(item);
        this.secondaryCount = count;
        return this;
    }

    /**
     * 设置副输�?
     * @param item 物品
     * @param count 数量
     * @return 配方构建�?
     */
    public mio_icif_WasherRecipeBuilder tertiaryOutput(Item item, int count) {
        this.tertiaryResult = new ItemStack(item);
        this.tertiaryCount = count;
        return this;
    }

    /**
     * 设置处理时间
     * @param processingTime 处理时间（ticks�?
     * @return 配方构建�?
     */
    public mio_icif_WasherRecipeBuilder processingTime(int processingTime) {
        this.processingTime = processingTime;
        return this;
    }

    /**
     * 设置每tick能量消耗?
     * @param energyPerTick 能量消耗（FE/tick�?
     * @return 配方构建�?
     */
    public mio_icif_WasherRecipeBuilder energyPerTick(int energyPerTick) {
        this.energyPerTick = energyPerTick;
        return this;
    }

    /**
     * 设置消耗的水量
     * @param waterAmount 水量（mb�?
     * @return 配方构建�?
     */
    public mio_icif_WasherRecipeBuilder waterAmount(int waterAmount) {
        this.waterAmount = waterAmount;
        return this;
    }

    @Override
    public mio_icif_WasherRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public mio_icif_WasherRecipeBuilder group(@Nullable String groupName) {
        // 洗矿配方不支持分组，忽略
        return this;
    }

    @Override
    public Item getResult() {
        return this.primaryResult.getItem();
    }

    @Override
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        // 创建配方
        mio_icif_WasherRecipe recipe = new mio_icif_WasherRecipe(
            "",
            this.ingredient,
            this.primaryResult, this.primaryCount,
            this.secondaryResult, this.secondaryCount,
            this.tertiaryResult, this.tertiaryCount,
            this.processingTime,
            this.energyPerTick,
            this.waterAmount
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


