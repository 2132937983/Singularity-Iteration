package com.miophas.singularity_iteration.core.prefab.blockentity;

import com.miophas.singularity_iteration.core.api.machine.IJadeDisplayDelegate;

import com.miophas.singularity_iteration.core.prefab.blockentity.GenericEnergyBlockEntity;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * Base block entity for a part (module) of an energy multiblock.
 *
 * <p>The part stores the position of its core and a completed flag. The validator
 * sets both in its {@code onStructureFormed} callback and clears both in its
 * {@code onStructureBroken} callback ({@link #detachFromCore}). Both values are saved
 * to NBT.
 *
 * <p>Jade shows the core block entity when a player looks at a connected part.
 */
@SuppressWarnings("null")
public abstract class MultiblockEnergyPart extends GenericEnergyBlockEntity implements IJadeDisplayDelegate {

    // Position of the core of the structure. Null while the part is not connected.
    @Nullable
    private BlockPos corePosition = null;
    private boolean structureCompleted = false;

    protected MultiblockEnergyPart(BlockPos pos, BlockState state, BlockEntityType<?> type,
                                   long capacity, long maxReceive, long maxExtract, ICableTier cableTier) {
        super(pos, state, type, capacity, maxReceive, maxExtract, cableTier);
    }

    /** Stores the core position. The validator calls this method when the structure forms. */
    public void setCorePosition(BlockPos pos) {
        this.corePosition = pos;
    }

    /** Stores the completed flag. The validator calls this method when the structure forms. */
    public void setStructureCompleted(boolean completed) {
        this.structureCompleted = completed;
    }

    /** @return {@code true} while the part belongs to a formed structure */
    public boolean isStructureCompleted() {
        return structureCompleted;
    }

    /** @return the core position, or {@code null} while the part is not connected */
    @Nullable
    public BlockPos getCorePosition() {
        return corePosition;
    }

    /**
     * Disconnects the part from its core.
     *
     * <p>This method clears the core position and the completed flag. It does not
     * tell the core or the manager.
     */
    public void detachFromCore() {
        this.corePosition = null;
        this.structureCompleted = false;
    }

    /**
     * Gets the block entity that Jade shows for this part.
     *
     * @return the core block entity while the part is connected and the level is set,
     *         otherwise {@code null}
     */
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

    /** Removes energy for internal use by the machine. It ignores the extract limit of the storage. */
    public long consumeEnergy(long amount, boolean simulate) {
        return getEnergyStorageInternal().consumeEnergyInternal(amount, simulate);
    }

    /** Adds energy that the machine makes. It ignores the receive limit of the storage. */
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
        // A tag without "coreX" keeps the current core position. It does not set it to null.
        if (tag.contains("coreX")) {
            corePosition = new BlockPos(tag.getInt("coreX"), tag.getInt("coreY"), tag.getInt("coreZ"));
        }
    }
}