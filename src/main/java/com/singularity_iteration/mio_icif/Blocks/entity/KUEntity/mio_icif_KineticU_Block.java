package com.singularity_iteration.mio_icif.Blocks.entity.KUEntity;

import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import com.singularity_iteration.mio_icif.api.tool.IWrenchable;
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
public class mio_icif_KineticU_Block extends BlockEntity implements MenuProvider, IMioIcifCapabilities.IKineticStorage, IWrenchable {
    
    protected final IMioIcifCapabilities.IKineticStorage kineticStorage;
    
    public mio_icif_KineticU_Block(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, 10000, 100, 100, 10000, 0.005f);
    }
    
    public mio_icif_KineticU_Block(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                   int capacity, int maxReceive, int maxExtract,
                                   int maxRPM, float frictionFactor) {
        super(type, pos, state);
        this.kineticStorage = MioIcifAPI.instance().getCapabilities().createKineticStorage(
            capacity, maxReceive, maxExtract, maxRPM, frictionFactor);
    }
    
    public IMioIcifCapabilities.IKineticStorage getKineticStorage() {
        return kineticStorage;
    }

    @Override
    public long receiveKinetic(long toReceive, boolean simulate) {
        return kineticStorage.receiveKinetic(toReceive, simulate);
    }

    @Override
    public long extractKinetic(long toExtract, boolean simulate) {
        return kineticStorage.extractKinetic(toExtract, simulate);
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
        kineticStorage.setKinetic(kinetic);
    }

    @Override
    public void applyFrictionLoss() {
        kineticStorage.applyFrictionLoss();
    }

    @Override
    public long generateKineticInternal(long amount, boolean simulate) {
        return kineticStorage.generateKineticInternal(amount, simulate);
    }
    
    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_KineticU_Block blockEntity) {
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
        if (kineticStorage.getKineticStored() <= 0) {
            return;
        }

        int myRPM = kineticStorage.getRPM();

        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = worldPosition.relative(direction);

            IMioIcifCapabilities.IKineticStorage adjacentKinetic = level.getCapability(
                IMioIcifCapabilities.KINETIC_STORAGE_BLOCK, adjacentPos, direction.getOpposite());

            if (adjacentKinetic == null) {
                adjacentKinetic = MioIcifAPI.instance().getCapabilities().adaptKineticStorage(
                    level.getBlockEntity(adjacentPos));
            }

            if (adjacentKinetic != null && adjacentKinetic.canReceiveKinetic()) {
                int adjacentRPM = adjacentKinetic.getRPM();

                if (myRPM > adjacentRPM) {
                    int rpmDiff = myRPM - adjacentRPM;

                    long maxTransfer = Math.min(kineticStorage.getMaxExtract(),
                                               adjacentKinetic.getMaxKineticStored() - adjacentKinetic.getKineticStored());
                    long kineticToTransfer = Math.min(maxTransfer, rpmDiff / 10);

                    if (kineticToTransfer > 0) {
                        long extracted = kineticStorage.extractKinetic(kineticToTransfer, false);
                        if (extracted > 0) {
                            long received = adjacentKinetic.receiveKinetic(extracted, false);
                            if (received < extracted) {
                                kineticStorage.receiveKinetic(extracted - received, false);
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
        return kineticStorage;
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