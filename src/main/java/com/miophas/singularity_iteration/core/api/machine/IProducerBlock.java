package com.miophas.singularity_iteration.core.api.machine;

import com.miophas.singularity_iteration.core.api.energy.IEnergyStorageAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

/**
 * Interface for production machines.
 * All machines that process recipes must implement this interface.
 */
public interface IProducerBlock {

    /**
     * Forces the machine to start work.
     */
    void forceStartWork();

    /**
     * Forces the machine to stop work.
     */
    void forceStopWork();

    /**
     * Gets the upgrade statistics.
     * @return the upgrade statistics
     */
    IMachineUpgradeStats getUpgradeStats();

    /**
     * Gets the index of the first upgrade slot.
     * @return the index of the first upgrade slot
     */
    int getUpgradeSlotStart();

    /**
     * Gets the number of upgrade slots.
     * @return the number of upgrade slots
     */
    int getUpgradeSlotCount();

    /**
     * Gets the item handler.
     * @return the item handler
     */
    IItemHandler getItemHandler();

    /**
     * Gets the list of upgrade items.
     * @return the list of upgrade items
     */
    List<ItemStack> getUpgrades();

    /**
     * Gets the work progress.
     * @return the current progress (ticks)
     */
    int getProgress();

    /**
     * Gets the maximum work progress.
     * @return the maximum progress (ticks)
     */
    int getMaxProgress();

    /**
     * Gets the base maximum progress, before upgrades.
     * @return the base maximum progress (ticks)
     */
    int getBaseMaxProgress();

    /**
     * Gets the energy use per tick.
     * @return the energy use per tick (EU)
     */
    long getEnergyPerTick();

    /**
     * Gets the slot layout.
     * @return the slot layout
     */
    ISlotLayout getSlotLayout();

    /**
     * Gets the container size.
     * @return the container size (number of slots)
     */
    int getContainerSize();

    /**
     * Gets the energy storage.
     * @return the energy storage
     */
    IEnergyStorageAccess getEnergyStorage();

    /**
     * Gets the work completion callback.
     * @return the work completion callback, or null
     */
    IWorkCompleteCallback getWorkCompleteCallback();

    /**
     * Checks if the machine is working.
     * @return true if the machine is working
     */
    boolean isWorking();

    /**
     * Gets the effective energy use per tick, with upgrades applied.
     * @return the effective energy use (EU)
     */
    long getEffectiveEnergyPerTick();

    /**
     * Gets the total processed amount.
     * @return the total processed amount
     */
    long getTotalProcessed();

    /**
     * Gets the current redstone mode.
     * @return the redstone mode; the default is NONE
     */
    default IMachineAPI.RedstoneMode getRedstoneMode() {
        return IMachineAPI.RedstoneMode.NONE;
    }

    /**
     * Sets the redstone mode.
     * @param mode the redstone mode
     */
    default void setRedstoneMode(IMachineAPI.RedstoneMode mode) {
        // Default: does nothing. Subclasses can override it.
    }

    default void setLit(boolean lit) {
        // Default: does nothing. Subclasses can override it.
    }

    default boolean isLit() {
        return isWorking();
    }

    default void serverTick() {
        // Default: does nothing. Subclasses can override it.
    }

    default IMachineAPI.MetalFormerMode getMetalFormerMode() {
        return IMachineAPI.MetalFormerMode.ROLLING;
    }

    default void setMetalFormerMode(IMachineAPI.MetalFormerMode mode) {
        // Default: does nothing. Subclasses can override it.
    }
}
