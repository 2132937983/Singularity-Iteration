// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.reactor;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.runtime.heat.HeatStorage;
import com.miophas.singularity_iteration.core.api.heat.IHeatStorage;
import java.util.function.BooleanSupplier;

/**
 * 反应堆外壳热的只读视图。
 *
 * <p>IC2 1.12.2 的反应堆没有对外 HU 能力：外壳热只能由堆内元件与冷却液处理，
 * 不能被管道抽取或注入。因此本视图只暴露读数（供热能显示用），
 * 传输一律返回 0，且 {@code canExtractHeat()} 恒为 false。
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
