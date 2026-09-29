package com.miophas.singularity_iteration.core.api.fluid;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 输入罐与输出罐彼此独立的机器。
 *
 * <p>继承自 {@link IFluidPorts}，因此核心自动化按端口契约而非具体机器识别它。
 */
public interface ISeparateFluidPorts extends IFluidPorts {

    @Override
    IFluidHandler getInputFluidTank();

    @Override
    IFluidHandler getOutputFluidTank();
}
