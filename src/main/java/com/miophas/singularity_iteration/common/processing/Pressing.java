// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import com.miophas.singularity_iteration.common.recipe.compressor.mio_icif_CompressorRecipe;
import com.miophas.singularity_iteration.core.api.machine.ProcessCost;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.112 (processing overhaul M5): the work of a press. Compacting a charge takes the work W = ∫P dV: a harder
 * material needs a higher pressure over the stroke, so a hydraulic press draws more power (power = pressure × flow) and
 * strokes longer. The compressor recipes carry that as their own time and EU/t (soft blocks 100 t × 5 EU/t, dense plates
 * 400 t × 20 EU/t, industrial diamond 600 t × 30 EU/t), and the compressors now run them as written; a recipe without them
 * runs the stock 300 t at 2 EU/t.
 *
 * <p>The oil pressure shown on the screens is the work of the operation against the advanced compressor's accumulator:
 * {@code 32 MPa × energy ÷ 20,000 EU} (a dense plate about 12.8 MPa, the industrial diamond about 28.8 MPa).
 */
public final class Pressing {
    private Pressing() {}

    /** The stock operation of a recipe without its own time and power. */
    public static final int STOCK_TICKS = 300;
    public static final long STOCK_POWER = 2L;
    /** Full accumulator pressure (MPa) and the energy it holds (EU). */
    public static final double FULL_PRESSURE_MPA = 32;
    public static final long ACCUMULATOR_ENERGY = 20_000L;
    /** The hardest stock operation (industrial diamond, 600 t × 30 EU/t) sizes the buffers. */
    public static final long MAX_OPERATION_ENERGY = 18_000L;

    /** The cost of one pressing of {@code recipe} as written (time and EU/t), or the stock one for what it leaves out. */
    public static ProcessCost cost(@Nullable mio_icif_CompressorRecipe recipe) {
        if (recipe == null) return ProcessCost.fixed(STOCK_TICKS, STOCK_POWER);
        int ticks = recipe.processingTimeOverride().orElse(STOCK_TICKS);
        long power = recipe.energyPerTickOverride().isPresent() ? recipe.energyPerTickOverride().getAsInt() : STOCK_POWER;
        return ProcessCost.fixed(ticks, power);
    }

    /** The oil pressure (MPa) the work of {@code energy} EU stands for. */
    public static double pressureMpa(long energy) {
        return FULL_PRESSURE_MPA * energy / ACCUMULATOR_ENERGY;
    }
}
