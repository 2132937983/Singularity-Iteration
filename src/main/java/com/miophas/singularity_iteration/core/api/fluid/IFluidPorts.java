package com.miophas.singularity_iteration.core.api.fluid;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Contract for the fluid port routing of a machine.
 *
 * <p>This interface is more general than {@link ISeparateFluidPorts}. It can return different ports for each side,
 * and it can declare the number of ports. The default implementation ignores the side and keeps the old
 * "input tank / output tank" meaning. Thus core automation can use existing
 * {@link ISeparateFluidPorts} implementations with no changes.
 */
public interface IFluidPorts {

    IFluidHandler getInputFluidTank();

    IFluidHandler getOutputFluidTank();

    /**
     * Gets the port for a side.
     *
     * @param input true to get the input port, false to get the output port
     * @param side the access side (can be null)
     */
    default IFluidHandler fluidPort(boolean input, @Nullable Direction side) {
        return input ? getInputFluidTank() : getOutputFluidTank();
    }

    /** Returns true if the port changes with the side. When true, the core does not cache the port as a single instance. */
    default boolean routesBySide() {
        return false;
    }

    /** Gets the number of ports. The default is two (input and output). */
    default int portCount() {
        return 2;
    }
}
