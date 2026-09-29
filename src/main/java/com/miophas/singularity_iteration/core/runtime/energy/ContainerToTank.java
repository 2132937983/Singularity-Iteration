// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Whole-container admission for an owned tank and inventory on their server thread. */
public final class ContainerToTank {
    private ContainerToTank() { }

    /** All-or-nothing fill of an owned tank. */
    public static boolean fillOwnedTank(FluidTank tank, FluidStack content) {
        if (content.isEmpty() || tank.fill(content, IFluidHandler.FluidAction.SIMULATE) != content.getAmount()) return false;
        var before = tank.getFluid().copy();
        if (tank.fill(content, IFluidHandler.FluidAction.EXECUTE) == content.getAmount()) return true;
        tank.setFluid(before);
        return false;
    }

    /** Moves all content from a detached single-item container into the tank. */
    public static boolean emptyCopiedContainer(FluidTank tank, IFluidHandlerItem container) {
        var content = container.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (content.isEmpty() || tank.fill(content, IFluidHandler.FluidAction.SIMULATE) != content.getAmount()) return false;
        if (!FluidStack.matches(content, container.drain(content, IFluidHandler.FluidAction.EXECUTE))) return false;
        return fillOwnedTank(tank, content);
    }

    /** Moves an exact amount from the tank into a detached single-item container. */
    public static boolean fillCopiedContainer(FluidTank tank, IFluidHandlerItem container, int amount) {
        if (amount <= 0) return false;
        var content = tank.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        if (content.getAmount() != amount || container.fill(content, IFluidHandler.FluidAction.SIMULATE) != amount) return false;
        if (container.fill(content, IFluidHandler.FluidAction.EXECUTE) != amount) return false;
        var before = tank.getFluid().copy();
        var drained = tank.drain(content, IFluidHandler.FluidAction.EXECUTE);
        if (FluidStack.matches(drained, content)) return true;
        tank.setFluid(before);
        return false;
    }

    /** Tank notifications must not expose a partially committed fuel/inventory state to neighbors. */
    public static void markUnsaved(BlockEntity owner) {
        if (owner.getLevel() instanceof ServerLevel level) {
            var pos = owner.getBlockPos();
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null) chunk.setUnsaved(true);
        }
    }
    public static boolean transfer(MachineItemHandler slots, int inputSlot, int outputSlot,
                                   FluidTank tank, FluidStack content, ItemStack emptyContainer) {
        if (inputSlot == outputSlot || inputSlot < 0 || outputSlot < 0
                || inputSlot >= slots.getSlots() || outputSlot >= slots.getSlots()) return false;
        var input = slots.getStackInSlot(inputSlot);
        var output = slots.getStackInSlot(outputSlot);
        if (input.isEmpty() || content.isEmpty() || emptyContainer.isEmpty()) return false;
        int limit = Math.min(slots.getSlotLimit(outputSlot), emptyContainer.getMaxStackSize());
        if (limit < 1 || !output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, emptyContainer) || output.getCount() >= limit)) return false;
        var inputBefore = input.copy(); var outputBefore = output.copy(); var fluidBefore = tank.getFluid().copy();
        if (tank.fill(content, IFluidHandler.FluidAction.SIMULATE) != content.getAmount()) return false;
        if (!ItemStack.matches(inputBefore, slots.getStackInSlot(inputSlot))
                || !ItemStack.matches(outputBefore, slots.getStackInSlot(outputSlot))) return false;
        int accepted = tank.fill(content, IFluidHandler.FluidAction.EXECUTE);
        if (accepted != content.getAmount()) {
            // This helper is restricted to an owned FluidTank, so a failed fill can be restored.
            tank.setFluid(fluidBefore);
            return false;
        }
        var nextOutput = outputBefore.isEmpty() ? emptyContainer.copyWithCount(1)
            : outputBefore.copyWithCount(outputBefore.getCount() + 1);
        if (!slots.scexCommitSlots(new int[]{inputSlot, outputSlot}, new ItemStack[]{inputBefore, outputBefore},
                new ItemStack[]{inputBefore.copyWithCount(inputBefore.getCount() - 1), nextOutput})) {
            tank.setFluid(fluidBefore);
            return false;
        }
        return true;
    }

    /** Inverse operation for a caller-prepared filled container and its exact fluid content. */
    public static boolean drainToContainer(MachineItemHandler slots, int inputSlot, int outputSlot,
                                           FluidTank tank, FluidStack content, ItemStack filledContainer) {
        if (inputSlot == outputSlot || inputSlot < 0 || outputSlot < 0
                || inputSlot >= slots.getSlots() || outputSlot >= slots.getSlots()
                || content.isEmpty() || filledContainer.isEmpty()) return false;
        var input = slots.getStackInSlot(inputSlot).copy();
        var output = slots.getStackInSlot(outputSlot).copy();
        int limit = Math.min(slots.getSlotLimit(outputSlot), filledContainer.getMaxStackSize());
        if (input.isEmpty() || limit < 1 || !output.isEmpty()
                && (!ItemStack.isSameItemSameComponents(output, filledContainer) || output.getCount() >= limit)) return false;
        var fluid = tank.getFluid().copy();
        var simulated = tank.drain(content, IFluidHandler.FluidAction.SIMULATE);
        if (!FluidStack.matches(simulated, content)) return false;
        if (!ItemStack.matches(input, slots.getStackInSlot(inputSlot)) || !ItemStack.matches(output, slots.getStackInSlot(outputSlot))) return false;
        if (!FluidStack.matches(tank.drain(content, IFluidHandler.FluidAction.EXECUTE), content)) {
            tank.setFluid(fluid); return false;
        }
        var next = output.isEmpty() ? filledContainer.copyWithCount(1) : output.copyWithCount(output.getCount() + 1);
        if (!slots.scexCommitSlots(new int[]{inputSlot, outputSlot}, new ItemStack[]{input, output},
                new ItemStack[]{input.copyWithCount(input.getCount() - 1), next})) {
            tank.setFluid(fluid); return false;
        }
        return true;
    }
}
