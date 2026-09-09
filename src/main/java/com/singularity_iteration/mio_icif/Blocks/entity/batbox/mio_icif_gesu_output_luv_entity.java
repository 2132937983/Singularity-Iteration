package com.singularity_iteration.mio_icif.Blocks.entity.batbox;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.energy.ICableTier;
import com.singularity_iteration.mio_icif.api.machine.MultiblockEnergyPart;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.grid.EnergyNetGlobal;
import com.singularity_iteration.mio_icif.energy.grid.IEnergyAcceptor;
import com.singularity_iteration.mio_icif.energy.grid.IEnergyEmitter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_gesu_output_luv_entity extends MultiblockEnergyPart {

    private static final long TRANSFER_SPEED = 81920L;
    private static final ICableTier LUV_TIER =
        MioIcifAPI.instance().getEnergyNetAPI().getCableTier("luv");
    private static final long LUV_IO_RATE = LUV_TIER.getPowerRating();

    public mio_icif_gesu_output_luv_entity(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.GESU_OUTPUT_LUV.get(), 1, 0, 0, LUV_TIER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_gesu_output_luv_entity blockEntity) {
        if (level.isClientSide()) return;

        if (!blockEntity.isStructureCompleted() || blockEntity.getCorePosition() == null) {
            blockEntity.setAsConsumer();
            return;
        }

        blockEntity.setAsPowerSource(LUV_IO_RATE);
    }

    @Override
    public double getOfferedEnergy() {
        if (!isPowerSource()) return 0.0D;
        BlockEntity coreBe = getCoreBlockEntity();
        if (coreBe instanceof mio_icif_gesu_core_entity core) {
            if (core.isStructureComplete() && core.getStoredEnergy() > 0) {
                return Math.min(TRANSFER_SPEED, core.getStoredEnergy());
            }
        }
        return 0.0D;
    }

    @Override
    public void drawEnergy(double amount) {
        if (!isPowerSource() || amount <= 0.0D) return;
        BlockEntity coreBe = getCoreBlockEntity();
        if (coreBe instanceof mio_icif_gesu_core_entity core) {
            core.consumeEnergy((long) amount, false);
        }
    }

    @Override
    public int getSourceTier() {
        if (!isPowerSource()) return -1;
        return EnergyNetGlobal.cableTierToSourceTier((CableTier) LUV_TIER);
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, Direction direction) {
        return isPowerSource();
    }

    @Override
    public double getDemandedEnergy() {
        return 0.0D;
    }

    @Override
    public double injectEnergy(Direction direction, double amount, double voltage) {
        return amount;
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
        return false;
    }

    @Nullable
    private BlockEntity getCoreBlockEntity() {
        if (getCorePosition() == null || level == null) return null;
        if (!level.isLoaded(getCorePosition())) return null;
        return level.getBlockEntity(getCorePosition());
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.mio_icif.gesu_output_luv");
    }
}