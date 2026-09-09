package com.singularity_iteration.mio_icif.Blocks.entity.wiring;

import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.IEnergyStorageAccess;
import com.singularity_iteration.mio_icif.api.energy.IWirelessPowerNode;
import com.singularity_iteration.mio_icif.api.internal.energy.GenericEnergyBlockEntity;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.EUApi;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.IEUEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("null")
public class mio_icif_wireless_power_transmission_node extends GenericEnergyBlockEntity implements IWirelessPowerNode {

    public static final long TRANSFER_SPEED = 32768L;
    public static final long DEFAULT_CAPACITY = 196608L;
    public static final long DEFAULT_MAX_RECEIVE = 32768L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;

    private BlockPos targetPosition = null;
    private boolean isActive = false;

    public mio_icif_wireless_power_transmission_node(BlockPos pos, BlockState state) {
        super(pos, state,
            MioIcifAPI.instance().getRegistries().getBlockEntityType("wireless_power_transmission_node"),
            DEFAULT_CAPACITY, DEFAULT_MAX_RECEIVE, DEFAULT_MAX_EXTRACT,
            MioIcifAPI.instance().getEnergyNetAPI().getCableTier("luv"));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_wireless_power_transmission_node blockEntity) {
        if (level.isClientSide()) return;

        blockEntity.wirelessTransfer();

        if (blockEntity.isActive != state.getValue(com.singularity_iteration.mio_icif.Blocks.Wiring.mio_icif_block_wireless_power_transmission_node.LIT)) {
            level.setBlockAndUpdate(pos, state.setValue(com.singularity_iteration.mio_icif.Blocks.Wiring.mio_icif_block_wireless_power_transmission_node.LIT, blockEntity.isActive));
        }
    }

    private void wirelessTransfer() {
        if (level == null || level.isClientSide) return;

        isActive = false;

        if (targetPosition == null) return;

        IEnergyStorageAccess storage = getEnergyStorage();
        long storedEnergy = storage.getAmount();
        if (storedEnergy < TRANSFER_SPEED) return;

        IEUEnergyStorage targetStorage = level.getCapability(EUApi.SIDED, targetPosition, null);
        if (targetStorage == null) return;

        long targetCurrent = targetStorage.getAmount();
        long targetMax = targetStorage.getCapacity();

        if (targetCurrent >= targetMax) return;

        long toTransfer = TRANSFER_SPEED;
        if (targetCurrent + toTransfer > targetMax) {
            toTransfer = targetMax - targetCurrent;
        }

        long extracted = storage.useEnergy(toTransfer, false);
        if (extracted > 0) {
            long received = targetStorage.generateEnergy(extracted, false);
            if (received > 0) {
                isActive = true;
            } else {
                storage.generateEnergy(extracted, false);
            }
        }
    }

    public BlockPos getTargetPosition() {
        return targetPosition;
    }

    public void setTargetPosition(BlockPos target) {
        this.targetPosition = target;
        setChanged();
    }

    public boolean isTransmitting() {
        return isActive;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (targetPosition != null) {
            tag.putInt("TargetX", targetPosition.getX());
            tag.putInt("TargetY", targetPosition.getY());
            tag.putInt("TargetZ", targetPosition.getZ());
        }
        tag.putBoolean("IsActive", isActive);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("TargetX")) {
            int x = tag.getInt("TargetX");
            int y = tag.getInt("TargetY");
            int z = tag.getInt("TargetZ");
            targetPosition = new BlockPos(x, y, z);
        } else {
            targetPosition = null;
        }
        isActive = tag.getBoolean("IsActive");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (targetPosition != null) {
            tag.putInt("TargetX", targetPosition.getX());
            tag.putInt("TargetY", targetPosition.getY());
            tag.putInt("TargetZ", targetPosition.getZ());
        }
        tag.putBoolean("IsActive", isActive);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("TargetX")) {
            int x = tag.getInt("TargetX");
            int y = tag.getInt("TargetY");
            int z = tag.getInt("TargetZ");
            targetPosition = new BlockPos(x, y, z);
        } else {
            targetPosition = null;
        }
        isActive = tag.getBoolean("IsActive");
    }
}