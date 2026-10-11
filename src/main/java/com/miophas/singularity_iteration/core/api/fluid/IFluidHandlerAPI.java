package com.miophas.singularity_iteration.core.api.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Collection;

/**
 * Fluid handling API.
 *
 * <p>Gives methods to query and manage the fluid storage of machines.
 * All amounts are in mB.
 */

public interface IFluidHandlerAPI {

    /**
     * Checks if the block at a position has fluid storage.
     */
    boolean hasFluidHandler(Level world, BlockPos pos);

    /**
     * Gets the data of one fluid tank.
     */
    IFluidTankInfo getFluidTankInfo(Level world, BlockPos pos, int tank);

    /**
     * Gets the data of all fluid tanks.
     */
    Collection<IFluidTankInfo> getAllFluidTanks(Level world, BlockPos pos);

    /**
     * Gets the number of fluid tanks.
     */
    int getTankCount(Level world, BlockPos pos);

    /**
     * Fills fluid into a tank.
     *
     * <p>Note: this method first checks if the given tank accepts the fluid (with isFluidValid).
     * But the real fill uses NeoForge IFluidHandler.fill(), which sends the fluid to the first tank that accepts it.
     * Thus the tank parameter is only for validation. It cannot force the fill into a specific tank.
     *
     * <p>For exact control of the target tank, use the NeoForge IFluidHandler capability directly.
     *
     * @param world the level
     * @param pos the block position
     * @param tank the target tank index (only used to check fluid compatibility)
     * @param fluid the fluid to fill
     * @param simulate true to only simulate
     * @return the amount that was filled (mB)
     */
    int fillFluid(Level world, BlockPos pos, int tank, FluidStack fluid, boolean simulate);

    /**
     * Drains fluid from a tank.
     *
     * @param maxDrain the maximum amount to drain (mB)
     * @param simulate true to only simulate
     * @return the drained fluid; an empty stack if nothing was drained
     */
    FluidStack drainFluid(Level world, BlockPos pos, int tank, int maxDrain, boolean simulate);

    /**
     * Data of one fluid tank.
     */
    public interface IFluidTankInfo {
        /**
         * Gets the tank index.
         */
        int getTankIndex();

        /**
         * Gets the current fluid.
         */
        FluidStack getFluid();

        /**
         * Gets the current fluid amount (mB).
         */
        int getAmount();

        /**
         * Gets the capacity (mB).
         */
        int getCapacity();

        /**
         * Gets the fill fraction (0.0 to 1.0). Returns 0.0 if the capacity is 0 or less.
         */
        default double getFillPercent() {
            if (getCapacity() <= 0) return 0.0;
            return (double) getAmount() / getCapacity();
        }

        /**
         * Checks if the tank is empty.
         */
        default boolean isEmpty() {
            return getFluid().isEmpty() || getAmount() <= 0;
        }

        /**
         * Checks if the tank is full.
         */
        default boolean isFull() {
            return getAmount() >= getCapacity();
        }
    }
}
