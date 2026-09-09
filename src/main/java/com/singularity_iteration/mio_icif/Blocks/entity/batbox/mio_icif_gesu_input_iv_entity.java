package com.singularity_iteration.mio_icif.Blocks.entity.batbox;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.MultiblockEnergyPart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("null")
public class mio_icif_gesu_input_iv_entity extends MultiblockEnergyPart {

    private static final ICableTier MAX_TIER =
        MioIcifAPI.instance().getEnergyNetAPI().getCableTier("max");
    private static final long MAX_IO_RATE = MAX_TIER.getPowerRating();
    private static final long BUFFER_CAPACITY = MAX_IO_RATE * 2;

    public mio_icif_gesu_input_iv_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.GESU_INPUT_IV.get(), BUFFER_CAPACITY, MAX_IO_RATE, 0, MAX_TIER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_gesu_input_iv_entity blockEntity) {
        if (level.isClientSide()) return;

        if (!blockEntity.isStructureCompleted() || blockEntity.getCorePosition() == null) {
            return;
        }

        if (blockEntity.getStoredEnergy() > 0) {
            BlockEntity coreBe = level.getBlockEntity(blockEntity.getCorePosition());
            if (coreBe instanceof mio_icif_gesu_core_entity core) {
                if (core.isStructureComplete()) {
                    long toTransfer = Math.min(blockEntity.getStoredEnergy(), MAX_IO_RATE);
                    long accepted = core.generateEnergyInternal(toTransfer, false);
                    if (accepted > 0) {
                        blockEntity.consumeEnergy(accepted, false);
                    }
                }
            } else if (coreBe == null && !level.isLoaded(blockEntity.getCorePosition())) {
                return;
            }
        }
    }

    @Override
    public double getDemandedEnergy() {
        if (isPowerSource()) return 0.0D;
        long localSpace = getEnergyCapacity() - getStoredEnergy();
        if (localSpace <= 0) return 0.0D;
        Level level = getLevel();
        if (level != null && getCorePosition() != null && isStructureCompleted()) {
            BlockEntity coreBe = level.getBlockEntity(getCorePosition());
            if (coreBe instanceof mio_icif_gesu_core_entity core && core.isStructureComplete()) {
                long coreSpace = core.getEnergyCapacity() - core.getStoredEnergy();
                if (coreSpace <= 0) return 0.0D;
                long totalSpace = coreSpace + localSpace;
                return Math.min(totalSpace, MAX_IO_RATE);
            }
        }
        return Math.min(localSpace, MAX_IO_RATE);
    }

    @Override
    public double injectEnergy(Direction direction, double amount, double voltage) {
        if (isPowerSource()) return amount;
        long toAdd = Math.min((long) amount, MAX_IO_RATE);
        long spaceAvailable = getEnergyCapacity() - getStoredEnergy();
        long accepted = Math.min(toAdd, spaceAvailable);
        getEnergyStorageInternal().setEnergy(getStoredEnergy() + accepted);
        return amount - accepted;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.gesu_input_iv");
    }
}