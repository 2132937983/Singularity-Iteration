// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import com.miophas.singularity_iteration.core.runtime.uu.IndependentUuValueIndex;

import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;

/** Ordinary saved-item observations mapped by Minecraft's own versioned data fixer. */
public final class UuReferenceCatalog {
    public record Catalog(Map<IndependentUuValueIndex.Key, Double> prices,
            Set<IndependentUuValueIndex.Key> denied, int unmapped, int entries) { }
    private UuReferenceCatalog() { }

    /** Server-owned vanilla identity migration cache. Prices are never cached here. */
    public static final class MigrationCache {
        private final HolderLookup.Provider registries;
        private final LinkedHashMap<String, ItemStack> items = new LinkedHashMap<>(128, .75f, true);
        private long migrations;
        public MigrationCache(HolderLookup.Provider registries) { this.registries = registries; }
        boolean matches(HolderLookup.Provider provider) { return registries == provider; }
        public long migrations() { return migrations; }
        private ItemStack map(String nbt) {
            var known = items.get(nbt);
            if (known != null) return known.copy();
            var migrated = migrateVanilla(nbt, registries); migrations++;
            items.put(nbt, migrated.copy());
            if (items.size() > 4096) items.remove(items.keySet().iterator().next());
            return migrated;
        }
    }

    public static ItemStack migrateVanilla(String savedItem, HolderLookup.Provider registries) {
        try {
            CompoundTag legacy = TagParser.parseTag(savedItem);
            if (!legacy.getString("id").startsWith("minecraft:") || legacy.getByte("Count") != 1)
                throw new IllegalArgumentException("Expected one vanilla reference item");
            var updated = DataFixers.getDataFixer().update(References.ITEM_STACK,
                    new Dynamic<>(NbtOps.INSTANCE, legacy), 1343,
                    SharedConstants.getCurrentVersion().getDataVersion().getVersion());
            var item = ItemStack.parse(registries, updated.getValue()).orElse(ItemStack.EMPTY);
            if (item.isEmpty() || item.getCount() != 1) throw new IllegalArgumentException("Unmapped reference item");
            return item;
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) {
            throw new IllegalArgumentException("Invalid reference item NBT", error);
        }
    }

    public static Catalog read(Reader reader, HolderLookup.Provider registries) throws IOException {
        return read(reader, new MigrationCache(registries));
    }
    public static Catalog read(Reader reader, MigrationCache mappings) throws IOException {
        StringBuilder data = new StringBuilder(); char[] buffer = new char[4096]; int count;
        while ((count = reader.read(buffer)) != -1) {
            if (data.length() + count > 4 * 1024 * 1024) throw new IOException("UU catalog exceeds 4 MiB");
            data.append(buffer, 0, count);
        }
        var root = JsonParser.parseString(data.toString()).getAsJsonObject();
        if (root.get("schema").getAsInt() != 1 || root.get("data_version").getAsInt() != 1343)
            throw new IllegalArgumentException("Unknown reference catalog version");
        var rows = root.getAsJsonArray("entries");
        if (rows.size() > 16384) throw new IllegalArgumentException("Reference catalog too large");
        Map<IndependentUuValueIndex.Key, Double> prices = new HashMap<>();
        Set<IndependentUuValueIndex.Key> denied = new HashSet<>();
        int unmapped = 0;
        for (var entry : rows) {
            var row = entry.getAsJsonObject();
            String nbt = row.get("legacy_stack").getAsString();
            if (nbt.length() > 8192) throw new IllegalArgumentException("Reference item NBT too large");
            ItemStack item;
            try { item = mappings.map(nbt); }
            catch (IllegalArgumentException unavailable) { unmapped++; continue; }
            var key = IndependentUuValueIndex.keyOf(item);
            String raw = row.get("raw_value").getAsString();
            if (raw.equals("Infinity")) {
                if (prices.containsKey(key)) throw new IllegalArgumentException("Conflicting finite and denied quote");
                denied.add(key); continue;
            }
            double buckets = Double.parseDouble(raw) / 100000.0;
            if (!StoredPattern.validCosts(buckets, 0) || denied.contains(key))
                throw new IllegalArgumentException("Invalid reference price");
            var old = prices.putIfAbsent(key, buckets);
            if (old != null && Math.abs(old - buckets) > Math.max(old, buckets) * 1e-12)
                throw new IllegalArgumentException("Multiple reference prices map to one item");
        }
        return new Catalog(Map.copyOf(prices), Set.copyOf(denied), unmapped, rows.size());
    }
}
