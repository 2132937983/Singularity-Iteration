package com.miophas.singularity_iteration.core.api.machine;

/**
 * Interface for generators that can be forced to start or stop.
 *
 * <p>{@code GeneratorHelper} uses this interface to change the burn progress of a generator.
 * It does not write directly to the {@code burnTime} field of the implementation class.
 */
public interface IBurnControl {

    /** @return the default burn duration (ticks), or 0 if there is none */
    int getDefaultBurnTime();

    /** Sets the remaining burn time of the current burn (ticks). */
    void setBurnTime(int ticks);
}
