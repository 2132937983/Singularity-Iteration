// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

/**
 * 0.1.10.28 (X4): the automatic stabiliser of the exchange (proposal D4), a small central bank. Pure arithmetic, no game
 * types (the offline harness runs it).
 *
 * <p>The ledger records the coins the market creates (sales, bounties, futures) and destroys (purchases, fees, futures) per
 * day. At each change of the market day the stabiliser compares the net issue of the last 7 days with a target: a weekly
 * amount per player who sold to the market in those days, or a share of the money supply (the net issue since the start of
 * the record) when that is more. Above the target the factor falls by one step; below {@link #RECOVER} of the target it rises
 * by one step up to 1; in between it holds, so the net issue settles just under the target. The factor scales the sell depths of every commodity and the daily coin allowance: large
 * sales and automated selling create fewer coins, while a small sale keeps nearly its price.
 *
 * <p>The target per player keeps a young server free: a supply estimate that starts at zero would count all early coins as
 * inflation (Harness1128: a share of the supply alone held the factor at its lowest for most of the first year).
 */
public final class Stabilizer {
    private Stabilizer() {}

    /** The settings; {@code enabled} false keeps the factor at 1. */
    public record Rules(boolean enabled, double targetPercentPerWeek, long targetPerPlayerWeek, double step, double minFactor) {
        public Rules {
            if (!(step > 0 && step < 1) || !(minFactor > 0 && minFactor <= 1) || targetPercentPerWeek < 0 || targetPerPlayerWeek < 0)
                throw new IllegalArgumentException("stabilizer rules");
        }
    }

    /** Days of the net issue that the stabiliser compares with the target. */
    public static final int WINDOW_DAYS = 7;
    /**
     * Below this share of the target the factor recovers. A wide band (half the target) only ever lowered the factor of a
     * server whose issue sits near the target: each week above it took a step, none below it gave one back (Harness1128).
     */
    public static final double RECOVER = 0.8;

    /** The weekly target: per active seller (at least one), or the share of the supply when that is more. */
    public static double target(Rules r, long supply, int activeSellers) {
        return Math.max((double) r.targetPerPlayerWeek() * Math.max(1, activeSellers), Math.max(0, supply) * r.targetPercentPerWeek() / 100.0);
    }

    /** The factor after a day with the net issue {@code net7} of the last 7 days. */
    public static double next(Rules r, double factor, long net7, long supply, int activeSellers) {
        if (!r.enabled()) return 1.0;
        double f = Double.isFinite(factor) ? Math.max(r.minFactor(), Math.min(1.0, factor)) : 1.0;
        double t = target(r, supply, activeSellers);
        if (net7 > t) return Math.max(r.minFactor(), f * (1 - r.step()));
        if (net7 < t * RECOVER) return Math.min(1.0, f * (1 + r.step()));
        return f;
    }

    /** The bid curve with the depths scaled by the factor (the unit price is unchanged). */
    public static SellDepth.Curve scale(SellDepth.Curve c, double factor) {
        if (!(factor > 0) || factor >= 1) return c;
        return new SellDepth.Curve(c.unit(), Math.max(1e-9, c.lp() * factor), Math.max(1e-9, c.lg() * factor));
    }

    /** The daily allowance scaled by the factor (0 = no allowance stays 0). */
    public static long allowance(long perDay, double factor) {
        if (perDay <= 0 || !(factor > 0) || factor >= 1) return perDay;
        return Math.max(1, (long) Math.floor(perDay * factor));
    }

    /** The money supply estimate after a closed day: the net issue added, never below zero. */
    public static long supply(long supply, long minted, long burned) {
        return Math.max(0, supply + minted - burned);
    }
}
