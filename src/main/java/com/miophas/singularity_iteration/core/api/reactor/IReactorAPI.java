package com.miophas.singularity_iteration.core.api.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;

/**
 * Nuclear reactor API.
 *
 * <p>Use this interface to work with the mio_icif nuclear reactor system. It lets you:
 * <ul>
 *   <li>Query the properties of reactor components.</li>
 *   <li>Check if an item is a reactor component.</li>
 *   <li>Get the type of a reactor component.</li>
 *   <li>Monitor the operating state of a reactor.</li>
 *   <li>Get the structure and heat data of a reactor.</li>
 * </ul>
 */
public interface IReactorAPI {

    /**
     * Checks if the item is a nuclear reactor component.
     *
     * @param stack the item stack
     * @return true if the item is a reactor component
     */
    boolean isReactorComponent(ItemStack stack);

    /**
     * Gets the type of a reactor component.
     *
     * @param stack the item stack
     * @return the component type, or null if the item is not a reactor component
     */
    ReactorComponentType getComponentType(ItemStack stack);

    /**
     * Gets the neutron pulse output of a reactor component.
     *
     * @param stack the item stack
     * @return the number of neutron pulses, or 0 if the item is not a fuel rod
     */
    int getNeutronPulseOutput(ItemStack stack);

    /**
     * Gets the heat output of a reactor component.
     *
     * @param stack the item stack
     * @return the heat output (HU)
     */
    int getHeatOutput(ItemStack stack);

    /**
     * Gets the maximum heat storage of a reactor component.
     *
     * @param stack the item stack
     * @return the maximum heat storage (HU)
     */
    int getMaxHeatStorage(ItemStack stack);

    /**
     * Gets the heat transfer efficiency of a reactor component.
     *
     * @param stack the item stack
     * @return the heat transfer efficiency (0 to 100)
     */
    int getHeatTransferEfficiency(ItemStack stack);

    /**
     * Checks if the item is MOX fuel.
     *
     * @param stack the item stack
     * @return true if the item is MOX fuel
     */
    boolean isMoxFuel(ItemStack stack);

    /**
     * Checks if the item is a fuel rod.
     *
     * @param stack the item stack
     * @return true if the item is a fuel rod
     */
    boolean isFuelRod(ItemStack stack);

    /**
     * Gets the IDs of all registered reactor components.
     */
    Collection<net.minecraft.resources.ResourceLocation> getAllReactorComponentIds();

    // ========== Reactor monitoring API ==========

    /**
     * Checks if the reactor is running.
     *
     * @param world the level
     * @param pos the position of the reactor controller
     * @return true if the reactor is running
     */
    boolean isReactorRunning(Level world, BlockPos pos);

    /**
     * Gets the current heat of the reactor.
     *
     * @param world the level
     * @param pos the position of the reactor controller
     * @return the current heat value
     */
    long getReactorCurrentHeat(Level world, BlockPos pos);

    /**
     * Gets the maximum heat capacity of the reactor.
     *
     * @param world the level
     * @param pos the position of the reactor controller
     * @return the maximum heat capacity
     */
    long getReactorMaxHeat(Level world, BlockPos pos);

    /**
     * Gets the current temperature of the reactor.
     *
     * @param world the level
     * @param pos the position of the reactor controller
     * @return the current temperature (degrees Celsius)
     */
    double getReactorCurrentTemperature(Level world, BlockPos pos);

    /**
     * Gets the current energy generation of the reactor.
     *
     * @param world the level
     * @param pos the position of the reactor controller
     * @return the energy generation (EU/tick)
     */
    long getReactorCurrentEnergyGeneration(Level world, BlockPos pos);

    /**
     * Gets the number of available columns of the reactor.
     *
     * @param world the level
     * @param pos the position of the reactor controller
     * @return the number of available columns
     */
    int getReactorAvailableColumns(Level world, BlockPos pos);

    /**
     * Gets the reactor mode.
     *
     * @param world the level
     * @param pos the position of the reactor controller
     * @return the reactor mode (GENERATOR or FLUID)
     */
    ReactorMode getReactorMode(Level world, BlockPos pos);

    /**
     * Gets the detailed structure status of the reactor.
     *
     * @param world the level
     * @param pos the position of the reactor controller
     * @return the detailed structure status
     */
    ReactorStructureStatus getReactorStructureStatusDetailed(Level world, BlockPos pos);

    /**
     * Reactor modes.
     */
    enum ReactorMode {
        /** Generator mode: the reactor makes EU. */
        GENERATOR,
        /** Fluid mode: the reactor makes heat or steam. */
        FLUID
    }

    /**
     * Status of the reactor structure.
     *
     * @param valid true if the structure is complete
     * @param errors the list of error messages
     * @param chambersConnected the number of connected reactor chambers
     */
    record ReactorStructureStatus(
        boolean valid,
        List<String> errors,
        int chambersConnected
    ) {
        /**
         * Creates a valid structure status.
         */
        public static ReactorStructureStatus valid(int chambersConnected) {
            return new ReactorStructureStatus(true, List.of(), chambersConnected);
        }

        /**
         * Creates an invalid structure status.
         */
        public static ReactorStructureStatus invalid(List<String> errors) {
            return new ReactorStructureStatus(false, errors, 0);
        }
    }

    // ========== Fuel rod API ==========

    /**
     * Gets the number of neutron pulses of a fuel rod.
     *
     * @param stack the fuel rod item
     * @return the number of neutron pulses
     */
    int getFuelRodPulses(ItemStack stack);

    /**
     * Gets the number of cells in a fuel rod (single, dual or quad).
     *
     * @param stack the fuel rod item
     * @return 1 for single, 2 for dual, 4 for quad
     */
    int getFuelRodCells(ItemStack stack);

    /**
     * Checks if the fuel rod is depleted.
     *
     * @param stack the fuel rod item
     * @return true if the fuel rod is depleted
     */
    boolean isFuelRodDepleted(ItemStack stack);

    /**
     * Gets the heat output of a fuel rod.
     *
     * @param stack the fuel rod item
     * @return the heat output
     */
    long getFuelRodHeatOutput(ItemStack stack);

    /**
     * Gets the item that the fuel rod becomes when it is depleted.
     *
     * @param stack the fuel rod item
     * @return the depleted item
     */
    ItemStack getFuelRodDepletedItem(ItemStack stack);

    /**
     * Gets the type of a fuel rod.
     *
     * @param stack the fuel rod item
     * @return the fuel rod type
     */
    FuelRodType getFuelRodType(ItemStack stack);

    /**
     * Fuel rod types.
     */
    enum FuelRodType {
        SINGLE(1),
        DUAL(2),
        QUAD(4),
        /** 0.1.10.38: eight cells, two quads in one slot (the octuple proton rod). */
        OCTUPLE(8);

        private final int cells;

        FuelRodType(int cells) {
            this.cells = cells;
        }

        public int getCells() {
            return cells;
        }
    }
}
