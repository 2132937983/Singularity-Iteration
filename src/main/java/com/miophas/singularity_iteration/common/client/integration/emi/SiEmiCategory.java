// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration.emi;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * 0.1.11.209: an EMI category named by a given text (the title of its JEI twin, or a machine's name), so the native EMI
 * categories need no "emi.category.*" keys of their own.
 */
public class SiEmiCategory extends EmiRecipeCategory {
    private final Component name;

    public SiEmiCategory(String path, ItemStack icon, Component name) {
        super(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, path), EmiStack.of(icon));
        this.name = name;
    }

    public SiEmiCategory(String path, ItemLike icon, String titleKey) {
        this(path, new ItemStack(icon), Component.translatable(titleKey));
    }

    @Override
    public Component getName() {
        return name;
    }
}
