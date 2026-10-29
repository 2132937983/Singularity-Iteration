// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * 0.1.10.26 (X3): the bounty tables and reward pools of the {@code si_exchange} datapacks, parsed from JSON (pure Java
 * with Gson; the offline harness reads the shipped files with it).
 *
 * <pre>{@code
 * data/<ns>/si_exchange/bounties/<name>.json   one table entry, or {"bounties": [ ... ]}
 * { "tier": "advanced", "weight": 10, "max_open": 1, "duration_days": 7, "cooldown_days": 7,
 *   (0.1.10.34: instead of "duration_days", "rarity": "rare" and / or "days": [1, 4] - a range drawn per bounty)
 *   "conditions": { "min_day": 7, "advancement": "minecraft:story/smelt_iron" },
 *   "requirements": [ { "item": "mio_icif:normal/item_circuit", "count": 32 },
 *                     (0.1.10.36: any item sharing a material tag of "item" counts, e.g. another mod's copper ingot for ours;
 *                      { "item": "minecraft:copper_ingot", "tag": "c:ingots/copper", ... } names the accepted tag itself)
 *                     { "item": "minecraft:dragon_head", "count": 1, "value": 20000 } ],
 *   "reward": { "pool": "mio_icif:advanced", "rolls": 2, "multiplier": 1.3 },
 *   "split": "contribution", "player_share": 0.5 }
 *
 * data/<ns>/si_exchange/rewards/<name>.json    the pool <ns>:<name>
 * { "entries": [ { "type": "coins", "weight": 3 },                                  value x multiplier, minted on payment
 *                { "type": "coins", "amount": 5000, "weight": 1 },                  a fixed amount
 *                { "type": "item", "item": "mio_icif:normal/item_circuit", "source": "treasury", "weight": 2 },  count by value
 *                { "type": "item", "item": "mio_icif:upgrade/overclocker_upgrade", "count": 4, "source": "issue", "weight": 1 },
 *                { "type": "item", "item": "minecraft:elytra", "count": 1, "source": "vault", "weight": 1 },
 *                { "type": "uu", "mb": 1000, "weight": 1 } ] }
 * }</pre>
 * Table ids: {@code id} when given, else {@code <ns>:<path>} of the file ({@code #<n>} added for the n-th entry of a list).
 * A later file with the same id replaces the earlier one; {@code "remove": true} drops it. Invalid entries are skipped and
 * reported ({@link Parsed#errors}).
 */
public final class BountyTables {
    private BountyTables() {}

    public enum Tier {
        NORMAL, ADVANCED, SPECIAL;

        public String id() { return name().toLowerCase(Locale.ROOT); }

        static Tier parse(String s) {
            return switch (s.toLowerCase(Locale.ROOT)) {
                case "normal" -> NORMAL;
                case "advanced" -> ADVANCED;
                case "special" -> SPECIAL;
                default -> throw new IllegalArgumentException("tier must be normal, advanced or special");
            };
        }
    }

    public enum Split { FIRST, CONTRIBUTION }

    /** What a reward line pays: coins, items, or UU-matter (mB). */
    public enum Kind { COINS, ITEM, UU }

    /**
     * Where a reward comes from: {@code MINTED} new coins at payment; {@code TREASURY} the market's stock (reserved at
     * posting); {@code VAULT} the bounty vault (rare items delivered to earlier bounties, reserved at posting);
     * {@code ISSUE} created at payment within the weekly cap of the tier (items and UU-matter).
     */
    public enum Source { MINTED, TREASURY, VAULT, ISSUE }

    /** One requirement: {@code value} coins per item, or 0 to value it at the market's ask (commodities) when posting. */
    public record Requirement(String item, long count, double value, String tag) {
        /** 0.1.10.36: {@code tag} "" = the item and its material tags ({@link ItemMatch}). */
        public Requirement(String item, long count, double value) {
            this(item, count, value, "");
        }
    }

    public record Table(String id, Tier tier, int weight, int maxOpen, int days, int cooldownDays, long minDay, String advancement,
                        List<Requirement> requirements, String pool, int rolls, double multiplier, Split split, double playerShare,
                        String rarity, int minDays, int maxDays) {
        /**
         * {@code days} / {@code multiplier} / {@code playerShare} of -1 take the tier's setting. 0.1.10.34: {@code rarity}
         * ("" = by tier and weight) and {@code minDays..maxDays} (-1 = by the rarity) choose the range of days when
         * {@code days} is -1.
         */
        public Table(String id, Tier tier, int weight, int maxOpen, int days, int cooldownDays, long minDay, String advancement,
                     List<Requirement> requirements, String pool, int rolls, double multiplier, Split split, double playerShare) {
            this(id, tier, weight, maxOpen, days, cooldownDays, minDay, advancement, requirements, pool, rolls, multiplier, split, playerShare,
                "", -1, -1);
        }
    }

    /**
     * One entry of a pool. {@code count} 0 (items from the treasury only) = as many as {@code value x multiplier} buys at the
     * market's ask; {@code amount} for coins 0 = by value; {@code multiplier} scales the value of this entry (1 = the bounty's).
     */
    public record Entry(Kind kind, Source source, String item, long count, int weight, double multiplier) { }

    public record Pool(String id, List<Entry> entries) { }

    /** A table or pool file that failed: the file and the reason. */
    public record Error(String file, String message) {
        @Override public String toString() { return file + ": " + message; }
    }

    public record Parsed(Map<String, Table> tables, Map<String, Pool> pools, List<Error> errors) {
        public static final Parsed EMPTY = new Parsed(Map.of(), Map.of(), List.of());

        public List<Table> tier(Tier tier) {
            List<Table> out = new ArrayList<>();
            for (Table t : tables.values()) if (t.tier() == tier) out.add(t);
            return out;
        }
    }

    public static final int MAX_REQUIREMENTS = 8, MAX_ROLLS = 4, MAX_ENTRIES = 64;
    public static final long MAX_COUNT = 1_000_000;

    /**
     * Parses all files. {@code bounties} and {@code pools}: file id ({@code ns:path} without the folder) to JSON, read in id
     * order. {@code itemExists} says whether an item id is registered (entries naming a missing item are skipped quietly
     * when {@code optional} is set on them, else reported).
     */
    public static Parsed parse(Map<String, JsonElement> bounties, Map<String, JsonElement> pools, Predicate<String> itemExists) {
        List<Error> errors = new ArrayList<>();
        Map<String, Pool> poolMap = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> f : new TreeMap<>(pools).entrySet()) {
            try {
                Pool p = pool(f.getKey(), obj(f.getValue(), "pool"), itemExists, errors);
                poolMap.put(p.id(), p);
            } catch (RuntimeException e) {
                errors.add(new Error("rewards/" + f.getKey(), e.getMessage()));
            }
        }
        Map<String, Table> tableMap = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> f : new TreeMap<>(bounties).entrySet()) {
            String file = "bounties/" + f.getKey();
            try {
                JsonObject root = obj(f.getValue(), "bounty file");
                if (root.has("bounties")) {
                    JsonArray list = root.getAsJsonArray("bounties");
                    for (int i = 0; i < list.size(); i++) {
                        try {
                            table(f.getKey() + "#" + i, obj(list.get(i), "bounty"), itemExists, tableMap);
                        } catch (RuntimeException e) {
                            errors.add(new Error(file + " #" + i, e.getMessage()));
                        }
                    }
                } else {
                    table(f.getKey(), root, itemExists, tableMap);
                }
            } catch (RuntimeException e) {
                errors.add(new Error(file, e.getMessage()));
            }
        }
        for (Table t : tableMap.values()) {
            if (!t.pool().isEmpty() && !poolMap.containsKey(t.pool()))
                errors.add(new Error("bounties/" + t.id(), "reward pool " + t.pool() + " does not exist (coins are paid instead)"));
        }
        return new Parsed(Collections.unmodifiableMap(tableMap), Collections.unmodifiableMap(poolMap), List.copyOf(errors));
    }

    static void table(String fileId, JsonObject o, Predicate<String> itemExists, Map<String, Table> out) {
        String id = string(o, "id", fileId);
        if (bool(o, "remove", false)) { out.remove(id); return; }
        Tier tier = Tier.parse(string(o, "tier", "advanced"));
        int weight = (int) range(o, "weight", 10, 0, 1_000_000);
        int maxOpen = (int) range(o, "max_open", 1, 1, 64);
        int days = (int) range(o, "duration_days", -1, -1, 60);
        if (days == 0) throw new IllegalArgumentException("duration_days must be 1..60");
        int cooldown = (int) range(o, "cooldown_days", 0, 0, 3650);
        // 0.1.10.34: the rarity and a range of days
        String rarity = string(o, "rarity", "").toLowerCase(Locale.ROOT);
        if (!rarity.isEmpty() && BountyPacing.Rarity.of(rarity) == null)
            throw new IllegalArgumentException("rarity must be common, uncommon, rare or legendary");
        int minDays = -1, maxDays = -1;
        if (o.has("days")) {
            if (!o.get("days").isJsonArray() || o.getAsJsonArray("days").size() != 2) throw new IllegalArgumentException("days must be [min, max]");
            JsonArray dr = o.getAsJsonArray("days");
            minDays = dr.get(0).getAsInt();
            maxDays = dr.get(1).getAsInt();
            if (minDays < 1 || maxDays < minDays || maxDays > 60) throw new IllegalArgumentException("days must be [min, max] within 1..60");
        }
        long minDay = 0;
        String advancement = "";
        if (o.has("conditions")) {
            JsonObject c = obj(o.get("conditions"), "conditions");
            minDay = range(c, "min_day", 0, 0, 1_000_000);
            advancement = string(c, "advancement", "");
        }
        if (!o.has("requirements")) throw new IllegalArgumentException("requirements missing");
        JsonArray reqs = o.getAsJsonArray("requirements");
        if (reqs.isEmpty() || reqs.size() > MAX_REQUIREMENTS) throw new IllegalArgumentException("requirements: 1.." + MAX_REQUIREMENTS + " entries");
        List<Requirement> list = new ArrayList<>();
        for (JsonElement e : reqs) {
            JsonObject r = obj(e, "requirement");
            String item = string(r, "item", "");
            if (item.isEmpty()) throw new IllegalArgumentException("requirement without item");
            if (!itemExists.test(item)) {
                if (bool(o, "optional", false)) return;   // a table for a mod that is not installed
                throw new IllegalArgumentException("unknown item " + item);
            }
            for (Requirement x : list) if (x.item().equals(item)) throw new IllegalArgumentException("item " + item + " twice");
            long count = range(r, "count", 1, 1, MAX_COUNT);
            double value = number(r, "value", 0);
            if (value < 0 || !Double.isFinite(value)) throw new IllegalArgumentException("value must be 0 or more");
            // 0.1.10.36: "tag" - the accepted items besides the item itself (without it: the item's material tags)
            String tag = string(r, "tag", "");
            if (!tag.isEmpty() && net.minecraft.resources.ResourceLocation.tryParse(tag.startsWith("#") ? tag.substring(1) : tag) == null)
                throw new IllegalArgumentException("bad tag " + tag);
            list.add(new Requirement(item, count, value, tag));
        }
        String pool = "";
        int rolls = 1;
        double multiplier = -1;
        if (o.has("reward")) {
            JsonObject r = obj(o.get("reward"), "reward");
            pool = string(r, "pool", "");
            rolls = (int) range(r, "rolls", 1, 1, MAX_ROLLS);
            multiplier = number(r, "multiplier", -1);
            if (multiplier != -1 && !(multiplier > 0 && multiplier <= 100)) throw new IllegalArgumentException("multiplier must be 0..100");
        }
        Split split = switch (string(o, "split", "first").toLowerCase(Locale.ROOT)) {
            case "first" -> Split.FIRST;
            case "contribution" -> Split.CONTRIBUTION;
            default -> throw new IllegalArgumentException("split must be first or contribution");
        };
        double share = number(o, "player_share", -1);
        if (share != -1 && !(share > 0 && share <= 1)) throw new IllegalArgumentException("player_share must be 0..1");
        out.remove(id);
        out.put(id, new Table(id, tier, weight, maxOpen, days, cooldown, minDay, advancement, List.copyOf(list), pool, rolls, multiplier, split, share,
            rarity, minDays, maxDays));
    }

    static Pool pool(String id, JsonObject o, Predicate<String> itemExists, List<Error> errors) {
        if (!o.has("entries")) throw new IllegalArgumentException("entries missing");
        JsonArray arr = o.getAsJsonArray("entries");
        if (arr.size() > MAX_ENTRIES) throw new IllegalArgumentException("more than " + MAX_ENTRIES + " entries");
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            try {
                Entry e = entry(obj(arr.get(i), "entry"), itemExists);
                if (e != null) entries.add(e);
            } catch (RuntimeException ex) {
                errors.add(new Error("rewards/" + id + " #" + i, ex.getMessage()));
            }
        }
        return new Pool(id, List.copyOf(entries));
    }

    static Entry entry(JsonObject o, Predicate<String> itemExists) {
        int weight = (int) range(o, "weight", 1, 0, 1_000_000);
        double multiplier = number(o, "multiplier", 1);
        if (!(multiplier > 0 && multiplier <= 100)) throw new IllegalArgumentException("multiplier must be 0..100");
        switch (string(o, "type", "item").toLowerCase(Locale.ROOT)) {
            case "coins" -> {
                long amount = range(o, "amount", 0, 0, 1_000_000_000L);
                return new Entry(Kind.COINS, Source.MINTED, "", amount, weight, multiplier);
            }
            case "uu" -> {
                long mb = range(o, "mb", 1000, 1, 1_000_000_000L);
                return new Entry(Kind.UU, Source.ISSUE, "", mb, weight, 1);
            }
            case "item" -> {
                String item = string(o, "item", "");
                if (item.isEmpty()) throw new IllegalArgumentException("item entry without item");
                if (!itemExists.test(item)) {
                    if (bool(o, "optional", false)) return null;
                    throw new IllegalArgumentException("unknown item " + item);
                }
                Source source = switch (string(o, "source", "treasury").toLowerCase(Locale.ROOT)) {
                    case "treasury" -> Source.TREASURY;
                    case "vault" -> Source.VAULT;
                    case "issue" -> Source.ISSUE;
                    default -> throw new IllegalArgumentException("source must be treasury, vault or issue");
                };
                long count = range(o, "count", 0, 0, MAX_COUNT);
                if (count == 0 && source != Source.TREASURY) throw new IllegalArgumentException("count is needed unless the source is treasury");
                return new Entry(Kind.ITEM, source, item, count, weight, multiplier);
            }
            default -> throw new IllegalArgumentException("type must be coins, item or uu");
        }
    }

    // ------------------------------------------------------------------ JSON helpers (no Minecraft types)

    static JsonObject obj(JsonElement e, String what) {
        if (e == null || !e.isJsonObject()) throw new IllegalArgumentException(what + " must be an object");
        return e.getAsJsonObject();
    }

    static String string(JsonObject o, String key, String fallback) {
        if (!o.has(key)) return fallback;
        JsonElement e = o.get(key);
        if (!e.isJsonPrimitive() || !((JsonPrimitive) e).isString()) throw new IllegalArgumentException(key + " must be a string");
        return e.getAsString();
    }

    static boolean bool(JsonObject o, String key, boolean fallback) {
        if (!o.has(key)) return fallback;
        JsonElement e = o.get(key);
        if (!e.isJsonPrimitive() || !((JsonPrimitive) e).isBoolean()) throw new IllegalArgumentException(key + " must be true or false");
        return e.getAsBoolean();
    }

    static double number(JsonObject o, String key, double fallback) {
        if (!o.has(key)) return fallback;
        JsonElement e = o.get(key);
        if (!e.isJsonPrimitive() || !((JsonPrimitive) e).isNumber()) throw new IllegalArgumentException(key + " must be a number");
        return e.getAsDouble();
    }

    static long range(JsonObject o, String key, long fallback, long min, long max) {
        if (!o.has(key)) return fallback;
        double v = number(o, key, fallback);
        if (v != Math.rint(v) || v < min || v > max) throw new IllegalArgumentException(key + " must be a whole number " + min + ".." + max);
        return (long) v;
    }
}
