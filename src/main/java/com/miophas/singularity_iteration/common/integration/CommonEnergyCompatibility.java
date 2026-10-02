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
        IEUEnergyStorage mi = findMi(level, pos, input);
        if (mi != null) return mi;
        // GregTech Modern machines and cables expose only their own EU container (no FE).
        return GTCompat.findSink(level, pos, input);
    }

    private static IEUEnergyStorage findMi(Level level, BlockPos pos, Direction input) {
        if (!MICompat.isMILoaded()) return null;
        Object storage = MICompat.getMIStorage(level, pos, input);
        // 兜底：某些 MI 版本对带 side 的查询只暴露部分面，无 side 查询可拿到默认视图。
        if (storage == null && input != null) storage = MICompat.getMIStorage(level, pos, null);
        IEUEnergyStorage wrapped = storage == null ? null : MICompat.wrapMIStorage(storage);
        EnergyBridge.debug("compat-find|" + pos + "|" + input,
            "findEuStorage({}, side={}) -> wrapped={} storage={}",
            pos, input, wrapped != null,
            storage == null ? "null" : storage.getClass().getName());
        return wrapped;
    }

    @Override public IEnergySink findNetworkSink(Level level, BlockPos pos) {
        return AE2Compat.isAE2Loaded() && AE2Compat.isAe2NetworkBlock(level, pos) ? new Ae2EnergySink(level, pos) : null;
    }

    @Override public long push(Level level, BlockPos target, Direction output, long amount) {
        return EnergyBridge.pushToStorage(level, target, output, amount);
    }

    @Override public long pushDirect(Level level, BlockPos target, Direction output, long amount, CableTier tier) {
        // GT used to receive the cable tier's voltage with amps rounded UP: that over-volted
        // (exploded) lower-tier GT machines and could book more EU than was offered. The sink
        // now picks a packet at or below the machine's own input voltage, whole amps only.
        var gt = GTCompat.findSink(level, target, output.getOpposite());
        if (gt != null) {
            long moved = gt.receive(amount, false);
            if (moved > 0) return Math.min(moved, amount);
        }
        return push(level, target, output, amount);
    }
}
