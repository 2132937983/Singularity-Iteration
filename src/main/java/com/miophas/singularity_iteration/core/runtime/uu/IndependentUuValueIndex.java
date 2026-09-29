// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.uu;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Small, deterministic UU value table owned by SCEX.
 *
 * <p>The index is deliberately a value table rather than a recipe solver.  A
 * caller may populate it from a reviewed data source or a server configuration;
 * this class never consults legacy UU graph code.  Entries are component aware
 * and updates are bounded so a malformed or unbounded data pack cannot grow the
 * hot path indefinitely.</p>
 */
public final class IndependentUuValueIndex {
    public static final class Key {
        private final String itemId;
        private final ItemStack item;
        private final int hash;

        /** Explicit legacy keys form a separate namespace and never match item-backed keys. */
        public Key(String itemId, int componentHash) {
            if (itemId == null || itemId.isBlank()) throw new IllegalArgumentException("item id");
            this.itemId = itemId; this.item = null; this.hash = componentHash;
        }
        private Key(ItemStack stack) {
            this.item = stack.copyWithCount(1);
            this.itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            this.hash = ItemStack.hashItemAndComponents(item);
        }
        public String itemId() { return itemId; }
        public int componentHash() { return item == null ? hash : item.getComponents().hashCode(); }
        @Override public int hashCode() { return 31 * itemId.hashCode() + hash; }
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key) || !itemId.equals(key.itemId)) return false;
            if (item == null || key.item == null) return item == null && key.item == null && hash == key.hash;
            return ItemStack.isSameItemSameComponents(item, key.item);
        }
    }

    private final int maxEntries;
    private final LinkedHashMap<Key, Double> values = new LinkedHashMap<>();

    public IndependentUuValueIndex(int maxEntries) {
        if (maxEntries <= 0) throw new IllegalArgumentException("max entries");
        this.maxEntries = maxEntries;
    }

    public synchronized boolean register(Key key, double buckets) {
        Objects.requireNonNull(key, "key");
        if (!UuScanResult.validCosts(buckets, 0)) return false;
        if (!values.containsKey(key) && values.size() >= maxEntries) return false;
        values.put(key, buckets);
        return true;
    }

    public boolean register(ItemStack stack, double buckets) {
        if (stack == null || stack.isEmpty() || stack.getCount() != 1) return false;
        return register(keyOf(stack), buckets);
    }

    public synchronized OptionalDouble lookup(Key key) {
        Double value = values.get(key);
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    public OptionalDouble lookup(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return OptionalDouble.empty();
        return lookup(keyOf(stack));
    }

    public synchronized Map<Key, Double> snapshot() {
        return Map.copyOf(values);
    }

    public synchronized int size() { return values.size(); }

    public static Key keyOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) throw new IllegalArgumentException("item stack");
        return new Key(stack);
    }
}
