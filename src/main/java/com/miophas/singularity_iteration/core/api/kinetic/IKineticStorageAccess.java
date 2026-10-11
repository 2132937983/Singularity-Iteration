package com.miophas.singularity_iteration.core.api.kinetic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Access interface for kinetic storage.
 *
 * <p>Gives read and write access to a kinetic block. Use it to query and change the kinetic state.
 * KU (Kinetic Units) is the kinetic energy unit of mio_icif.
 * <p>
 * The write methods use the same design as {@code chargeEnergy/dischargeEnergy} of {@code IEnergyTileAccess}.
 * Addon mods can use these methods to put kinetic energy into the kinetic system or take it out.
 */
public interface IKineticStorageAccess {

    /**
     * Gets the level of the block.
     */
    Level getWorld();

    /**
     * Gets the block position.
     */
    BlockPos getPos();

    /**
     * Gets the stored kinetic energy (KU).
     */
    long getKineticStored();

    /**
     * Gets the maximum kinetic capacity (KU).
     */
    long getMaxKineticStored();

    /**
     * Gets the current rotation speed (RPM).
     */
    int getRPM();

    /**
     * Checks if the block is a kinetic source.
     */
    boolean isKineticSource();

    /**
     * Checks if the block is a kinetic conductor.
     */
    boolean isKineticConductor();

    /**
     * Checks if kinetic energy can be extracted.
     */
    boolean canExtractKinetic();

    /**
     * Checks if the block can receive kinetic energy.
     */
    boolean canReceiveKinetic();

    /**
     * Puts kinetic energy into the storage.
     * <p>
     * The stored value does not go above the maximum capacity. If {@link #canReceiveKinetic()} returns false, this method always returns 0.
     *
     * @param amount    the kinetic energy to insert (KU); must be ≥ 0
     * @param simulate  if true, only simulate; the storage does not change
     * @return the kinetic energy that was inserted (KU)
     */
    long insertKinetic(long amount, boolean simulate);

    /**
     * Takes kinetic energy from the storage.
     * <p>
     * The result is not more than the stored amount. If {@link #canExtractKinetic()} returns false, this method always returns 0.
     *
     * @param amount    the kinetic energy to extract (KU); must be ≥ 0
     * @param simulate  if true, only simulate; the storage does not change
     * @return the kinetic energy that was extracted (KU)
     */
    long extractKinetic(long amount, boolean simulate);

    /**
     * Checks if the block is in overspeed.
     * Overspeed starts at a rotation speed of 8000 RPM or more.
     */
    boolean isOverspeed();

    /**
     * Gets the kinetic loss per tick (KU).
     * A higher rotation speed gives a faster loss.
     */
    long getKineticLossPerTick();

    /**
     * Gets the maximum input rate for one tick (KU/t).
     */
    long getMaxReceive();

    /**
     * Gets the maximum output rate for one tick (KU/t).
     */
    long getMaxExtract();

    /**
     * Sets the stored kinetic energy directly.
     * <p>
     * The value is clamped to [0, {@link #getMaxKineticStored()}].
     * <p>
     * <b>Note:</b> this method uses receiveKinetic/extractKinetic internally.
     * If the block does not accept input or output now (for example, canReceiveKinetic() returns false),
     * the operation can fail without an error. After the call, use getKineticStored() to check the real value.
     *
     * @param amount the new kinetic energy value (KU)
     */
    void setKineticStored(long amount);
}
