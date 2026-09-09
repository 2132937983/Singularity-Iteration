package com.singularity_iteration.mio_icif.api.machine;

import com.singularity_iteration.mio_icif.api.internal.energy.GenericEnergyBlockEntity;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("null")
public abstract class MultiblockEnergyCore extends GenericEnergyBlockEntity {

    private boolean structureComplete = false;

    protected MultiblockEnergyCore(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                   long capacity, long maxReceive, long maxExtract, ICableTier cableTier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, cableTier);
    }

    public boolean isStructureComplete() {
        return structureComplete;
    }

    public void setStructureComplete(boolean complete) {
        this.structureComplete = complete;
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

    public void setMaxReceive(long maxReceive) {
        getEnergyStorageInternal().setMaxReceive(maxReceive);
    }

    public void setMaxExtract(long maxExtract) {
        getEnergyStorageInternal().setMaxExtract(maxExtract);
    }

    public long getMaxReceive() {
        return getEnergyStorageInternal().getMaxReceive();
    }

    public long getMaxExtract() {
        return getEnergyStorageInternal().getMaxExtract();
    }

    public abstract void onMultiblockFormed(IMultiblockStructure structure);

    public abstract void onMultiblockBroken();

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("structureComplete", structureComplete);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("structureComplete")) {
            structureComplete = tag.getBoolean("structureComplete");
        }
    }
}