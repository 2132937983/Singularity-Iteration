package com.singularity_iteration.mio_icif.energy.heat;

import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

/**
 * 使用 DataComponent 存储热能的实现
 * 用于物品（如热电池、燃料棒等）
 */
@SuppressWarnings("null")
public class ComponentHeatStorage implements IHeatStorage {
    
    protected final MutableDataComponentHolder parent;
    protected final DataComponentType<Long> heatComponent;
    protected final long capacity;
    protected final long maxReceive;
    protected final long maxExtract;
    protected final int baseTemp;
    protected final int maxTemp;
    protected final float lossFactor;
    
    public ComponentHeatStorage(MutableDataComponentHolder parent, 
                                DataComponentType<Long> heatComponent,
                                long capacity, long maxReceive, long maxExtract,
                                int baseTemp, int maxTemp, float lossFactor) {
        this.parent = parent;
        this.heatComponent = heatComponent;
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.baseTemp = baseTemp;
        this.maxTemp = maxTemp;
        this.lossFactor = lossFactor;
    }
    
    public ComponentHeatStorage(MutableDataComponentHolder parent,
                                DataComponentType<Long> heatComponent,
                                long capacity, long maxReceive, long maxExtract) {
        this(parent, heatComponent, capacity, maxReceive, maxExtract, 20, 1000, 0.01f);
    }
    
    @Override
    public long receiveHeat(long toReceive, boolean simulate) {
        if (!canReceiveHeat() || toReceive <= 0) {
            return 0;
        }
        
        long currentHeat = getHeatStored();
        long heatReceived = Math.min(this.capacity - currentHeat, 
                                     Math.min(this.maxReceive, toReceive));
        if (heatReceived < 0) heatReceived = 0;
        if (!simulate && heatReceived > 0) {
            setHeat(currentHeat + heatReceived);
        }
        return heatReceived;
    }
    
    @Override
    public long extractHeat(long toExtract, boolean simulate) {
        if (!canExtractHeat() || toExtract <= 0) {
            return 0;
        }
        
        long currentHeat = getHeatStored();
        long heatExtracted = Math.min(currentHeat,
                                     Math.min(this.maxExtract, toExtract));
        if (!simulate && heatExtracted > 0) {
            setHeat(currentHeat - heatExtracted);
        }
        return heatExtracted;
    }
    
    @Override
    public long getHeatStored() {
        Long heat = this.parent.get(this.heatComponent);
        return heat != null ? heat : 0;
    }
    
    @Override
    public long getMaxHeatStored() {
        return this.capacity;
    }
    
    @Override
    public boolean canExtractHeat() {
        return this.maxExtract > 0 && getHeatStored() > 0;
    }
    
    @Override
    public boolean canReceiveHeat() {
        return this.maxReceive > 0 && getHeatStored() < this.capacity;
    }
    
    @Override
    public int getTemperature() {
        if (this.capacity == 0) return this.baseTemp;
        long heatPercent = (getHeatStored() * 100) / this.capacity;
        return this.baseTemp + (int)((heatPercent * (this.maxTemp - this.baseTemp)) / 100);
    }
    
    @Override
    public long getHeatLossPerTick() {
        int temp = getTemperature();
        if (temp <= this.baseTemp) return 0;
        return (long) ((temp - this.baseTemp) * this.lossFactor);
    }
    
    protected void setHeat(long heat) {
        this.parent.set(this.heatComponent, Math.max(0, Math.min(this.capacity, heat)));
    }
    
    public long applyHeatLoss() {
        long loss = getHeatLossPerTick();
        if (loss > 0) {
            long currentHeat = getHeatStored();
            long actualLoss = Math.min(loss, currentHeat);
            if (actualLoss > 0) {
                setHeat(currentHeat - actualLoss);
                return actualLoss;
            }
        }
        return 0;
    }
}