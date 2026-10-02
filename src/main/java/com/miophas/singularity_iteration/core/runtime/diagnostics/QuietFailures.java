package com.miophas.singularity_iteration.core.runtime.diagnostics;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate-limited reporting for exceptions that tick code deliberately survives (a neighbour's
 * capability throwing, a reflective compat call failing). The machine keeps running, but the
 * failure is no longer silent: the first occurrence per key is logged with its stack trace, and
 * repeats at most once a minute with a count.
 */
public final class QuietFailures {
    private QuietFailures() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long INTERVAL_MS = 60_000;
    private static final ConcurrentHashMap<String, long[]> SEEN = new ConcurrentHashMap<>();

    public static void report(String key, String what, Throwable failure) {
        long now = System.currentTimeMillis();
        long[] state = SEEN.computeIfAbsent(key, k -> new long[]{0, 0});
        synchronized (state) {
            state[1]++;
            if (state[0] != 0 && now - state[0] < INTERVAL_MS) return;
            boolean first = state[0] == 0;
            state[0] = now;
            if (first) LOGGER.warn("[SI] {} failed (continuing): {}", what, failure.toString(), failure);
            else LOGGER.warn("[SI] {} still failing ({} times so far): {}", what, state[1], failure.toString());
        }
    }

    /** Test hook: how often a key was reported. */
    public static long count(String key) {
        long[] state = SEEN.get(key);
        return state == null ? 0 : state[1];
    }
}
