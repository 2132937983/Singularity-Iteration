// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.11.222 (cave overhaul G2, decision one A): grid path finding for a flying Wingman.
 *
 * <p>A node is a block cell where the Wingman's body stands: its box with the feet on the cell's floor (the top of a slab,
 * a carpet or a snow layer in the cell, at most {@link #LOW_BLOCK}; otherwise the cell's bottom) meets no block. From a node
 * the search goes to the 26 cells round it, a diagonal only when every straight step it is made of is free too (no corner
 * cutting). A step costs its length, plus {@code heightCost} for every block of air under the feet (at most
 * {@link #HEIGHT_CAP}: the caller sets it under a ceiling so that paths keep to the floor), plus {@link #WATER_COST} in water.
 * Lava, fire, magma, cactus, berry bushes, wither roses and powder snow are never entered.
 *
 * <p>A {@link Search} is A* with the straight distance as estimate; it runs in slices ({@link Search#step}) so that one search
 * may take several ticks, and it gives up after {@link #MAX_NODES} expanded nodes.
 */
public final class WingmanPathing {
    private WingmanPathing() {}

    /** Expanded nodes after which a search fails. */
    public static final int MAX_NODES = 1500;
    /** Highest block (a slab, a carpet, a snow layer) that the feet stand on inside a cell. */
    public static final double LOW_BLOCK = 0.6;
    /** Blocks of air under the feet that count for the height cost at most. */
    public static final int HEIGHT_CAP = 6;
    /** Extra cost of a step in water. */
    public static final double WATER_COST = 1.5;

    static long key(int x, int y, int z) { return BlockPos.asLong(x, y, z); }

    /** Never entered: they burn, poison or trap. */
    static boolean dangerous(BlockState s) {
        return s.is(BlockTags.FIRE) || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.CACTUS) || s.is(Blocks.SWEET_BERRY_BUSH)
            || s.is(Blocks.WITHER_ROSE) || s.is(Blocks.POWDER_SNOW) || s.is(BlockTags.CAMPFIRES) || s.getFluidState().is(FluidTags.LAVA);
    }

    /** The cells of one search: where the body stands in a cell, measured once each. */
    static final class Cells {
        private static final byte UNKNOWN = 0, NO = 1;   // otherwise 2 + the floor offset in twentieths
        final Level level;
        final Entity who;
        final double halfWidth, height;
        private final Long2ByteOpenHashMap fit = new Long2ByteOpenHashMap();
        private final Long2ByteOpenHashMap gap = new Long2ByteOpenHashMap();

        Cells(Level level, Entity who) {
            this.level = level;
            this.who = who;
            this.halfWidth = who.getBbWidth() / 2;
            this.height = who.getBbHeight();
            fit.defaultReturnValue(UNKNOWN);
            gap.defaultReturnValue((byte) -1);
        }

        /** Height of the feet above the cell's bottom when the body stands in cell (x, y, z), or NaN when it does not fit. */
        double base(int x, int y, int z) {
            long k = key(x, y, z);
            byte b = fit.get(k);
            if (b == UNKNOWN) {
                b = measure(x, y, z);
                fit.put(k, b);
            }
            return b == NO ? Double.NaN : (b - 2) / 20.0;
        }

        boolean fits(int x, int y, int z) { return !Double.isNaN(base(x, y, z)); }

        private byte measure(int x, int y, int z) {
            BlockPos p = new BlockPos(x, y, z);
            if (!level.isLoaded(p)) return NO;
            BlockState s = level.getBlockState(p);
            if (dangerous(s) || dangerous(level.getBlockState(p.above())) || dangerous(level.getBlockState(p.below()))) return NO;
            double floor = 0;
            var shape = s.getCollisionShape(level, p);
            if (!shape.isEmpty()) {
                floor = shape.max(Direction.Axis.Y);
                if (floor > LOW_BLOCK) return NO;
            }
            AABB box = new AABB(x + 0.5 - halfWidth, y + floor + 1.0E-3, z + 0.5 - halfWidth, x + 0.5 + halfWidth, y + floor + height, z + 0.5 + halfWidth);
            if (!level.noCollision(who, box)) return NO;
            return (byte) (2 + Math.round(floor * 20));
        }

        /** Cells of air under a body standing in (x, y, z), at most {@link #HEIGHT_CAP}. */
        int floorGap(int x, int y, int z) {
            long k = key(x, y, z);
            byte g = gap.get(k);
            if (g >= 0) return g;
            int n = 0;
            if (base(x, y, z) == 0) {
                while (n < HEIGHT_CAP) {
                    BlockPos p = new BlockPos(x, y - n - 1, z);
                    if (!level.isLoaded(p) || !level.getBlockState(p).getCollisionShape(level, p).isEmpty()) break;
                    n++;
                }
            }
            gap.put(k, (byte) n);
            return n;
        }

        boolean water(int x, int y, int z) { return level.getFluidState(new BlockPos(x, y, z)).is(FluidTags.WATER); }

        /** The feet of a body standing in cell (x, y, z) (the cell must fit). */
        Vec3 feet(int x, int y, int z) { return new Vec3(x + 0.5, y + base(x, y, z), z + 0.5); }
    }

    /** The cell a body at {@code feet} stands in, or the nearest fitting cell round it (up first, then down, then aside), or null. */
    @Nullable
    static BlockPos cellNear(Cells cells, Vec3 feet, int reach) {
        BlockPos at = BlockPos.containing(feet.x, feet.y + 1.0E-3, feet.z);
        if (cells.fits(at.getX(), at.getY(), at.getZ())) return at;
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int dy = -reach; dy <= reach; dy++) for (int dx = -reach; dx <= reach; dx++) for (int dz = -reach; dz <= reach; dz++) {
            int x = at.getX() + dx, y = at.getY() + dy, z = at.getZ() + dz;
            if (!cells.fits(x, y, z)) continue;
            double d = cells.feet(x, y, z).distanceToSqr(feet);
            if (d < bestD) { bestD = d; best = new BlockPos(x, y, z); }
        }
        return best;
    }

    /** Where a search stands. */
    public enum Status { RUNNING, FOUND, FAILED }

    private static final class Node implements Comparable<Node> {
        final int x, y, z;
        double g, f;
        @Nullable Node parent;
        boolean closed;

        Node(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }

        @Override
        public int compareTo(Node o) { return Double.compare(f, o.f); }
    }

    /** One A* search from a cell to a cell, run in slices. */
    public static final class Search {
        final Cells cells;
        final BlockPos start, goal;
        final double heightCost;
        /** 0.1.11.224 (G5): extra cost of a cell (the cells round a stall); zero for none. */
        final java.util.function.LongToDoubleFunction penalty;
        private final PriorityQueue<Node> open = new PriorityQueue<>();
        private final Long2ObjectOpenHashMap<Node> nodes = new Long2ObjectOpenHashMap<>();
        private int expanded;
        private Status status = Status.RUNNING;
        @Nullable private List<BlockPos> path;
        private double cost;

        Search(Cells cells, BlockPos start, BlockPos goal, double heightCost, java.util.function.LongToDoubleFunction penalty) {
            this.cells = cells;
            this.start = start;
            this.goal = goal;
            this.heightCost = heightCost;
            this.penalty = penalty;
            if (!cells.fits(start.getX(), start.getY(), start.getZ()) || !cells.fits(goal.getX(), goal.getY(), goal.getZ())) {
                status = Status.FAILED;
                return;
            }
            Node n = new Node(start.getX(), start.getY(), start.getZ());
            n.f = estimate(n);
            nodes.put(key(n.x, n.y, n.z), n);
            open.add(n);
        }

        public Status status() { return status; }

        /** Nodes expanded so far. */
        public int expanded() { return expanded; }

        /** The cells from the start to the goal (both included) once found, else null. */
        @Nullable public List<BlockPos> path() { return path; }

        /** The cost of the path once found (its length plus the height and water costs). */
        public double cost() { return cost; }

        private double estimate(Node n) {
            double dx = n.x - goal.getX(), dy = n.y - goal.getY(), dz = n.z - goal.getZ();
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        /** Expands at most {@code budget} nodes; returns the status after. */
        public Status step(int budget) {
            while (status == Status.RUNNING && budget-- > 0) {
                Node n = open.poll();
                if (n == null) { status = Status.FAILED; break; }
                if (n.closed) continue;
                n.closed = true;
                if (n.x == goal.getX() && n.y == goal.getY() && n.z == goal.getZ()) { finish(n); break; }
                if (++expanded >= MAX_NODES) { status = Status.FAILED; break; }
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    int x = n.x + dx, y = n.y + dy, z = n.z + dz;
                    if (!cells.fits(x, y, z)) continue;
                    int axes = (dx != 0 ? 1 : 0) + (dy != 0 ? 1 : 0) + (dz != 0 ? 1 : 0);
                    if (axes > 1 && (dx != 0 && !cells.fits(n.x + dx, n.y, n.z) || dy != 0 && !cells.fits(n.x, n.y + dy, n.z)
                        || dz != 0 && !cells.fits(n.x, n.y, n.z + dz))) continue;
                    long k = key(x, y, z);
                    Node m = nodes.get(k);
                    if (m != null && m.closed) continue;
                    double g = n.g + Math.sqrt(axes) + heightCost * cells.floorGap(x, y, z) + (cells.water(x, y, z) ? WATER_COST : 0)
                        + penalty.applyAsDouble(k);
                    if (m == null) {
                        m = new Node(x, y, z);
                        nodes.put(k, m);
                    } else if (g >= m.g) continue;
                    m.g = g;
                    m.f = g + estimate(m);
                    m.parent = n;
                    open.add(m);
                }
            }
            return status;
        }

        private void finish(Node end) {
            List<BlockPos> cellsOnPath = new ArrayList<>();
            for (Node n = end; n != null; n = n.parent) cellsOnPath.add(new BlockPos(n.x, n.y, n.z));
            Collections.reverse(cellsOnPath);
            path = cellsOnPath;
            cost = end.g;
            status = Status.FOUND;
        }
    }

    /** A search for {@code who} from its place to the cell near {@code goal}, or null when either end has no fitting cell. */
    @Nullable
    public static Search search(Level level, Entity who, Vec3 goal, double heightCost) {
        return search(level, who, goal, heightCost, k -> 0);
    }

    /** The same, with an extra cost for some cells (0.1.11.224: the cells round a stall). */
    @Nullable
    public static Search search(Level level, Entity who, Vec3 goal, double heightCost, java.util.function.LongToDoubleFunction penalty) {
        Cells cells = new Cells(level, who);
        BlockPos from = cellNear(cells, who.position(), 1);
        BlockPos to = cellNear(cells, goal, 2);
        if (from == null || to == null) return null;
        return new Search(cells, from, to, heightCost, penalty);
    }

    /** The feet places along a found path (one per cell). */
    public static List<Vec3> waypoints(Search s) {
        List<Vec3> out = new ArrayList<>();
        if (s.path() == null) return out;
        for (BlockPos p : s.path()) out.add(s.cells.feet(p.getX(), p.getY(), p.getZ()));
        return out;
    }

    /** A search run to its end at once (for tests and for short ways); null when it could not start. */
    @Nullable
    public static Search findNow(Level level, Entity who, Vec3 goal, double heightCost) {
        Search s = search(level, who, goal, heightCost);
        if (s != null) s.step(MAX_NODES * 30);
        return s;
    }
}
