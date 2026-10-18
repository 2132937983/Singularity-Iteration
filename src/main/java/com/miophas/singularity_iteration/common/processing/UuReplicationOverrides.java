// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miophas.singularity_iteration.core.runtime.uu.IndependentUuValueIndex;
import com.mojang.logging.LogUtils;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * Replication lists of data packs (0.1.10.5): {@code data/<namespace>/uu_replication/*.json}.
 *
 * <p>The UU prices of the replicator come from the reviewed IC2 reference catalog and from the
 * recipes that lead to an item ({@link UuPricingLifecycle}). A replication list adds items to that
 * table, changes their price, gives them an energy cost, or takes them out. Its entries are fixed
 * prices: they win over the reference catalog and over recipe prices, and recipes made from them
 * get prices too (copper ingots from raw copper, for example). SI ships its own list in
 * {@code data/mio_icif/uu_replication/defaults.json}; a data pack replaces it with a file at the
 * same path, or adds its own files.
 *
 * <pre>{@code
 * {
 *   "entries": [
 *     { "item": "minecraft:netherite_scrap", "uu": 1500 },
 *     { "item": "minecraft:nether_star", "uu": 100000, "eu": 60000000 },
 *     { "tag": "c:ores/tin", "uu": 1.4 },
 *     { "item": "minecraft:dragon_egg", "deny": true },
 *     { "item": "othermod:gem", "uu": 40, "required": false }
 *   ]
 * }
 * }</pre>
 * <ul>
 *   <li>{@code item} or {@code tag} (an item tag, with or without "#"): what the entry is for.</li>
 *   <li>{@code uu}: UU matter per copy in mB (more than 0).</li>
 *   <li>{@code eu}: optional EU per copy. Without it the replicator uses its standard rate,
 *       512 EU per 0.1 mB ({@link #STANDARD_EU_PER_MB} EU per mB). The replicator spreads it over
 *       the work, at most 8 192 EU per tick.</li>
 *   <li>{@code deny: true}: the item cannot be scanned and replicated.</li>
 *   <li>{@code required: false}: an unknown item or tag is skipped without a warning (for
 *       items of mods that may be absent).</li>
 * </ul>
 * Files are read in the order of their ids (namespace, then path); a later entry for the same
 * item wins. A broken file or entry is skipped with a warning in the log; the rest of the price
 * table still loads. At most {@link #MAX_ITEMS} items, {@link #MAX_TAG_ITEMS} per tag.
 */
public final class UuReplicationOverrides {
    private UuReplicationOverrides() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String FOLDER = "uu_replication";
    public static final int MAX_ITEMS = 4096, MAX_TAG_ITEMS = 512;
    /** Standard energy of the replicator: 512 EU per 0.1 mB of UU matter. */
    public static final long STANDARD_EU_PER_MB = 5120;
    private static final double MAX_UU_MB = 1.0e9;
    private static final long MAX_EU = 1_000_000_000_000L;

    /** The merged lists: fixed prices (buckets), EU per copy, denied items, and counts for the log. */
    public record Overrides(Map<IndependentUuValueIndex.Key, Double> prices, Map<IndependentUuValueIndex.Key, Long> energy,
                            Set<IndependentUuValueIndex.Key> denied, int files, int entries, int skipped) {
        public static final Overrides EMPTY = new Overrides(Map.of(), Map.of(), Set.of(), 0, 0, 0);

        /** The reference catalog with these entries applied (a price wins over a denial and the other way round). */
        public UuReferenceCatalog.Catalog apply(UuReferenceCatalog.Catalog base) {
            var prices = new HashMap<>(base.prices());
            var denied = new HashSet<>(base.denied());
            for (var key : this.denied) {
                prices.remove(key);
                denied.add(key);
            }
            for (var row : this.prices.entrySet()) {
                denied.remove(row.getKey());
                prices.put(row.getKey(), row.getValue());
            }
            return new UuReferenceCatalog.Catalog(Map.copyOf(prices), Set.copyOf(denied), base.unmapped(), base.entries() + entries);
        }
    }

    /** Reads every replication list. Never throws: a broken file or entry is skipped with a warning. */
    public static Overrides load(ResourceManager resources, HolderLookup.Provider registries) {
        Map<ResourceLocation, Resource> found;
        try {
            found = new TreeMap<>(resources.listResources(FOLDER, id -> id.getPath().endsWith(".json")));
        } catch (RuntimeException e) {
            LOGGER.warn("[UU] Replication lists cannot be listed: {}", e.toString());
            return Overrides.EMPTY;
        }
        var prices = new LinkedHashMap<IndependentUuValueIndex.Key, Double>();
        var energy = new LinkedHashMap<IndependentUuValueIndex.Key, Long>();
        var denied = new LinkedHashSet<IndependentUuValueIndex.Key>();
        int files = 0, entries = 0, skipped = 0;
        for (var file : found.entrySet()) {
            JsonObject root;
            try (Reader reader = file.getValue().openAsReader()) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                LOGGER.warn("[UU] Replication list {} skipped: {}", file.getKey(), e.toString());
                skipped++;
                continue;
            }
            files++;
            JsonElement rows = root.get("entries");
            if (rows == null || !rows.isJsonArray()) {
                LOGGER.warn("[UU] Replication list {} has no \"entries\" array", file.getKey());
                skipped++;
                continue;
            }
            int index = 0;
            for (JsonElement row : rows.getAsJsonArray()) {
                index++;
                try {
                    Entry entry = parse(row.getAsJsonObject());
                    List<Item> items = resolve(entry, registries);
                    if (items == null) {
                        if (entry.required()) {
                            LOGGER.warn("[UU] {} entry {}: unknown {} {}", file.getKey(), index, entry.tag() ? "tag" : "item", entry.id());
                            skipped++;
                        }
                        continue;
                    }
                    for (Item item : items) {
                        var key = IndependentUuValueIndex.keyOf(new ItemStack(item));
                        if (!prices.containsKey(key) && !denied.contains(key) && prices.size() + denied.size() >= MAX_ITEMS) {
                            throw new IllegalArgumentException("more than " + MAX_ITEMS + " items in replication lists");
                        }
                        if (entry.deny()) {
                            prices.remove(key);
                            energy.remove(key);
                            denied.add(key);
                        } else {
                            denied.remove(key);
                            prices.put(key, entry.uuMb() / 1000.0);
                            if (entry.eu() > 0) energy.put(key, entry.eu());
                            else energy.remove(key);
                        }
                    }
                    entries++;
                } catch (RuntimeException e) {
                    LOGGER.warn("[UU] {} entry {} skipped: {}", file.getKey(), index, e.getMessage());
                    skipped++;
                }
            }
        }
        return new Overrides(Map.copyOf(prices), Map.copyOf(energy), Set.copyOf(denied), files, entries, skipped);
    }

    private record Entry(String id, boolean tag, double uuMb, long eu, boolean deny, boolean required) { }

    private static Entry parse(JsonObject o) {
        boolean hasItem = o.has("item"), hasTag = o.has("tag");
        if (hasItem == hasTag) throw new IllegalArgumentException("needs exactly one of \"item\" and \"tag\"");
        String id = (hasItem ? o.get("item") : o.get("tag")).getAsString().trim();
        if (hasTag && id.startsWith("#")) id = id.substring(1);
        if (ResourceLocation.tryParse(id) == null || id.indexOf(':') <= 0) throw new IllegalArgumentException("bad id " + id);
        boolean deny = o.has("deny") && o.get("deny").getAsBoolean();
        boolean required = !o.has("required") || o.get("required").getAsBoolean();
        double uu = 0;
        long eu = 0;
        if (!deny) {
            if (!o.has("uu")) throw new IllegalArgumentException("needs \"uu\" (mB) or \"deny\": true");
            uu = o.get("uu").getAsDouble();
            if (!Double.isFinite(uu) || uu <= 0 || uu > MAX_UU_MB) throw new IllegalArgumentException("\"uu\" must be above 0 and at most 1e9 mB");
            if (o.has("eu")) {
                eu = o.get("eu").getAsLong();
                if (eu <= 0 || eu > MAX_EU) throw new IllegalArgumentException("\"eu\" must be above 0 and at most 1e12");
            }
        }
        return new Entry(id, hasTag, uu, eu, deny, required);
    }

    /** The items of an entry, or null when the item or tag is unknown. */
    private static List<Item> resolve(Entry entry, HolderLookup.Provider registries) {
        var items = registries.lookupOrThrow(Registries.ITEM);
        ResourceLocation id = ResourceLocation.parse(entry.id());
        if (!entry.tag()) {
            var holder = items.get(ResourceKey.create(Registries.ITEM, id));
            if (holder.isEmpty() || new ItemStack(holder.get().value()).isEmpty()) return null;
            return List.of(holder.get().value());
        }
        var set = items.get(TagKey.create(Registries.ITEM, id));
        if (set.isEmpty()) return null;
        List<Item> out = new ArrayList<>();
        for (Holder<Item> holder : set.get()) {
            if (out.size() >= MAX_TAG_ITEMS) throw new IllegalArgumentException("tag " + id + " has more than " + MAX_TAG_ITEMS + " items");
            if (!new ItemStack(holder.value()).isEmpty()) out.add(holder.value());
        }
        return out;
    }
}
