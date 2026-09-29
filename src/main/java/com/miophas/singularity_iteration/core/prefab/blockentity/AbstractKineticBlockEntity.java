package com.miophas.singularity_iteration.core.prefab.blockentity;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.tool.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class AbstractKineticBlockEntity extends BlockEntity implements MenuProvider, IMioIcifCapabilities.IKineticStorage, IWrenchable {
    
    protected final IMioIcifCapabilities.IKineticStorage kineticStorage;
    
    public AbstractKineticBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, 10000, 100, 100, 10000, 0.005f);
    }
    
    public AbstractKineticBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                   int capacity, int maxReceive, int maxExtract,
                                   int maxRPM, float frictionFactor) {
        super(type, pos, state);
        this.kineticStorage = com.miophas.singularity_iteration.core.runtime.energy.ThermalOutput.enabled()
            ? new com.miophas.singularity_iteration.core.runtime.energy.PlatformKineticStorage(capacity, maxReceive, maxExtract, maxRPM, frictionFactor)
            : MioIcifAPI.instance().getCapabilities().createKineticStorage(
            capacity, maxReceive, maxExtract, maxRPM, frictionFactor);
    }
    
    public IMioIcifCapabilities.IKineticStorage getKineticStorage() {
        return this;
    }

    @Override
    public long receiveKinetic(long toReceive, boolean simulate) {
        long received = kineticStorage.receiveKinetic(toReceive, simulate);
        if (!simulate && received > 0) setChanged();
        return received;
    }

    @Override
    public long extractKinetic(long toExtract, boolean simulate) {
        long extracted = kineticStorage.extractKinetic(toExtract, simulate);
        if (!simulate && extracted > 0) setChanged();
        return extracted;
    }

    @Override
    public long getKineticStored() {
        return kineticStorage.getKineticStored();
    }

    @Override
    public long getMaxKineticStored() {
        return kineticStorage.getMaxKineticStored();
    }

    @Override
    public boolean canExtractKinetic() {
        return kineticStorage.canExtractKinetic();
    }

    @Override
    public boolean canReceiveKinetic() {
        return kineticStorage.canReceiveKinetic();
    }

    @Override
    public int getRPM() {
        return kineticStorage.getRPM();
    }

    @Override
    public boolean isOverspeed() {
        return kineticStorage.isOverspeed();
    }

    @Override
    public long getKineticLossPerTick() {
        return kineticStorage.getKineticLossPerTick();
    }

    @Override
    public long getMaxReceive() {
        return kineticStorage.getMaxReceive();
    }

    @Override
    public long getMaxExtract() {
        return kineticStorage.getMaxExtract();
    }

    @Override
    public void setKinetic(long kinetic) {
        long before = kineticStorage.getKineticStored();
        kineticStorage.setKinetic(kinetic);
        if (kineticStorage.getKineticStored() != before) setChanged();
    }

    @Override
    public void applyFrictionLoss() {
        long before = kineticStorage.getKineticStored();
        kineticStorage.applyFrictionLoss();
        if (kineticStorage.getKineticStored() != before) setChanged();
    }

    @Override
    public long generateKineticInternal(long amount, boolean simulate) {
        long generated = kineticStorage.generateKineticInternal(amount, simulate);
        if (!simulate && generated > 0) setChanged();
        return generated;
    }
    
    public static void tick(Level level, BlockPos pos, BlockState state, AbstractKineticBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        
        long kineticBefore = blockEntity.kineticStorage.getKineticStored();
        blockEntity.kineticStorage.applyFrictionLoss();
        long kineticAfter = blockEntity.kineticStorage.getKineticStored();
        if (kineticBefore != kineticAfter) {
            blockEntity.setChanged();
        }
        
        blockEntity.distributeKinetic();
    }
    
    protected void distributeKinetic() {
        if (level == null || level.isClientSide || isRemoved() || kineticStorage.getKineticStored() <= 0) {
            return;
        }

        int myRPM = kineticStorage.getRPM();

        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = worldPosition.relative(direction);
            if (!level.hasChunkAt(adjacentPos)) continue;

            IMioIcifCapabilities.IKineticStorage adjacentKinetic = level.getCapability(
                IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, adjacentPos, direction.getOpposite());

            if (adjacentKinetic == null) {
                adjacentKinetic = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(
                    level.getBlockEntity(adjacentPos));
            }

            if (adjacentKinetic != null && adjacentKinetic.canReceiveKinetic()) {
                int adjacentRPM = adjacentKinetic.getRPM();

                if (myRPM > adjacentRPM) {
                    long rpmDiff = (long) myRPM - adjacentRPM;

                    long maxTransfer = Math.min(kineticStorage.getMaxExtract(),
                                               adjacentKinetic.getMaxKineticStored() - adjacentKinetic.getKineticStored());
                    long kineticToTransfer = Math.min(maxTransfer, rpmDiff / 10);

                    if (kineticToTransfer > 0) {
                        long extracted = kineticStorage.extractKinetic(kineticToTransfer, false);
                        if (extracted > 0) {
                            long received = adjacentKinetic.receiveKinetic(extracted, false);
                            if (received < extracted) {
                                // Returning our reserved units must bypass the external input limit.
                                kineticStorage.generateKineticInternal(extracted - Math.max(0, received), false);
                            }
                            if (received > 0) {
                                var target = level.getBlockEntity(adjacentPos);
                                if (target != null) target.setChanged();
                            }
                            setChanged();
                        }
                    }
                }
            }
        }

    }
    
    @Nullable
    public IMioIcifCapabilities.IKineticStorage getKineticStorageCapability(@Nullable Direction side) {
        return this;
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("kinetic", kineticStorage.getKineticStored());
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("kinetic", net.minecraft.nbt.Tag.TAG_INT)) {
            kineticStorage.setKinetic(tag.getInt("kinetic"));
        } else if (tag.contains("kinetic", net.minecraft.nbt.Tag.TAG_LONG)) {
            kineticStorage.setKinetic(tag.getLong("kinetic"));
        }
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.kinetic_block");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null;
    }
}
