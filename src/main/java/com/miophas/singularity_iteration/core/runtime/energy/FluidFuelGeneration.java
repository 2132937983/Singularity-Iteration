// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.runtime.energy.engine.ConsumableGeneration;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Bounded generation using the generator's owned FluidTank and independent EU storage. */
public final class FluidFuelGeneration {
    private FluidFuelGeneration() { }
    public static ConsumableGeneration.Step tick(FluidTank fuel, CustomEUEnergyStorage storage,
            long credit, int millibucketsPerUnit, long energyPerUnit, long rate) {
        var quote = storage.scexNetworkQuote();
        if (!storage.scexNetworkControlled() || !(quote.ownerLevel() instanceof ServerLevel level)
                || !level.getServer().isSameThread() || millibucketsPerUnit <= 0)
            throw new IllegalStateException("Fuel generation requires an owned server-thread state");
        long available = !fuel.isEmpty() && fuel.isFluidValid(fuel.getFluid())
            ? fuel.getFluidAmount() / millibucketsPerUnit : 0;
        var plan = ConsumableGeneration.plan(available, credit, energyPerUnit, rate,
            storage.scexExactAmount().roomBelow(storage.getCapacity()).whole(), 0);
        int consumed = Math.toIntExact(plan.fuelConsumed() * millibucketsPerUnit);
        if (consumed > 0 && fuel.drain(consumed, IFluidHandler.FluidAction.SIMULATE).getAmount() != consumed)
            throw new IllegalStateException("Owned fuel tank did not honor its available amount");
        if (!storage.scexNetworkQuote().equals(quote)) throw new IllegalStateException("Fuel-generation quote changed");
        if (consumed > 0 && fuel.drain(consumed, IFluidHandler.FluidAction.EXECUTE).getAmount() != consumed)
            throw new IllegalStateException("Owned fuel tank changed during commit");
        if (storage.generateEnergyInternal(plan.generated(), false) != plan.generated())
            throw new IllegalStateException("Owned EU capacity changed during fuel commit");
        return plan;
    }
}
