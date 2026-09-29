// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Debit before delivery; retain uncertain external calls without automatically replaying them. */
public final class FluidTransferBuffer {
    private FluidStack pending = FluidStack.EMPTY;
    private FluidStack uncertain = FluidStack.EMPTY;
    private String phase = "";
    private CompoundTag unrecognizedSave;
    private final Runnable changed;
    private boolean transferring;

    public FluidTransferBuffer(Runnable changed) { this.changed = changed; }
    public FluidStack pending() { return pending.copy(); }
    public FluidStack uncertain() { return uncertain.copy(); }
    public String uncertainPhase() { return phase; }
    public boolean isBlocked() { return unrecognizedSave != null || !phase.isEmpty(); }
    public boolean hasPending() { return !pending.isEmpty(); }

    /** No fresh extraction, including when the pending amount is smaller than the budget. */
    public int deliverPending(IFluidHandler target, int budget, java.util.function.BooleanSupplier current) {
        if (!hasPending()) return 0;
        return move(net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler.INSTANCE, target, budget, current);
    }

    /** Server-thread operation. External handlers must honor their reported drain/fill quantities. */
    public int move(IFluidHandler source, IFluidHandler target, int budget) {
        return move(source, target, budget, () -> true);
    }

    /** Recheck owner identity after foreign callbacks; confirmed custody is retained on removal. */
    public int move(IFluidHandler source, IFluidHandler target, int budget, java.util.function.BooleanSupplier current) {
        if (transferring || isBlocked() || source == target || budget <= 0 || !current.getAsBoolean()) return 0;
        transferring = true;
        try {
            if (pending.isEmpty()) {
                var available = source.drain(budget, IFluidHandler.FluidAction.SIMULATE);
                if (!current.getAsBoolean() || available.isEmpty()) return 0;
                available = available.copyWithAmount(Math.min(budget, available.getAmount()));
                int quoted = target.fill(available.copy(), IFluidHandler.FluidAction.SIMULATE);
                if (!current.getAsBoolean()) return 0;
                if (quoted < 0 || quoted > available.getAmount())
                    throw new IllegalStateException("Fluid handler reported an invalid fill quote");
                if (quoted <= 0) return 0;
                uncertain = available.copyWithAmount(quoted);
                phase = "drain";
                changed.run();
                // Persisting the intent may synchronously remove the owner.
                // Retain the durable intent without calling a stale source.
                if (!current.getAsBoolean()) return 0;
                var actual = source.drain(uncertain.copy(), IFluidHandler.FluidAction.EXECUTE);
                // A thrown/invalid response cannot prove how much left the external source.
                // Leave the intent durable and blocked instead of extracting the same amount again.
                if (!actual.isEmpty() && (actual.getAmount() > uncertain.getAmount()
                        || !FluidStack.isSameFluidSameComponents(actual, uncertain)))
                    throw new IllegalStateException("Fluid handler returned an invalid drained stack");
                pending = actual.copy();
                clearIntent();
                changed.run();
                if (pending.isEmpty() || !current.getAsBoolean()) return 0;
            }
            var offered = pending.copyWithAmount(Math.min(budget, pending.getAmount()));
            // Reserve the offered portion before calling the external handler. A callback can save
            // this state, and a thrown response must never cause this reservation to be sent again.
            pending.shrink(offered.getAmount());
            if (pending.isEmpty()) pending = FluidStack.EMPTY;
            uncertain = offered.copy();
            phase = "fill";
            changed.run();
            // The persistence callback may synchronously unload/replace the
            // owner.  Do not call a foreign handler with a stale reservation;
            // leave the offered amount in the durable uncertain phase instead.
            if (!current.getAsBoolean()) return 0;
            int accepted = target.fill(offered, IFluidHandler.FluidAction.EXECUTE);
            if (accepted < 0 || accepted > uncertain.getAmount())
                throw new IllegalStateException("Fluid handler reported an invalid accepted quantity");
            int rejected = uncertain.getAmount() - accepted;
            if (rejected > 0) {
                if (pending.isEmpty()) pending = uncertain.copyWithAmount(rejected);
                else pending.grow(rejected);
            }
            clearIntent();
            changed.run();
            return accepted;
        } finally {
            transferring = false;
        }
    }

    private void clearIntent() { uncertain = FluidStack.EMPTY; phase = ""; }

    public Tag save(HolderLookup.Provider registries) {
        if (unrecognizedSave != null) return unrecognizedSave.copy();
        var tag = new CompoundTag();
        tag.putInt("TransferVersion", 2);
        tag.put("Pending", pending.saveOptional(registries));
        tag.put("Uncertain", uncertain.saveOptional(registries));
        tag.putString("Phase", phase);
        return tag;
    }
    public void load(HolderLookup.Provider registries, CompoundTag tag) {
        if (transferring) throw new IllegalStateException("Cannot replace an active fluid transfer");
        pending = FluidStack.EMPTY; clearIntent(); unrecognizedSave = null;
        if (!tag.contains("TransferVersion")) {
            pending = FluidStack.parseOptional(registries, tag);
            if (!tag.isEmpty() && pending.isEmpty()) unrecognizedSave = tag.copy();
            return;
        }
        if (!tag.contains("TransferVersion", Tag.TAG_INT) || tag.getInt("TransferVersion") != 2
                || !tag.contains("Pending", Tag.TAG_COMPOUND) || !tag.contains("Uncertain", Tag.TAG_COMPOUND)
                || !tag.contains("Phase", Tag.TAG_STRING)) {
            unrecognizedSave = tag.copy(); return;
        }
        var savedPending = tag.getCompound("Pending");
        var savedUncertain = tag.getCompound("Uncertain");
        pending = FluidStack.parseOptional(registries, savedPending);
        uncertain = FluidStack.parseOptional(registries, savedUncertain);
        phase = tag.getString("Phase");
        if ((!savedPending.isEmpty() && pending.isEmpty()) || (!savedUncertain.isEmpty() && uncertain.isEmpty())
                || !(phase.isEmpty() || phase.equals("drain") || phase.equals("fill"))
                || phase.isEmpty() != uncertain.isEmpty()
                || phase.equals("drain") && !pending.isEmpty()
                || phase.equals("fill") && !pending.isEmpty() && !FluidStack.isSameFluidSameComponents(pending, uncertain))
            unrecognizedSave = tag.copy();
    }
}
