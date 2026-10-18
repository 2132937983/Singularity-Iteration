package com.miophas.singularity_iteration.core.api.machine;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.api.energy.IEnergyStorageAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for energy blocks.
 * All blocks that can store or move energy must implement this interface.
 */
public interface IEnergyBlock {
    
    /**
     * Returns the energy storage.
     *
     * @return the energy storage instance
     */
    IEnergyStorageAccess getEnergyStorage();
    
    /**
     * Returns true if this block is a power source.
     *
     * @return true if it is a power source
     */
    boolean isPowerSource();
    
    /**
     * Returns the power output (EU/t).
     *
     * @return the power output
     */
    long getPowerOutput();
    
    /**
     * Returns the effective cable tier.
     *
     * @return the cable tier
     */
    ICableTier getEffectiveCableTier();
    
    /**
     * Returns the effective energy capacity (EU).
     *
     * @return the energy capacity
     */
    long getEffectiveCapacity();
    
    /**
     * Returns the effective maximum receive rate (EU/t).
     *
     * @return the maximum receive rate
     */
    long getEffectiveMaxReceive();
    
    /**
     * Uses energy (internal work). The maxExtract limit does not apply.
     *
     * <p>Use this method for internal processing and operation of a machine.
     * It is different from external output (extract/discharge): the {@code maxExtract} limit does not apply.
     *
     * @param amount the amount to use (EU)
     * @param simulate if true, only simulate (do not use energy)
     * @return the energy that was used (EU)
     */
    long useEnergy(long amount, boolean simulate);
    
    /**
     * Generates energy (internal generation). The maxReceive limit does not apply.
     *
     * <p>Use this method for the internal energy production of a generator.
     * It is different from external input (receive/charge): the {@code maxReceive} limit does not apply.
     *
     * @param amount the amount to generate (EU)
     * @param simulate if true, only simulate (do not generate energy)
     * @return the energy that was generated (EU)
     */
    long generateEnergy(long amount, boolean simulate);
    
    /**
     * Returns true if a connection is possible on the given side.
     *
     * <p>Since 0.1.10.4 the SI energy network also uses this for machines (an
     * {@code AbstractEnergyBlockEntity}): a side that returns false takes and gives no EU, also from
     * a directly touching generator. A machine whose answer changes calls
     * {@code IndependentSiEnergy.changed(blockEntity)}. The base class opens all sides.
     *
     * @param side the side, or null for no specific side
     * @return true if a connection is possible
     */
    boolean canConnect(@Nullable Direction side);
    
    /**
     * Returns the machine type.
     *
     * <p>This is only meaningful for real machines (producers, generators and so on).
     * Energy blocks that are not machines, such as cables and energy storage boxes, return {@link IMachineAPI.MachineType#CUSTOM}.
     *
     * @return the machine type; the default is {@link IMachineAPI.MachineType#CUSTOM}
     */
    default IMachineAPI.MachineType getMachineType() {
        return IMachineAPI.MachineType.CUSTOM;
    }
    
    /**
     * Returns the world.
     *
     * @return the world instance
     */
    Level getLevel();
    
    /**
     * Returns the position.
     *
     * @return the block position
     */
    BlockPos getBlockPos();
    
    /**
     * Returns the block state.
     *
     * @return the block state
     */
    BlockState getBlockState();
}