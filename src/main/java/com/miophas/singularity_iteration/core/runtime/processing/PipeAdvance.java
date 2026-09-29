// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Persist one reserved pipe through placement of the next tip and conversion of the previous tip. */
public final class PipeAdvance {
    public enum Outcome { APPLIED, RETRY, UNCERTAIN }
    public interface WorldAccess {
        Outcome placeTip(BlockPos target);
        Outcome replaceOldTip(BlockPos previous);
    }
    private int phase; // 0 idle, 1 new tip, 2 old tip conversion, 3 completed; negative means unresolved world change.
    private BlockPos previous, target;
    private ItemStack reserved = ItemStack.EMPTY;
    private CompoundTag unresolved = new CompoundTag();
    private final Runnable changed;
    private boolean busy;
    public PipeAdvance(Runnable changed) { this.changed = changed; }
    public boolean active() { return phase != 0 || !unresolved.isEmpty(); }
    public boolean isBusy() { return busy; }
    public boolean uncertain() { return phase < 0 || !unresolved.isEmpty(); }
    public boolean complete() { return phase == 3 && unresolved.isEmpty(); }
    public BlockPos target() { return target; }
    public ItemStack reserved() { return reserved.copy(); }
    public boolean begin(MachineItemHandler inventory, int slot, ItemStack expectedPipe, BlockPos previous, BlockPos target) {
        if (busy || active() || target == null || slot < 0 || slot >= inventory.getSlots() || expectedPipe.isEmpty()
                || previous != null && !previous.below().equals(target)) return false;
        var before = inventory.getStackInSlot(slot).copy();
        if (before.isEmpty() || !ItemStack.isSameItemSameComponents(before, expectedPipe)) return false;
        busy = true;
        try {
            this.previous = previous == null ? null : previous.immutable(); this.target = target.immutable();
            reserved = before.copyWithCount(1); phase = 1;
            if (!inventory.scexCommitSlots(new int[]{slot}, new ItemStack[]{before},
                    new ItemStack[]{before.copyWithCount(before.getCount() - 1)})) {
                clear(); return false;
            }
            changed.run(); return true;
        } finally { busy = false; }
    }
    public boolean advance(WorldAccess world) {
        if (busy || !active() || uncertain()) return false;
        busy = true;
        try {
            if (phase == 1) {
                phase = -1; changed.run(); // A save during callbacks must not replay an unconfirmed placement.
                Outcome result = world.placeTip(target);
                if (result == Outcome.UNCERTAIN) return false;
                if (result == Outcome.RETRY) { phase = 1; changed.run(); return false; }
                reserved = ItemStack.EMPTY; phase = previous == null ? 3 : 2; changed.run();
            }
            if (phase == 2) {
                phase = -2; changed.run();
                Outcome result = world.replaceOldTip(previous);
                if (result == Outcome.UNCERTAIN) return false;
                phase = result == Outcome.APPLIED ? 3 : 2; changed.run();
            }
            return complete();
        } finally { busy = false; }
    }
    public void finish() {
        if (busy || !complete()) throw new IllegalStateException("Unfinished pipe advance");
        clear(); changed.run();
    }
    private void clear() { phase = 0; previous = target = null; reserved = ItemStack.EMPTY; unresolved = new CompoundTag(); }
    public CompoundTag save(HolderLookup.Provider registries) {
        if (!unresolved.isEmpty()) return unresolved.copy();
        var tag = new CompoundTag(); tag.putInt("phase", phase);
        if (target != null) tag.putLong("target", target.asLong());
        if (previous != null) tag.putLong("previous", previous.asLong());
        if (!reserved.isEmpty()) tag.put("reserved", reserved.save(registries));
        return tag;
    }
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (busy) throw new IllegalStateException("Cannot load during pipe advance");
        clear(); phase = tag.getInt("phase");
        if (phase == 0 && !tag.contains("reserved") && !tag.contains("target")) return;
        if (tag.contains("target")) target = BlockPos.of(tag.getLong("target"));
        if (tag.contains("previous")) previous = BlockPos.of(tag.getLong("previous"));
        if (tag.contains("reserved")) reserved = ItemStack.parse(registries, tag.getCompound("reserved")).orElse(ItemStack.EMPTY);
        boolean valid = phase >= -2 && phase <= 3 && phase != 0 && target != null
            && (previous == null || previous.below().equals(target))
            && ((phase == 1 || phase == -1) ? reserved.getCount() == 1 : reserved.isEmpty());
        if (!valid) unresolved = tag.copy();
    }
    public boolean belongsTo(BlockPos machine, int minY, int maxY) {
        return !active() || !uncertain() && target != null && target.getX() == machine.getX() && target.getZ() == machine.getZ()
            && target.getY() >= minY && target.getY() < machine.getY() && target.getY() < maxY;
    }
}
