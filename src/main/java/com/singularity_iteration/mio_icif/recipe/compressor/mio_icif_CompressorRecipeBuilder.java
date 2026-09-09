package com.singularity_iteration.mio_icif.recipe.compressor;

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
 * 压缩机配方构建器
 * 用于数据生成时创建压缩机配方
 */
@SuppressWarnings("null")
public class mio_icif_CompressorRecipeBuilder implements RecipeBuilder {

    private final Ingredient ingredient;
    private final ItemStack result;
    private int processingTime = 200;
    private int energyPerTick = 10;
    private int ingredientCount = 1;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    private mio_icif_CompressorRecipeBuilder(Ingredient ingredient, ItemStack result) {
        this.ingredient = ingredient;
        this.result = result;
    }

    /**
     * 创建压缩机配方构建器
     * @param ingredient 输入原料
     * @param result 输出物品
     * @param count 输出数量
     * @return 配方构建�
 */
    public static mio_icif_CompressorRecipeBuilder compress(Ingredient ingredient, Item result, int count) {
        return new mio_icif_CompressorRecipeBuilder(ingredient, new ItemStack(result, count));
    }

    /**
     * 创建压缩机配方构建器（使用ItemStack�
 * @param ingredient 输入原料
     * @param result 输出物品栈
 * @return 配方构建�
 */
    public static mio_icif_CompressorRecipeBuilder compress(Ingredient ingredient, ItemStack result) {
        return new mio_icif_CompressorRecipeBuilder(ingredient, result);
    }

    /**
     * 设置处理时间
     * @param processingTime 处理时间（ticks�
 * @return 配方构建�
 */
    public mio_icif_CompressorRecipeBuilder processingTime(int processingTime) {
        this.processingTime = processingTime;
        return this;
    }

    /**
     * 设置每tick能量消耗
 * @param energyPerTick 能量消耗（FE/tick�
 * @return 配方构建�
 */
    public mio_icif_CompressorRecipeBuilder energyPerTick(int energyPerTick) {
        this.energyPerTick = energyPerTick;
        return this;
    }

    /**
     * 设置输入原料数量
     * @param count 原料数量
     * @return 配方构建�
 */
    public mio_icif_CompressorRecipeBuilder ingredientCount(int count) {
        this.ingredientCount = count;
        return this;
    }

    @Override
    public mio_icif_CompressorRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public mio_icif_CompressorRecipeBuilder group(@Nullable String groupName) {
        // 压缩机配方不支持分组，忽�
    return this;
    }

    @Override
    public Item getResult() {
        return this.result.getItem();
    }

    @Override
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        // 创建配方
        mio_icif_CompressorRecipe recipe = new mio_icif_CompressorRecipe(
            "",
            this.ingredient,
            this.result,
            this.processingTime,
            this.energyPerTick,
            this.ingredientCount
        );

        // 创建进度
        Advancement.Builder advancementBuilder = recipeOutput.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id));
        this.criteria.forEach(advancementBuilder::addCriterion);

        // 保存配方
        recipeOutput.accept(id, recipe, advancementBuilder.build(id.withPrefix("recipes/compressor/")));
    }
}


