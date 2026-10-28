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
 *     (0.1.10.34: "bid_price": the market's bid at the base price, at most base_price; the bid follows the price)
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
 * <p>0.1.10.26 (X3): {@code data/<namespace>/si_exchange/commodities/*.json} holds commodities in the same format; they
 * are read after all {@code si_futures} files (so they override them).
 */
public final class FutureCommodityLoader extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "si_futures";
    private static final Gson GSON = new GsonBuilder().create();

    /** 0.1.10.26: the commodity files of {@code si_exchange/commodities}, read with the others. */
    public static final String EXCHANGE_DIRECTORY = "si_exchange/commodities";

    private volatile Map<ResourceLocation, JsonElement> exchangeFiles = Map.of();

    public FutureCommodityLoader() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> files = super.prepare(manager, profiler);
        Map<ResourceLocation, JsonElement> extra = new java.util.HashMap<>();
        scanDirectory(manager, EXCHANGE_DIRECTORY, GSON, extra);
        exchangeFiles = extra;
        return files;
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
        for (var file : new TreeMap<>(exchangeFiles).entrySet()) {
            try {
                parseFile(file.getKey(), file.getValue(), byItem);
            } catch (RuntimeException e) {
                Singularity_Iteration.LOGGER.warn("[Futures] skipped {}/{}: {}", EXCHANGE_DIRECTORY, file.getKey(), e.getMessage());
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
        // 0.1.10.34 (item 10): the bid at the base price (the market buys for less than it sells)
        int bidPrice = GsonHelper.getAsInt(entry, "bid_price", 0);
        if (bidPrice < 0 || bidPrice > basePrice) throw new IllegalArgumentException("bid_price must be 0..base_price (0 = none)");
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
        // 0.1.10.25 (X1): the market of this commodity (-1 = the config value)
        FutureCommodity.Market market = FutureCommodity.Market.DEFAULT;
        if (entry.has("depth") || entry.has("export_per_day") || entry.has("import_per_day") || entry.has("import_premium")) {
            int player = -1, server = -1;
            if (entry.has("depth")) {
                JsonObject d = GsonHelper.getAsJsonObject(entry, "depth");
                player = GsonHelper.getAsInt(d, "player", -1);
                server = GsonHelper.getAsInt(d, "server", -1);
                if (player == 0 || player < -1 || server == 0 || server < -1) throw new IllegalArgumentException("depth.player / depth.server must be positive");
            }
            double export = GsonHelper.getAsDouble(entry, "export_per_day", -1);
            if (export != -1 && !(export >= 0.05 && export <= 1)) throw new IllegalArgumentException("export_per_day must be 0.05..1");
            int imports = GsonHelper.getAsInt(entry, "import_per_day", -1);
            if (imports < -1) throw new IllegalArgumentException("import_per_day must be 0 or more");
            double premium = GsonHelper.getAsDouble(entry, "import_premium", -1);
            if (premium != -1 && !(premium >= 0 && premium <= 2)) throw new IllegalArgumentException("import_premium must be 0..2");
            market = new FutureCommodity.Market(player, server, export, imports, premium);
        }
        // 0.1.10.26 (X3): the sell box and the auction also take the items of a tag, and stacks with changed components
        String tag = GsonHelper.getAsString(entry, "tag", "");
        if (!tag.isEmpty() && ResourceLocation.tryParse(tag.startsWith("#") ? tag.substring(1) : tag) == null)
            throw new IllegalArgumentException("tag must be an item tag id");
        if (tag.startsWith("#")) tag = tag.substring(1);
        boolean components = GsonHelper.getAsBoolean(entry, "accept_components", false);
        byItem.remove(key);   // a replacement moves to the end, after the entries it overrides
        byItem.put(key, new FutureCommodity(key, basePrice, volatility, category, period, unlock, market)
            .withSelling(new FutureCommodity.Selling(tag, components)).withBidPrice(bidPrice));
    }

    /** Entries in sync-packet form. */
    public static List<FutureCommoditySync.Entry> entries() {
        List<FutureCommoditySync.Entry> out = new ArrayList<>();
        for (FutureCommodity c : FutureCommodityManager.getCommodities())
            out.add(new FutureCommoditySync.Entry(c.getItemId(), c.getBasePrice(), c.getVolatility(), c.getCategory().getId(),
                c.getPeriodDays(), c.getUnlockAdvancement() == null ? "" : c.getUnlockAdvancement().toString(),
                c.getSelling().tag(), c.getSelling().acceptComponents()));
        return out;
    }
}
