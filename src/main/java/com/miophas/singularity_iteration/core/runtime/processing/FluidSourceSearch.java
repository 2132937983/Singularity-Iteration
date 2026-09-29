// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Bounded connected-fluid search. The caller decides availability without loading chunks. */
public final class FluidSourceSearch {
    public static final int RADIUS = 16, MAX_VISITED = 2048, MAX_PATH = 32, STEPS_PER_TICK = 16;
    public static final int BLOCKED = 0, FLOWING = 1, SOURCE = 2;
    private static final Direction[] DIRECTIONS = Direction.values();
    private record Node(BlockPos pos, Node parent, int depth) { }
    private final ArrayDeque<Node> pending = new ArrayDeque<>();
    private final HashSet<BlockPos> visited = new HashSet<>();
    private BlockPos origin;
    private boolean exhausted;

    public void begin(BlockPos start) {
        reset(); origin = start.immutable(); visited.add(origin); pending.add(new Node(origin, null, 0));
    }
    public void reset() { pending.clear(); visited.clear(); origin = null; exhausted = false; }
    public boolean active() { return origin != null && !exhausted; }
    public boolean exhausted() { return exhausted; }
    public int visitedCount() { return visited.size(); }

    /** At most budget cell queries; a returned path contains at most MAX_PATH cells. */
    public List<BlockPos> advance(ToIntFunction<BlockPos> cells, int budget) {
        if (!active() || budget <= 0) return List.of();
        for (int i = 0; i < Math.min(budget, STEPS_PER_TICK) && !pending.isEmpty(); i++) {
            var node = pending.removeFirst();
            int kind = cells.applyAsInt(node.pos());
            if (kind == SOURCE) {
                var reverse = new ArrayList<BlockPos>();
                for (var n = node; n != null; n = n.parent()) reverse.add(n.pos());
                var result = new ArrayList<BlockPos>(reverse.reversed());
                pending.clear(); exhausted = true;
                return List.copyOf(result);
            }
            if (kind != FLOWING || node.depth() + 1 >= MAX_PATH) continue;
            for (var direction : DIRECTIONS) {
                if (visited.size() >= MAX_VISITED) break;
                var next = node.pos().relative(direction);
                if (Math.abs((long) next.getX() - origin.getX()) > RADIUS
                        || Math.abs((long) next.getY() - origin.getY()) > RADIUS
                        || Math.abs((long) next.getZ() - origin.getZ()) > RADIUS || !visited.add(next)) continue;
                pending.addLast(new Node(next, node, node.depth() + 1));
            }
        }
        if (pending.isEmpty()) exhausted = true;
        return List.of();
    }
}
