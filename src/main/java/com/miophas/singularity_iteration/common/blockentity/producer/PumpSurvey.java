// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.blockentity.producer;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 0.1.11.7: the pump's survey of the body of fluid it works on, run a few hundred blocks per tick so it never stalls a
 * tick.
 *
 * <ul>
 *   <li><b>Pool</b> (pumping): a breadth-first search from a block of the pumped fluid through every connected block
 *       of the same fluid (sources and flowing). It counts the sources (one bucket each), notes whether the pool
 *       refills itself, and records per cross-section column the lowest and highest source, relative to the pump.
 *       Above {@link #CAP} sources it stops and reports a lower bound.</li>
 *   <li><b>Infinite</b>: a source with two horizontal source neighbours over a source or a solid floor comes back when
 *       taken (vanilla water); when the fluid may form new sources there, the pool never runs dry.</li>
 *   <li><b>Basin</b> (discharging): a search from the cell in front of the pump through cells the fluid may fill, up to
 *       the target height and inside the pump's horizontal range. It counts the free cells and the cells already full,
 *       and keeps the free cells lowest first as the fill plan.</li>
 * </ul>
 * The cross-section runs along the pump's facing (along X when the pump faces up or down): {@link #COLUMNS} columns over
 * the extent of the body. Heights are stored as {@code y - pumpY + OFFSET} in a byte ({@link #NONE} when a column is
 * empty).
 */
public final class PumpSurvey {
    /** Sources counted at most; a larger body reports "at least CAP". */
    public static final int CAP = 32768;
    /** Blocks visited per tick. */
    public static final int BUDGET = 768;
    public static final int COLUMNS = 32;
    public static final int OFFSET = 128, NONE = 0;
    /** Free cells kept in the fill plan at most. */
    public static final int PLAN_MAX = 4096;

    public enum Kind { POOL, BASIN }

    // ---- the search in progress
    private Kind kind;
    private Fluid fluid = Fluids.EMPTY;
    private BlockPos pump = BlockPos.ZERO;
    private Direction.Axis axis = Direction.Axis.X;
    private int targetY, rangeMinX, rangeMaxX, rangeMinZ, rangeMaxZ, floorY;
    private Predicate<BlockState> fluidBlock;
    private final LongArrayList queue = new LongArrayList();
    private int head;
    private final LongOpenHashSet seen = new LongOpenHashSet();
    private final Int2IntOpenHashMap colMin = new Int2IntOpenHashMap(), colMax = new Int2IntOpenHashMap();
    private final LongArrayList free = new LongArrayList();
    private int count, filled;
    private boolean infinite, capped, running;
    private int deepest;

    // ---- the last finished result
    private boolean hasResult;
    private Kind resultKind;
    private int resultCount, resultFilled, resultDeepest;
    private boolean resultInfinite, resultCapped;
    private final byte[] resultLow = new byte[COLUMNS], resultHigh = new byte[COLUMNS];
    private long[] plan = new long[0];
    private int planHead;
    private long finishedAt;

    // ------------------------------------------------------------------ start

    /** Starts a pool search from {@code seed}, a block of {@code fluid}. {@code fluidBlock} decides which states count. */
    public void startPool(BlockPos pump, Direction facing, BlockPos seed, Fluid fluid, Predicate<BlockState> fluidBlock) {
        reset(Kind.POOL, pump, facing);
        this.fluid = fluid;
        this.fluidBlock = fluidBlock;
        push(seed.asLong());
    }

    /**
     * Starts a basin search from the cell in front of the pump. Cells are inside the horizontal range box
     * [minX, maxX] x [minZ, maxZ], at or below {@code targetY} and at or above {@code floorY}.
     */
    public void startBasin(BlockPos pump, Direction facing, BlockPos front, Fluid fluid, int targetY, int floorY,
                           int minX, int maxX, int minZ, int maxZ) {
        reset(Kind.BASIN, pump, facing);
        this.fluid = fluid;
        this.targetY = targetY;
        this.floorY = floorY;
        rangeMinX = minX; rangeMaxX = maxX; rangeMinZ = minZ; rangeMaxZ = maxZ;
        push(front.asLong());
    }

    private void reset(Kind kind, BlockPos pump, Direction facing) {
        this.kind = kind;
        this.pump = pump.immutable();
        this.axis = facing.getAxis().isHorizontal() ? facing.getAxis() : Direction.Axis.X;
        queue.clear(); head = 0; seen.clear(); colMin.clear(); colMax.clear(); free.clear();
        count = 0; filled = 0; infinite = false; capped = false; deepest = Integer.MAX_VALUE;
        running = true;
    }

    private void push(long pos) {
        if (seen.add(pos)) queue.add(pos);
    }

    public boolean running() { return running; }

    /** Drops the search in progress (the result stays). */
    public void cancel() { running = false; queue.clear(); seen.clear(); }

    /** Forgets everything. */
    public void clear() {
        cancel();
        hasResult = false;
        plan = new long[0];
        planHead = 0;
    }

    // ------------------------------------------------------------------ step

    /** Visits up to {@link #BUDGET} blocks; returns true when the search finished in this step. */
    public boolean step(ServerLevel level, long gameTime) {
        if (!running) return false;
        int budget = BUDGET;
        while (budget-- > 0 && head < queue.size()) {
            long packed = queue.getLong(head++);
            BlockPos pos = BlockPos.of(packed);
            if (!level.isLoaded(pos)) continue;
            if (kind == Kind.POOL) visitPool(level, pos);
            else visitBasin(level, pos);
            if (capped) break;
        }
        if (head >= queue.size() || capped) {
            finish(gameTime);
            return true;
        }
        return false;
    }

    private void visitPool(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isFluid(state)) return;
        if (state.getFluidState().isSource()) {
            count++;
            column(pos);
            deepest = Math.min(deepest, pos.getY());
            if (!infinite && refills(level, pos, state)) infinite = true;
            if (count >= CAP) { capped = true; return; }
        }
        if (seen.size() > CAP * 4) { capped = true; return; }
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            long key = n.asLong();
            if (seen.contains(key) || !level.isLoaded(n)) continue;
            if (isFluid(level.getBlockState(n))) push(key);
        }
    }

    private boolean isFluid(BlockState state) {
        return !state.getFluidState().isEmpty() && state.getFluidState().getType().isSame(fluid) && fluidBlock.test(state);
    }

    /** A source that would form again when taken: two horizontal source neighbours over a source or a solid floor. */
    private boolean refills(ServerLevel level, BlockPos pos, BlockState state) {
        var fs = state.getFluidState();
        if (!fs.getFluidType().canConvertToSource(fs, level, pos)) return false;
        int around = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            var n = level.getFluidState(pos.relative(d));
            if (n.isSource() && n.getType().isSame(fluid)) around++;
        }
        if (around < 2) return false;
        BlockPos below = pos.below();
        var bs = level.getBlockState(below);
        return bs.getFluidState().isSource() && bs.getFluidState().getType().isSame(fluid) || bs.isFaceSturdy(level, below, Direction.UP);
    }

    private void visitBasin(ServerLevel level, BlockPos pos) {
        if (pos.getY() > targetY || pos.getY() < floorY || pos.getX() < rangeMinX || pos.getX() > rangeMaxX
                || pos.getZ() < rangeMinZ || pos.getZ() > rangeMaxZ) return;
        BlockState state = level.getBlockState(pos);
        var fs = state.getFluidState();
        boolean full = fs.isSource() && fs.getType().isSame(fluid);
        boolean open = full || fillable(state);
        if (!open) return;
        column(pos);
        if (full) filled++;
        else {
            count++;
            if (free.size() < PLAN_MAX) free.add(pos.asLong());
        }
        deepest = Math.min(deepest, pos.getY());
        if (count + filled >= CAP) { capped = true; return; }
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (!seen.contains(n.asLong()) && level.isLoaded(n)) push(n.asLong());
        }
    }

    /** A cell the fluid may flow into: air, flowing fluid of the same kind, or a block the fluid washes away. */
    private boolean fillable(BlockState state) {
        var fs = state.getFluidState();
        if (!fs.isEmpty()) return fs.getType().isSame(fluid) && !fs.isSource();
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) return false;
        return state.isAir() || state.canBeReplaced(fluid) && !(state.getBlock() instanceof LiquidBlock);
    }

    private void column(BlockPos pos) {
        int a = axis == Direction.Axis.X ? pos.getX() : pos.getZ();
        int y = pos.getY();
        colMin.put(a, colMin.containsKey(a) ? Math.min(colMin.get(a), y) : y);
        colMax.put(a, colMax.containsKey(a) ? Math.max(colMax.get(a), y) : y);
    }

    private void finish(long gameTime) {
        running = false;
        hasResult = true;
        resultKind = kind;
        resultCount = count;
        resultFilled = filled;
        resultInfinite = infinite;
        resultCapped = capped;
        resultDeepest = deepest == Integer.MAX_VALUE ? pump.getY() : deepest;
        java.util.Arrays.fill(resultLow, (byte) NONE);
        java.util.Arrays.fill(resultHigh, (byte) NONE);
        if (!colMin.isEmpty()) {
            int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
            for (int a : colMin.keySet()) { lo = Math.min(lo, a); hi = Math.max(hi, a); }
            // the pump's own column belongs in the picture, so the view shows where the intake sits
            int pa = axis == Direction.Axis.X ? pump.getX() : pump.getZ();
            lo = Math.min(lo, pa); hi = Math.max(hi, pa);
            int span = hi - lo + 1;
            if (span <= COLUMNS) {
                // a narrow body: each block row spreads over several columns, so the view is not a comb of thin strips
                for (int c = 0; c < COLUMNS; c++) {
                    int a = lo + (int) ((long) c * span / COLUMNS);
                    if (!colMin.containsKey(a)) continue;
                    resultLow[c] = (byte) rel(colMin.get(a));
                    resultHigh[c] = (byte) rel(colMax.get(a));
                }
            } else {
                for (int a : colMin.keySet()) {
                    int c = Math.min(COLUMNS - 1, (int) ((a - lo) * (long) COLUMNS / span));
                    int low = rel(colMin.get(a)), high = rel(colMax.get(a));
                    int curLow = resultLow[c] & 0xFF, curHigh = resultHigh[c] & 0xFF;
                    resultLow[c] = (byte) (curLow == NONE ? low : Math.min(curLow, low));
                    resultHigh[c] = (byte) (curHigh == NONE ? high : Math.max(curHigh, high));
                }
            }
            pumpColumn = Math.min(COLUMNS - 1, (int) ((pa - lo) * (long) COLUMNS / span + (span <= COLUMNS ? COLUMNS / (2L * span) : 0)));
            originA = lo;
            spanA = span;
        }
        if (kind == Kind.BASIN) {
            long[] cells = free.toLongArray();
            // lowest first, then nearest to the pump: the basin fills from the bottom like real water
            Long[] boxed = new Long[cells.length];
            for (int i = 0; i < cells.length; i++) boxed[i] = cells[i];
            java.util.Arrays.sort(boxed, java.util.Comparator.<Long>comparingInt(p -> BlockPos.getY(p))
                .thenComparingLong(p -> BlockPos.of(p).distManhattan(pump)));
            plan = new long[boxed.length];
            for (int i = 0; i < boxed.length; i++) plan[i] = boxed[i];
        } else {
            plan = new long[0];
        }
        planHead = 0;
        finishedAt = gameTime;
        queue.clear(); seen.clear(); colMin.clear(); colMax.clear(); free.clear();
    }

    private int pumpColumn, originA, spanA = 1;

    private int rel(int y) {
        return Math.max(1, Math.min(255, y - pump.getY() + OFFSET));
    }

    // ------------------------------------------------------------------ result

    public boolean hasResult() { return hasResult; }
    public Kind resultKind() { return resultKind; }
    /** Pool: sources counted. Basin: free cells. */
    public int count() { return resultCount; }
    /** Basin: cells already full. */
    public int filled() { return resultFilled; }
    public boolean infinite() { return resultInfinite; }
    public boolean capped() { return resultCapped; }
    public int deepest() { return resultDeepest; }
    public long finishedAt() { return finishedAt; }
    public int pumpColumn() { return pumpColumn; }
    public byte[] low() { return resultLow; }
    public byte[] high() { return resultHigh; }

    /** The cross-section column of a position (for the intake or the outlet marker). */
    public int columnOf(BlockPos pos) {
        int a = axis == Direction.Axis.X ? pos.getX() : pos.getZ();
        long half = spanA <= COLUMNS ? COLUMNS / (2L * Math.max(1, spanA)) : 0;   // the middle of the block's columns
        return Math.max(0, Math.min(COLUMNS - 1, (int) ((a - originA) * (long) COLUMNS / Math.max(1, spanA) + half)));
    }

    /** One bucket was taken from the pool (between two surveys). */
    public void tookOne() {
        if (hasResult && resultKind == Kind.POOL && !resultInfinite && resultCount > 0) resultCount--;
    }

    /** One cell of the basin was filled. */
    public void filledOne() {
        if (hasResult && resultKind == Kind.BASIN && resultCount > 0) { resultCount--; resultFilled++; }
    }

    /** The next cell of the fill plan for which {@code stillFree} holds (cells that are no longer free are skipped). */
    public BlockPos nextFree(Predicate<BlockPos> stillFree) {
        while (planHead < plan.length) {
            BlockPos p = BlockPos.of(plan[planHead]);
            if (stillFree.test(p)) return p;
            planHead++;
        }
        return null;
    }

    /** The cell returned by {@link #nextFree} was used. */
    public void usedFree() { if (planHead < plan.length) planHead++; }
}
