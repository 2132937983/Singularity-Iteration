// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Owned custody of loot after world removal and before inventory/entity delivery. */
public final class PendingDrops {
    public static final int MAX_STACKS = 256;
    private List<ItemStack> pending = new ArrayList<>();
    private ListTag unresolved = new ListTag();
    private final Runnable changed;
    private boolean delivering;
    private boolean capturing;
    public PendingDrops(Runnable changed) { this.changed = changed; }
    public boolean isEmpty() { return pending.isEmpty() && unresolved.isEmpty(); }
    public boolean isBusy() { return delivering || capturing; }
    public ItemStack first() { return pending.isEmpty() ? ItemStack.EMPTY : pending.getFirst().copy(); }
    public List<ItemStack> items() { return pending.stream().map(ItemStack::copy).toList(); }
    boolean beginWorldChange() {
        if (isBusy() || !isEmpty()) return false;
        capturing = true; return true;
    }
    void endWorldChange() { capturing = false; }
    public boolean stage(List<ItemStack> drops) {
        if (delivering || !isEmpty() || drops.size() > MAX_STACKS) return false;
        pending = new ArrayList<>();
        for (var stack : drops) if (!stack.isEmpty()) pending.add(stack.copy());
        if (!pending.isEmpty()) changed.run();
        return true;
    }
    public void cancel() { pending.clear(); unresolved.clear(); changed.run(); }
    public boolean commitOwned(MachineItemHandler inventory, int[] slots) {
        if (isBusy()) return false;
        if (pending.isEmpty()) return unresolved.isEmpty();
        var operation = RecipeSlots.outputs(inventory, slots, pending);
        if (operation.isEmpty()) return false;
        var before = pending;
        pending = new ArrayList<>();
        delivering = true;
        try {
            if (!operation.get().commit()) { pending = before; return false; }
            changed.run();
            return unresolved.isEmpty();
        } finally { delivering = false; }
    }
    /** Move only the first stack, with a bounded number of external slot calls. */
    public int deliver(IItemHandler target, int slotBudget) {
        return deliverRange(target, 0, slotBudget);
    }
    public int deliverRange(IItemHandler target, int firstSlot, int slotBudget) {
        if (isBusy() || pending.isEmpty() || slotBudget <= 0) return 0;
        if (firstSlot < 0) return 0;
        delivering = true;
        int moved = 0;
        try {
            int end = (int) Math.min(target.getSlots(), (long) firstSlot + slotBudget);
            for (int slot = firstSlot; slot < end && !pending.isEmpty(); slot++) {
                var offered = pending.getFirst().copy();
                var remainder = target.insertItem(slot, offered.copy(), false);
                if (!remainder.isEmpty() && (!ItemStack.isSameItemSameComponents(offered, remainder)
                        || remainder.getCount() > offered.getCount()))
                    throw new IllegalStateException("Item handler returned an invalid mining-loot remainder");
                int accepted = offered.getCount() - remainder.getCount();
                if (accepted > 0) {
                    moved += accepted;
                    if (remainder.isEmpty()) pending.removeFirst(); else pending.set(0, remainder.copy());
                    changed.run();
                    if (remainder.isEmpty()) break;
                }
            }
            return moved;
        } finally { delivering = false; }
    }
    public boolean spawn(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (isBusy() || pending.isEmpty() || !level.getServer().isSameThread()) return false;
        delivering = true;
        try {
            var entity = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + .5, pos.getY() + .5,
                pos.getZ() + .5, pending.getFirst().copy());
            if (!level.addFreshEntity(entity)) return false;
            pending.removeFirst(); changed.run(); return true;
        } finally { delivering = false; }
    }
    public ListTag save(HolderLookup.Provider registries) {
        var tag = new ListTag();
        for (var stack : pending) tag.add(stack.save(registries));
        for (var unknown : unresolved) tag.add(unknown.copy());
        return tag;
    }
    public void load(HolderLookup.Provider registries, ListTag tag) {
        pending = new ArrayList<>(); unresolved = new ListTag();
        if (tag.size() > MAX_STACKS) { unresolved = tag.copy(); return; }
        for (int i = 0; i < tag.size(); i++) {
            var raw = tag.getCompound(i);
            var stack = ItemStack.parseOptional(registries, raw);
            if (stack.isEmpty()) unresolved.add(raw.copy()); else pending.add(stack);
        }
    }
}
