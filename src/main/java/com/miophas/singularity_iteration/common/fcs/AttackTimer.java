// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.fcs;

/**
 * 0.1.10.23 (FCS P7): countdowns to the next attack of a creature for the behaviour predictor ("AIM 0.6s"), from what
 * every client sees: the item use time, the synced animation flags and the arm swings. Pure arithmetic in ticks.
 *
 * <ul>
 *   <li>Bow: vanilla skeletons release at {@link #BOW_DRAW} ticks of drawing ({@code RangedBowAttackGoal}).</li>
 *   <li>Crossbow: loading takes the charge time of the crossbow (25 ticks without Quick Charge); after it the pillager
 *       waits 20 to 39 ticks, so only the loading is counted down.</li>
 *   <li>Ghast: the charge flag rises at 10 ticks of charging and the fireball leaves at 20: {@link #GHAST_AFTER_FLAG}
 *       ticks after the flag.</li>
 *   <li>Melee: {@link Swings} learns the time between swings of a creature (vanilla melee mobs swing every 20 ticks
 *       while in reach; creatures of other mods differ) and counts down from the last swing.</li>
 * </ul>
 */
public final class AttackTimer {
    private AttackTimer() {}

    public static final int BOW_DRAW = 20;
    public static final int GHAST_AFTER_FLAG = 10;

    /** Ticks to the release of a bow drawn for {@code ticksUsing} ticks. */
    public static int bowLeft(int ticksUsing) { return Math.max(0, BOW_DRAW - ticksUsing); }

    /** Ticks to a loaded crossbow ({@code chargeTicks} = the charge time of the crossbow). */
    public static int crossbowLeft(int ticksUsing, int chargeTicks) { return Math.max(0, chargeTicks - ticksUsing); }

    /** The rising edge of a flag (the ghast charge): ticks since it rose. */
    public static final class Edge {
        private boolean was;
        private long rose = Long.MIN_VALUE;

        public void observe(long tick, boolean flag) {
            if (flag && !was) rose = tick;
            was = flag;
        }

        /** Ticks left of {@code delay} after the rise, or -1 when the flag is down. */
        public int left(long tick, int delay) {
            if (!was || rose == Long.MIN_VALUE) return -1;
            return (int) Math.max(0, delay - (tick - rose));
        }
    }

    /**
     * The learned swing interval of one melee creature: the shortest of its last {@link #KEPT} gaps between swings. A
     * melee creature swings when its cooldown is over and its target is in reach, so a gap is the cooldown plus a delay
     * that is never negative; the shortest gap is the cooldown (an average of the gaps came out 1 tick worse than a fixed
     * 20-tick guess for vanilla mobs in the offline test).
     */
    public static final class Swings {
        /** Shortest and longest gap that counts as one attack interval (ticks). */
        public static final int MIN_GAP = 5, MAX_GAP = 100;
        /** Gaps kept. */
        public static final int KEPT = 5;
        private boolean was;
        private long last = Long.MIN_VALUE;
        private final int[] gaps = new int[KEPT];
        private int samples;

        /** {@code swinging} = the creature's arm swing flag this tick. */
        public void observe(long tick, boolean swinging) {
            if (swinging && !was) {
                if (last != Long.MIN_VALUE) {
                    long gap = tick - last;
                    if (gap >= MIN_GAP && gap <= MAX_GAP) gaps[samples++ % KEPT] = (int) gap;
                }
                last = tick;
            }
            was = swinging;
        }

        /** The learned interval in ticks, NaN before two swings. */
        public double interval() {
            if (samples == 0) return Double.NaN;
            int min = Integer.MAX_VALUE;
            for (int i = 0; i < Math.min(samples, KEPT); i++) min = Math.min(min, gaps[i]);
            return min;
        }

        /** Ticks to the next swing, or -1 when unknown (no interval yet, or no swing for two intervals). */
        public int left(long tick) {
            double interval = interval();
            if (Double.isNaN(interval) || last == Long.MIN_VALUE) return -1;
            long since = tick - last;
            if (since > 2 * interval) return -1;
            return (int) Math.max(0, Math.round(interval - since));
        }
    }
}
