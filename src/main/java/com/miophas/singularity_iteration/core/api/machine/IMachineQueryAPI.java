package com.miophas.singularity_iteration.core.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Optional;

/**
 * Machine query API.
 *
 * <p>It gives machine detection, information queries and bulk lookups.
 * It was split from IMachineAPI to follow the single responsibility principle.
 */
public interface IMachineQueryAPI {

    /**
     * Checks if there is a machine at the given position.
     */
    boolean isMachine(Level world, BlockPos pos);

    /**
     * Gets the machine information.
     *
     * @param world the world
     * @param pos the block position
     * @return an Optional with the machine information, or an empty Optional if the position has no machine or the information is not available
     */
    Optional<IMachineInfo> getMachineInfo(Level world, BlockPos pos);

    /**
     * Gets all machines of the given type.
     */
    Collection<BlockPos> getMachinesByType(Level world, IMachineAPI.MachineType type);

    /**
     * Gets all machines in the given world.
     */
    Collection<BlockPos> getAllMachines(Level world);

    /**
     * Gets the work speed multiplier of the machine.
     *
     * @deprecated The upgrade plugin system controls the speed multiplier. Use
     * {@link IUpgradeStatsAPI#getEffectiveSpeedMultiplier} to get the correct value.
     */
    @Deprecated
    double getSpeedMultiplier(Level world, BlockPos pos);

    /**
     * Gets the energy usage multiplier of the machine.
     *
     * @deprecated The upgrade plugin system controls the energy multiplier. Use
     * {@link IUpgradeStatsAPI#getEffectiveEnergyMultiplier} to get the correct value.
     */
    @Deprecated
    double getEnergyMultiplier(Level world, BlockPos pos);

    /**
     * Gets the cable tier of the machine.
     *
     * @deprecated The upgrade plugin system controls the cable tier. Use
     * {@link IUpgradeStatsAPI#getEffectiveCableTier} to get the correct value.
     */
    @Deprecated
    com.miophas.singularity_iteration.core.api.energy.ICableTier getCableTier(Level world, BlockPos pos);
}
