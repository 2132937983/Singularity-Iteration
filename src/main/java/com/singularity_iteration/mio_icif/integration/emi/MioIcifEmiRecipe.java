package com.singularity_iteration.mio_icif.integration.emi;

import com.singularity_iteration.mio_icif.recipe.canner.mix.MixRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * EMI 通用配方实现
 * 包装 Minecraft 配方为 EMI 可识别的格式
 */
@SuppressWarnings("null")
public class MioIcifEmiRecipe implements EmiRecipe {

    private final EmiRecipeCategory category;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;
    private final ResourceLocation id;

    public MioIcifEmiRecipe(EmiRecipeCategory category, RecipeHolder<? extends Recipe<?>> recipeHolder) {
        this.category = category;
        // 为 EMI 合成配方添加 / 前缀，避免 "not present in recipe manager" 错误
        // 同时避免与 JEI 配方 ID 冲突
        this.id = ResourceLocation.fromNamespaceAndPath(
                recipeHolder.id().getNamespace(),
                "/emi/" + recipeHolder.id().getPath());

        // 解析输入
        this.inputs = new ArrayList<>();
        Recipe<?> recipe = recipeHolder.value();
        for (var ingredient : recipe.getIngredients()) {
            if (!ingredient.isEmpty()) {
                inputs.add(EmiIngredient.of(ingredient));
            }
        }

        // 解析输出
        this.outputs = new ArrayList<>();
        ItemStack resultItem;
        if (recipe instanceof MixRecipe mixRecipe) {
            resultItem = mixRecipe.getDynamicResultCell();
        } else {
            resultItem = recipe.getResultItem(null);
        }
        outputs.add(EmiStack.of(resultItem));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return 162;
    }

    @Override
    public int getDisplayHeight() {
        return 72;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        // 输入槽位
        if (!inputs.isEmpty()) {
            widgets.addSlot(inputs.get(0), 49, 10);
        }

        // 如果有第二个输入（如装罐机）
        if (inputs.size() > 1) {
            widgets.addSlot(inputs.get(1), 80, 44);
        }

        // 输出槽位
        if (!outputs.isEmpty()) {
            widgets.addSlot(outputs.get(0), 109, 28).recipeContext(this);
        }
    }
}