// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Independently designed, bounded publication of observed entries after one
 * complete frame. A transient change restored before publication cancels.
 * Callers supply immutable values and frame numbers; no world is accessed.
 */
public final class DeferredEntries<K, V> implements AutoCloseable {
    public record Change<K, V>(K key, V before, V after) { }
    public record Metrics(int published, int pending, long advancedFrame, boolean closed, long dropped) { }
    private record Pending<V>(V value, long observedFrame) { }
    private final long owner = Thread.currentThread().threadId();
    private final int maximumPublished, maximumPending;
    private final Map<K, V> published = new HashMap<>();
    private final Map<K, Pending<V>> pending = new LinkedHashMap<>();
    private long advancedFrame = -1, observedFrame = -1;
    private boolean closed;
    /** 0.1.11.32 (cable audit E-1): new entries left out at a bound (they threw, and the level rebuilt again and again). */
    private long dropped;

    public DeferredEntries(int maximumPublished, int maximumPending) {
        if (maximumPublished <= 0 || maximumPending <= 0) throw new IllegalArgumentException("Positive entry limits required");
        this.maximumPublished = maximumPublished; this.maximumPending = maximumPending;
    }
    private void owner() {
        if (Thread.currentThread().threadId() != owner) throw new IllegalStateException("Entry queue is thread-confined");
    }
    private void active() { owner(); if (closed) throw new IllegalStateException("Entry queue closed"); }

    /** A null value observes removal. Equal observations do not renew the delay. */
    public void observe(long frame, K key, V value) {
        active(); Objects.requireNonNull(key, "key");
        if (frame <= advancedFrame || frame < observedFrame || frame < 0)
            throw new IllegalArgumentException("Observe on the next monotonically increasing frame");
        observedFrame = frame;
        if (Objects.equals(published.get(key), value)) { pending.remove(key); return; }
        var old = pending.get(key);
        if (old != null && Objects.equals(old.value, value)) return;
        // 0.1.11.32: at the pending bound a new entry is left out (counted); a removal of a published entry is always
        // taken, so the published map never keeps a block that is gone
        if (old == null && pending.size() >= maximumPending && (value != null || !published.containsKey(key))) { dropped++; return; }
        pending.put(key, new Pending<>(value, frame));
    }

    /**
     * Publish matured entries atomically after checking the final size. Observing
     * this frame happens before advance, so an add/remove pair can still cancel.
     */
    public List<Change<K, V>> advance(long frame) {
        active();
        if (frame <= advancedFrame || frame < observedFrame || frame < 0)
            throw new IllegalArgumentException("Advance frames once in order");
        var changes = new ArrayList<Change<K, V>>();
        var additions = new ArrayList<Change<K, V>>();
        long nextSize = published.size();
        for (var item : pending.entrySet()) {
            if (item.getValue().observedFrame >= frame) continue;
            V before = published.get(item.getKey()), after = item.getValue().value;
            if (before == null && after != null) { additions.add(new Change<>(item.getKey(), null, after)); continue; }
            if (before != null && after == null) nextSize--;
            changes.add(new Change<>(item.getKey(), before, after));
        }
        // 0.1.11.32 (cable audit E-1): removals and updates first, then the new entries that fit; the rest are left out
        // (counted) instead of failing the whole frame (the level then rebuilt and hit the bound again)
        for (var addition : additions) {
            if (nextSize < maximumPublished) { changes.add(addition); nextSize++; }
            else { pending.remove(addition.key()); dropped++; }
        }
        for (var change : changes) {
            if (change.after == null) published.remove(change.key); else published.put(change.key, change.after);
            pending.remove(change.key);
        }
        advancedFrame = frame;
        return List.copyOf(changes);
    }

    /** Immediate lifecycle revocation also cancels unpublished entries. */
    public List<Change<K, V>> forgetIf(Predicate<K> predicate) {
        active(); Objects.requireNonNull(predicate, "predicate");
        var changes = new ArrayList<Change<K, V>>();
        var iterator = published.entrySet().iterator();
        while (iterator.hasNext()) {
            var item = iterator.next();
            if (predicate.test(item.getKey())) { changes.add(new Change<>(item.getKey(), item.getValue(), null)); iterator.remove(); }
        }
        pending.keySet().removeIf(predicate);
        return List.copyOf(changes);
    }
    public Metrics metrics() { owner(); return new Metrics(published.size(), pending.size(), advancedFrame, closed, dropped); }
    @Override public void close() { owner(); published.clear(); pending.clear(); closed = true; }
}
