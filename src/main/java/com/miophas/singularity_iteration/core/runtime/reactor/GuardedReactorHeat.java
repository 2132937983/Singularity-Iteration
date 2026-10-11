// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.reactor;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.runtime.heat.HeatStorage;
import com.miophas.singularity_iteration.core.api.heat.IHeatStorage;
import java.util.function.BooleanSupplier;

/**
 * Read-only view of the reactor hull heat.
 *
 * <p>The IC2 1.12.2 reactor has no external HU capability. Only components in the reactor and the coolant change hull heat.
 * Pipes cannot extract or insert this heat. Thus this view only gives readings (for heat displays).
 * All transfer methods return 0, and {@code canExtractHeat()} is always false.
 * When {@code available} is false, the readings are 0 (temperature is 20).
 */
public final class GuardedReactorHeat implements IHeatStorage,IMioIcifCapabilities.IHeatStorage {
    private final HeatStorage storage;private final BooleanSupplier available;
    public GuardedReactorHeat(HeatStorage storage,BooleanSupplier available){this.storage=storage;this.available=available;}
    @Override public long getHeatStored(){return available.getAsBoolean()?storage.getHeatStored():0;}
    @Override public long getMaxHeatStored(){return available.getAsBoolean()?storage.getMaxHeatStored():0;}
    @Override public long receiveHeat(long amount,boolean simulate){return 0;}
    @Override public long extractHeat(long amount,boolean simulate){return 0;}
    @Override public boolean canReceiveHeat(){return false;}
    @Override public boolean canExtractHeat(){return false;}
    @Override public int getTemperature(){return available.getAsBoolean()?storage.getTemperature():20;}
    @Override public boolean isOverheated(){return available.getAsBoolean()&&storage.isOverheated();}
    @Override public long getHeatLossPerTick(){return 0;}
    @Override public long getMaxReceive(){return 0;}
    @Override public long getMaxExtract(){return 0;}
}
