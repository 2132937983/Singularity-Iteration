// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.integration;

import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;

/** Opens the recipe viewer for an item when one is installed (JEI); safe without it (0.1.8.0). */
@OnlyIn(Dist.CLIENT)
public final class RecipeLookup {
    private RecipeLookup() {}

    private static final boolean JEI = ModList.get().isLoaded("jei");

    public static boolean available() { return JEI; }

    /** Shows the recipes of {@code stack} ({@code uses}: where it is used). False when no viewer opened. */
    public static boolean show(ItemStack stack, boolean uses) {
        if (!JEI) return false;
        try {
            return com.miophas.singularity_iteration.common.client.integration.jei.JeiBridge.show(stack, uses);
        } catch (LinkageError | RuntimeException e) {
            return false;
        }
    }
}
