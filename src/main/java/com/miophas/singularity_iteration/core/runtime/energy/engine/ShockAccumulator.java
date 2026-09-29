package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.ObjIntConsumer;

/** Per settlement: maximum exposure along one source/receiver path, sum across distinct paths. */
public final class ShockAccumulator<P, E> {
    private final Map<P, IdentityHashMap<E, Integer>> paths = new HashMap<>();
    public void add(P path, E entity, double exposure) {
        int value = (int) Math.max(0, exposure); // IC2 truncates before accumulating and rounding damage.
        if (value > 0) paths.computeIfAbsent(path, ignored -> new IdentityHashMap<>()).merge(entity, value, Math::max);
    }
    public void apply(ObjIntConsumer<E> consumer) {
        var totals = new IdentityHashMap<E, Long>();
        for (var entities : paths.values()) entities.forEach((entity, energy) -> totals.merge(entity, (long) energy, Long::sum));
        paths.clear();
        totals.forEach((entity, energy) -> consumer.accept(entity, (int) Math.min(Integer.MAX_VALUE, (energy + 63) / 64)));
    }
    public void clear() { paths.clear(); }
}
