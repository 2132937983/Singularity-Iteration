// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.util;

import com.miophas.singularity_iteration.common.item.cell.mio_icif_cells;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.function.Predicate;

/**
 * Fluid container slots of machines (0.1.10.6). Before, many machines accepted only their own
 * buckets and full SI cells in a container slot: a partly filled universal cell, a hot coolant
 * bucket or a fluid container of another mod was refused, and some slots destroyed the empty
 * container. These helpers go through the item fluid capability, so every container that can give
 * the fluid works, and the container that is left goes to the output slot (never lost).
 *
 * <p>Machines keep their own code for their own buckets and cells; they call these helpers only for
 * containers that the old code does not handle.
 */
public final class FluidContainerSlots {
    private FluidContainerSlots() { }

    /** The fluid that one item of the stack can give (simulated drain), or EMPTY. */
    public static FluidStack peek(ItemStack stack) {
        if (stack.isEmpty()) return FluidStack.EMPTY;
        IFluidHandlerItem handler = stack.copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) return FluidStack.EMPTY;
        FluidStack content = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        return content == null ? FluidStack.EMPTY : content;
    }

    /** True if one item of the stack can give a fluid that {@code accepts} takes. */
    public static boolean holds(ItemStack stack, Predicate<Fluid> accepts) {
        FluidStack content = peek(stack);
        return !content.isEmpty() && accepts.test(content.getFluid());
    }

    /** True if one item of the stack can give this fluid (source and flowing forms are the same). */
    public static boolean holds(ItemStack stack, Fluid fluid) {
        return holds(stack, f -> f.isSame(fluid));
    }

    /**
     * Empties one container from {@code inSlot} into {@code tank}, only as much as the tank takes.
     * A container that still holds fluid stays in the input slot (only when the slot holds one item);
     * an emptied container goes to {@code outSlot}. Nothing happens if the output slot cannot take it,
     * if the tank takes nothing, or if the container cannot give the partial amount (a bucket). An
     * emptied SI cell becomes a plain empty cell, so it stacks with the others.
     *
     * @param outSlot the slot for emptied containers; -1 if the machine has none (then an emptied
     *                container that does not vanish stays in the input slot)
     * @return true if fluid moved
     */
    public static boolean emptyInto(IItemHandlerModifiable inventory, int inSlot, int outSlot, IFluidHandler tank,
                                    Predicate<Fluid> accepts) {
        ItemStack input = inventory.getStackInSlot(inSlot);
        if (input.isEmpty() || plainTankFull(tank)) return false;
        ItemStack one = input.copyWithCount(1);
        IFluidHandlerItem handler = one.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) return false;
        FluidStack offered = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (offered == null || offered.isEmpty() || !accepts.test(offered.getFluid())) return false;
        int room = tank.fill(offered.copy(), IFluidHandler.FluidAction.SIMULATE);
        if (room <= 0) return false;
        FluidStack drained = handler.drain(offered.copyWithAmount(Math.min(room, offered.getAmount())), IFluidHandler.FluidAction.EXECUTE);
        if (drained == null || drained.isEmpty() || drained.getAmount() > room
                || !FluidStack.isSameFluidSameComponents(drained, offered)) return false;
        ItemStack rest = handler.getContainer();
        boolean stillHolds = !rest.isEmpty() && !peek(rest).isEmpty();
        if (!stillHolds && !rest.isEmpty() && mio_icif_cells.isFluidCell(rest)) {
            ItemStack plain = mio_icif_cells.getEmptyCellForStack(rest);
            if (!plain.isEmpty()) rest = plain;
        }
        ItemStack output = outSlot >= 0 ? inventory.getStackInSlot(outSlot) : ItemStack.EMPTY;
        boolean keepInInput = stillHolds || (!rest.isEmpty() && outSlot < 0);
        if (keepInInput) {
            if (input.getCount() > 1) return false;
        } else if (!rest.isEmpty() && !output.isEmpty()) {
            int limit = Math.min(output.getMaxStackSize(), inventory.getSlotLimit(outSlot));
            if (!ItemStack.isSameItemSameComponents(output, rest) || output.getCount() + rest.getCount() > limit) return false;
        }
        tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (keepInInput) {
            inventory.setStackInSlot(inSlot, rest);
        } else {
            inventory.setStackInSlot(inSlot, input.copyWithCount(input.getCount() - 1));
            if (!rest.isEmpty()) {
                inventory.setStackInSlot(outSlot, output.isEmpty() ? rest : output.copyWithCount(output.getCount() + rest.getCount()));
            }
        }
        return true;
    }

    /**
     * 0.1.10.8: true for a full plain {@link FluidTank}. Its {@code fill} then takes nothing, so
     * {@code emptyInto} cannot move anything and the container is not copied or asked. Any other
     * tank (also a subclass of FluidTank) is asked as before.
     */
    static boolean plainTankFull(IFluidHandler tank) {
        return tank != null && tank.getClass() == FluidTank.class
            && ((FluidTank) tank).getFluidAmount() >= ((FluidTank) tank).getCapacity();
    }

    /** {@link #emptyInto(IItemHandlerModifiable, int, int, IFluidHandler, Predicate)} for one fluid. */
    public static boolean emptyInto(IItemHandlerModifiable inventory, int inSlot, int outSlot, IFluidHandler tank, Fluid fluid) {
        return emptyInto(inventory, inSlot, outSlot, tank, f -> f.isSame(fluid));
    }

    /**
     * Pulls one fluid from a neighbour into a tank, at most {@code budget} mB. Unlike a plain
     * {@code drain(int)}, it asks the neighbour for this fluid by name, so a neighbour with several
     * tanks gives it even when its first tank holds something else.
     *
     * @return the amount moved
     */
    public static int pull(IFluidHandler from, IFluidHandler into, Fluid fluid, int budget) {
        if (from == null || into == null || from == into || budget <= 0) return 0;
        int room = into.fill(new FluidStack(fluid, budget), IFluidHandler.FluidAction.SIMULATE);
        if (room <= 0) return 0;
        FluidStack offered = from.drain(new FluidStack(fluid, room), IFluidHandler.FluidAction.SIMULATE);
        if (offered == null || offered.isEmpty() || !offered.getFluid().isSame(fluid)) return 0;
        int amount = Math.min(room, offered.getAmount());
        FluidStack drained = from.drain(offered.copyWithAmount(amount), IFluidHandler.FluidAction.EXECUTE);
        if (drained == null || drained.isEmpty()) return 0;
        return into.fill(drained.copyWithAmount(Math.min(drained.getAmount(), amount)), IFluidHandler.FluidAction.EXECUTE);
    }
}
