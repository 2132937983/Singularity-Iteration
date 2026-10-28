// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

/**
 * 0.1.10.35 (item 3): commissions and resting orders last hours of the server running (pure; the panel and the offline
 * harness use it too).
 *
 * <p>Before, a commission lasted 1 to 7 market days and an order 7: a market day is 20 minutes of the server running, so the
 * longest commission ended after 2 hours 20 minutes. Now the clock is the overworld's game time (ticks the server ran): a
 * {@code /time set} does not move it, and it stands still while the server is stopped or a single player game is paused, so
 * a server that is stopped at night does not lose its commissions. One hour is 72,000 ticks.
 */
public final class CommissionHours {
    private CommissionHours() {}

    public static final long TICKS_PER_HOUR = 72_000L, TICKS_PER_SECOND = 20L;
    /** The choices of the commission form (any hour up to the maximum is accepted). */
    public static final int[] OPTIONS = {1, 3, 6, 12, 24, 48, 72, 168};
    /** The longest commission and order. */
    public static final int MAX_HOURS = 168;
    /** Under this many hours left a commission is "ending soon". */
    public static final int SOON_HOURS = 3;

    /** Hours within 1 and {@code max} (and {@link #MAX_HOURS}). */
    public static int clamp(long hours, int max) {
        int m = Math.max(1, Math.min(MAX_HOURS, max));
        return (int) Math.max(1, Math.min(m, hours));
    }

    /** The game time at which something placed at {@code now} for {@code hours} ends. */
    public static long expiresAt(long now, int hours) {
        return now + Math.max(1, hours) * TICKS_PER_HOUR;
    }

    /** Whole seconds left at {@code now} (0 when over). */
    public static long secondsLeft(long now, long expiresAt) {
        return expiresAt <= now ? 0 : (expiresAt - now + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
    }

    public static boolean expired(long now, long expiresAt) { return expiresAt > 0 && now >= expiresAt; }

    /** Fewer than {@link #SOON_HOURS} left (and not over). */
    public static boolean soon(long secondsLeft) { return secondsLeft > 0 && secondsLeft < SOON_HOURS * 3600L; }

    /** {hours, minutes} of a number of seconds (minutes rounded up, so "0h 1m" until the end). */
    public static long[] hoursMinutes(long seconds) {
        long minutes = (Math.max(0, seconds) + 59) / 60;
        return new long[] {minutes / 60, minutes % 60};
    }

    /** The next choice above ({@code dir} 1) or below ({@code dir} -1) {@code hours}, within 1 and {@code max}. */
    public static int step(int hours, int dir, int max) {
        int m = clamp(max, MAX_HOURS);
        if (dir > 0) {
            for (int o : OPTIONS) if (o > hours && o <= m) return o;
            return Math.max(1, Math.min(m, Math.max(hours, 1)));
        }
        for (int i = OPTIONS.length - 1; i >= 0; i--) if (OPTIONS[i] < hours && OPTIONS[i] <= m) return OPTIONS[i];
        return 1;
    }
}
