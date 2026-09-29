package com.miophas.singularity_iteration.core.prefab.fluid;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 多个流体槽合并成一个 {@link IFluidHandler} 的通用实现。
 *
 * <p>模型与 IC2 的 {@code Fluids} 组件同构：每个槽带一个<b>方向角色</b>与自己的
 * {@code accepts} 谓词，输入槽不可被抽、输出槽不可被灌是<b>结构性</b>保证，
 * 不依赖调用方的索引判断，也不会把产物槽灌进东西、不会从原料槽抽走东西。
 *
 * <ul>
 *   <li>{@code fill}：按注册顺序遍历可灌槽并累加（修正了 IC2 里剩余量未递减导致的重复分发）。</li>
 *   <li>{@code drain(FluidStack)}：按注册顺序遍历可抽槽、按流体类型累加，够量即停。</li>
 *   <li>{@code drain(int)}：先用第一个非空可抽槽确定流体类型，再按该类型累加。</li>
 *   <li>{@code getFluidInTank/getTankCapacity/isFluidValid}：按注册顺序直接代理，保住 GUI 的槽位索引。</li>
 * </ul>
 *
 * <p>{@code null} 方向视为任意面（IC2 原版在 null 面上所有槽都不可访问，这里显式修正）。
 */
public final class FluidTankGroup implements IFluidHandler {

    /** 槽位方向角色。 */
    public enum Role {
        /** 只可灌入。 */
        INPUT_ONLY,
        /** 只可抽出。 */
        OUTPUT_ONLY,
        /** 既可灌入也可抽出。 */
        IO,
        /** 对外不可见，仅供内部逻辑使用。 */
        INTERNAL
    }

    /** 一个槽位：处理器 + 方向角色。 */
    public record Tank(IFluidHandler handler, Role role) {
        public Tank {
            if (handler == null) throw new IllegalArgumentException("fluid handler");
            if (role == null) throw new IllegalArgumentException("role");
        }
    }

    private final List<Tank> tanks;

    public FluidTankGroup(List<Tank> tanks) {
        if (tanks == null || tanks.isEmpty()) throw new IllegalArgumentException("at least one tank");
        this.tanks = List.copyOf(tanks);
    }

    /** 便捷构造：单个只进槽。 */
    public static FluidTankGroup inputOnly(IFluidHandler tank) {
        return new FluidTankGroup(List.of(new Tank(tank, Role.INPUT_ONLY)));
    }

    /** 便捷构造：一个只进槽 + 一个只出槽（IC2 双槽机器的常见形态）。 */
    public static FluidTankGroup inputOutput(IFluidHandler input, IFluidHandler output) {
        return new FluidTankGroup(List.of(new Tank(input, Role.INPUT_ONLY), new Tank(output, Role.OUTPUT_ONLY)));
    }

    @Override
    public int getTanks() { return tanks.size(); }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return inRange(tank) ? tanks.get(tank).handler().getFluidInTank(0) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return inRange(tank) ? tanks.get(tank).handler().getTankCapacity(0) : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return inRange(tank) && tanks.get(tank).handler().isFluidValid(0, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return fill(resource, action, null);
    }

    public int fill(FluidStack resource, FluidAction action, @Nullable Direction side) {
        if (resource == null || resource.isEmpty()) return 0;
        FluidStack remaining = resource.copy();
        int total = 0;
        for (Tank entry : tanks) {
            if (!canFill(entry.role())) continue;
            if (remaining.isEmpty()) break;
            int accepted = entry.handler().fill(remaining, action);
            if (accepted > 0) {
                remaining.shrink(accepted);
                total += accepted;
            }
        }
        return total;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return drain(resource, action, null);
    }

    public FluidStack drain(FluidStack resource, FluidAction action, @Nullable Direction side) {
        if (resource == null || resource.isEmpty()) return FluidStack.EMPTY;
        FluidStack total = null;
        int remaining = resource.getAmount();
        for (Tank entry : tanks) {
            if (remaining <= 0) break;
            if (!canDrain(entry.role())) continue;
            FluidStack drained = entry.handler().drain(resource.copyWithAmount(remaining), action);
            if (drained == null || drained.isEmpty()) continue;
            if (total == null) {
                total = drained.copyWithAmount(0);
            }
            total.grow(drained.getAmount());
            remaining -= drained.getAmount();
        }
        return total != null ? total : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return drain(maxDrain, action, null);
    }

    public FluidStack drain(int maxDrain, FluidAction action, @Nullable Direction side) {
        if (maxDrain <= 0) return FluidStack.EMPTY;
        for (Tank entry : tanks) {
            if (!canDrain(entry.role())) continue;
            FluidStack probe = entry.handler().drain(maxDrain, FluidAction.SIMULATE);
            if (probe != null && !probe.isEmpty()) {
                return drain(probe.copyWithAmount(maxDrain), action, side);
            }
        }
        return FluidStack.EMPTY;
    }

    private static boolean canFill(Role role) {
        return role == Role.INPUT_ONLY || role == Role.IO;
    }

    private static boolean canDrain(Role role) {
        return role == Role.OUTPUT_ONLY || role == Role.IO;
    }

    /** 供 GUI/菜单按角色枚举槽位。 */
    public List<Tank> tanks() {
        return tanks;
    }

    private boolean inRange(int tank) {
        return tank >= 0 && tank < tanks.size();
    }

    /** 兼容早期调用：按注册顺序取处理器列表。 */
    public List<IFluidHandler> handlers() {
        var handlers = new ArrayList<IFluidHandler>(tanks.size());
        for (Tank entry : tanks) {
            handlers.add(entry.handler());
        }
        return List.copyOf(handlers);
    }
}
