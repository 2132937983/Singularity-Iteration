package com.singularity_iteration.mio_icif.recipe.canner.mix;

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
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 混合模式配方构建�? * 用于数据生成时创建混合配方? */
@SuppressWarnings("null")
public class MixRecipeBuilder implements RecipeBuilder {

    private final FluidStack inputFluid;
    private final Ingredient materialIngredient;
    private int materialCount = 1;
    private final FluidStack resultFluid;
    private final ItemStack resultCell;
    private int processingTime = 300;
    private int energyPerTick = 15;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    private MixRecipeBuilder(FluidStack inputFluid, Ingredient materialIngredient, 
                             FluidStack resultFluid, ItemStack resultCell) {
        this.inputFluid = inputFluid;
        this.materialIngredient = materialIngredient;
        this.resultFluid = resultFluid;
        this.resultCell = resultCell;
    }

    /**
     * 创建混合配方构建�?     * @param inputFluid 输入流体
     * @param materialIngredient 材料原料
     * @param resultFluid 结果流体
     * @param resultCell 结果单元
     * @return 配方构建�?     */
    public static MixRecipeBuilder mix(FluidStack inputFluid, Ingredient materialIngredient,
                                        FluidStack resultFluid, ItemStack resultCell) {
        return new MixRecipeBuilder(inputFluid, materialIngredient, resultFluid, resultCell);
    }

    /**
     * 创建混合配方构建器（简化版�?     * @param inputFluid 输入流体
     * @param materialIngredient 材料原料
     * @param resultFluid 结果流体
     * @param resultCell 结果单元物品
     * @return 配方构建�?     */
    public static MixRecipeBuilder mix(FluidStack inputFluid, Ingredient materialIngredient,
                                        FluidStack resultFluid, Item resultCell) {
        return new MixRecipeBuilder(inputFluid, materialIngredient, resultFluid, new ItemStack(resultCell));
    }

    /**
     * 设置材料数量
     * @param count 材料数量
     * @return 配方构建�?     */
    public MixRecipeBuilder materialCount(int count) {
        this.materialCount = count;
        return this;
    }

    /**
     * 设置处理时间
     * @param processingTime 处理时间（ticks�?     * @return 配方构建�?     */
    public MixRecipeBuilder processingTime(int processingTime) {
        this.processingTime = processingTime;
        return this;
    }

    /**
     * 设置每tick能量消耗?     * @param energyPerTick 能量消耗（FE/tick�?     * @return 配方构建�?     */
    public MixRecipeBuilder energyPerTick(int energyPerTick) {
        this.energyPerTick = energyPerTick;
        return this;
    }

    @Override
    public MixRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public MixRecipeBuilder group(@Nullable String groupName) {
        // 混合配方不支持分组，忽略
        return this;
    }

    @Override
    public Item getResult() {
        return this.resultCell.getItem();
    }

    @Override
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        // 创建配方
        MixRecipe recipe = new MixRecipe(
            "",
            this.inputFluid,
            this.materialIngredient,
            this.materialCount,
            this.resultFluid,
            this.resultCell,
            this.processingTime,
            this.energyPerTick
        );

        // 创建进度
        Advancement.Builder advancementBuilder = recipeOutput.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id));
        this.criteria.forEach(advancementBuilder::addCriterion);

        // 保存配方
        recipeOutput.accept(id, recipe, advancementBuilder.build(id.withPrefix("recipes/mix/")));
    }
}

