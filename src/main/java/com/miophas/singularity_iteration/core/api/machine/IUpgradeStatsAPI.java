package com.miophas.singularity_iteration.core.api.machine;

import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Upgrade statistics API.
 *
 * <p>It gives statistics about machine upgrades and queries for the effective parameters.
 * It was split from IMachineAPI to follow the single responsibility principle.
 */
public interface IUpgradeStatsAPI {

    /**
     * Gets the upgrade statistics of the machine.
     */
    Optional<IMachineUpgradeStats> getUpgradeStats(Level world, BlockPos pos);

    /**
     * Gets the actual process time multiplier after the upgrades are applied.
     *
     * <p><b>Note</b>: This method returns the <b>process time multiplier</b>. It does not return a speed multiplier.
     * A lower value is faster (for example 0.5 means half the process time and double the speed).
     *
     * <p><b>Name</b>: The method name keeps "Speed" for backward compatibility. The actual meaning is "TimeMultiplier".
     * To get the real speed multiplier, use {@code 1.0 / getEffectiveProcessTimeMultiplier(world, pos)}.
     *
     * @return the process time multiplier (a lower value is faster; 1.0 is the base speed)
     * @deprecated The meaning does not agree with the method name. Use {@link #getEffectiveProcessTimeMultiplier(Level, BlockPos)} instead.
     */
    @Deprecated
    double getEffectiveSpeedMultiplier(Level world, BlockPos pos);

    /**
     * Gets the actual process time multiplier after the upgrades are applied (method name with the correct meaning).
     *
     * <p>A lower value is faster (for example 0.5 means half the process time and double the speed).
     * To get the real speed multiplier, use {@code 1.0 / getEffectiveProcessTimeMultiplier(world, pos)}.
     *
     * @return the process time multiplier (a lower value is faster; 1.0 is the base speed)
     */
    double getEffectiveProcessTimeMultiplier(Level world, BlockPos pos);

    /**
     * Gets the actual energy usage multiplier after the upgrades are applied.
     *
     * @return the energy usage multiplier
     */
    double getEffectiveEnergyMultiplier(Level world, BlockPos pos);

    /**
     * Gets the effective cable tier after the upgrades are applied.
     *
     * @return the effective cable tier
     */
    Optional<ICableTier> getEffectiveCableTier(Level world, BlockPos pos);

    /**
     * Gets the energy capacity bonus from the upgrades.
     *
     * @return the capacity bonus (EU)
     */
    long getEnergyCapacityBonus(Level world, BlockPos pos);

    /**
     * Gets the actual work time after the upgrades are applied.
     *
     * @return the actual work time (ticks)
     */
    int getEffectiveProcessTicks(Level world, BlockPos pos);

    /**
     * Gets the actual energy for each tick after the upgrades are applied.
     *
     * @return the actual energy for each tick (EU)
     */
    long getEffectiveEnergyPerTick(Level world, BlockPos pos);
}