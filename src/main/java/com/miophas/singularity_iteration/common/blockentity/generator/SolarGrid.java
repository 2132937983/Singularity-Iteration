package com.miophas.singularity_iteration.common.blockentity.generator;

import com.miophas.singularity_iteration.common.block.generator.SolarFootprint;
import com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage;
import com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 0.1.11.154: solar grid-tie. Panels of one kind side by side on their joining faces ({@link SolarFootprint#gridSides})
 * work as one bank, like a charging matrix:
 * <ul>
 *   <li>the grid's capacity is the sum of its panels'; every panel of a grid may hold all of it (its storage capacity is
 *       the grid's), and the grid stops making energy when the grid is full;</li>
 *   <li>once a tick the grid's energy is moved, exactly (fractions included), to the panels it is drawn from: those that
 *       lost energy since the last tick to cables or charged items, in proportion to what each gave. A panel nothing draws
 *       from passes on what it made. While nothing draws (or for {@link #FOLLOW_TICKS} after the last draw) the energy is
 *       spread over the panels in proportion to their own capacity;</li>
 *   <li>each panel may send as many packets a tick as the whole grid ({@link #packets}), of its own size (its tier).</li>
 * </ul>
 * So one cable on any panel carries the whole grid's output and reaches all it stores. 0.1.11.155: the pooled capacity and
 * the energy following the draw (0.1.11.154 only spread it by capacity, so a cable got one panel's buffer: a basic panel's
 * 2 EU).
 *
 * <p>A grid is found from a panel by a walk over joining neighbours in loaded chunks and kept until a panel joins (placed or
 * loaded next to it), leaves (broken or unloaded) or turns.
 */
public final class SolarGrid {
    /** The walk stops at this many panels. */
    public static final int MAX_PANELS = 1024;
    /** Ticks the energy keeps going to the panels last drawn from when nothing drew this tick. */
    public static final int FOLLOW_TICKS = 40;

    private final List<mio_icif_solar_generator> panels;
    /** The sum of the panels' own capacities. */
    private final long capacity;
    private boolean valid = true;
    private long balancedAt = Long.MIN_VALUE;
    /** The grid's stored EU after the last balance, plus what its panels made since. */
    private double storedNow;
    /** What each panel was last drawn of (EU), and when. */
    private double[] lastDraw;
    private long lastDrawAt = Long.MIN_VALUE;

    private SolarGrid(List<mio_icif_solar_generator> panels) {
        this.panels = panels;
        long sum = 0;
        for (var p : panels) sum = saturatingAdd(sum, p.baseCapacity());
        this.capacity = sum;
    }

    public int size() { return panels.size(); }
    public boolean valid() { return valid; }
    public void invalidate() { valid = false; }
    public List<mio_icif_solar_generator> panels() { return panels; }

    /** The group of {@code start}: the panels reached through joining faces. Each of them is given the new group and its capacity. */
    static SolarGrid build(mio_icif_solar_generator start) {
        Level level = start.getLevel();
        var found = new LinkedHashMap<BlockPos, mio_icif_solar_generator>();
        found.put(start.getBlockPos(), start);
        if (level != null) {
            var queue = new ArrayDeque<mio_icif_solar_generator>();
            queue.add(start);
            walk:
            while (!queue.isEmpty()) {
                var at = queue.poll();
                BlockState state = at.getBlockState();
                for (Direction side : SolarFootprint.gridSides(state)) {
                    BlockPos next = at.getBlockPos().relative(side);
                    if (found.containsKey(next) || !level.isLoaded(next)) continue;
                    if (!(level.getBlockEntity(next) instanceof mio_icif_solar_generator other) || other.isRemoved()) continue;
                    if (!SolarFootprint.joins(state, side, other.getBlockState())) continue;
                    found.put(next, other);
                    queue.add(other);
                    if (found.size() >= MAX_PANELS) break walk;
                }
            }
        }
        var grid = new SolarGrid(List.copyOf(found.values()));
        long each = grid.size() > 1 ? grid.capacity : start.baseCapacity();
        for (var panel : grid.panels) {
            if (panel.grid != null && panel.grid != grid) panel.grid.invalidate();   // the rest of an old group looks again
            panel.grid = grid;
            panel.joinGrid(grid.size() > 1 ? each : panel.baseCapacity());
        }
        grid.storedNow = grid.stored();
        return grid;
    }

    /** Packets a tick a panel of the group may send: its own count for every panel of the group, at most {@code limit}. */
    int packets(int own, int limit) {
        return (int) Math.max(0, Math.min(limit, (long) own * (valid ? panels.size() : 1)));
    }

    /** The group's stored EU (whole). */
    public long stored() {
        long sum = 0;
        for (var p : panels) sum = saturatingAdd(sum, p.gridStorage().getAmount());
        return sum;
    }

    /** The group's capacity in EU: the sum of its panels' own capacities. */
    public long capacity() { return capacity; }

    /** Whether the grid holds its capacity (its panels then make nothing). */
    boolean full() { return storedNow >= capacity; }

    /** A panel made {@code eu} this tick. */
    void produced(double eu) { storedNow += eu; }

    private static long saturatingAdd(long a, long b) {
        long r = a + b;
        return ((a ^ r) & (b ^ r)) < 0 ? Long.MAX_VALUE : r;
    }

    /**
     * Moves the grid's energy to where it is drawn, once a game tick (the first panel to tick does it). Energy is moved,
     * never made or lost: what some panels give, the others take.
     */
    void balance(long gameTime) {
        if (balancedAt == gameTime) return;
        balancedAt = gameTime;
        int n = panels.size();
        if (n < 2) return;
        boolean exact = true;
        for (var p : panels) {
            if (p.isRemoved()) { valid = false; return; }
            exact &= p.gridStorage().scexNetworkControlled();
        }
        // in units of 2^-52 EU when every panel keeps exact balances (the independent network), else whole EU
        int shift = exact ? EnergyAmount.FRACTION_BITS : 0;
        BigInteger[] have = new BigInteger[n], room = new BigInteger[n], target = new BigInteger[n];
        BigInteger total = BigInteger.ZERO, roomTotal = BigInteger.ZERO;
        double[] draw = new double[n];
        boolean drawn = false;
        for (int i = 0; i < n; i++) {
            var panel = panels.get(i);
            CustomEUEnergyStorage s = panel.gridStorage();
            EnergyAmount amount = exact ? s.scexExactAmount() : EnergyAmount.of(s.getAmount());
            have[i] = exact ? amount.units() : BigInteger.valueOf(s.getAmount());
            room[i] = BigInteger.valueOf(s.getCapacity()).shiftLeft(shift);
            total = total.add(have[i]);
            roomTotal = roomTotal.add(room[i]);
            // what left the panel since the last balance: what it held then, plus what it made, less what it holds now
            draw[i] = Math.max(0, panel.gridAfter + panel.gridMade - amount.toDouble());
            if (draw[i] > 1e-6) drawn = true;
            panel.gridMade = 0;
        }
        storedNow = total.doubleValue() / Math.scalb(1.0, shift);
        if (drawn) { lastDraw = draw; lastDrawAt = gameTime; }
        else if (lastDraw != null && gameTime - lastDrawAt <= FOLLOW_TICKS) draw = lastDraw;
        else draw = null;
        if (roomTotal.signum() <= 0 || total.compareTo(roomTotal) > 0) { remember(have, shift); return; }
        // the weights: what each panel gave (to the panels drawn from), else each panel's own capacity
        BigInteger[] weight = new BigInteger[n];
        BigInteger weights = BigInteger.ZERO;
        for (int i = 0; i < n; i++) {
            weight[i] = draw != null ? BigInteger.valueOf(draw[i] > 1e-6 ? Math.max(1, Math.round(draw[i] * 1024)) : 0)
                : BigInteger.valueOf(panels.get(i).baseCapacity());
            weights = weights.add(weight[i]);
        }
        if (weights.signum() <= 0) { remember(have, shift); return; }
        BigInteger given = BigInteger.ZERO;
        for (int i = 0; i < n; i++) {
            target[i] = total.multiply(weight[i]).divide(weights).min(room[i]);
            given = given.add(target[i]);
        }
        // what is left (rounding, or more than a panel holds) goes to panels with room, the weighted ones first
        BigInteger left = total.subtract(given);
        for (int pass = 0; pass < 2 && left.signum() > 0; pass++) {
            for (int i = 0; i < n && left.signum() > 0; i++) {
                if (pass == 0 && weight[i].signum() == 0) continue;
                BigInteger space = room[i].subtract(target[i]);
                if (space.signum() <= 0) continue;
                BigInteger put = space.min(left);
                target[i] = target[i].add(put);
                left = left.subtract(put);
            }
        }
        if (left.signum() != 0) { remember(have, shift); return; }
        // take first, then give: every write stays within the panel's capacity
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < n; i++) {
                int c = target[i].compareTo(have[i]);
                if (pass == 0 ? c >= 0 : c <= 0) continue;
                CustomEUEnergyStorage s = panels.get(i).gridStorage();
                BigInteger diff = target[i].subtract(have[i]).abs();
                if (!exact) s.setEnergy(target[i].longValueExact());
                else if (pass == 0) s.scexConsumeEnergy(EnergyAmount.fromUnits(diff), false);
                else s.scexGenerateEnergy(EnergyAmount.fromUnits(diff), false);
            }
        }
        remember(target, shift);
    }

    /** What each panel holds after the balance, to see next tick what was drawn from it. */
    private void remember(BigInteger[] held, int shift) {
        double scale = Math.scalb(1.0, -shift);
        for (int i = 0; i < panels.size(); i++) panels.get(i).gridAfter = held[i].doubleValue() * scale;
    }
}
