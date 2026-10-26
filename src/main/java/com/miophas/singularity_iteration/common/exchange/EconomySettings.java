// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.miophas.singularity_iteration.common.Singularity_Iteration_Config;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 0.1.10.26 (X3): the economy settings of the exchange in one place, with the priority of the proposal (D1): a value
 * changed in the server config &gt; {@code data/<ns>/si_exchange/economy.json} of the datapacks &gt; the mod default. A
 * config value counts as changed when it differs from its default; a pack maker ships defaults in a datapack and a server
 * operator still overrides them in the config.
 *
 * <p>{@code economy.json} uses the names of the {@code FutureMarket} config entries ({@link #KEYS}), plus the reward pool
 * ids of the bounty tiers ({@code bountyNormalPool}, {@code bountyAdvancedPool}, {@code bountySpecialPool}). Several files:
 * the later one (resource order) wins key by key.
 */
public final class EconomySettings {
    private EconomySettings() {}

    /** One setting: its config entry (null for the pool ids), range and type. */
    public record Key(String name, Supplier<ModConfigSpec.ConfigValue<?>> config, double min, double max, char type, Object fallback) { }

    public static final Map<String, Key> KEYS;

    static {
        Map<String, Key> k = new LinkedHashMap<>();
        // 0.1.10.20 - 0.1.10.25 market settings
        put(k, "buyItemsPerDay", () -> Singularity_Iteration_Config.FUTURE_BUY_ITEMS_PER_DAY, 0, 1_000_000, 'i');
        put(k, "spread", () -> Singularity_Iteration_Config.FUTURE_SPREAD, 0, 0.5, 'd');
        put(k, "sellDepthPlayer", () -> Singularity_Iteration_Config.EXCHANGE_SELL_DEPTH_PLAYER, 1, 1_000_000, 'i');
        put(k, "sellDepthServer", () -> Singularity_Iteration_Config.EXCHANGE_SELL_DEPTH_SERVER, 1, 10_000_000, 'i');
        put(k, "sellExportPerDay", () -> Singularity_Iteration_Config.EXCHANGE_SELL_EXPORT, 0.05, 1, 'd');
        put(k, "sellAllowancePerDay", () -> Singularity_Iteration_Config.EXCHANGE_SELL_ALLOWANCE, 0, Integer.MAX_VALUE, 'i');
        put(k, "sellAllowanceDays", () -> Singularity_Iteration_Config.EXCHANGE_SELL_ALLOWANCE_DAYS, 1, 30, 'i');
        put(k, "sellAllowanceInSingleplayer", () -> Singularity_Iteration_Config.EXCHANGE_SELL_ALLOWANCE_SINGLEPLAYER, 0, 1, 'b');
        put(k, "exchangeFee", () -> Singularity_Iteration_Config.EXCHANGE_FEE, 0, 0.2, 'd');
        put(k, "importPerDay", () -> Singularity_Iteration_Config.EXCHANGE_IMPORT_PER_DAY, 0, 100_000, 'i');
        put(k, "importPremium", () -> Singularity_Iteration_Config.EXCHANGE_IMPORT_PREMIUM, 0, 2, 'd');
        put(k, "listingRequesters", () -> Singularity_Iteration_Config.EXCHANGE_LISTING_REQUESTERS, 1, 100, 'i');
        put(k, "listingFulfilled", () -> Singularity_Iteration_Config.EXCHANGE_LISTING_FULFILLED, 1, 100, 'i');
        put(k, "listingMinPlayMinutes", () -> Singularity_Iteration_Config.EXCHANGE_LISTING_MIN_PLAY_MINUTES, 0, 100_000, 'i');
        put(k, "uuDepth", () -> Singularity_Iteration_Config.EXCHANGE_UU_DEPTH, 0, 99, 'i');
        // 0.1.10.26 bounty tiers
        put(k, "bountyNormalMin", () -> Singularity_Iteration_Config.BOUNTY_NORMAL_MIN, 0, 20, 'i');
        put(k, "bountyNormalMax", () -> Singularity_Iteration_Config.BOUNTY_NORMAL_MAX, 0, 20, 'i');
        put(k, "bountyNormalValue", () -> Singularity_Iteration_Config.BOUNTY_NORMAL_VALUE, 10, 100_000_000, 'i');
        put(k, "bountyNormalDays", () -> Singularity_Iteration_Config.BOUNTY_NORMAL_DAYS, 1, 30, 'i');
        put(k, "bountyNormalMultiplier", () -> Singularity_Iteration_Config.BOUNTY_NORMAL_MULTIPLIER, 0.1, 10, 'd');
        put(k, "bountyNormalMinQuantity", () -> Singularity_Iteration_Config.BOUNTY_NORMAL_MIN_QUANTITY, 1, 100_000, 'i');
        put(k, "bountyNormalMaxQuantity", () -> Singularity_Iteration_Config.BOUNTY_NORMAL_MAX_QUANTITY, 1, 100_000, 'i');
        put(k, "bountyAdvancedPerWeekMin", () -> Singularity_Iteration_Config.BOUNTY_ADVANCED_MIN, 0, 21, 'i');
        put(k, "bountyAdvancedPerWeekMax", () -> Singularity_Iteration_Config.BOUNTY_ADVANCED_MAX, 0, 21, 'i');
        put(k, "bountyAdvancedDays", () -> Singularity_Iteration_Config.BOUNTY_ADVANCED_DAYS, 1, 60, 'i');
        put(k, "bountyAdvancedMultiplier", () -> Singularity_Iteration_Config.BOUNTY_ADVANCED_MULTIPLIER, 0.1, 10, 'd');
        put(k, "bountyAdvancedPlayerShare", () -> Singularity_Iteration_Config.BOUNTY_ADVANCED_SHARE, 0.05, 1, 'd');
        put(k, "bountySpecialChancePerDay", () -> Singularity_Iteration_Config.BOUNTY_SPECIAL_CHANCE, 0, 1, 'd');
        put(k, "bountySpecialMaxOpen", () -> Singularity_Iteration_Config.BOUNTY_SPECIAL_MAX_OPEN, 0, 8, 'i');
        put(k, "bountySpecialDays", () -> Singularity_Iteration_Config.BOUNTY_SPECIAL_DAYS, 1, 60, 'i');
        put(k, "bountyIssuePerWeekNormal", () -> Singularity_Iteration_Config.BOUNTY_ISSUE_NORMAL, 0, 1_000_000, 'i');
        put(k, "bountyIssuePerWeekAdvanced", () -> Singularity_Iteration_Config.BOUNTY_ISSUE_ADVANCED, 0, 1_000_000, 'i');
        put(k, "bountyIssuePerWeekSpecial", () -> Singularity_Iteration_Config.BOUNTY_ISSUE_SPECIAL, 0, 1_000_000, 'i');
        put(k, "bountyUuPerWeek", () -> Singularity_Iteration_Config.BOUNTY_UU_PER_WEEK, 0, 1_000_000_000, 'i');
        put(k, "bountyTreasuryLow", () -> Singularity_Iteration_Config.BOUNTY_TREASURY_LOW, 0, 1_000_000, 'i');
        // datapack only
        k.put("bountyNormalPool", new Key("bountyNormalPool", null, 0, 0, 's', "mio_icif:normal"));
        k.put("bountyAdvancedPool", new Key("bountyAdvancedPool", null, 0, 0, 's', "mio_icif:advanced"));
        k.put("bountySpecialPool", new Key("bountySpecialPool", null, 0, 0, 's', "mio_icif:special"));
        KEYS = Collections.unmodifiableMap(k);
    }

    private static void put(Map<String, Key> k, String name, Supplier<ModConfigSpec.ConfigValue<?>> config, double min, double max, char type) {
        k.put(name, new Key(name, config, min, max, type, null));
    }

    /** The values of the datapacks (economy.json), set on reload. */
    private static volatile Map<String, JsonPrimitive> datapack = Map.of();

    /** Checks the values of an economy file: unknown keys, wrong types and values out of range are reported and dropped. */
    public static Map<String, JsonPrimitive> validate(String file, Map<String, JsonElement> values, List<String> errors) {
        Map<String, JsonPrimitive> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : values.entrySet()) {
            Key key = KEYS.get(e.getKey());
            if (key == null) { errors.add(file + ": unknown key " + e.getKey()); continue; }
            JsonElement v = e.getValue();
            if (!v.isJsonPrimitive()) { errors.add(file + ": " + key.name() + " must be a value"); continue; }
            JsonPrimitive p = v.getAsJsonPrimitive();
            switch (key.type()) {
                case 's' -> { if (!p.isString()) { errors.add(file + ": " + key.name() + " must be a string"); continue; } }
                case 'b' -> { if (!p.isBoolean()) { errors.add(file + ": " + key.name() + " must be true or false"); continue; } }
                default -> {
                    if (!p.isNumber()) { errors.add(file + ": " + key.name() + " must be a number"); continue; }
                    double d = p.getAsDouble();
                    if (!(d >= key.min() && d <= key.max()) || key.type() == 'i' && d != Math.rint(d)) {
                        errors.add(file + ": " + key.name() + " must be " + (key.type() == 'i' ? "a whole number " : "") + key.min() + ".." + key.max());
                        continue;
                    }
                }
            }
            out.put(key.name(), p);
        }
        return out;
    }

    public static void apply(Map<String, JsonPrimitive> values) { datapack = Map.copyOf(values); }

    public static Map<String, JsonPrimitive> datapackValues() { return datapack; }

    /** Where the value of a key comes from now: "config", "datapack" or "default". */
    public static String origin(String name) {
        Key key = KEYS.get(name);
        if (key == null) return "-";
        if (key.config() != null && changed(key.config().get())) return "config";
        return datapack.containsKey(name) ? "datapack" : "default";
    }

    static boolean changed(ModConfigSpec.ConfigValue<?> c) {
        Object v = c.get(), d = c.getDefault();
        if (v instanceof Number a && d instanceof Number b) return Math.abs(a.doubleValue() - b.doubleValue()) > 1e-12;
        return !v.equals(d);
    }

    static long i(String name) {
        Key key = KEYS.get(name);
        ModConfigSpec.ConfigValue<?> c = key.config().get();
        if (!changed(c)) {
            JsonPrimitive p = datapack.get(name);
            if (p != null) return p.getAsLong();
        }
        return ((Number) c.get()).longValue();
    }

    static double d(String name) {
        Key key = KEYS.get(name);
        ModConfigSpec.ConfigValue<?> c = key.config().get();
        if (!changed(c)) {
            JsonPrimitive p = datapack.get(name);
            if (p != null) return p.getAsDouble();
        }
        return ((Number) c.get()).doubleValue();
    }

    static boolean b(String name) {
        Key key = KEYS.get(name);
        ModConfigSpec.ConfigValue<?> c = key.config().get();
        if (!changed(c)) {
            JsonPrimitive p = datapack.get(name);
            if (p != null) return p.getAsBoolean();
        }
        return (Boolean) c.get();
    }

    static String s(String name) {
        JsonPrimitive p = datapack.get(name);
        return p != null ? p.getAsString() : (String) KEYS.get(name).fallback();
    }

    // ------------------------------------------------------------------ typed getters

    public static int buyItemsPerDay() { return (int) i("buyItemsPerDay"); }
    public static double spread() { return d("spread"); }
    public static int sellDepthPlayer() { return (int) i("sellDepthPlayer"); }
    public static int sellDepthServer() { return (int) i("sellDepthServer"); }
    public static double sellExportPerDay() { return d("sellExportPerDay"); }
    public static long sellAllowancePerDay() { return i("sellAllowancePerDay"); }
    public static int sellAllowanceDays() { return (int) i("sellAllowanceDays"); }
    public static boolean sellAllowanceInSingleplayer() { return b("sellAllowanceInSingleplayer"); }
    public static double exchangeFee() { return d("exchangeFee"); }
    public static int importPerDay() { return (int) i("importPerDay"); }
    public static double importPremium() { return d("importPremium"); }
    public static int listingRequesters() { return (int) i("listingRequesters"); }
    public static int listingFulfilled() { return (int) i("listingFulfilled"); }
    public static int listingMinPlayMinutes() { return (int) i("listingMinPlayMinutes"); }
    public static int uuDepth() { return (int) i("uuDepth"); }

    /** The settings of the bounty tiers; {@code seed} fixes the random choices of a world. */
    public static BountyBoard.Rules rules(long seed) {
        return new BountyBoard.Rules((int) i("bountyNormalMin"), (int) Math.max(i("bountyNormalMin"), i("bountyNormalMax")),
            i("bountyNormalValue"), (int) i("bountyNormalDays"), d("bountyNormalMultiplier"), (int) i("bountyNormalMinQuantity"),
            (int) Math.max(i("bountyNormalMinQuantity"), i("bountyNormalMaxQuantity")), 100, s("bountyNormalPool"),
            (int) i("bountyAdvancedPerWeekMin"), (int) Math.max(i("bountyAdvancedPerWeekMin"), i("bountyAdvancedPerWeekMax")),
            (int) i("bountyAdvancedDays"), d("bountyAdvancedMultiplier"), d("bountyAdvancedPlayerShare"), s("bountyAdvancedPool"),
            d("bountySpecialChancePerDay"), (int) i("bountySpecialMaxOpen"), (int) i("bountySpecialDays"), s("bountySpecialPool"),
            i("bountyIssuePerWeekNormal"), i("bountyIssuePerWeekAdvanced"), i("bountyIssuePerWeekSpecial"), i("bountyUuPerWeek"),
            i("bountyTreasuryLow"), seed);
    }

    /** Lines for {@code /mio_icif bounty settings}: key, value now and where it comes from. */
    public static List<String> describe() {
        List<String> out = new ArrayList<>();
        for (Key key : KEYS.values()) {
            String value = switch (key.type()) {
                case 'i' -> key.config() == null ? "" : Long.toString(i(key.name()));
                case 'd' -> key.config() == null ? "" : Double.toString(d(key.name()));
                case 'b' -> Boolean.toString(b(key.name()));
                default -> s(key.name());
            };
            out.add(key.name() + " = " + value + " (" + origin(key.name()) + ")");
        }
        return out;
    }
}
