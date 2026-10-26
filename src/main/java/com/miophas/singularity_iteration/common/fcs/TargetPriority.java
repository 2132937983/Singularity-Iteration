// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.fcs;

/**
 * 0.1.10.23 (FCS P6): priority of a creature for the wearer (the "#1" to "#3" ranks of the behaviour predictor tags)
 * and the choice of the locked target for one more guided shot. Pure arithmetic; units are blocks, ticks and health
 * points.
 *
 * <h2>Score</h2>
 * {@code score = hostility weight x danger x urgency}:
 * <ul>
 *   <li>hostility weight by {@link Rating#hostility}: 0 passive 0.02, 1 neutral 0.1, 2 hostile 0.35, 3 watches the
 *       wearer 0.6, 4 targets the wearer 1.0;</li>
 *   <li>danger = the damage of one hit + 1.5 x the explosion radius (at least 1);</li>
 *   <li>urgency = {@code 40 / (40 + eta)}, eta = the ticks to its attack: the known countdown (a drawn bow, a swelling
 *       creeper), else the time to close to its reach at its closing speed (at most 400).</li>
 * </ul>
 *
 * <h2>Assignment</h2>
 * {@link #assign}: a greedy shot-to-target assignment. One more shot goes to the target with the largest
 * {@code weight x pHit x min(1, damage / remaining)}, where remaining = health - the expected damage of the shots
 * already flying at it; a target that the shots in flight already cover keeps a small value ({@link #COVERED}), so
 * the extra shots of a volley go to the next target first and to the top target when all are covered. Before
 * 0.1.10.23 the shots took the locks in turn.
 */
public final class TargetPriority {
    private TargetPriority() {}

    /** Value factor of a target that the shots in flight already cover. */
    public static final double COVERED = 0.05;
    private static final double[] HOSTILITY = {0.02, 0.1, 0.35, 0.6, 1.0};

    /**
     * What the score needs to know about one creature.
     *
     * @param distance    blocks to the wearer
     * @param closing     closing speed toward the wearer (blocks per tick; negative when it moves away)
     * @param damage      damage of one hit (health points)
     * @param blastRadius explosion radius (0 without)
     * @param hostility   0 passive, 1 neutral, 2 hostile, 3 watches the wearer, 4 targets the wearer
     * @param reach       blocks from which it can attack (melee about 2, bows about 15)
     * @param attackTicks ticks to its attack when known (a countdown), else -1
     */
    public record Rating(double distance, double closing, double damage, double blastRadius, int hostility, double reach, double attackTicks) { }

    /** Wind-up of a ranged attack without a known countdown (a bow draw is 20 ticks). */
    public static final double RANGED_WINDUP = 20;

    /**
     * Ticks until the creature of {@code r} can attack (at most 400): the known countdown, else the time to close to its
     * reach, plus {@link #RANGED_WINDUP} for a ranged attacker (reach above 4 blocks) that has not started its attack.
     */
    public static double eta(Rating r) {
        if (r.attackTicks() >= 0) return Math.min(400, r.attackTicks());
        double gap = Math.max(0, r.distance() - r.reach());
        double windup = r.reach() > 4 ? RANGED_WINDUP : 0;
        if (gap == 0) return windup;
        return Math.min(400, windup + gap / Math.max(r.closing(), 0.02));
    }

    /** The priority score (larger first). */
    public static double score(Rating r) {
        double weight = HOSTILITY[Math.max(0, Math.min(HOSTILITY.length - 1, r.hostility()))];
        double danger = Math.max(1, r.damage() + 1.5 * Math.max(0, r.blastRadius()));
        return weight * danger * 40 / (40 + eta(r));
    }

    /**
     * Chance that a guided shot hits a target at {@code distance} that moves at {@code speed} blocks per tick: 0.98 at
     * close range, less with range and fast motion (the misses of the offline guidance comparison were zigzagging and
     * fast targets), between 0.3 and 0.98.
     */
    public static double hitChance(double distance, double speed) {
        double p = 0.98 - Math.max(0, distance) / 400 - 0.8 * Math.max(0, speed - 0.15);
        return Math.max(0.3, Math.min(0.98, p));
    }

    /**
     * The target for one more shot of {@code damage}, or -1 when there is none (all arrays have the same length).
     *
     * @param weight   the priority score of each target
     * @param pHit     the hit chance of the shot at each target
     * @param health   the health of each target (with absorption)
     * @param inFlight the expected damage already flying at each target
     */
    public static int assign(double[] weight, double[] pHit, double[] health, double[] inFlight, double damage) {
        int best = -1;
        double bestValue = -1;
        for (int i = 0; i < weight.length; i++) {
            double remaining = health[i] - inFlight[i];
            double value = Math.max(1.0E-6, weight[i]) * pHit[i] * (remaining > 0 ? Math.min(1, damage / remaining) : COVERED);
            if (value > bestValue) { bestValue = value; best = i; }
        }
        return best;
    }
}
