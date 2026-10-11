package com.miophas.singularity_iteration.core.api.reactor;


import com.miophas.singularity_iteration.core.api.energy.event.EnergyTileEvent;
import com.miophas.singularity_iteration.core.api.reactor.IReactorAPI.ReactorMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

/**
 * Interface for the main control block of a nuclear reactor.
 *
 * <p>The API layer ({@code ReactorAPIImpl}, {@code MachineAPIImpl} and others) uses this interface
 * to access the reactor. It does not depend on the implementation class
 * {@code mio_icif_nuclear_reactor_generator}.
 * Thus internal refactors (rename, move, field changes) do not break the API layer build.
 *
 * <p>This interface extends {@link IReactor}. It joins the reactor core simulation interface
 * and the API control interface.
 */
public interface IReactorController extends IReactor {

    boolean isRunning();

    @Override
    default long getHeat() {
        return getCurrentHeat();
    }

    /**
     * Sets the reactor heat.
     *
     * <p><b>Restricted operation:</b> The API cannot set the heat directly at this time.
     * This method always throws {@link UnsupportedOperationException}.
     * To change the heat, use the internal reactor mechanisms (fuel rods make heat, heat vents remove heat).
     *
     * @param heat the new heat value
     * @throws UnsupportedOperationException always; this implementation does not support this operation
     * @see IReactorAPI#getReactorCurrentHeat(Level, BlockPos) to query the current heat
     */
    @Override
    default void setHeat(long heat) {
        throw new UnsupportedOperationException(
            "setHeat is not supported via API. Use reactor internal mechanisms instead.");
    }

    /**
     * Adds heat to the reactor.
     *
     * <p><b>Restricted operation:</b> The API cannot add heat directly at this time.
     * This method always throws {@link UnsupportedOperationException}.
     * To change the heat, use the internal reactor mechanisms (fuel rods make heat, heat vents remove heat).
     *
     * @param heat the heat value to add
     * @return the heat value that was added
     * @throws UnsupportedOperationException always; this implementation does not support this operation
     * @see IReactorAPI#getReactorCurrentHeat(Level, BlockPos) to query the current heat
     */
    @Override
    default long addHeat(long heat) {
        throw new UnsupportedOperationException(
            "addHeat is not supported via API. Use reactor internal mechanisms instead.");
    }

    /**
     * Sets the maximum heat capacity of the reactor.
     *
     * <p><b>Restricted operation:</b> The API cannot set the maximum heat at this time.
     * This method always throws {@link UnsupportedOperationException}.
     * The reactor structure and the number of connected reactor chambers set the maximum heat.
     *
     * @param maxHeat the new maximum heat capacity
     * @throws UnsupportedOperationException always; this implementation does not support this operation
     * @see IReactorAPI#getReactorMaxHeat(Level, BlockPos) to query the maximum heat
     */
    @Override
    default void setMaxHeat(long maxHeat) {
        throw new UnsupportedOperationException(
            "setMaxHeat is not supported via API. Max heat is determined by reactor structure.");
    }

    /**
     * Adds heat to emit (remove) from the reactor.
     *
     * <p><b>Restricted operation:</b> The API cannot remove heat directly at this time.
     * This method always throws {@link UnsupportedOperationException}.
     * Heat vent components remove heat automatically.
     *
     * @param heat the heat to emit
     * @throws UnsupportedOperationException always; this implementation does not support this operation
     * @see IReactorAPI#getReactorCurrentTemperature(Level, BlockPos) to query the temperature (it shows the cooling effect indirectly)
     */
    @Override
    default void addEmitHeat(long heat) {
        throw new UnsupportedOperationException(
            "addEmitHeat is not supported via API. Heat dissipation is handled by heat vents.");
    }

    default float getHeatEffectModifier() {
        return 1.0f;
    }

    /**
     * <b>Restricted operation:</b> Sets the heat effect modifier.
     *
     * <p>The API cannot set the modifier directly at this time.
     * This method always throws {@link UnsupportedOperationException}.
     * The reactor calculates the modifier from its temperature automatically. You do not need to set it.
     *
     * @param modifier the modifier
     * @throws UnsupportedOperationException always; this implementation does not support this operation
     * @see #getHeatEffectModifier() to get the current modifier
     */
    default void setHeatEffectModifier(float modifier) {
        throw new UnsupportedOperationException(
            "setHeatEffectModifier is not supported via API. Modifier is computed from temperature.");
    }

    default long getReactorEnergyOutput() {
        return getCurrentEnergyGeneration();
    }

    /**
     * Gets the item at a grid position.
     *
     * <p><b>Restricted operation:</b> The API cannot access item slots by grid position at this time.
     * This method always throws {@link UnsupportedOperationException}.
     * To access items, use {@link #getItemHandler()} to get the item handler.
     *
     * @param x X coordinate (0-based column index)
     * @param y Y coordinate (0-based row index)
     * @return the item at this position
     * @throws UnsupportedOperationException always; this implementation does not support this operation
     * @see #getItemHandler() to access all slots through the item handler
     */
    default ItemStack getItemAt(int x, int y) {
        throw new UnsupportedOperationException(
            "getItemAt is not supported via API. Use getItemHandler() instead.");
    }

    /**
     * Sets the item at a grid position.
     *
     * <p><b>Restricted operation:</b> The API cannot set item slots by grid position at this time.
     * This method always throws {@link UnsupportedOperationException}.
     * To change items, use {@link #getItemHandler()} to get the item handler.
     *
     * @param x X coordinate (0-based column index)
     * @param y Y coordinate (0-based row index)
     * @param stack the item to put
     * @throws UnsupportedOperationException always; this implementation does not support this operation
     * @see #getItemHandler() to access all slots through the item handler
     */
    default void setItemAt(int x, int y, ItemStack stack) {
        throw new UnsupportedOperationException(
            "setItemAt is not supported via API. Use getItemHandler() instead.");
    }

    /**
     * Makes the reactor explode.
     *
     * <p><b>Restricted operation:</b> The API cannot start an explosion at this time.
     * This method always throws {@link UnsupportedOperationException}.
     * The internal logic starts the explosion automatically only when the heat is more than
     * the maximum capacity (meltdown).
     *
     * @throws UnsupportedOperationException always; this implementation does not support this operation
     * @see IReactorAPI#getReactorCurrentTemperature(Level, BlockPos) to monitor the temperature and prevent a meltdown
     */
    default void explode() {
        throw new UnsupportedOperationException(
            "explode is not supported via API. Explosion is triggered automatically on meltdown.");
    }

    default int getTickRate() {
        return 20;
    }

    default boolean produceEnergy() {
        return isRunning();
    }

    default boolean isFluidCooled() {
        return getApiReactorMode() == ReactorMode.FLUID;
    }

    long getCurrentHeat();

    long getMaxHeat();

    /**
     * Gets the current reactor temperature in degrees Celsius.
     *
     * <p>The return type is {@code double} to keep decimal precision (for example 20.5 °C).
     *
     * @return the current temperature in degrees Celsius
     */
    double getCurrentTemperature();

    long getCurrentEnergyGeneration();

    int getAvailableColumns();

    /**
     * Gets the list of reactor sub-blocks. The list includes the main control block and the connected reactor chambers.
     *
     * <p>The list contains the markers of all sub-blocks that are part of the energy network.
     * The grid queries the energy properties of each sub-block separately.
     *
     * @return the list of sub-block markers
     * @see EnergyTileEvent.IEnergyTileMarker
     */
    List<? extends EnergyTileEvent.IEnergyTileMarker> getSubTiles();

    boolean isValidFluidReactorStructure();

    IItemHandler getItemHandler();

    IFluidHandler getFluidHandler();

    ReactorMode getApiReactorMode();
}
