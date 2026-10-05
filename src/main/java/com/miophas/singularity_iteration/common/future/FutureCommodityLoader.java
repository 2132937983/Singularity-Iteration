package com.miophas.singularity_iteration.common.future;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Futures market commodities from datapacks: {@code data/<namespace>/si_futures/<name>.json}.
 *
 * <pre>{@code
 * {
 *   "category": "mineral",                 // default for the entries below (mineral, agriculture, wood, food, other)
 *   "commodities": [
 *     { "item": "minecraft:diamond", "base_price": 800, "volatility": 0.22 },
 *     { "item": "minecraft:netherite_ingot", "base_price": 4000, "volatility": 0.30,
 *       "period_days": 3,                    // price moves every 3 days (default 1)
 *       "unlock": { "advancement": "minecraft:nether/obtain_ancient_debris" } },
 *     { "item": "minecraft:coal", "remove": true }   // drop an entry another pack added
 *   ]
 * }
 * }</pre>
 * A file may also be a single entry object. Files are read in resource-location order; a later
 * entry for the same item replaces the earlier one, so a datapack overrides the defaults shipped
 * in {@code data/mio_icif/si_futures}. Invalid entries are skipped with a log line.
 */
public final class FutureCommodityLoader extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "si_futures";
    private static final Gson GSON = new GsonBuilder().create();

    public FutureCommodityLoader() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, FutureCommodity> byItem = new LinkedHashMap<>();
        for (var file : new TreeMap<>(files).entrySet()) {
            try {
                parseFile(file.getKey(), file.getValue(), byItem);
            } catch (RuntimeException e) {
                Singularity_Iteration.LOGGER.warn("[Futures] skipped {}: {}", file.getKey(), e.getMessage());
            }
        }
        FutureCommodityManager.apply(new ArrayList<>(byItem.values()));
        Singularity_Iteration.LOGGER.info("[Futures] {} commodities from datapacks", byItem.size());
    }

    static void parseFile(ResourceLocation file, JsonElement json, Map<String, FutureCommodity> byItem) {
        JsonObject root = GsonHelper.convertToJsonObject(json, "futures file");
        String category = GsonHelper.getAsString(root, "category", "other");
        if (root.has("commodities")) {
            for (JsonElement entry : GsonHelper.getAsJsonArray(root, "commodities")) {
                try {
                    parseEntry(GsonHelper.convertToJsonObject(entry, "commodity"), category, byItem);
                } catch (RuntimeException e) {
                    Singularity_Iteration.LOGGER.warn("[Futures] {}: skipped entry {}: {}", file, entry, e.getMessage());
                }
            }
        } else {
            parseEntry(root, category, byItem);
        }
    }

    static void parseEntry(JsonObject entry, String defaultCategory, Map<String, FutureCommodity> byItem) {
        ResourceLocation itemId = ResourceLocation.parse(GsonHelper.getAsString(entry, "item"));
        String key = itemId.toString();
        if (GsonHelper.getAsBoolean(entry, "remove", false)) {
            byItem.remove(key);
            return;
        }
        if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
            // optional mod item: quietly absent when the mod is not installed
            return;
        }
        int basePrice = GsonHelper.getAsInt(entry, "base_price");
        float volatility = GsonHelper.getAsFloat(entry, "volatility", 0.05F);
        if (basePrice <= 0) throw new IllegalArgumentException("base_price must be positive");
        if (!Float.isFinite(volatility)) throw new IllegalArgumentException("volatility must be a number");
        if (volatility < 0 || volatility > 1) throw new IllegalArgumentException("volatility must be 0..1");
        int period = GsonHelper.getAsInt(entry, "period_days", 1);
        if (period < 1 || period > 3650) throw new IllegalArgumentException("period_days must be 1..3650");
        CommodityCategory category = CommodityCategory.fromId(GsonHelper.getAsString(entry, "category", defaultCategory));
        ResourceLocation unlock = null;
        if (entry.has("unlock")) {
            JsonObject u = GsonHelper.getAsJsonObject(entry, "unlock");
            if (u.has("advancement")) unlock = ResourceLocation.parse(GsonHelper.getAsString(u, "advancement"));
        }
        byItem.remove(key);   // a replacement moves to the end, after the entries it overrides
        byItem.put(key, new FutureCommodity(key, basePrice, volatility, category, period, unlock));
    }

    /** Entries in sync-packet form. */
    public static List<FutureCommoditySync.Entry> entries() {
        List<FutureCommoditySync.Entry> out = new ArrayList<>();
        for (FutureCommodity c : FutureCommodityManager.getCommodities())
            out.add(new FutureCommoditySync.Entry(c.getItemId(), c.getBasePrice(), c.getVolatility(), c.getCategory().getId(),
                c.getPeriodDays(), c.getUnlockAdvancement() == null ? "" : c.getUnlockAdvancement().toString()));
        return out;
    }
}
