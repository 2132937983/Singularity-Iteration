// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.energy;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Exact access surface implemented by the admitted SI EnergyTileAccessWrapper. */
public interface IEnergyTileAccess {
    Level getWorld();
    BlockPos getPos();
    long getStoredEnergy();
    long getMaxEnergy();
    long getOfferedEnergy();
    long getDemandedEnergy();
    int getSourceTier();
    int getSinkTier();
    Optional<ICableTier> getCableTier();
    boolean acceptsEnergyFrom(Direction direction);
    boolean emitsEnergyTo(Direction direction);
    boolean isSource();
    boolean isSink();
    boolean isConductor();
    long chargeEnergy(long amount, boolean simulate);
    long dischargeEnergy(long amount, boolean simulate);
    long useEnergy(long amount, boolean simulate);
    long generateEnergy(long amount, boolean simulate);
    boolean setEnergy(long amount);
    boolean setCapacity(long capacity);
    long getMaxReceive();
    long getMaxExtract();
}
