package com.miophas.singularity_iteration.core.api.machine;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collection;

/**
 * Machine information.
 *
 * <p>A read-only view of the runtime state of a machine.
 * Use it to query the energy, progress, recipe and other information of the machine.
 *
 * <p>Example:
 * <pre>{@code
 * IMachineInfo info = machineAPI.getMachineInfo(world, pos);
 * if (info != null) {
 *     long energy = info.getStoredEnergy();
 *     int progress = info.getProgress();
 *     boolean working = info.isWorking();
 * }
 * }</pre>
 */
public interface IMachineInfo {

    /**
     * Gets the world of the machine.
     */
    Level getWorld();

    /**
     * Gets the block position of the machine.
     */
    BlockPos getPos();

    /**
     * Gets the machine type.
     */
    IMachineAPI.MachineType getMachineType();

    /**
     * Gets the energy that the machine stores now, in EU.
     */
    long getStoredEnergy();

    /**
     * Gets the maximum energy capacity of the machine, in EU.
     */
    long getMaxEnergy();

    /**
     * Gets the current work progress of the machine, in ticks.
     */
    int getProgress();

    /**
     * Gets the maximum work progress of the machine, in ticks.
     */
    int getMaxProgress();

    /**
     * Gets the work progress as a fraction (0.0 to 1.0). Returns 0.0 if the maximum progress is 0 or less.
     */
    default double getProgressPercent() {
        if (getMaxProgress() <= 0) return 0.0;
        return (double) getProgress() / getMaxProgress();
    }

    /**
     * Checks if the machine is working now.
     */
    boolean isWorking();

    /**
     * Gets the current energy consumption of the machine, in EU/t.
     */
    long getEnergyPerTick();

    /**
     * Gets the base energy consumption of the machine, in EU/t. Upgrades do not change this value.
     */
    long getBaseEnergyPerTick();

    /**
     * Gets the cable tier of the machine.
     */
    ICableTier getCableTier();

    /**
     * Gets the current input item of the machine.
     */
    ItemStack getInputItem();

    /**
     * Gets the current output item of the machine.
     */
    ItemStack getOutputItem();

    /**
     * Gets the items in all input slots of the machine.
     */
    Collection<ItemStack> getInputItems();

    /**
     * Gets the items in all output slots of the machine.
     */
    Collection<ItemStack> getOutputItems();

    /**
     * Gets the duration of the current run, in ticks.
     */
    long getRuntimeTicks();

    /**
     * Gets the total number of items that the machine processed.
     */
    long getTotalProcessed();

    /**
     * Gets the speed multiplier of the machine.
     */
    double getSpeedMultiplier();

    /**
     * Gets the energy consumption multiplier of the machine.
     */
    double getEnergyMultiplier();

    /**
     * Checks if the machine accepts upgrades.
     */
    boolean supportsUpgrades();

    /**
     * Gets the installed upgrades.
     */
    Collection<ItemStack> getInstalledUpgrades();

    /**
     * Gets the efficiency of the machine as a percentage.
     *
     * <p>The efficiency is the work output for each unit of energy input:
     * {@code (speedMultiplier / energyMultiplier) * 100}.
     * 100% means that no upgrade has an effect.
     * Less than 100% means that the machine uses more energy than it gains in speed
     * (for example with an overclocker upgrade).
     * More than 100% means better energy efficiency (for example with an efficiency upgrade).
     *
     * <p>If the energy multiplier is zero or negative (a bug or a state that is not valid),
     * this method returns 0.0. This shows that the machine cannot work correctly.
     */
    default double getEfficiency() {
        double speed = getSpeedMultiplier();
        double energy = getEnergyMultiplier();
        // If the energy multiplier is 0 or negative, the machine state is not valid. The efficiency is 0.
        if (energy <= 0) return 0.0;
        return (speed / energy) * 100.0;
    }
}