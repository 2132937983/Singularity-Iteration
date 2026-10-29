package com.miophas.singularity_iteration.common.client.integration.emi;

import com.miophas.singularity_iteration.common.recipe.blast_furnace.mio_icif_BlastFurnaceRecipe;
import com.miophas.singularity_iteration.common.recipe.canner.mix.MixRecipe;
import com.miophas.singularity_iteration.common.recipe.centrifuge.mio_icif_CentrifugeRecipe;
import com.miophas.singularity_iteration.common.recipe.mio_icif_PowderRecipe;
import com.miophas.singularity_iteration.common.recipe.washer.mio_icif_WasherRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    /** A line under the slots (minimum heat, water), or null. */
    private @Nullable Component info;

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
                // 按配方数量显示输入材料（打粉 / 混合）
                if (recipe instanceof mio_icif_PowderRecipe powder && powder.getIngredientCount() > 1) {
                    inputs.add(EmiIngredient.of(ingredient, powder.getIngredientCount()));
                } else if (recipe instanceof MixRecipe mix && mix.getMaterialCount() > 1) {
                    inputs.add(EmiIngredient.of(ingredient, mix.getMaterialCount()));
                } else {
                    inputs.add(EmiIngredient.of(ingredient));
                }
            }
        }

        // 解析输出
        this.outputs = new ArrayList<>();
        // 0.1.10.36: machines with by-products show every output (before, only the primary one), as their JEI categories do
        if (recipe instanceof mio_icif_CentrifugeRecipe centrifuge) {
            centrifuge.getAllResults().forEach(stack -> outputs.add(EmiStack.of(stack)));
            info = Component.translatable("jei.mio_icif.centrifuge.heat", number(centrifuge.getMinHeatRequired()));
        } else if (recipe instanceof mio_icif_WasherRecipe washer) {
            washer.getAllResults().forEach(stack -> outputs.add(EmiStack.of(stack)));
            info = Component.translatable("jei.mio_icif.washer.water", number(washer.getWaterAmount()));
        } else if (recipe instanceof mio_icif_BlastFurnaceRecipe blast) {
            outputs.add(EmiStack.of(blast.getResult()));
            if (!blast.getSecondaryResult().isEmpty()) outputs.add(EmiStack.of(blast.getSecondaryResult()));
        } else {
            ItemStack resultItem;
            if (recipe instanceof MixRecipe mixRecipe) {
                resultItem = mixRecipe.getDynamicResultCell();
            } else {
                resultItem = recipe.getResultItem(null);
            }
            outputs.add(EmiStack.of(resultItem));
        }
    }

    private static String number(long n) {
        return String.format(Locale.ROOT, "%,d", n);
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

        // 输出槽位：一个输出在中间；有副产物时排成一行
        if (outputs.size() == 1) {
            widgets.addSlot(outputs.get(0), 109, 28).recipeContext(this);
        } else {
            for (int i = 0; i < outputs.size(); i++) {
                widgets.addSlot(outputs.get(i), 100 + i * 18, 28).recipeContext(this);
            }
        }

        if (info != null) {
            widgets.addText(info, 2, 62, 0x404040, false);
        }
    }
}