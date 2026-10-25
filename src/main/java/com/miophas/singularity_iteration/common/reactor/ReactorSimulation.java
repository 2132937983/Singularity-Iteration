// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.reactor;

import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Part;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorCycle.Profile;

/**
 * <p><b>EN:</b> Reactor design simulator (0.1.8.0, guide page "Reactor simulator"). It runs the same model
 * as the reactor block ({@link ReactorCycle}), from a cold reactor with new components and full
 * fuel. No Minecraft dependency. One cycle is 20 ticks (1 second). The run stops at
 * {@link #CYCLES} cycles or at a meltdown (hull heat at or above the maximum hull heat).
 * <p><b>ZH:</b> 反应堆设计模拟器（0.1.8.0，手册页“反应堆模拟器”）。使用与反应堆方块相同的模型（{@link ReactorCycle}），
 * 从冷态、全新组件、满燃料开始。不依赖 Minecraft。一个周期为 20 tick（1 秒）。达到 {@link #CYCLES} 个周期或熔毁（外壳热量达到最大值）时停止。
 * <p><b>JA:</b> 原子炉設計シミュレーター（0.1.8.0、ガイドページ「原子炉シミュレーター」）。原子炉ブロックと同じモデル（{@link ReactorCycle}）を、
 * 冷えた炉・新品の部品・満タンの燃料から動かす。Minecraft に依存しない。1 サイクルは 20 tick（1 秒）。{@link #CYCLES} サイクル、
 * または溶融（外殻熱が最大値以上）で停止する。
 */
public final class ReactorSimulation {
    private ReactorSimulation() {}

    /** Simulated time: 100 cycles = 100 seconds. */
    public static final int CYCLES = 100;
    /** Remaining cycles of a new fuel rod in the simulation (longer than the run). */
    static final int FUEL = 20_000;
    /** 0.1.10.19: cycles of the full run ({@link #fullRun}): the life of a uranium rod (5 h 33 min). */
    public static final int FULL_CYCLES = FUEL;

    /**
     * Result of one run.
     *
     * @param euPerTick      mean output over the run, EU/t
     * @param heatPerCycle   heat that the fuel makes per cycle (mean of the last 10 cycles)
     * @param ventPerCycle   heat that the vents remove per cycle (mean of the last 10 cycles)
     * @param hullTrend      mean hull heat change per cycle over the last 10 cycles (0 or less = stable)
     * @param hullHeat       hull heat at the end of the run
     * @param maxHull        maximum hull heat (10 000 + plating)
     * @param peakHull       highest hull heat in the run
     * @param melted         components that melted (overheated and were destroyed)
     * @param meltdownCycle  cycle of the meltdown, or -1
     * @param cycles         cycles run
     * @param firstMeltCycle cycle in which the first component melted, or -1 (0.1.10.19)
     * @param steadyCycle    first cycle after which no heat value changes any more (steady state), or -1 (0.1.10.19)
     */
    public record Report(double euPerTick, long heatPerCycle, long ventPerCycle, double hullTrend, long hullHeat,
                         long maxHull, long peakHull, int melted, int meltdownCycle, int cycles, int firstMeltCycle,
                         int steadyCycle) {
        /** No meltdown, no melted component, and the hull does not heat up. */
        public boolean stable() { return meltdownCycle < 0 && melted == 0 && hullTrend <= 0.0; }

        /** 1 - peak hull heat / maximum hull heat (0..1). */
        public double margin() { return maxHull <= 0 ? 0 : Math.max(0, 1.0 - (double) peakHull / maxHull); }

        /** Seconds until the hull reaches its maximum at the current trend, or -1 when it does not rise. */
        public int secondsToMeltdown() {
            if (meltdownCycle >= 0) return meltdownCycle;
            if (hullTrend <= 0) return -1;
            return (int) Math.min(Integer.MAX_VALUE, cycles + Math.ceil((maxHull - hullHeat) / hullTrend));
        }
    }

    /**
     * Runs a design.
     *
     * @param profiles 54 slots (row-major, 9 per row; slot = row * 9 + column), null = empty
     * @param columns  reactor columns: 3 + chambers (3..9)
     */
    public static Report run(Profile[] profiles, int columns) {
        return simulate(profiles, columns, CYCLES, false);
    }

    /**
     * 0.1.10.19: runs a design for the life of a uranium rod ({@link #FULL_CYCLES}), and stops early at the
     * steady state (no component heat and no hull heat changes from one cycle to the next; the model has no
     * randomness, so the state then stays until the fuel ends). The 100 cycles of {@link #run} can call a design
     * stable while its vents still heat up; this run sees them melt. Used by the JEI reactor layouts.
     */
    public static Report fullRun(Profile[] profiles, int columns) {
        return simulate(profiles, columns, FULL_CYCLES, true);
    }

    private static Report simulate(Profile[] profiles, int columns, int maxCycles, boolean stopWhenSteady) {
        if (profiles.length != 54 || columns < 3 || columns > 9) throw new IllegalArgumentException("Invalid design");
        Part[] parts = new Part[54];
        for (int i = 0; i < 54; i++) {
            Profile p = profiles[i];
            if (p == null || i % 9 >= columns) continue;
            parts[i] = new Part(p, 0, p.kind() == ReactorCycle.Kind.FUEL ? FUEL : 0);
        }
        long heat = 0, peak = 0, maxHull = 10_000;
        double eu = 0;
        int melted = 0, meltdown = -1, cycles = 0, firstMelt = -1, steady = -1;
        long[] history = new long[maxCycles + 1], made = new long[maxCycles + 1], vented = new long[maxCycles + 1];
        for (int c = 0; c < maxCycles; c++) {
            ReactorCycle.Result r = ReactorCycle.step(parts, columns, heat, true);
            Part[] next = r.parts();
            boolean same = r.hullHeat() == heat;
            for (int i = 0; i < 54; i++) {
                if (parts[i] != null && next[i] == null && parts[i].profile().storesHeat()) {
                    melted++;
                    if (firstMelt < 0) firstMelt = c + 1;
                }
                if (same && !sameHeat(parts[i], next[i])) same = false;
            }
            parts = next;
            heat = r.hullHeat();
            maxHull = r.maxHullHeat();
            peak = Math.max(peak, heat);
            eu += r.euPerTick();
            cycles = c + 1;
            history[cycles] = heat;
            made[cycles] = r.generatedHeat();
            vented[cycles] = r.emittedHeat();
            if (heat >= maxHull) { meltdown = cycles; break; }
            if (same) {
                if (steady < 0) steady = cycles;
            } else {
                steady = -1;
            }
            if (stopWhenSteady && steady >= 0) break;
        }
        // steady state: the components start cold, so the first cycles vent less than later ones
        int window = Math.min(10, cycles);
        double trend = 0, heatMean = 0, ventMean = 0;
        if (window > 0) {
            trend = (history[cycles] - history[cycles - window]) / (double) window;
            for (int c = cycles - window + 1; c <= cycles; c++) { heatMean += made[c]; ventMean += vented[c]; }
            heatMean /= window;
            ventMean /= window;
        }
        if (stopWhenSteady && steady >= 0) {
            // at the steady state the hull does not change and the vents remove what the fuel makes
            trend = 0;
            heatMean = made[cycles];
            ventMean = vented[cycles];
        }
        return new Report(cycles == 0 ? 0 : eu / cycles, Math.round(heatMean), Math.round(ventMean), trend, heat, maxHull, peak, melted,
            meltdown, cycles, firstMelt, steady);
    }

    /** The same component with the same stored value (heat, reflector use, breeding progress), or both slots empty; fuel time is not compared. */
    private static boolean sameHeat(Part before, Part after) {
        if (before == null || after == null) return before == after;
        return before.profile() == after.profile() && before.stored() == after.stored();
    }

    /**
     * Parses a preset: rows separated by "/", cells by ",", each cell a component key of
     * {@link ReactorInventory} or "_" for empty. The number of cells of the first row sets the
     * columns. Unknown keys count as empty.
     *
     * @return the 54 profiles; {@code columnsOut[0]} receives the columns
     */
    public static Profile[] parse(String preset, int[] columnsOut) {
        Profile[] out = new Profile[54];
        String[] rows = preset == null || preset.isBlank() ? new String[0] : preset.trim().split("/");
        int columns = 3;
        for (int r = 0; r < rows.length && r < 6; r++) {
            String[] cells = rows[r].split(",");
            if (r == 0) columns = Math.max(3, Math.min(9, cells.length));
            for (int c = 0; c < cells.length && c < columns; c++) {
                String key = cells[c].trim();
                if (!key.isEmpty() && !key.equals("_")) out[r * 9 + c] = ReactorInventory.profile(key);
            }
        }
        if (columnsOut != null && columnsOut.length > 0) columnsOut[0] = columns;
        return out;
    }
}
