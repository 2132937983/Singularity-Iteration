// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory.blade;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 0.1.11.235 (SlashBlade collab B3): the targets a quantum blade has entangled (its special effect): each hit entangles
 * the target for {@link #TICKS} ticks, at most {@link #MAX} at a time (a new one pushes out the oldest); the collapse cut
 * strikes them with the suit's locks. Server side, by the wielder's UUID, not saved. No SlashBlade class here.
 */
public final class Entanglement {
    public static final int MAX = 6, TICKS = 200;

    private static final Map<UUID, Entanglement> BY_WIELDER = new ConcurrentHashMap<>();

    /** Entity id → game time the entanglement ends, oldest first. */
    private final LinkedHashMap<Integer, Long> until = new LinkedHashMap<>();

    public static Entanglement of(UUID wielder) { return BY_WIELDER.computeIfAbsent(wielder, k -> new Entanglement()); }

    public static void forget(UUID wielder) { BY_WIELDER.remove(wielder); }

    /** Entangles {@code entityId} at game time {@code now} (again: renewed and moved to the newest). */
    public synchronized void add(int entityId, long now) {
        until.remove(entityId);
        until.put(entityId, now + TICKS);
        while (until.size() > MAX) until.remove(until.keySet().iterator().next());
    }

    /** The entangled ids at {@code now}, oldest first (ended ones are dropped). */
    public synchronized List<Integer> live(long now) {
        for (Iterator<Map.Entry<Integer, Long>> it = until.entrySet().iterator(); it.hasNext(); )
            if (it.next().getValue() <= now) it.remove();
        return new ArrayList<>(until.keySet());
    }

    /** The collapse cut's targets: the locks first, then the entangled ones, without repeats, at most {@code max}. */
    public static List<Integer> targets(int[] locks, List<Integer> entangled, int max) {
        List<Integer> out = new ArrayList<>();
        for (int id : locks) if (!out.contains(id) && out.size() < max) out.add(id);
        for (int id : entangled) if (!out.contains(id) && out.size() < max) out.add(id);
        return out;
    }
}
