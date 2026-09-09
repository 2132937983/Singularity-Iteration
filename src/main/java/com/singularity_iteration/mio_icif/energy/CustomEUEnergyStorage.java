package com.singularity_iteration.mio_icif.energy;

import com.singularity_iteration.mio_icif.api.energy.IEnergyStorageAccess;
import com.singularity_iteration.mio_icif.api.energy.tile.IExplosionPowerOverride;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.IEUEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("null")
public class CustomEUEnergyStorage implements IEUEnergyStorage, IEnergyStorageAccess {

    protected long energy;
    protected long capacity;
    protected long maxReceive;
    protected long maxExtract;
    protected final CableTier cableTier;

    protected BlockPos pos;
    protected Level level;
    
    protected long powerOutput = 0;
    protected boolean isPowerSource = false;
    protected boolean outputEnabled = true; // 红石控制输出开关

    public CustomEUEnergyStorage(long capacity, long maxReceive, long maxExtract, CableTier cableTier) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.cableTier = cableTier;
        this.energy = 0;
    }

    public CustomEUEnergyStorage(long capacity, long maxTransfer, CableTier cableTier) {
        this(capacity, maxTransfer, maxTransfer, cableTier);
    }

    // Explicitly override to resolve duplicate default method conflict between IEnergyStorageAccess and ILongEnergyStorage
    @Override
    public int getEnergyStored() {
        return (int) Math.min(energy, Integer.MAX_VALUE);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(capacity, Integer.MAX_VALUE);
    }

    public void setBlockContext(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    public void setAsPowerSource(long powerOutput) {
        this.isPowerSource = true;
        this.powerOutput = powerOutput;
    }

    public long getPowerOutput() {
        return isPowerSource ? powerOutput : 0;
    }

    public boolean isPowerSource() {
        return isPowerSource;
    }
    
    /**
     * 设置能量输出是否启用（红石控制）
     * @param enabled true=允许输出, false=禁止输出
     */
    public void setOutputEnabled(boolean enabled) {
        this.outputEnabled = enabled;
    }
    
    /**
     * 检查能量输出是否启用
     */
    public boolean isOutputEnabled() {
        return this.outputEnabled;
    }

    public long getPowerRating() {
        return cableTier.getPowerRating();
    }

    public long extractPowerForConsumer(long amount, boolean simulate) {
        if (!isPowerSource) {
            return 0;
        }
        return extract(amount, simulate);
    }

    public boolean isOverloaded(long gridPower) {
        return gridPower > getPowerRating();
    }

    public void triggerOverloadExplosion() {
        if (level == null || pos == null) {
            return;
        }

        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IExplosionPowerOverride override && !override.shouldExplode()) {
                return;
            }

            float explosionPower = calculateExplosionPower();
            if (be instanceof IExplosionPowerOverride override) {
                explosionPower = override.getExplosionPower(cableTier.getTierIndex(), explosionPower);
            }

            if (explosionPower > 0.0F) {
                level.explode(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    explosionPower,
                    Level.ExplosionInteraction.BLOCK
                );
            }

            level.removeBlock(pos, false);
        }
    }

    protected float calculateExplosionPower() {
        return (float) Math.min(cableTier.powerRating / 1024.0D, 2.0D + cableTier.tierIndex * 0.5D);
    }

    /**
     * 检查实现 {@link IExplosionPowerOverride} 的方块是否应爆炸。
     * 未实现该接口时默认返回 true。
     */
    public static boolean shouldTileExplode(BlockEntity tile) {
        return !(tile instanceof IExplosionPowerOverride override) || override.shouldExplode();
    }

    /**
     * 解析实现 {@link IExplosionPowerOverride} 的方块的实际爆炸威力。
     * 未实现该接口时返回 basePower。
     */
    public static float resolveTileExplosionPower(BlockEntity tile, int tier, float basePower) {
        if (tile instanceof IExplosionPowerOverride override) {
            return override.getExplosionPower(tier, basePower);
        }
        return basePower;
    }

    @Override
    public boolean canConnect(CableTier cableTier) {
        return cableTier.powerRating <= this.cableTier.powerRating;
    }

    @Override
    public long receive(long maxReceive, boolean simulate) {
        long energyReceived = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
        if (!simulate) {
            energy += energyReceived;
        }
        return energyReceived;
    }

    @Override
    public long extract(long maxExtract, boolean simulate) {
        // 如果输出被禁用（红石控制），则不允许提取能量
        if (!this.outputEnabled) {
            return 0;
        }
        long energyExtracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));
        if (!simulate) {
            energy -= energyExtracted;
        }
        return energyExtracted;
    }

    @Override
    public long getAmount() {
        return energy;
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    @Override
    public void setStored(long amount) {
        this.energy = Math.max(0, Math.min(amount, capacity));
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }

    public void setEnergy(long energy) {
        this.energy = Math.max(0, Math.min(capacity, energy));
    }

    public long getMaxExtract() {
        return maxExtract;
    }

    public long getMaxReceive() {
        return maxReceive;
    }

    public void setCapacity(long capacity) {
        this.capacity = Math.max(0, capacity);
        this.energy = Math.min(this.energy, this.capacity);
    }

    public void setMaxReceive(long maxReceive) {
        this.maxReceive = Math.max(0, maxReceive);
    }

    public void setMaxExtract(long maxExtract) {
        this.maxExtract = Math.max(0, maxExtract);
    }

    public CableTier getCableTier() {
        return cableTier;
    }

    public long consumeEnergyInternal(long amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        long energyConsumed = Math.min(this.energy, amount);
        if (!simulate) {
            this.energy -= energyConsumed;
        }
        return energyConsumed;
    }

    public long generateEnergyInternal(long amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        long energyGenerated = Math.min(this.capacity - this.energy, amount);
        if (!simulate) {
            this.energy += energyGenerated;
        }
        return energyGenerated;
    }

    @Override
    public long useEnergy(long amount, boolean simulate) {
        return consumeEnergyInternal(amount, simulate);
    }

    @Override
    public long generateEnergy(long amount, boolean simulate) {
        return generateEnergyInternal(amount, simulate);
    }
}