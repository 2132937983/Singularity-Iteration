package com.singularity_iteration.mio_icif.Blocks.entity.wiring;

import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_wire;
import com.singularity_iteration.mio_icif.energy.EnergyUnit.CableTier;
import com.singularity_iteration.mio_icif.energy.grid.EnergyNetGlobal;
import com.singularity_iteration.mio_icif.energy.grid.NodeStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("null")
public class mio_icif_wire_detector extends mio_icif_wire {

    private static final int TICK_RATE = 32;

    public static final CableTier CABLE_TIER = CableTier.IV;
    public static final double CONDUCTION_LOSS = 0.5D;

    private int ticker = 0;
    private int redstoneLevel = 0;
    private int comparatorLevel = 0;

    public mio_icif_wire_detector(BlockPos pos, BlockState state) {
        super(pos, state, mio_icif_block_entities.WIRE_DETECTOR.get(), CABLE_TIER, false,
              CABLE_TIER.conductorBreakdownEnergy,
              CABLE_TIER.insulationBreakdownEnergy,
              CABLE_TIER.insulationEnergyAbsorption,
              CONDUCTION_LOSS,
              0.0f);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_wire_detector blockEntity) {
        if (level.isClientSide) return;

        mio_icif_wire.tick(level, pos, state, blockEntity);

        if (++blockEntity.ticker % TICK_RATE == 0) {
            blockEntity.detectEnergy();
        }
    }

    public void detectEnergy() {
        if (level == null || level.isClientSide) return;

        NodeStats stats = EnergyNetGlobal.getNodeStats(this);
        double energyIn = stats != null ? stats.getEnergyIn() : 0.0D;

        boolean newActive = energyIn > 0.0D;
        int newRedstoneLevel = newActive ? 15 : 0;

        double breakdownEnergy = getConductorBreakdownEnergy() - 1.0D;
        int newComparatorLevel;
        if (breakdownEnergy > 0) {
            double ratio = energyIn / breakdownEnergy;
            if (ratio < 0.0D) ratio = 0.0D;
            if (ratio > 1.0D) ratio = 1.0D;
            newComparatorLevel = (int) (ratio * 15.0D);
        } else {
            newComparatorLevel = 0;
        }

        BlockState currentState = getBlockState();
        boolean currentActive = currentState.getValue(com.singularity_iteration.mio_icif.Blocks.Wiring.mio_icif_block_wire_detector.ACTIVE);
        if (newActive != currentActive) {
            level.setBlockAndUpdate(worldPosition, currentState.setValue(com.singularity_iteration.mio_icif.Blocks.Wiring.mio_icif_block_wire_detector.ACTIVE, newActive));
        }

        boolean changed = false;
        if (newRedstoneLevel != this.redstoneLevel) {
            this.redstoneLevel = newRedstoneLevel;
            changed = true;
        }
        if (newComparatorLevel != this.comparatorLevel) {
            this.comparatorLevel = newComparatorLevel;
            changed = true;
        }

        if (changed) {
            setChanged();
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    public int getRedstoneLevel() {
        return redstoneLevel;
    }

    public int getComparatorLevel() {
        return comparatorLevel;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RedstoneLevel", redstoneLevel);
        tag.putInt("ComparatorLevel", comparatorLevel);
        tag.putInt("Ticker", ticker);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        redstoneLevel = tag.getInt("RedstoneLevel");
        comparatorLevel = tag.getInt("ComparatorLevel");
        ticker = tag.getInt("Ticker");
    }
}