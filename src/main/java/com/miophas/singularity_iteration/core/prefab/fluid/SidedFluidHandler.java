package com.miophas.singularity_iteration.core.prefab.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 按面别限制填充/抽取的流体处理器视图。
 *
 * <p>只在能力边界上做限制，不改变被包装处理器的余额与校验规则。
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
