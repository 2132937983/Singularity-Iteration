// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import java.util.function.BooleanSupplier;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** A real foreign receipt plus a conductor toll, charged only when energy was delivered. */
public final class RoutedFeTarget implements IEnergyStorage {
    private final IEnergyStorage target;
    private final int limit, loss;
    private final BooleanSupplier current;
    private int delivered, charged;
    public RoutedFeTarget(IEnergyStorage target, int limit, int loss, BooleanSupplier current) {
        if (limit < 0 || loss < 0) throw new IllegalArgumentException("Negative route allowance");
        this.target = java.util.Objects.requireNonNull(target);
        this.limit = limit; this.loss = loss; this.current = java.util.Objects.requireNonNull(current);
    }
    public int delivered() { return delivered; }
    public int charged() { return charged; }
    @Override public int receiveEnergy(int amount, boolean simulate) {
        int allowed = Math.min(amount, limit);
        if (allowed <= loss || !current.getAsBoolean()) return 0;
        int accepted = target.receiveEnergy(allowed - loss, simulate);
        if (accepted < 0 || accepted > allowed - loss) throw new IllegalStateException("Invalid routed FE receipt");
        int debit = accepted == 0 ? 0 : Math.addExact(accepted, loss);
        if (!simulate) { delivered = accepted; charged = debit; }
        return debit;
    }
    @Override public boolean canReceive() { return limit > loss && current.getAsBoolean() && target.canReceive(); }
    @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
    @Override public boolean canExtract() { return false; }
    @Override public int getEnergyStored() { return target.getEnergyStored(); }
    @Override public int getMaxEnergyStored() { return target.getMaxEnergyStored(); }
}
