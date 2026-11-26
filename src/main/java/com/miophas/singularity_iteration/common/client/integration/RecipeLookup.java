// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration;

import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;

/** Opens the recipe viewer for an item when one is installed (EMI or JEI); safe without one (0.1.8.0; EMI 0.1.11.209). */
@OnlyIn(Dist.CLIENT)
public final class RecipeLookup {
    private RecipeLookup() {}

    private static final boolean JEI = ModList.get().isLoaded("jei");
    /** 0.1.11.209: with EMI installed EMI is the viewer, also beside JEI (EMI then never calls the mod's JEI plugin). */
    private static final boolean EMI = ModList.get().isLoaded("emi");

    public static boolean available() { return EMI || JEI; }

    /** Shows the recipes of {@code stack} ({@code uses}: where it is used). False when no viewer opened. */
    public static boolean show(ItemStack stack, boolean uses) {
        if (!EMI && !JEI) return false;
        try {
            if (EMI) return com.miophas.singularity_iteration.common.client.integration.emi.EmiBridge.show(stack, uses);
            return com.miophas.singularity_iteration.common.client.integration.jei.JeiBridge.show(stack, uses);
        } catch (LinkageError | RuntimeException e) {
            return false;
        }
    }
}
