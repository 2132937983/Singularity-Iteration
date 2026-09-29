package com.miophas.singularity_iteration.common.integration;

import com.miophas.singularity_iteration.common.integration.ae2.AE2Compat;
import com.miophas.singularity_iteration.common.integration.ae2.Ae2EnergySink;
import com.miophas.singularity_iteration.common.integration.gt.GTCompat;
import com.miophas.singularity_iteration.common.integration.mi.EnergyBridge;
import com.miophas.singularity_iteration.common.integration.mi.MICompat;
import com.miophas.singularity_iteration.core.api.energy.grid.IEnergySink;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.storage.IEUEnergyStorage;
import com.miophas.singularity_iteration.core.platform.neoforge.energy.EnergyCompatibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Maintains the legacy MI/GT/AE2 selection order outside the core classpath. */
public final class CommonEnergyCompatibility implements EnergyCompatibility {
    @Override public IEUEnergyStorage findEuStorage(Level level, BlockPos pos, Direction input) {
        if (!MICompat.isMILoaded()) return null;
        Object storage = MICompat.getMIStorage(level, pos, input);
        return storage == null ? null : MICompat.wrapMIStorage(storage);
    }

    @Override public IEnergySink findNetworkSink(Level level, BlockPos pos) {
        return AE2Compat.isAE2Loaded() && AE2Compat.isAe2NetworkBlock(level, pos) ? new Ae2EnergySink(level, pos) : null;
    }

    @Override public long push(Level level, BlockPos target, Direction output, long amount) {
        return EnergyBridge.pushToStorage(level, target, output, amount);
    }

    @Override public long pushDirect(Level level, BlockPos target, Direction output, long amount, CableTier tier) {
        if (GTCompat.isGTLoaded()) {
            Object container = GTCompat.getGTEnergyContainer(level, target, output.getOpposite());
            if (container != null && GTCompat.inputsEnergy(container, output.getOpposite())) {
                long[] packet = GTCompat.calculateGTVoltageAndAmperage(amount, tier);
                return GTCompat.acceptEnergyFromNetwork(container, output.getOpposite(), packet[0], packet[1]) * packet[0];
            }
        }
        return push(level, target, output, amount);
    }
}
