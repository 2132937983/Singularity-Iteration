// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import java.util.Objects;
import java.util.function.Consumer;

/** Persistent ownership between two non-transactional inventory handlers. */
public final class ItemTransferBuffer {
    private final Runnable changed;
    private ItemStack pending = ItemStack.EMPTY, uncertain = ItemStack.EMPTY;
    private String phase = "";
    private CompoundTag unrecognized;
    private boolean transferring;
    private Consumer<ItemStack> removalSink;

    public ItemTransferBuffer(Runnable changed) { this.changed = changed; }
    public ItemStack pending() { return pending.copy(); }
    public boolean hasPending() { return !pending.isEmpty(); }
    public boolean blocked() { return unrecognized != null || !phase.isEmpty(); }
    public String phase() { return phase; }

    /** Deliver known-owned input even when the original inventory no longer exists. */
    public int deliverPending(IItemHandler target, int slot, int budget) {
        if (!hasPending()) return 0;
        return move(net.neoforged.neoforge.items.wrapper.EmptyItemHandler.INSTANCE, 0, target, slot, budget);
    }

    /** Only confirmed ownership is released. Uncertain external calls are never refunded. */
    public void closeForRemoval(Consumer<ItemStack> sink) {
        if (removalSink != null) return;
        removalSink = Objects.requireNonNull(sink);
        releaseRemovedPending();
    }

    private void releaseRemovedPending() {
        if (removalSink == null || unrecognized != null || pending.isEmpty()) return;
        ItemStack owned = pending;
        pending = ItemStack.EMPTY;
        changed.run();
        removalSink.accept(owned);
    }

    /** targetSlot=-1 uses the handler's normal stacking algorithm. Simulation never authorizes a refund. */
    public int move(IItemHandler source, int sourceSlot, IItemHandler target, int targetSlot, int budget) {
        if (transferring || removalSink != null || blocked() || budget <= 0 || source == target) return 0;
        transferring = true;
        try {
            if (pending.isEmpty()) {
                var available = source.extractItem(sourceSlot, budget, true).copy();
                if (removalSink != null || available.isEmpty()) return 0;
                if (available.getCount() > budget) throw new IllegalStateException("Oversized item extraction quote");
                int quoted = accepted(available, insert(target, targetSlot, available.copy(), true));
                if (removalSink != null || quoted == 0) return 0;
                uncertain = available.copyWithCount(quoted); phase = "extract"; changed.run();
                var actual = source.extractItem(sourceSlot, quoted, false);
                if (!actual.isEmpty() && (actual.getCount() > quoted || !ItemStack.isSameItemSameComponents(actual, uncertain)))
                    throw new IllegalStateException("Invalid item extraction receipt");
                pending = actual.copy(); clearIntent(); changed.run();
                if (removalSink != null) { releaseRemovedPending(); return 0; }
            }
            if (pending.isEmpty()) return 0;
            var offer = pending.copyWithCount(Math.min(budget, pending.getCount()));
            pending.shrink(offer.getCount());
            uncertain = offer.copy(); phase = "insert"; changed.run();
            // The persistence callback can synchronously remove the owner.  A
            // stale handler must never receive a reserved item after removal;
            // the uncertain phase intentionally quarantines that ownership.
            if (removalSink != null) return 0;
            var remainder = insert(target, targetSlot, offer.copy(), false);
            int moved = accepted(uncertain, remainder);
            if (!remainder.isEmpty()) {
                if (pending.isEmpty()) pending = remainder.copy(); else pending.grow(remainder.getCount());
            }
            clearIntent(); changed.run();
            releaseRemovedPending();
            return moved;
        } finally { transferring = false; }
    }
    private static ItemStack insert(IItemHandler handler, int slot, ItemStack stack, boolean simulate) {
        return slot < 0 ? ItemHandlerHelper.insertItemStacked(handler, stack, simulate) : handler.insertItem(slot, stack, simulate);
    }
    private static int accepted(ItemStack offered, ItemStack remainder) {
        if (remainder == null || !remainder.isEmpty() && (remainder.getCount() > offered.getCount()
                || !ItemStack.isSameItemSameComponents(offered, remainder)))
            throw new IllegalStateException("Invalid item insertion receipt");
        return offered.getCount() - remainder.getCount();
    }
    private void clearIntent() { uncertain = ItemStack.EMPTY; phase = ""; }
    public CompoundTag save(HolderLookup.Provider registries) {
        if (unrecognized != null) return unrecognized.copy();
        var tag = new CompoundTag(); tag.putInt("version", 1);
        tag.put("pending", pending.saveOptional(registries));
        tag.put("uncertain", uncertain.saveOptional(registries)); tag.putString("phase", phase);
        return tag;
    }
    public void load(HolderLookup.Provider registries, CompoundTag tag) {
        if (transferring || removalSink != null) throw new IllegalStateException("Active or removed item transfer cannot reload");
        pending = ItemStack.EMPTY; clearIntent(); unrecognized = null;
        if (tag.isEmpty()) return;
        if (tag.getInt("version") != 1) { unrecognized = tag.copy(); return; }
        pending = ItemStack.parseOptional(registries, tag.getCompound("pending"));
        uncertain = ItemStack.parseOptional(registries, tag.getCompound("uncertain")); phase = tag.getString("phase");
        if (!(phase.isEmpty() || phase.equals("extract") || phase.equals("insert"))
                || phase.isEmpty() != uncertain.isEmpty()
                || !tag.getCompound("pending").isEmpty() && pending.isEmpty()
                || !tag.getCompound("uncertain").isEmpty() && uncertain.isEmpty()
                || phase.equals("extract") && !pending.isEmpty()
                || !pending.isEmpty() && !uncertain.isEmpty() && !ItemStack.isSameItemSameComponents(pending, uncertain))
            unrecognized = tag.copy();
    }
}
