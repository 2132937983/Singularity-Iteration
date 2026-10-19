// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

/**
 * 0.1.10.7: pacing of the item ejector and pulling upgrades. After a run that moved nothing, the
 * upgrades of that machine wait {@link #idleDelay(int)} ticks (4, 8, 16, then 20); any change of the
 * machine's inventory runs them again at the next tick. While items move they run every tick, as
 * before. The common config can switch the pacing off (then they run every tick, as before 0.1.10.7).
 *
 * <p>0.1.10.8: the fluid ejector and pulling upgrades of machines with the built-in fluid transfer
 * use the same waits; a change of the machine's own tanks or inventory runs them at the next tick.
 */
public final class AutomationPacing {
    private AutomationPacing() { }

    private static volatile boolean enabled = true;

    public static boolean enabled() { return enabled; }

    public static void setEnabled(boolean value) { enabled = value; }

    /** Ticks to wait after {@code idleRuns} runs in a row that moved nothing. */
    public static int idleDelay(int idleRuns) {
        return idleRuns <= 1 ? 4 : idleRuns == 2 ? 8 : idleRuns == 3 ? 16 : 20;
    }
}
