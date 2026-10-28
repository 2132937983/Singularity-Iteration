// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * 0.1.10.26 (X3): the {@code si_exchange} datapacks.
 * <pre>
 * data/&lt;ns&gt;/si_exchange/
 *   bounties/*.json      bounty tables ({@link BountyTables})
 *   rewards/*.json       reward pools ({@link BountyTables})
 *   listings/*.json      player listings open from the start
 *   economy.json         economy settings ({@link EconomySettings})
 *   commodities/*.json   commodities in the si_futures format (read by the futures loader)
 * </pre>
 * Checked on every {@code /reload}; mistakes go to the log and to {@code /mio_icif bounty errors}.
 */
public final class ExchangeTables {
    private ExchangeTables() {}

    public static final String DIRECTORY = "si_exchange";

    /** A listing of a datapack: item, currency ({@code coin} or {@code uu}) and the first price. */
    public record DatapackListing(String item, ExchangeData.Currency currency, long price) { }

    public record Snapshot(BountyTables.Parsed tables, List<DatapackListing> listings, List<String> errors) {
        static final Snapshot EMPTY = new Snapshot(BountyTables.Parsed.EMPTY, List.of(), List.of());
    }

    private static volatile Snapshot current = Snapshot.EMPTY;

    public static Snapshot current() { return current; }

    public static BountyTables.Parsed tables() { return current.tables(); }

    /** 0.1.10.33: the stack size of an item id (64 when unknown). */
    static int maxStack(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return 64;
        return Math.max(1, new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(rl)).getMaxStackSize());
    }

    static boolean itemExists(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.ITEM.containsKey(rl);
    }

    /** Reads the files of {@code si_exchange} (ids relative to it, as {@code ns:folder/name}). */
    public static Snapshot read(Map<ResourceLocation, JsonElement> files) {
        Map<String, JsonElement> bounties = new LinkedHashMap<>(), pools = new LinkedHashMap<>();
        Map<String, JsonElement> economy = new LinkedHashMap<>();
        List<DatapackListing> listings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> f : new TreeMap<>(files).entrySet()) {
            ResourceLocation id = f.getKey();
            String path = id.getPath();
            if (path.startsWith("bounties/")) bounties.put(id.getNamespace() + ":" + path.substring(9), f.getValue());
            else if (path.startsWith("rewards/")) pools.put(id.getNamespace() + ":" + path.substring(8), f.getValue());
            else if (path.startsWith("listings/")) listings(id, f.getValue(), listings, errors);
            else if (path.equals("economy")) {
                if (!f.getValue().isJsonObject()) { errors.add(id + ": economy.json must be an object"); continue; }
                Map<String, JsonElement> values = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> e : f.getValue().getAsJsonObject().entrySet()) values.put(e.getKey(), e.getValue());
                Map<String, com.google.gson.JsonPrimitive> ok = EconomySettings.validate(id.toString(), values, errors);
                economy.putAll(ok);
            } else if (!path.startsWith("commodities/")) {
                errors.add(id + ": unknown file (expected bounties/, rewards/, listings/, commodities/ or economy.json)");
            }
        }
        BountyTables.Parsed parsed = BountyTables.parse(bounties, pools, ExchangeTables::itemExists);
        for (BountyTables.Error e : parsed.errors()) errors.add(e.toString());
        // 0.1.10.33: a requirement above eight inventory slots of its item is cut to eight slots when posted
        for (BountyTables.Table t : parsed.tables().values()) {
            for (BountyTables.Requirement q : t.requirements()) {
                int stack = maxStack(q.item());
                long cut = BountyMath.tableCount(q.count(), stack);
                if (cut < q.count())
                    errors.add(t.id() + ": " + q.item() + " x" + q.count() + " is more than " + BountyMath.TABLE_SLOTS + " slots (stack " + stack
                        + "); posted as x" + cut);
            }
        }
        Map<String, com.google.gson.JsonPrimitive> eco = new LinkedHashMap<>();
        economy.forEach((k, v) -> eco.put(k, v.getAsJsonPrimitive()));
        EconomySettings.apply(eco);
        return new Snapshot(parsed, List.copyOf(listings), List.copyOf(errors));
    }

    static void listings(ResourceLocation file, JsonElement json, List<DatapackListing> out, List<String> errors) {
        List<JsonElement> entries = new ArrayList<>();
        if (json.isJsonObject() && json.getAsJsonObject().has("listings")) json.getAsJsonObject().getAsJsonArray("listings").forEach(entries::add);
        else entries.add(json);
        for (JsonElement e : entries) {
            try {
                JsonObject o = BountyTables.obj(e, "listing");
                String item = BountyTables.string(o, "item", "");
                if (!itemExists(item)) {
                    if (!BountyTables.bool(o, "optional", false)) errors.add(file + ": unknown item " + item);
                    continue;
                }
                ExchangeData.Currency currency = switch (BountyTables.string(o, "currency", "coin").toLowerCase(Locale.ROOT)) {
                    case "coin", "coins" -> ExchangeData.Currency.COIN;
                    case "uu" -> ExchangeData.Currency.UU;
                    default -> throw new IllegalArgumentException("currency must be coin or uu");
                };
                long price = BountyTables.range(o, "price", 0, 1, 1_000_000_000_000L);
                if (price < 1) throw new IllegalArgumentException("price missing");
                out.removeIf(l -> l.item().equals(item));
                out.add(new DatapackListing(item, currency, price));
            } catch (RuntimeException ex) {
                errors.add(file + ": " + ex.getMessage());
            }
        }
    }

    /** The reload listener of {@code data/<ns>/si_exchange}. */
    public static final class Loader extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new GsonBuilder().create();

        public Loader() { super(GSON, DIRECTORY); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Snapshot s;
            try {
                s = read(files);
            } catch (RuntimeException e) {
                s = new Snapshot(BountyTables.Parsed.EMPTY, List.of(), List.of("si_exchange: " + e.getMessage()));
            }
            current = s;
            for (String e : s.errors()) Singularity_Iteration.LOGGER.warn("[Exchange] {}", e);
            Singularity_Iteration.LOGGER.info("[Exchange] {} bounty tables, {} reward pools, {} listings, {} economy values, {} problems",
                s.tables().tables().size(), s.tables().pools().size(), s.listings().size(), EconomySettings.datapackValues().size(), s.errors().size());
        }
    }
}
