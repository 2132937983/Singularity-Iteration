// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import com.miophas.singularity_iteration.common.recipe.mio_icif_PowderRecipe;
import com.miophas.singularity_iteration.core.api.machine.ProcessCost;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.108 (processing overhaul M1): the energy of grinding, by Bond's law. The energy to grind a material from a
 * given feed size to a given product size is proportional to its work index Wi (kWh/t): W = 10 Wi (1/√P80 − 1/√F80).
 * The macerators take ore lumps to powder, the same step for every input, so the energy per operation is
 * {@code EU_PER_WORK_INDEX × Wi × items × efficiency}: hematite (12.7) costs 600 EU in the macerator, as every
 * operation did before. The machine's power is fixed; the time follows: ⌈energy ÷ power⌉ ticks.
 */
public final class Comminution {
    private Comminution() {}

    /** EU per unit of work index: 600 EU for hematite (Wi 12.7), the old fixed macerator operation. */
    public static final double EU_PER_WORK_INDEX = 600.0 / 12.7;

    /** Energy (EU, at least 1) to grind {@code items} of a material of work index {@code workIndex} at an efficiency factor. */
    public static long energy(double workIndex, int items, double efficiency) {
        return Math.max(1L, Math.round(EU_PER_WORK_INDEX * workIndex * Math.max(1, items) * efficiency));
    }

    /**
     * The cost of one macerator operation on {@code input}: a time the recipe sets runs at the machine's power (the
     * recipe's override); otherwise the material's work index (or the default) gives the energy.
     *
     * @param power      the machine's power (EU/t)
     * @param efficiency energy factor of the machine (1 open circuit; less for a closed circuit)
     */
    public static ProcessCost maceratorCost(ItemStack input, @Nullable mio_icif_PowderRecipe recipe, long power, double efficiency) {
        if (recipe != null && recipe.processingTimeOverride().isPresent())
            return ProcessCost.fixed(recipe.processingTimeOverride().getAsInt(), power);
        int items = recipe == null ? 1 : recipe.getIngredientCount();
        return new ProcessCost(energy(SiMaterials.workIndexOrDefault(input), items, efficiency), Math.max(1L, power));
    }
}
