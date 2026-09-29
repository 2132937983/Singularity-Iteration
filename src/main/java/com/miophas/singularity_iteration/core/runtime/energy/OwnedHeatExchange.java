// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Exact owned-tank exchange; input, output and all resulting HU must fit before consuming fluid. */
public final class OwnedHeatExchange {
    private OwnedHeatExchange() { }
    public static boolean exchange(FluidTank input, FluidTank output, IMioIcifCapabilities.IHeatStorage heat,
                                   int millibuckets, FluidStack result, long generatedHeat) {
        if (input == output || millibuckets <= 0 || generatedHeat <= 0 || result.isEmpty()
                || input.getFluidAmount() < millibuckets || !input.isFluidValid(input.getFluid())
                || heat.generateHeatInternal(generatedHeat, true) != generatedHeat
                || output.fill(result, IFluidHandler.FluidAction.SIMULATE) != result.getAmount()) return false;
        var inputBefore = input.getFluid().copy();
        var outputBefore = output.getFluid().copy();
        var drained = input.drain(millibuckets, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != millibuckets
                || output.fill(result, IFluidHandler.FluidAction.EXECUTE) != result.getAmount()) {
            input.setFluid(inputBefore); output.setFluid(outputBefore);
            return false;
        }
        long generated = heat.generateHeatInternal(generatedHeat, false);
        if (generated != generatedHeat) {
            if (generated > 0) heat.consumeHeatInternal(generated, false);
            input.setFluid(inputBefore); output.setFluid(outputBefore);
            return false;
        }
        return true;
    }
}
