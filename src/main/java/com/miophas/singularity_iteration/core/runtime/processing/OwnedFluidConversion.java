// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** One conversion on the owning server thread. Tank callbacks may only mark saving state. */
public final class OwnedFluidConversion {
    private OwnedFluidConversion() { }

    public static Optional<Prepared> prepare(FluidTank input, FluidStack payment, FluidTank output,
                                             FluidStack product, MachineItemHandler inventory,
                                             int outputSlot, ItemStack byproduct) {
        if (input == output || payment.isEmpty() || product.isEmpty()
                || !FluidStack.matches(input.drain(payment, IFluidHandler.FluidAction.SIMULATE), payment)
                || output.fill(product, IFluidHandler.FluidAction.SIMULATE) != product.getAmount())
            return Optional.empty();
        ItemStack before = ItemStack.EMPTY, after = ItemStack.EMPTY;
        if (!byproduct.isEmpty()) {
            if (outputSlot < 0 || outputSlot >= inventory.getSlots()) return Optional.empty();
            before = inventory.getStackInSlot(outputSlot).copy();
            int limit = Math.min(inventory.getSlotLimit(outputSlot), byproduct.getMaxStackSize());
            if (byproduct.getCount() > limit || !before.isEmpty()
                    && (!ItemStack.isSameItemSameComponents(before, byproduct)
                        || before.getCount() > limit - byproduct.getCount())) return Optional.empty();
            after = before.isEmpty() ? byproduct.copy() : before.copyWithCount(before.getCount() + byproduct.getCount());
        }
        return Optional.of(new Prepared(input, payment.copy(), output, product.copy(), inventory,
            byproduct.isEmpty() ? -1 : outputSlot, before, after));
    }

    public static final class Prepared {
        private final FluidTank input, output;
        private final FluidStack payment, product, inputBefore, outputBefore;
        private final MachineItemHandler inventory;
        private final int slot;
        private final ItemStack before, after;
        private boolean used;

        private Prepared(FluidTank input, FluidStack payment, FluidTank output, FluidStack product,
                         MachineItemHandler inventory, int slot, ItemStack before, ItemStack after) {
            this.input = input; this.payment = payment; this.output = output; this.product = product;
            this.inventory = inventory; this.slot = slot; this.before = before; this.after = after;
            inputBefore = input.getFluid().copy(); outputBefore = output.getFluid().copy();
        }

        /** Caller commits its paid-work counters before this method can notify inventory observers. */
        public boolean commit() {
            if (used || !FluidStack.matches(inputBefore, input.getFluid())
                    || !FluidStack.matches(outputBefore, output.getFluid())
                    || slot >= 0 && !ItemStack.matches(before, inventory.getStackInSlot(slot))) return false;
            used = true;
            if (!FluidStack.matches(input.drain(payment, IFluidHandler.FluidAction.SIMULATE), payment)
                    || output.fill(product, IFluidHandler.FluidAction.SIMULATE) != product.getAmount()) return false;
            if (!FluidStack.matches(input.drain(payment, IFluidHandler.FluidAction.EXECUTE), payment)
                    || output.fill(product, IFluidHandler.FluidAction.EXECUTE) != product.getAmount()) {
                restore(); return false;
            }
            if (slot >= 0 && !inventory.scexCommitSlots(new int[]{slot}, new ItemStack[]{before}, new ItemStack[]{after})) {
                restore(); return false;
            }
            return true;
        }

        private void restore() {
            input.setFluid(inputBefore.copy()); output.setFluid(outputBefore.copy());
        }
    }
}
