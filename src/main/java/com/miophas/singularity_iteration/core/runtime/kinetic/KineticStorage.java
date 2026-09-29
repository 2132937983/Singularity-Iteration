package com.miophas.singularity_iteration.core.runtime.kinetic;

import com.miophas.singularity_iteration.core.api.kinetic.IKineticStorage;

import com.miophas.singularity_iteration.core.api.util.BoundedUnits;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * KU (Kinetic Units) 动能存储的参考实现
 * 实现了 IKineticStorage 接口和 NBT 序列化
 * 
 * 与 FE 和 HU 的区别：
 * - 动能会有摩擦损失
 * - 有转速概念（RPM）
 * - 需要机械传动来传输
 */
@SuppressWarnings("null")
public class KineticStorage implements IKineticStorage, INBTSerializable<Tag> {
    
    protected long kinetic;
    protected long capacity;
    protected long maxReceive;
    protected long maxExtract;
    protected int maxRPM;
    protected float frictionFactor;
    
    public KineticStorage(long capacity, long maxReceive, long maxExtract, 
                          int maxRPM, float frictionFactor) {
        this.capacity = Math.max(0, capacity);
        this.maxReceive = Math.max(0, maxReceive);
        this.maxExtract = Math.max(0, maxExtract);
        this.maxRPM = Math.max(0, maxRPM);
        this.frictionFactor = BoundedUnits.nonNegativeFactor(frictionFactor);
        this.kinetic = 0;
    }
    
    public KineticStorage(long capacity, long maxReceive, long maxExtract) {
        this(capacity, maxReceive, maxExtract, 10000, 0.005f);
    }
    
    public KineticStorage(long capacity, long maxReceive, long maxExtract, long kinetic,
                          int maxRPM, float frictionFactor) {
        this(capacity, maxReceive, maxExtract, maxRPM, frictionFactor);
        setKinetic(kinetic);
    }
    
    @Override
    public long receiveKinetic(long toReceive, boolean simulate) {
        if (!canReceiveKinetic() || toReceive <= 0) {
            return 0;
        }
        
        long kineticReceived = Math.min(this.capacity - this.kinetic, 
                                        Math.min(this.maxReceive, toReceive));
        if (kineticReceived < 0) kineticReceived = 0;
        if (!simulate && kineticReceived > 0) {
            this.kinetic += kineticReceived;
        }
        return kineticReceived;
    }
    
    @Override
    public long extractKinetic(long toExtract, boolean simulate) {
        if (!canExtractKinetic() || toExtract <= 0) {
            return 0;
        }
        
        long kineticExtracted = Math.min(this.kinetic, 
                                        Math.min(this.maxExtract, toExtract));
        if (!simulate && kineticExtracted > 0) {
            this.kinetic -= kineticExtracted;
        }
        return kineticExtracted;
    }
    
    @Override
    public long getKineticStored() {
        return this.kinetic;
    }
    
    @Override
    public long getMaxKineticStored() {
        return this.capacity;
    }
    
    @Override
    public boolean canExtractKinetic() {
        return this.maxExtract > 0 && this.kinetic > 0;
    }
    
    @Override
    public boolean canReceiveKinetic() {
        return this.maxReceive > 0 && this.kinetic < this.capacity;
    }
    
    @Override
    public long getMaxReceive() {
        return this.maxReceive;
    }
    
    @Override
    public long getMaxExtract() {
        return this.maxExtract;
    }
    
    @Override
    public int getRPM() {
        return BoundedUnits.gauge(this.kinetic, this.capacity, 0, this.maxRPM);
    }
    
    public void setKinetic(long kinetic) {
        this.kinetic = BoundedUnits.clamp(kinetic, this.capacity);
    }
    
    public void applyFrictionLoss() {
        this.kinetic -= getKineticLossPerTick();
    }

    @Override
    public long getKineticLossPerTick() { return BoundedUnits.friction(this.kinetic, this.frictionFactor); }

    public long generateKineticInternal(long amount, boolean simulate) {
        long kineticGenerated = BoundedUnits.receive(this.kinetic, this.capacity, amount, Long.MAX_VALUE);
        if (!simulate) {
            this.kinetic += kineticGenerated;
        }
        return kineticGenerated;
    }

    @Override
    public Tag serializeNBT(HolderLookup.Provider provider) {
        return LongTag.valueOf(this.kinetic);
    }
    
    @Override
    public void deserializeNBT(HolderLookup.Provider provider, Tag nbt) {
        if (nbt instanceof LongTag longNbt) {
            setKinetic(longNbt.getAsLong());
        } else if (nbt instanceof net.minecraft.nbt.IntTag intNbt) {
            setKinetic(intNbt.getAsInt());
        }
    }
}
