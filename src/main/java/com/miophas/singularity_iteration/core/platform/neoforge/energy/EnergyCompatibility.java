package com.miophas.singularity_iteration.core.platform.neoforge.energy;

import com.miophas.singularity_iteration.core.api.energy.grid.IEnergySink;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.energy.storage.IEUEnergyStorage;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Host-provided optional integrations. Native EU and FE paths do not require a provider. */
public interface EnergyCompatibility {
    default @Nullable IEUEnergyStorage findEuStorage(Level level, BlockPos pos, Direction input) { return null; }
    default @Nullable IEnergySink findNetworkSink(Level level, BlockPos pos) { return null; }
    default long push(Level level, BlockPos target, Direction output, long amount) { return 0; }
    default long pushDirect(Level level, BlockPos target, Direction output, long amount, CableTier tier) { return push(level, target, output, amount); }

    static EnergyCompatibility get() { return Holder.provider; }
    static void install(EnergyCompatibility provider) { Holder.provider = Objects.requireNonNull(provider); }

    final class Holder {
        private static volatile EnergyCompatibility provider = new EnergyCompatibility() {};
        private Holder() {}
    }
}
