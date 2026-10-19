// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * 0.1.10.12: the last receiver explosions caused by an overvoltage in the energy network (dimension,
 * position, game time). The reactor accident audit reads it to tell whether a machine next to a
 * reactor blew up just before the reactor failed. Server thread only; at most {@value #LIMIT} entries.
 */
public final class OverloadBlastLog {
    private OverloadBlastLog() {}

    public static final int LIMIT = 64;

    /** One overvoltage explosion. */
    public record Blast(String dimension, int x, int y, int z, long gameTime) { }

    private static final ArrayDeque<Blast> BLASTS = new ArrayDeque<>();

    public static synchronized void record(String dimension, int x, int y, int z, long gameTime) {
        if (BLASTS.size() == LIMIT) BLASTS.removeFirst();
        BLASTS.addLast(new Blast(dimension, x, y, z, gameTime));
    }

    /** The explosions in {@code dimension} within {@code radius} blocks of the point since {@code since}. */
    public static synchronized List<Blast> near(String dimension, int x, int y, int z, int radius, long since) {
        var out = new ArrayList<Blast>();
        long r2 = (long) radius * radius;
        for (var blast : BLASTS) {
            if (!blast.dimension().equals(dimension) || blast.gameTime() < since) continue;
            long dx = blast.x() - x, dy = blast.y() - y, dz = blast.z() - z;
            if (dx * dx + dy * dy + dz * dz <= r2) out.add(blast);
        }
        return out;
    }

    public static synchronized void clear() { BLASTS.clear(); }
}
