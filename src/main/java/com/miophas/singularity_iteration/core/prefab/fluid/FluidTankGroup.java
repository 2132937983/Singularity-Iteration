package com.miophas.singularity_iteration.core.prefab.fluid;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * General implementation that joins several fluid tanks into one {@link IFluidHandler}.
 *
 * <p>The model has the same structure as the IC2 {@code Fluids} component. Each tank has a <b>direction role</b>
 * and its own {@code accepts} predicate. The structure itself makes sure that nothing drains an input tank
 * and nothing fills an output tank. This does not depend on index checks by the caller.
 * Thus nothing fills a product tank and nothing drains a raw-material tank.
 *
 * <ul>
 *   <li>{@code fill}: goes through the fillable tanks in registration order and adds up the amounts.
 *       (This fixes an IC2 bug: IC2 did not decrease the remaining amount, so it distributed the same fluid again.)</li>
 *   <li>{@code drain(FluidStack)}: goes through the drainable tanks in registration order, adds up the amounts
 *       of the requested fluid type, and stops when the amount is sufficient.</li>
 *   <li>{@code drain(int)}: uses the first drainable tank that is not empty to find the fluid type.
 *       Then it adds up the amounts of that type.</li>
 *   <li>{@code getFluidInTank/getTankCapacity/isFluidValid}: delegate directly in registration order.
 *       This keeps the GUI slot indexes stable.</li>
 * </ul>
 *
 * <p>A {@code null} side means any side. (In the original IC2, no tank is accessible from a null side.
 * This class fixes that on purpose.)
 */
public final class FluidTankGroup implements IFluidHandler {

    /** Direction role of a tank. */
    public enum Role {
        /** Fill only. */
        INPUT_ONLY,
        /** Drain only. */
        OUTPUT_ONLY,
        /** Fill and drain. */
        IO,
        /** Not available from outside. Only internal logic uses it. */
        INTERNAL
    }

    /** One tank: a handler and a direction role. Both must not be null. */
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

    /** Shortcut factory: one input-only tank. */
    public static FluidTankGroup inputOnly(IFluidHandler tank) {
        return new FluidTankGroup(List.of(new Tank(tank, Role.INPUT_ONLY)));
    }

    /** Shortcut factory: one input-only tank and one output-only tank (the usual layout of an IC2 two-tank machine). */
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

    /** Returns the tanks so that a GUI or menu can list them by role. The list is immutable. */
    public List<Tank> tanks() {
        return tanks;
    }

    private boolean inRange(int tank) {
        return tank >= 0 && tank < tanks.size();
    }

    /** For compatibility with older callers: returns the handlers in registration order as an immutable list. */
    public List<IFluidHandler> handlers() {
        var handlers = new ArrayList<IFluidHandler>(tanks.size());
        for (Tank entry : tanks) {
            handlers.add(entry.handler());
        }
        return List.copyOf(handlers);
    }
}
