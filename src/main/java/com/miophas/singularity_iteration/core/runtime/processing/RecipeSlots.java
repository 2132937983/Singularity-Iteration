// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Prepare every owned recipe slot before consuming input or publishing any result. */
public final class RecipeSlots {
    private RecipeSlots() { }

    /** Owner-only partial insertion. Output-slot external admission rules remain unchanged. */
    public static ItemStack insertOutput(MachineItemHandler inventory, int[] slots, ItemStack result) {
        if (result.isEmpty()) return ItemStack.EMPTY;
        int[] indices = slots.clone();
        ItemStack[] before = new ItemStack[indices.length], after = new ItemStack[indices.length];
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] < 0 || indices[i] >= inventory.getSlots()) return result.copy();
            for (int j = 0; j < i; j++) if (indices[i] == indices[j]) return result.copy();
            before[i] = inventory.getStackInSlot(indices[i]).copy();
            after[i] = before[i].copy();
        }
        int remaining = result.getCount();
        for (int pass = 0; pass < 2; pass++) for (int i = 0; i < indices.length && remaining > 0; i++) {
            var stack = after[i];
            if (pass == 0 ? stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, result) : !stack.isEmpty()) continue;
            int room = Math.max(0, Math.min(inventory.getSlotLimit(indices[i]), result.getMaxStackSize()) - stack.getCount());
            int added = Math.min(room, remaining);
            if (added > 0) { after[i] = result.copyWithCount(stack.getCount() + added); remaining -= added; }
        }
        if (remaining == result.getCount()) return result.copy();
        return inventory.scexCommitSlots(indices, before, after) ? result.copyWithCount(remaining) : result.copy();
    }

    /** Pack all outputs against one shared snapshot, including several drops competing for one slot. */
    public static Optional<Prepared> outputs(MachineItemHandler inventory, int[] outputSlots, List<ItemStack> results) {
        if (outputSlots.length == 0 || results.isEmpty()) return Optional.empty();
        int[] indices = outputSlots.clone();
        ItemStack[] before = new ItemStack[indices.length], after = new ItemStack[indices.length];
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] < 0 || indices[i] >= inventory.getSlots()) return Optional.empty();
            for (int j = 0; j < i; j++) if (indices[j] == indices[i]) return Optional.empty();
            before[i] = inventory.getStackInSlot(indices[i]).copy(); after[i] = before[i].copy();
        }
        for (var result : results) {
            if (result == null) return Optional.empty();
            if (result.isEmpty()) continue;
            int remaining = result.getCount();
            for (int pass = 0; pass < 2; pass++) for (int i = 0; i < indices.length && remaining > 0; i++) {
                var stack = after[i];
                if (pass == 0 ? stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, result) : !stack.isEmpty()) continue;
                int limit = Math.min(inventory.getSlotLimit(indices[i]), result.getMaxStackSize());
                int added = Math.min(remaining, Math.max(0, limit - stack.getCount()));
                if (added > 0) { after[i] = result.copyWithCount(stack.getCount() + added); remaining -= added; }
            }
            if (remaining != 0) return Optional.empty();
        }
        return Optional.of(new Prepared(inventory, indices, before, after));
    }

    /** Explicit input-only consumption, such as a recycler roll that yields no item. */
    public static Optional<Prepared> consumeOnly(MachineItemHandler inventory, int slot, int consumed) {
        if (slot < 0 || slot >= inventory.getSlots() || consumed <= 0) return Optional.empty();
        var before = inventory.getStackInSlot(slot).copy();
        if (before.getCount() < consumed) return Optional.empty();
        return Optional.of(new Prepared(inventory, new int[]{slot}, new ItemStack[]{before},
            new ItemStack[]{before.copyWithCount(before.getCount() - consumed)}));
    }

    /** Merge independent lanes so inventory observers cannot see only the first finished lane. */
    public static Optional<Prepared> combine(List<Prepared> lanes) {
        if (lanes.isEmpty()) return Optional.empty();
        var inventory = lanes.getFirst().inventory;
        int count = 0;
        for (var lane : lanes) {
            if (lane.inventory != inventory || !lane.current()) return Optional.empty();
            if (lane.indices.length > inventory.getSlots() - count) return Optional.empty();
            count += lane.indices.length;
        }
        int[] indices = new int[count];
        ItemStack[] before = new ItemStack[count], after = new ItemStack[count];
        int offset = 0;
        for (var lane : lanes) for (int i = 0; i < lane.indices.length; i++) {
            for (int j = 0; j < offset; j++) if (indices[j] == lane.indices[i]) return Optional.empty();
            indices[offset] = lane.indices[i];
            before[offset] = lane.before[i].copy(); after[offset] = lane.after[i].copy();
            offset++;
        }
        return Optional.of(new Prepared(inventory, indices, before, after));
    }
    public static Optional<Prepared> prepare(MachineItemHandler inventory, int inputSlot, int consumed,
                                             int[] outputSlots, List<ItemStack> results) {
        if (consumed <= 0 || inputSlot < 0 || inputSlot >= inventory.getSlots()
                || results.isEmpty() || results.size() > outputSlots.length) return Optional.empty();
        int[] indices = new int[outputSlots.length + 1];
        ItemStack[] before = new ItemStack[indices.length], after = new ItemStack[indices.length];
        indices[0] = inputSlot;
        before[0] = inventory.getStackInSlot(inputSlot).copy();
        if (before[0].getCount() < consumed) return Optional.empty();
        after[0] = before[0].copyWithCount(before[0].getCount() - consumed);
        boolean hasOutput = false;
        for (int i = 0; i < outputSlots.length; i++) {
            int slot = outputSlots[i];
            if (slot < 0 || slot >= inventory.getSlots()) return Optional.empty();
            for (int j = 0; j <= i; j++) if (indices[j] == slot) return Optional.empty();
            indices[i + 1] = slot;
            before[i + 1] = inventory.getStackInSlot(slot).copy();
            ItemStack result = i < results.size() ? results.get(i) : ItemStack.EMPTY;
            if (result == null) return Optional.empty();
            after[i + 1] = before[i + 1].copy();
            if (result.isEmpty()) continue;
            hasOutput = true;
            var current = before[i + 1];
            int limit = Math.min(inventory.getSlotLimit(slot), result.getMaxStackSize());
            if (result.getCount() > limit || !current.isEmpty()
                    && (!ItemStack.isSameItemSameComponents(current, result) || current.getCount() > limit - result.getCount()))
                return Optional.empty();
            after[i + 1] = current.isEmpty() ? result.copy() : current.copyWithCount(current.getCount() + result.getCount());
        }
        return hasOutput ? Optional.of(new Prepared(inventory, indices, before, after)) : Optional.empty();
    }
    public static final class Prepared {
        private final MachineItemHandler inventory;
        private final int[] indices;
        private final ItemStack[] before, after;
        private boolean used;
        private Prepared(MachineItemHandler inventory, int[] indices, ItemStack[] before, ItemStack[] after) {
            this.inventory = inventory; this.indices = indices; this.before = before; this.after = after;
        }
        private boolean current() {
            if (used) return false;
            for (int i = 0; i < indices.length; i++)
                if (!ItemStack.matches(before[i], inventory.getStackInSlot(indices[i]))) return false;
            return true;
        }
        public boolean commit() {
            if (!current()) return false;
            used = true;
            return inventory.scexCommitSlots(indices, before, after);
        }
        /** The caller's owned tank may only mark saving state during its change notification. */
        public boolean commitWithFluid(FluidTank tank, FluidStack payment) {
            if (!current() || payment.isEmpty()
                    || !FluidStack.matches(tank.drain(payment, IFluidHandler.FluidAction.SIMULATE), payment)) return false;
            var fluidBefore = tank.getFluid().copy();
            if (!FluidStack.matches(tank.drain(payment, IFluidHandler.FluidAction.EXECUTE), payment)) {
                tank.setFluid(fluidBefore); return false;
            }
            if (!commit()) { tank.setFluid(fluidBefore); return false; }
            return true;
        }
    }
}
