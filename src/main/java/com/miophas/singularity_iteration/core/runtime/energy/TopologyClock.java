package com.miophas.singularity_iteration.core.runtime.energy;

import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Per-level counter of EU-topology edits: a block-entity block placed, removed or replaced, a
 * cable's ports or colour changed. Network scans (energy terminal, voltage detector, HUD) cache
 * their result against this stamp instead of re-walking thousands of cables every few ticks.
 * Weak keys: an unloaded level is dropped with no cleanup hook.
 */
public final class TopologyClock {
    private TopologyClock() {}

    private static final Map<Level, long[]> CLOCKS = new WeakHashMap<>();

    public static synchronized void bump(Level level) {
        if (level == null || level.isClientSide()) return;
        CLOCKS.computeIfAbsent(level, l -> new long[1])[0]++;
    }

    public static synchronized long stamp(Level level) {
        long[] c = CLOCKS.get(level);
        return c == null ? 0 : c[0];
    }
}
