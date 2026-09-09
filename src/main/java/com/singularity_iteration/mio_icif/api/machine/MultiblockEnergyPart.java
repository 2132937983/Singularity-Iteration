package com.singularity_iteration.mio_icif.api.machine;

import com.singularity_iteration.mio_icif.api.internal.energy.GenericEnergyBlockEntity;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public abstract class MultiblockEnergyPart extends GenericEnergyBlockEntity implements IJadeDisplayDelegate {

    @Nullable
    private BlockPos corePosition = null;
    private boolean structureCompleted = false;

    protected MultiblockEnergyPart(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                   long capacity, long maxReceive, long maxExtract, ICableTier cableTier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, cableTier);
    }

    public void setCorePosition(BlockPos pos) {
        this.corePosition = pos;
    }

    public void setStructureCompleted(boolean completed) {
        this.structureCompleted = completed;
    }

    public boolean isStructureCompleted() {
        return structureCompleted;
    }

    @Nullable
    public BlockPos getCorePosition() {
        return corePosition;
    }

    public void detachFromCore() {
        this.corePosition = null;
        this.structureCompleted = false;
    }

    @Override
    @Nullable
    public BlockEntity getJadeDisplayTarget() {
        if (!structureCompleted || corePosition == null || level == null) {
            return null;
        }
        return level.getBlockEntity(corePosition);
    }

    public long getStoredEnergy() {
        return getEnergyStorageInternal().getAmount();
    }

    public long getEnergyCapacity() {
        return getEnergyStorageInternal().getCapacity();
    }

    public long receiveEnergy(long amount, boolean simulate) {
        return getEnergyStorageInternal().receive(amount, simulate);
    }

    public long extractEnergy(long amount, boolean simulate) {
        return getEnergyStorageInternal().extract(amount, simulate);
    }

    public long consumeEnergy(long amount, boolean simulate) {
        return getEnergyStorageInternal().consumeEnergyInternal(amount, simulate);
    }

    public long generateEnergyInternal(long amount, boolean simulate) {
        return getEnergyStorageInternal().generateEnergyInternal(amount, simulate);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("structureCompleted", structureCompleted);
        if (corePosition != null) {
            tag.putInt("coreX", corePosition.getX());
            tag.putInt("coreY", corePosition.getY());
            tag.putInt("coreZ", corePosition.getZ());
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("structureCompleted")) {
            structureCompleted = tag.getBoolean("structureCompleted");
        }
        if (tag.contains("coreX")) {
            corePosition = new BlockPos(tag.getInt("coreX"), tag.getInt("coreY"), tag.getInt("coreZ"));
        }
    }
}