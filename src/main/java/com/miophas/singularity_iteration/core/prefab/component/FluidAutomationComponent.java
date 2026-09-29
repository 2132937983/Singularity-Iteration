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
 * 流体弹射/吸取升级的通用自动化组件。
 *
 * <p>只负责预算、方向、先扣后交付的缓冲与脏标记，不认识任何具体机器或流体。
 * 宿主（继承核心基类或自有 {@link BlockEntity}）提供世界上下文、本机与相邻
 * 流体处理器以及升级统计即可复用，避免每个基类各维护一份实现。
 *
 * <p>NBT 键沿用 {@code scex_fluid_output_pending} / {@code scex_fluid_input_pending}。
 */
public final class FluidAutomationComponent {

    /** 组件需要的宿主上下文。 */
    public interface Host {
        @Nullable Level level();

        BlockPos worldPosition();

        /** 本机器对外的流体处理器（不区分方向）。 */
        @Nullable IFluidHandler ownFluidHandler();

        /** 相邻位置的流体处理器；宿主决定是否做区块加载检查。 */
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

    public FluidTransferBuffer outputBuffer() { return output; }

    public FluidTransferBuffer inputBuffer() { return input; }

    /** 按升级数量驱动一次弹射/吸取；无相应升级时直接返回。 */
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

    /** 便捷实现：把相邻位置的方块实体标记为未保存，供宿主委托。 */
    public static void markNeighborUnsaved(@Nullable Level level, BlockPos pos) {
        if (level == null) return;
        BlockEntity neighbor = level.getBlockEntity(pos);
        if (neighbor != null) {
            ContainerToTank.markUnsaved(neighbor);
        }
    }
}
