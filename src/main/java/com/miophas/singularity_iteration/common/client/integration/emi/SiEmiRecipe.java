// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;

/**
 * 0.1.11.209: a native EMI recipe built from its lookup stacks and a layout. {@link #getInputs()} and
 * {@link #getOutputs()} decide where the recipe is found ("recipes for" / "uses of"), the layout decides what is drawn;
 * they may differ, as in the JEI categories: a fluid recipe is also found through the cells and buckets that hold the
 * fluid, without showing them. Such recipes are kept out of EMI's recipe tree ({@link #noTree()}).
 */
public final class SiEmiRecipe extends BasicEmiRecipe {
    private final BiConsumer<WidgetHolder, SiEmiRecipe> layout;
    private boolean tree = true;

    public SiEmiRecipe(EmiRecipeCategory category, ResourceLocation id, int width, int height,
                       List<EmiIngredient> inputs, List<EmiStack> outputs, BiConsumer<WidgetHolder, SiEmiRecipe> layout) {
        super(category, id, width, height);
        this.inputs = List.copyOf(inputs);
        this.outputs = List.copyOf(outputs);
        this.layout = layout;
    }

    /** Leaves the recipe out of EMI's recipe tree (lookup-only stacks, or nothing to craft). */
    public SiEmiRecipe noTree() {
        tree = false;
        return this;
    }

    @Override
    public boolean supportsRecipeTree() {
        return tree && super.supportsRecipeTree();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        layout.accept(widgets, this);
    }
}
