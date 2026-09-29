package com.miophas.singularity_iteration.core.runtime.heat;

import com.miophas.singularity_iteration.core.api.heat.IHeatStorage;

import com.miophas.singularity_iteration.core.api.util.BoundedUnits;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * HU (Heat Units) 热能存储的参考实现
 * 实现了 IHeatStorage 接口和 NBT 序列化
 * 
 * 与 FE 的区别：
 * - 热能会自然散失
 * - 有温度概念
 * - 传输效率受距离和介质影响
 */
@SuppressWarnings("null")
public class HeatStorage implements IHeatStorage, INBTSerializable<Tag> {
    
    protected long heat;
    protected long capacity;
    protected long maxReceive;
    protected long maxExtract;
    protected int baseTemp;
    protected int maxTemp;
    protected float lossFactor;
    
    public HeatStorage(long capacity, long maxReceive, long maxExtract, 
                       int baseTemp, int maxTemp, float lossFactor) {
        this.capacity = Math.max(0, capacity);
        this.maxReceive = Math.max(0, maxReceive);
        this.maxExtract = Math.max(0, maxExtract);
        this.baseTemp = baseTemp;
        this.maxTemp = Math.max(baseTemp, maxTemp);
        this.lossFactor = BoundedUnits.nonNegativeFactor(lossFactor);
        this.heat = 0;
    }
    
    public HeatStorage(long capacity, long maxReceive, long maxExtract) {
        this(capacity, maxReceive, maxExtract, 20, 1000, 0.01f);
    }
    
    public HeatStorage(long capacity, long maxReceive, long maxExtract, long heat,
                       int baseTemp, int maxTemp, float lossFactor) {
        this(capacity, maxReceive, maxExtract, baseTemp, maxTemp, lossFactor);
        setHeat(heat);
    }
    
    @Override
    public long receiveHeat(long toReceive, boolean simulate) {
        if (!canReceiveHeat() || toReceive <= 0) {
            return 0;
        }
        
        long heatReceived = Math.min(this.capacity - this.heat, 
                                     Math.min(this.maxReceive, toReceive));
        if (heatReceived < 0) heatReceived = 0;
        if (!simulate && heatReceived > 0) {
            this.heat += heatReceived;
        }
        return heatReceived;
    }
    
    @Override
    public long extractHeat(long toExtract, boolean simulate) {
        if (!canExtractHeat() || toExtract <= 0) {
            return 0;
        }
        
        long heatExtracted = Math.min(this.heat, 
                                     Math.min(this.maxExtract, toExtract));
        if (!simulate && heatExtracted > 0) {
            this.heat -= heatExtracted;
        }
        return heatExtracted;
    }
    
    @Override
    public long getHeatStored() {
        return this.heat;
    }
    
    @Override
    public long getMaxHeatStored() {
        return this.capacity;
    }
    
    @Override
    public boolean canExtractHeat() {
        return this.maxExtract > 0 && this.heat > 0;
    }
    
    @Override
    public boolean canReceiveHeat() {
        return this.maxReceive > 0 && this.heat < this.capacity;
    }
    
    public long getMaxExtract() {
        return this.maxExtract;
    }
    
    public long getMaxReceive() {
        return this.maxReceive;
    }
    
    @Override
    public int getTemperature() {
        return BoundedUnits.gauge(this.heat, this.capacity, this.baseTemp, this.maxTemp);
    }
    
    @Override
    public long getHeatLossPerTick() {
        int temp = getTemperature();
        if (temp <= this.baseTemp) return 0;
        return Math.min(this.heat, (long) (((long) temp - this.baseTemp) * this.lossFactor));
    }
    
    public void setHeat(long heat) {
        this.heat = BoundedUnits.clamp(heat, this.capacity);
    }
    
    public void setCapacity(long capacity) {
        this.capacity = Math.max(0, capacity);
        this.heat = BoundedUnits.clamp(this.heat, this.capacity);
    }
    
    public long applyHeatLoss() {
        long loss = getHeatLossPerTick();
        if (loss > 0 && this.heat > 0) {
            long actualLoss = Math.min(loss, this.heat);
            this.heat -= actualLoss;
            return actualLoss;
        }
        return 0;
    }
    
    public long consumeHeatInternal(long amount, boolean simulate) {
        long heatConsumed = BoundedUnits.extract(this.heat, amount, Long.MAX_VALUE);
        if (!simulate) {
            this.heat -= heatConsumed;
        }
        return heatConsumed;
    }
    
    public long generateHeatInternal(long amount, boolean simulate) {
        long heatGenerated = BoundedUnits.receive(this.heat, this.capacity, amount, Long.MAX_VALUE);
        if (!simulate) {
            this.heat += heatGenerated;
        }
        return heatGenerated;
    }
    
    public int getBaseTemperature() {
        return this.baseTemp;
    }
    
    public int getMaxTemperature() {
        return this.maxTemp;
    }
    
    public float getLossFactor() {
        return this.lossFactor;
    }
    
    @Override
    public Tag serializeNBT(HolderLookup.Provider provider) {
        return LongTag.valueOf(this.getHeatStored());
    }
    
    @Override
    public void deserializeNBT(HolderLookup.Provider provider, Tag nbt) {
        if (nbt instanceof LongTag longNbt) {
            setHeat(longNbt.getAsLong());
        } else if (nbt instanceof net.minecraft.nbt.IntTag intNbt) {
            setHeat(intNbt.getAsInt());
        }
    }
}
