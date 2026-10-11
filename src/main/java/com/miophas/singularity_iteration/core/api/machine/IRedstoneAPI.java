package com.miophas.singularity_iteration.core.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Redstone control API.
 *
 * <p>Gives redstone signal control for machines.
 * It was split from IMachineAPI to follow the single responsibility principle.
 */
public interface IRedstoneAPI {

    // ========== Enum versions (recommended) ==========

    /**
     * Gets the redstone mode of a machine (enum version).
     * @return the redstone mode; NONE if the block is not a machine
     */
    @NotNull
    IMachineAPI.RedstoneMode getRedstoneModeEnum(Level world, BlockPos pos);

    /**
     * Sets the redstone mode of a machine (enum version).
     */
    void setRedstoneModeEnum(Level world, BlockPos pos, IMachineAPI.RedstoneMode mode);

    /**
     * Checks if the redstone signal lets the machine work.
     */
    boolean isRedstoneAllowed(Level world, BlockPos pos);

    // ========== int versions (backward compatibility) ==========

    /**
     * Gets the redstone mode of an energy storage block.
     *
     * @return the redstone mode (0-7); -1 if the block is not an energy storage block
     */
    int getRedstoneMode(Level world, BlockPos pos);

    /**
     * Sets the redstone mode of an energy storage block.
     *
     * @param mode the redstone mode (0-7)
     * @return true if the mode was set
     */
    boolean setRedstoneMode(Level world, BlockPos pos, int mode);

    /**
     * Gets the name of a redstone mode.
     *
     * @param mode the redstone mode (0-7)
     * @return the mode name as plain English text for logs and tools, never null.
     *         For UI text, use a translation key.
     */
    String getRedstoneModeName(int mode);

    /**
     * Checks if the energy storage block emits a redstone signal now.
     */
    boolean isEmittingRedstone(Level world, BlockPos pos);

    /**
     * Gets the redstone signal strength of the energy storage block.
     *
     * @return the signal strength (0-15)
     */
    int getRedstoneSignalStrength(Level world, BlockPos pos);

    /**
     * Checks if the energy storage block receives a redstone input signal.
     */
    boolean hasRedstoneInput(Level world, BlockPos pos);

    /**
     * Checks if the energy storage block allows energy output.
     */
    boolean isEnergyOutputEnabled(Level world, BlockPos pos);
}