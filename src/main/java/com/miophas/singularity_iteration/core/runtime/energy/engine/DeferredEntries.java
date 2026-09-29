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
    public record Metrics(int published, int pending, long advancedFrame, boolean closed) { }
    private record Pending<V>(V value, long observedFrame) { }
    private final long owner = Thread.currentThread().threadId();
    private final int maximumPublished, maximumPending;
    private final Map<K, V> published = new HashMap<>();
    private final Map<K, Pending<V>> pending = new LinkedHashMap<>();
    private long advancedFrame = -1, observedFrame = -1;
    private boolean closed;

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
        if (old == null && pending.size() == maximumPending) throw new IllegalStateException("Pending entry limit reached");
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
        long nextSize = published.size();
        for (var item : pending.entrySet()) {
            if (item.getValue().observedFrame >= frame) continue;
            V before = published.get(item.getKey()), after = item.getValue().value;
            if (before == null) nextSize++;
            if (after == null) nextSize--;
            changes.add(new Change<>(item.getKey(), before, after));
        }
        if (nextSize > maximumPublished) throw new IllegalStateException("Published entry limit reached");
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
    public Metrics metrics() { owner(); return new Metrics(published.size(), pending.size(), advancedFrame, closed); }
    @Override public void close() { owner(); published.clear(); pending.clear(); closed = true; }
}
