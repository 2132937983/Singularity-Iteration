// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** One basic-miner operation: an X-then-Z tunnel, or one downward block, with a single paid budget. */
public final class MiningRoute {
    public static final int RADIUS = 6;
    private BlockPos start, target, cursor;
    private long cost;
    private boolean paid, invalid;
    private final Runnable changed;
    public MiningRoute(Runnable changed) { this.changed = changed; }
    public boolean active() { return target != null; }
    public boolean invalid() { return invalid; }
    public boolean paid() { return paid; }
    public long cost() { return cost; }
    public BlockPos target() { return target; }
    public boolean complete() { return active() && cursor.equals(target); }
    public boolean begin(BlockPos from, BlockPos to, long cost) {
        if (active() || invalid || cost <= 0 || !validEndpoints(from, to)) return false;
        this.start = from.immutable(); this.target = to.immutable(); cursor = start;
        this.cost = cost; paid = false; changed.run(); return true;
    }
    public BlockPos next() {
        if (!active() || invalid || complete()) return null;
        if (cursor.getY() != target.getY()) return cursor.below();
        if (cursor.getX() != target.getX()) return cursor.offset(Integer.compare(target.getX(), cursor.getX()), 0, 0);
        return cursor.offset(0, 0, Integer.compare(target.getZ(), cursor.getZ()));
    }
    public void markPaid() { if (!active() || invalid) throw new IllegalStateException("No payable mining route"); paid = true; }
    public void advance(BlockPos actual) {
        if (actual == null || !actual.equals(next())) throw new IllegalStateException("Mining route advanced out of order");
        cursor = actual.immutable(); changed.run();
    }
    public void finish() {
        if (!complete() || invalid) throw new IllegalStateException("Cannot finish an incomplete route");
        start = target = cursor = null; cost = 0; paid = false; changed.run();
    }
    private static boolean validEndpoints(BlockPos from, BlockPos to) {
        if (from == null || to == null) return false;
        long dx = Math.abs((long) from.getX() - to.getX()), dz = Math.abs((long) from.getZ() - to.getZ());
        long dy = (long) from.getY() - to.getY();
        return dx <= RADIUS && dz <= RADIUS && (dy == 0 && dx + dz > 0 || dy == 1 && dx == 0 && dz == 0);
    }
    public CompoundTag save() {
        var tag = new CompoundTag(); tag.putBoolean("invalid", invalid);
        if (active()) {
            tag.putLong("start", start.asLong()); tag.putLong("target", target.asLong()); tag.putLong("cursor", cursor.asLong());
            tag.putLong("cost", cost); tag.putBoolean("paid", paid);
        }
        return tag;
    }
    public void load(CompoundTag tag) {
        start = target = cursor = null; cost = 0; paid = false; invalid = tag.getBoolean("invalid");
        if (!tag.contains("target")) return;
        start = BlockPos.of(tag.getLong("start")); target = BlockPos.of(tag.getLong("target")); cursor = BlockPos.of(tag.getLong("cursor"));
        cost = tag.getLong("cost"); paid = tag.getBoolean("paid");
        if (!validEndpoints(start, target) || cost <= 0) { invalid = true; return; }
        // A cursor must belong to this exact monotonic route, not merely lie in its bounding square.
        var savedCursor = cursor; cursor = start;
        for (int i = 0; i <= 2 * RADIUS && !cursor.equals(savedCursor) && !complete(); i++) cursor = next();
        invalid |= !cursor.equals(savedCursor); cursor = savedCursor;
    }
    public boolean belongsTo(BlockPos machine, int minY, int maxY) {
        return !active() || !invalid && start.getX() == machine.getX() && start.getZ() == machine.getZ()
            && start.getY() <= machine.getY() && start.getY() >= minY && target.getY() >= minY
            && start.getY() < maxY && target.getY() < maxY;
    }
}
