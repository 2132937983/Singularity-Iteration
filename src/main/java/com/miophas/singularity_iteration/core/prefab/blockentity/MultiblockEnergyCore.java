package com.miophas.singularity_iteration.core.prefab.blockentity;

import com.miophas.singularity_iteration.core.api.machine.IMultiblockStructure;

import com.miophas.singularity_iteration.core.prefab.blockentity.GenericEnergyBlockEntity;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base block entity for the controller (core) of an energy multiblock.
 *
 * <p>The core holds the energy storage of the machine. The validator of the
 * structure calls {@link #onMultiblockFormed} and {@link #onMultiblockBroken}
 * through the multiblock manager. See
 * the content-module class {@code mio_icif_multiblock_validator}
 * for the multiblock overview.
 *
 * <p>The structure complete flag is saved to NBT. The structure registry is not
 * saved. Thus after a world load the flag can be {@code true} while no manager is
 * registered. The subclasses form the structure again on their first server tick.
 */
@SuppressWarnings("null")
public abstract class MultiblockEnergyCore extends GenericEnergyBlockEntity {

    private boolean structureComplete = false;

    protected MultiblockEnergyCore(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                   long capacity, long maxReceive, long maxExtract, ICableTier cableTier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, cableTier);
    }

    /** @return {@code true} if the structure was complete at the last formed or broken event */
    public boolean isStructureComplete() {
        return structureComplete;
    }

    /**
     * Sets the structure complete flag.
     *
     * <p>This method does not form or break a structure. It only stores the flag.
     */
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

    /** Removes energy for internal use by the machine. It ignores the extract limit of the storage. */
    public long consumeEnergy(long amount, boolean simulate) {
        return getEnergyStorageInternal().consumeEnergyInternal(amount, simulate);
    }

    /** Adds energy that the machine makes. It ignores the receive limit of the storage. */
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

    /**
     * Called after the structure forms.
     *
     * <p>The implementation must set the structure complete flag and read the module
     * counts from {@link IMultiblockStructure#getStructureData()}.
     *
     * @param structure the formed structure
     */
    public abstract void onMultiblockFormed(IMultiblockStructure structure);

    /**
     * Called after the structure breaks.
     *
     * <p>The implementation must clear the structure complete flag and drop its
     * reference to the structure. The block code can call this method more than one
     * time for one break, so the implementation must accept repeated calls.
     */
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