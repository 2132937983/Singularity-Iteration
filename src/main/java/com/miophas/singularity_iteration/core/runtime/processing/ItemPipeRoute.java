// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Incremental breadth-first route to an actual accepting slot, including routes back out of dead ends.
 * Each step is one edge, one slot simulation or one cached-path validation. No recursive item insertion.
 */
public final class ItemPipeRoute {
    public static final int MAX_VISITED = 16_384;
    private static final Direction[] SIDES = Direction.values();
    public interface Access {
        BlockPos pipe(BlockPos from, Direction side);
        IItemHandler inventory(BlockPos from, Direction side);
        long revision();
    }
    public record Target(BlockPos pipe, Direction side, int slot, int accepted) { }
    private static final class Revision { boolean changed; }
    /** Per-node observers contain no strong reference to a route, tile, or world.
     * A restarted search gets a new revision so old dependencies expire naturally.
     */
    public static final class Watch {
        private final WeakHashMap<Revision, Boolean> observers = new WeakHashMap<>();
        public void observe(ItemPipeRoute route) { if (route != null) observers.put(route.localRevision, Boolean.TRUE); }
        public void changed() { for (Revision revision : observers.keySet()) revision.changed = true; }
    }
    private final BlockPos start;
    private final BlockPos excludedContainer;
    private final ItemStack item;
    private final HashMap<BlockPos, BlockPos> parents = new HashMap<>();
    private final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
    private BlockPos current;
    private int sideIndex, slotIndex;
    private BlockPos validationAt;
    private Target candidate;
    private Revision localRevision = new Revision();
    private long revision = Long.MIN_VALUE;
    private boolean exhausted;
    private long work;

    public ItemPipeRoute(BlockPos start, BlockPos excludedContainer, ItemStack item) {
        this.start = start.immutable(); this.excludedContainer = excludedContainer; this.item = item.copy();
    }
    private void restart(long now) {
        parents.clear(); queue.clear(); parents.put(start, start); queue.add(start);
        current = null; sideIndex = slotIndex = 0; validationAt = null; candidate = null;
        localRevision = new Revision();
        exhausted = false; revision = now;
    }
    public boolean exhausted() { return exhausted; }
    public long work() { return work; }
    public int visited() { return parents.size(); }
    /** A confirmed rejection skips this slot for this search, without revisiting earlier outlets. */
    public void rejectTarget() { candidate = null; validationAt = null; }
    /** A successful target remains cached. Every reuse validates the complete path against one revision. */
    public Target advance(Access access, int budget) {
        if (localRevision.changed || revision != access.revision()) restart(access.revision());
        while (budget-- > 0 && !exhausted) {
            work++;
            if (localRevision.changed || revision != access.revision()) { restart(access.revision()); continue; }
            if (candidate != null) {
                if (!validationAt.equals(start)) {
                    BlockPos next = validationAt, from = parents.get(next);
                    Direction side = Direction.fromDelta(next.getX()-from.getX(), next.getY()-from.getY(), next.getZ()-from.getZ());
                    if (side == null || !next.equals(access.pipe(from, side))) { restart(access.revision()); continue; }
                    validationAt = from; continue;
                }
                IItemHandler handler = access.inventory(candidate.pipe, candidate.side);
                if (handler == null || candidate.slot >= slots(handler)) { rejectTarget(); continue; }
                int accepted = accepted(handler, candidate.slot, item);
                if (localRevision.changed || revision != access.revision()) { restart(access.revision()); continue; }
                if (accepted == 0) { rejectTarget(); continue; }
                validationAt = candidate.pipe;
                return new Target(candidate.pipe, candidate.side, candidate.slot, accepted);
            }
            if (current == null) {
                current = queue.poll(); sideIndex = slotIndex = 0;
                if (current == null) { exhausted = true; break; }
            }
            Direction side = SIDES[sideIndex];
            BlockPos next = access.pipe(current, side);
            if (next != null) {
                if (parents.size() < MAX_VISITED && !parents.containsKey(next)) {
                    parents.put(next, current); queue.add(next);
                }
                nextSide(); continue;
            }
            IItemHandler handler = current.relative(side).equals(excludedContainer) ? null : access.inventory(current, side);
            if (handler == null || slotIndex >= slots(handler)) { nextSide(); continue; }
            int slot = slotIndex++;
            int accepted = accepted(handler, slot, item);
            if (accepted == 0) continue;
            candidate = new Target(current, side, slot, accepted);
            validationAt = current;
        }
        return null;
    }
    private void nextSide() { slotIndex = 0; if (++sideIndex == SIDES.length) current = null; }
    private static int slots(IItemHandler handler) {
        try { return Math.max(0, handler.getSlots()); }
        catch (RuntimeException unavailable) { return 0; }
    }
    public static boolean validRemainder(ItemStack offered, ItemStack remainder) {
        return remainder != null && (remainder.isEmpty() || (ItemStack.isSameItemSameComponents(offered, remainder)
            && remainder.getCount() >= 0 && remainder.getCount() <= offered.getCount()));
    }
    public static int accepted(IItemHandler handler, int slot, ItemStack item) {
        try {
            ItemStack remainder = handler.insertItem(slot, item.copy(), true);
            return validRemainder(item, remainder) ? item.getCount() - remainder.getCount() : 0;
        } catch (RuntimeException unavailable) {
            // No execution was requested. Skip the faulty quote within the same
            // search so other inventories still get their bounded opportunity.
            return 0;
        }
    }
}
