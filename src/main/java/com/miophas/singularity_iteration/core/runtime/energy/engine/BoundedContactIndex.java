// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Insertion-ordered contacts with an independent, bounded maintenance rotation.
 * Owned by one thread. Snapshots contain immutable entries, so foreign callbacks
 * may add/remove contacts without invalidating a caller's current traversal.
 * Cached membership never substitutes for a transaction's live validation.
 */
public final class BoundedContactIndex<K, V> {
    private final int capacity;
    private final LinkedHashMap<K, Map.Entry<K, V>> contacts = new LinkedHashMap<>();
    private final LinkedHashMap<K, Map.Entry<K, V>> maintenance = new LinkedHashMap<>();
    private List<Map.Entry<K, V>> snapshot = List.of();
    private boolean dirty, maintaining;

    public BoundedContactIndex(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Positive contact capacity required");
        this.capacity = capacity;
    }

    public int size() { return contacts.size(); }
    public boolean isEmpty() { return contacts.isEmpty(); }
    public boolean containsKey(K key) { return contacts.containsKey(key); }

    /** A snapshot entry is current only until that exact registration is removed. */
    public boolean isCurrent(Map.Entry<K, V> entry) {
        Objects.requireNonNull(entry);
        return contacts.get(entry.getKey()) == entry;
    }

    /** Existing contacts retain their position; capacity pressure never evicts a live contact. */
    public boolean putIfAbsent(K key, V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        if (contacts.containsKey(key) || contacts.size() == capacity) return false;
        // Every admission has its own identity, even for the same key/value.
        // Map.entry is value-based and does not promise distinct instances.
        Map.Entry<K, V> entry = new AbstractMap.SimpleImmutableEntry<>(key, value);
        contacts.put(key, entry);
        maintenance.put(key, entry);
        dirty = true;
        return true;
    }

    /** Lifecycle predicates are internal and must not reenter this index. */
    public void removeIf(Predicate<? super K> removed) {
        var iterator = contacts.keySet().iterator();
        while (iterator.hasNext()) {
            K key = iterator.next();
            if (removed.test(key)) {
                iterator.remove();
                maintenance.remove(key);
                dirty = true;
            }
        }
    }

    public List<Map.Entry<K, V>> snapshot() {
        if (dirty) {
            snapshot = List.copyOf(contacts.values());
            dirty = false;
        }
        return snapshot;
    }

    /**
     * Check at most limit contacts in rotation, without holding an iterator
     * across callbacks. New/removal callbacks cannot resurrect an obsolete value.
     * Returns the number of checks. A throwing callback stays scheduled.
     */
    public int maintain(int limit, BiPredicate<? super K, ? super V> keep) {
        if (limit < 0) throw new IllegalArgumentException("Nonnegative maintenance limit required");
        Objects.requireNonNull(keep);
        if (maintaining) return 0;
        maintaining = true;
        int checked = 0;
        try {
            int count = Math.min(limit, maintenance.size());
            while (checked < count && !maintenance.isEmpty()) {
                var entry = maintenance.pollFirstEntry().getValue();
                K key = entry.getKey(); V value = entry.getValue();
                boolean retained = true;
                try {
                    checked++;
                    retained = keep.test(key, value);
                } finally {
                    // Entry identity distinguishes removal/re-addition even
                    // when a callback registers the same adapter object again.
                    if (contacts.get(key) == entry) {
                        if (retained) maintenance.putLast(key, entry);
                        else {
                            contacts.remove(key);
                            dirty = true;
                        }
                    }
                }
            }
            return checked;
        } finally {
            maintaining = false;
        }
    }

    public void clear() {
        contacts.clear();
        maintenance.clear();
        snapshot = List.of();
        dirty = false;
    }
}
