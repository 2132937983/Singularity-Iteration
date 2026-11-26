// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.11.209: opens EMI's recipe screen for an item (see {@code RecipeLookup}). With EMI installed the JEI runtime never
 * reaches the mod (EMI does not call the JEI plugin of a mod that has an EMI plugin), so EMI must open the recipes.
 * Only touched when EMI is loaded.
 */
public final class EmiBridge {
    private EmiBridge() {}

    /** Shows the recipes of {@code stack} ({@code uses}: where it is used). False when EMI has none to show. */
    public static boolean show(ItemStack stack, boolean uses) {
        if (stack.isEmpty()) return false;
        EmiStack emi = EmiStack.of(stack);
        var manager = EmiApi.getRecipeManager();
        if (manager != null && (uses ? manager.getRecipesByInput(emi) : manager.getRecipesByOutput(emi)).isEmpty()) return false;
        if (uses) EmiApi.displayUses(emi);
        else EmiApi.displayRecipes(emi);
        return true;
    }
}
