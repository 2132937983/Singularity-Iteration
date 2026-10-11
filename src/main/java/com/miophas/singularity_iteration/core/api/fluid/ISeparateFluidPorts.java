package com.miophas.singularity_iteration.core.api.fluid;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A machine whose input tank and output tank are separate.
 *
 * <p>It extends {@link IFluidPorts}. Thus the core automation identifies it by the port contract, not by the specific machine.
 */
public interface ISeparateFluidPorts extends IFluidPorts {

    @Override
    IFluidHandler getInputFluidTank();

    @Override
    IFluidHandler getOutputFluidTank();
}
