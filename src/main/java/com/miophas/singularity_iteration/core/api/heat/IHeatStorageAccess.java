package com.miophas.singularity_iteration.core.api.heat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Heat storage access interface.
 *
 * <p>It gives read and write access to heat blocks. Use it to query and change the heat state.
 * HU (Heat Units) is the heat unit of mio_icif.
 * <p>
 * The write methods use the same design as {@code chargeEnergy/dischargeEnergy} in {@code IEnergyTileAccess}.
 * Add-on mods can use these methods to put heat into the heat system or to remove heat from it directly.
 */
public interface IHeatStorageAccess {

    /**
     * Gets the world of the block.
     */
    Level getWorld();

    /**
     * Gets the block position.
     */
    BlockPos getPos();

    /**
     * Gets the heat that is stored now (HU).
     */
    long getHeatStored();

    /**
     * Gets the maximum heat capacity (HU).
     */
    long getMaxHeatStored();

    /**
     * Gets the temperature (degrees Celsius).
     */
    int getTemperature();

    /**
     * Checks if the block is a heat source.
     */
    boolean isHeatSource();

    /**
     * Checks if the block is a heat conductor.
     */
    boolean isHeatConductor();

    /**
     * Checks if heat can be extracted.
     */
    boolean canExtractHeat();

    /**
     * Checks if heat can be received.
     */
    boolean canReceiveHeat();

    /**
     * Puts heat into the storage.
     * <p>
     * The result does not go above the maximum capacity. If {@link #canReceiveHeat()} returns false, this method always returns 0.
     *
     * @param amount    the heat to put in (HU). It must be ≥ 0.
     * @param simulate  if true, only simulate. The storage does not change.
     * @return the heat that was put in (HU)
     */
    long insertHeat(long amount, boolean simulate);

    /**
     * Extracts heat from the storage.
     * <p>
     * The result does not go above the stored quantity. If {@link #canExtractHeat()} returns false, this method always returns 0.
     *
     * @param amount    the heat to extract (HU). It must be ≥ 0.
     * @param simulate  if true, only simulate. The storage does not change.
     * @return the heat that was extracted (HU)
     */
    long extractHeat(long amount, boolean simulate);

    /**
     * Checks if the block is overheated.
     * The block is overheated when the temperature is ≥ 800°C.
     */
    boolean isOverheated();

    /**
     * Gets the heat loss for each tick (HU).
     * A higher temperature causes a faster loss.
     */
    long getHeatLossPerTick();

    /**
     * Sets the stored heat directly.
     * <p>
     * The value is clamped to the range [0, {@link #getMaxHeatStored()}].
     * <p>
     * <b>Note:</b> This method uses receiveHeat/extractHeat internally.
     * If the block does not accept or release heat now (for example canReceiveHeat() returns false),
     * the operation can fail without a message. After the call, use getHeatStored() to verify the actual value.
     *
     * @param amount the new heat value (HU)
     */
    void setHeatStored(long amount);
}