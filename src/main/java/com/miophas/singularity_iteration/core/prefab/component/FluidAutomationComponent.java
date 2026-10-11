package com.miophas.singularity_iteration.core.prefab.component;

import com.miophas.singularity_iteration.core.api.machine.IMachineUpgradeStats;
import com.miophas.singularity_iteration.core.runtime.energy.ContainerToTank;
import com.miophas.singularity_iteration.core.runtime.processing.FluidTransferBuffer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * General automation component for the fluid ejector and fluid pulling upgrades.
 *
 * <p>It is only responsible for the budget, the direction, the buffer (deduct first, then deliver)
 * and the dirty flag. It does not know any specific machine or fluid.
 * A host (a subclass of a core base class or its own {@link BlockEntity}) can reuse it.
 * The host supplies the world context, its own fluid handler, the adjacent fluid handlers and the upgrade statistics.
 * Thus each base class does not need its own copy of this logic.
 *
 * <p>The NBT keys stay {@code scex_fluid_output_pending} / {@code scex_fluid_input_pending}.
 */
public final class FluidAutomationComponent {

    /** Host context that the component needs. */
    public interface Host {
        @Nullable Level level();

        BlockPos worldPosition();

        /** The external fluid handler of this machine (the same for all sides). */
        @Nullable IFluidHandler ownFluidHandler();

        /** The fluid handler at an adjacent position. The host decides if it checks that the chunk is loaded. */
        @Nullable IFluidHandler adjacentFluidHandler(BlockPos pos, @Nullable Direction side);

        IMachineUpgradeStats upgradeStats();

        default void markUnsaved() { }

        default void markNeighborUnsaved(BlockPos pos) { }
    }

    private static final List<Direction> ALL_DIRECTIONS = List.of(Direction.values());

    private final FluidTransferBuffer output;
    private final FluidTransferBuffer input;
    private final Host host;

    public FluidAutomationComponent(Host host) {
        this.host = host;
        this.output = new FluidTransferBuffer(host::markUnsaved);
        this.input = new FluidTransferBuffer(host::markUnsaved);
    }

    /** @deprecated Internal mutable buffer; use pendingOutput() for inspection and save/load for persistence. */
    @Deprecated(since = "0.1.7.14", forRemoval = false)
    @org.jetbrains.annotations.ApiStatus.Internal
    public FluidTransferBuffer outputBuffer() { return output; }

    /** @deprecated Internal mutable buffer; use pendingInput() for inspection and save/load for persistence. */
    @Deprecated(since = "0.1.7.14", forRemoval = false)
    @org.jetbrains.annotations.ApiStatus.Internal
    public FluidTransferBuffer inputBuffer() { return input; }

    /** Defensive snapshots: mutating the returned stack cannot change pending transfers. */
    public FluidStack pendingOutput() { return output.pending().copy(); }
    public FluidStack pendingInput() { return input.pending().copy(); }

    /** Runs one eject/pull cycle. The upgrade count sets the amount. Returns immediately if no related upgrade is installed. Does nothing on the client side. */
    public void runAutomation() {
        Level level = host.level();
        if (level == null || level.isClientSide()) {
            return;
        }
        IFluidHandler own = host.ownFluidHandler();
        if (own == null || own.getTanks() == 0) {
            return;
        }
        IMachineUpgradeStats stats = host.upgradeStats();
        int ejectors = stats.getFluidEjectorCount();
        if (ejectors > 0) {
            ejectFluids(own, ejectors);
        }
        int pullers = stats.getFluidPullingCount();
        if (pullers > 0) {
            pullFluids(own, pullers);
        }
    }

    public void ejectFluids(IFluidHandler own, int upgradeCount) {
        if (upgradeCount <= 0) return;
        var stats = host.upgradeStats();
        var configured = stats.getFluidEjectorDirections();
        Iterable<Direction> directions = configured.isEmpty() ? ALL_DIRECTIONS : configured;
        for (Direction direction : directions) {
            // IC2 ItemUpgradeModule.onTick gives EACH neighbour a transfer allowance.
            // A shared, oversized allowance empties a reactor into the first exchanger,
            // starving parallel exchangers until thousands of mB accumulate in that tank.
            int budget = stats.getFluidEjectorCount() == upgradeCount
                ? stats.getFluidEjectorTransferLimit(direction) : IMachineUpgradeStats.fluidTransferPerStack(upgradeCount);
            if (budget <= 0) continue;
            BlockPos adjacentPos = host.worldPosition().relative(direction);
            var adjacent = host.adjacentFluidHandler(adjacentPos, direction.getOpposite());
            if (adjacent == null) continue;
            var before = output.pending();
            int moved = output.move(own, adjacent, budget);
            if (moved > 0 || !FluidStack.matches(before, output.pending())) {
                host.markUnsaved();
                host.markNeighborUnsaved(adjacentPos);
            }
        }
    }

    public void pullFluids(IFluidHandler own, int upgradeCount) {
        if (upgradeCount <= 0) return;
        var stats = host.upgradeStats();
        var configured = stats.getFluidPullingDirections();
        Iterable<Direction> directions = configured.isEmpty() ? ALL_DIRECTIONS : configured;
        for (Direction direction : directions) {
            int budget = stats.getFluidPullingCount() == upgradeCount
                ? stats.getFluidPullingTransferLimit(direction) : IMachineUpgradeStats.fluidTransferPerStack(upgradeCount);
            if (budget <= 0) continue;
            BlockPos adjacentPos = host.worldPosition().relative(direction);
            var adjacent = host.adjacentFluidHandler(adjacentPos, direction.getOpposite());
            if (adjacent == null) continue;
            var before = input.pending();
            int moved = input.move(adjacent, own, budget);
            if (moved > 0 || !FluidStack.matches(before, input.pending())) {
                host.markUnsaved();
                host.markNeighborUnsaved(adjacentPos);
            }
        }
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("scex_fluid_output_pending", output.save(registries));
        tag.put("scex_fluid_input_pending", input.save(registries));
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        output.load(registries, tag.getCompound("scex_fluid_output_pending"));
        input.load(registries, tag.getCompound("scex_fluid_input_pending"));
    }

    /** Shortcut implementation: marks the block entity at the adjacent position as unsaved. A host can delegate to it. */
    public static void markNeighborUnsaved(@Nullable Level level, BlockPos pos) {
        if (level == null) return;
        BlockEntity neighbor = level.getBlockEntity(pos);
        if (neighbor != null) {
            ContainerToTank.markUnsaved(neighbor);
        }
    }
}
