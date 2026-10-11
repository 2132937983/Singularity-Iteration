package com.miophas.singularity_iteration.core.prefab.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A fluid handler view that limits fill/drain for each side.
 *
 * <p>It applies the limits only at the capability boundary. It does not change the amounts or the validation rules of the wrapped handler.
 */
public final class SidedFluidHandler implements IFluidHandler {

    private final IFluidHandler delegate;
    private final boolean canFill;
    private final boolean canDrain;

    public SidedFluidHandler(IFluidHandler delegate, boolean canFill, boolean canDrain) {
        if (delegate == null) throw new IllegalArgumentException("fluid handler");
        this.delegate = delegate;
        this.canFill = canFill;
        this.canDrain = canDrain;
    }

    @Override
    public int getTanks() { return delegate.getTanks(); }

    @Override
    public FluidStack getFluidInTank(int tank) { return delegate.getFluidInTank(tank); }

    @Override
    public int getTankCapacity(int tank) { return delegate.getTankCapacity(tank); }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) { return delegate.isFluidValid(tank, stack); }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!canFill) return 0;
        return delegate.fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (!canDrain) return FluidStack.EMPTY;
        return delegate.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (!canDrain) return FluidStack.EMPTY;
        return delegate.drain(maxDrain, action);
    }
}
