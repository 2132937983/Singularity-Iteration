// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import com.miophas.singularity_iteration.core.runtime.uu.IndependentUuValueIndex;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Explicit reviewed item identities; never guesses a mod mapping from a display name. */
public final class UuMappedCatalog {
    public static final String RESOURCE_PATH = "data/mio_icif/uu/mapped_ic2.json";
    private static final int MAX_INPUT = 4 * 1024 * 1024;
    private static final int MAX_ROWS = 16384;
    private static final Set<String> ROOT_FIELDS = Set.of("schema", "data_version", "entries");
    // Schema 4 also admits direct SI-authored rows: an explicit target and price with no legacy counterpart.
    private static final Set<String> REQUIRED_ROW_FIELDS = Set.of("target_item", "raw_value", "source_row");
    private static final Set<String> ROW_FIELDS = Set.of("legacy_stack", "target_item", "raw_value", "source_row", "target_stack");
    private record Row(String legacy, String target, String raw, int source, String stack) { }
    private record Document(int schema, List<Row> rows) { }
    /** Compound equality includes NBT types and ignores field order; retained keys are never mutated. */
    private record Legacy(CompoundTag identity) {
        private Legacy { identity = identity.copy(); }
        private boolean stateful() { return identity.contains("tag"); }
    }
    // R128's finite admission surface, from normal R93 saved identities. No wildcard profiles.
    private static final Set<String> PRISTINE_COMPONENTS = Set.of(
            "heat_storage", "tri_heat_storage", "hex_heat_storage", "advanced_heat_exchanger",
            "advanced_heat_vent", "component_heat_exchanger", "dual_mox_fuel_rod", "dual_uranium_fuel_rod",
            "mox_fuel_rod", "uranium_fuel_rod", "heat_exchanger", "heat_vent", "lzh_condensator",
            "neutron_reflector", "overclocked_heat_vent", "quad_mox_fuel_rod", "quad_uranium_fuel_rod",
            "rsh_condensator", "reactor_heat_exchanger", "reactor_heat_vent", "thick_neutron_reflector");
    private static final Set<String> CABLE_STATES = Set.of(
            "0:0", "0:1", "1:0", "2:0", "2:1", "3:0", "3:1", "4:0", "4:1", "5:0", "6:0");
    public record ReviewedCatalog(UuReferenceCatalog.Catalog catalog, Set<String> componentScopedItems) {
        public ReviewedCatalog { componentScopedItems = Set.copyOf(componentScopedItems); }
        /** Explicit component prototypes have only observed authority, not inferred variants. */
        public boolean allowsDerivedOutput(IndependentUuValueIndex.Key output) {
            return !componentScopedItems.contains(output.itemId());
        }
    }
    private UuMappedCatalog() { }

    /** The caller owns the Reader. A failed read publishes no partial catalog. */
    public static UuReferenceCatalog.Catalog read(Reader reader, HolderLookup.Provider registries) throws IOException {
        return readReviewed(reader, registries).catalog();
    }

    public static ReviewedCatalog readReviewed(Reader reader, HolderLookup.Provider registries) throws IOException {
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(registries, "registries");
        StringBuilder input = new StringBuilder();
        char[] buffer = new char[4096];
        int count;
        while ((count = reader.read(buffer)) != -1) {
            if (input.length() + count > MAX_INPUT) throw new IOException("Mapped UU catalog exceeds 4 MiB");
            input.append(buffer, 0, count);
        }
        String text = input.toString();
        if (text.getBytes(StandardCharsets.UTF_8).length > MAX_INPUT)
            throw new IOException("Mapped UU catalog exceeds 4 MiB UTF-8");
        Document document = parse(text);
        List<Row> rows = document.rows();
        var items = registries.lookupOrThrow(Registries.ITEM);
        Map<IndependentUuValueIndex.Key, Double> prices = new HashMap<>();
        Set<IndependentUuValueIndex.Key> denied = new HashSet<>();
        Set<Legacy> legacyIdentities = new HashSet<>();
        Set<ResourceLocation> targets = new HashSet<>();
        Set<Integer> sourceRows = new HashSet<>();
        Set<IndependentUuValueIndex.Key> identities = new HashSet<>();
        Set<String> componentScopedItems = new HashSet<>();
        int unmapped = 0;
        for (Row row : rows) {
            Legacy legacy = row.legacy() == null ? null
                    : document.schema() >= 3 ? statefulLegacy(row.legacy()) : legacy(row.legacy());
            if (legacy != null && legacy.stateful() && row.stack() == null)
                throw new IllegalArgumentException("Stateful legacy identity requires an explicit reviewed target prototype");
            ResourceLocation target = explicitId(row.target(), "mio_icif");
            if (row.stack() != null) { savedStack(row.stack(), target); componentScopedItems.add(target.toString()); }
            boolean infinite = row.raw().equals("Infinity");
            double buckets = infinite ? 0 : price(row.raw());
            if ((legacy != null && !legacyIdentities.add(legacy)) || !targets.add(target) || !sourceRows.add(row.source()))
                throw new IllegalArgumentException("Duplicate mapped UU identity, target, or source row");
            var holder = items.get(ResourceKey.create(Registries.ITEM, target));
            if (holder.isEmpty()) { unmapped++; continue; }
            ItemStack item = row.stack() == null ? new ItemStack(holder.orElseThrow().value())
                    : explicitStack(row.stack(), row.target(), registries);
            if (item.isEmpty() || item.getCount() != 1 || item.getMaxStackSize() < 1) {
                unmapped++;
                continue;
            }
            if (ItemStack.STRICT_SINGLE_ITEM_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), item).error().isPresent()) {
                unmapped++;
                continue;
            }
            var key = IndependentUuValueIndex.keyOf(item);
            if (!identities.add(key)) throw new IllegalArgumentException("Multiple targets resolve to one mapped UU item");
            if (infinite) denied.add(key);
            else prices.put(key, buckets);
        }
        return new ReviewedCatalog(new UuReferenceCatalog.Catalog(Map.copyOf(prices), Set.copyOf(denied), unmapped, rows.size()), componentScopedItems);
    }

    @SuppressWarnings("deprecation") // 2.10 and 2.11 Gson both support this strict-reader switch.
    private static Document parse(String text) throws IOException {
        try (JsonReader json = new JsonReader(new StringReader(text))) {
            json.setLenient(false);
            Set<String> fields = new HashSet<>();
            int schema = -1;
            int version = -1;
            List<Row> rows = new ArrayList<>();
            json.beginObject();
            while (json.hasNext()) {
                String name = json.nextName();
                if (!ROOT_FIELDS.contains(name) || !fields.add(name))
                    throw new IllegalArgumentException("Unknown or duplicate mapped UU root field");
                switch (name) {
                    case "schema" -> schema = integer(json);
                    case "data_version" -> version = integer(json);
                    case "entries" -> {
                        json.beginArray();
                        while (json.hasNext()) {
                            if (rows.size() >= MAX_ROWS) throw new IllegalArgumentException("Mapped UU catalog exceeds row limit");
                            rows.add(row(json));
                        }
                        json.endArray();
                    }
                    default -> throw new IllegalArgumentException("Unknown mapped UU root field");
                }
            }
            json.endObject();
            if (!fields.equals(ROOT_FIELDS) || schema < 1 || schema > 4 || version != 1343 || json.peek() != JsonToken.END_DOCUMENT
                    || schema == 1 && rows.stream().anyMatch(row -> row.stack() != null)
                    || schema < 4 && rows.stream().anyMatch(row -> row.legacy() == null))
                throw new IllegalArgumentException("Unknown or incomplete mapped UU catalog schema");
            return new Document(schema, List.copyOf(rows));
        } catch (IllegalStateException malformed) {
            throw new IllegalArgumentException("Invalid mapped UU catalog structure", malformed);
        }
    }

    private static Row row(JsonReader json) throws IOException {
        Set<String> fields = new HashSet<>();
        String legacy = null;
        String target = null;
        String raw = null;
        String stack = null;
        int source = -1;
        json.beginObject();
        while (json.hasNext()) {
            String name = json.nextName();
            if (!ROW_FIELDS.contains(name) || !fields.add(name))
                throw new IllegalArgumentException("Unknown or duplicate mapped UU entry field");
            switch (name) {
                case "legacy_stack" -> legacy = string(json, 8192);
                case "target_item" -> target = string(json, 256);
                case "raw_value" -> raw = string(json, 128);
                case "source_row" -> source = integer(json);
                case "target_stack" -> stack = string(json, 8192);
                default -> throw new IllegalArgumentException("Unknown mapped UU entry field");
            }
        }
        json.endObject();
        if (!fields.containsAll(REQUIRED_ROW_FIELDS)) throw new IllegalArgumentException("Incomplete mapped UU entry");
        return new Row(legacy, target, raw, source, stack);
    }

    /** An explicit saved prototype retains the complete registered component identity. */
    public static ItemStack explicitStack(String saved, String target, HolderLookup.Provider registries) {
        var tag = savedStack(saved, explicitId(target, "mio_icif"));
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        try {
            var stack = ItemStack.STRICT_SINGLE_ITEM_CODEC.parse(ops, tag).getOrThrow();
            var canonicalTag = ItemStack.STRICT_SINGLE_ITEM_CODEC.encodeStart(ops, stack).getOrThrow();
            var canonical = ItemStack.STRICT_SINGLE_ITEM_CODEC.parse(ops, canonicalTag).getOrThrow();
            // The codec deliberately omits component patches that equal the registered
            // item defaults. Compare the decoded full identity, not the saved patch's
            // wire spelling, while the strict parser and savedStack still reject unknown
            // fields, component ids, component types, item ids, and non-single counts.
            if (stack.isEmpty() || stack.getCount() != 1 || canonical.getCount() != 1
                    || !ItemStack.isSameItemSameComponents(stack, canonical))
                throw new IllegalArgumentException("Explicit mapped stack loses or normalizes saved fields");
            return stack;
        } catch (IllegalStateException malformed) {
            throw new IllegalArgumentException("Invalid explicit mapped component identity", malformed);
        }
    }

    private static net.minecraft.nbt.CompoundTag savedStack(String saved, ResourceLocation target) {
        if (saved == null || saved.length() > 8192)
            throw new IllegalArgumentException("Explicit mapped stack exceeds limit");
        try {
            var tag = TagParser.parseTag(saved);
            if (!tag.getAllKeys().equals(Set.of("id", "count", "components"))
                    || !tag.contains("id", Tag.TAG_STRING) || !tag.getString("id").equals(target.toString())
                    || !tag.contains("count", Tag.TAG_INT) || tag.getInt("count") != 1
                    || !tag.contains("components", Tag.TAG_COMPOUND))
                throw new IllegalArgumentException("Explicit one-item prototype with exact ID and component patch required");
            return tag;
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException malformed) {
            throw new IllegalArgumentException("Invalid explicit mapped stack", malformed);
        }
    }

    private static String string(JsonReader json, int limit) throws IOException {
        if (json.peek() != JsonToken.STRING) throw new IllegalArgumentException("Mapped UU field must be a string");
        String value = json.nextString();
        if (value.length() > limit) throw new IllegalArgumentException("Mapped UU string exceeds limit");
        return value;
    }

    private static int integer(JsonReader json) throws IOException {
        if (json.peek() != JsonToken.NUMBER) throw new IllegalArgumentException("Mapped UU field must be an integer");
        String value = json.nextString();
        if (value.length() > 10 || !value.matches("0|[1-9][0-9]*"))
            throw new IllegalArgumentException("Invalid mapped UU nonnegative integer");
        return Integer.parseInt(value);
    }

    private static ResourceLocation explicitId(String value, String namespace) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null || !id.getNamespace().equals(namespace) || !id.toString().equals(value))
            throw new IllegalArgumentException("Unexpected mapped UU item namespace or noncanonical ID");
        return id;
    }

    /** Deliberately accepts only flat, stateless legacy stacks; no NBT field can be ignored. */
    private static Legacy legacy(String saved) {
        if (saved.length() > 8192) throw new IllegalArgumentException("Mapped UU legacy NBT exceeds limit");
        String text = saved.trim();
        if (!text.startsWith("{") || !text.endsWith("}")) throw new IllegalArgumentException("Invalid mapped UU legacy NBT");
        String[] parts = text.substring(1, text.length() - 1).split(",", -1);
        if (parts.length < 3 || parts.length > 4) throw new IllegalArgumentException("Unsupported mapped UU legacy fields");
        Set<String> fields = new HashSet<>();
        ResourceLocation id = null;
        short damage = -1;
        for (String part : parts) {
            int colon = part.indexOf(':');
            if (colon <= 0) throw new IllegalArgumentException("Invalid mapped UU legacy field");
            String name = unquote(part.substring(0, colon).trim());
            String value = part.substring(colon + 1).trim();
            if (!fields.add(name)) throw new IllegalArgumentException("Duplicate mapped UU legacy field");
            switch (name) {
                case "id" -> id = explicitId(unquote(value), "ic2");
                case "Count" -> {
                    if (!value.equals("1b") && !value.equals("1B"))
                        throw new IllegalArgumentException("Mapped UU reference must contain exactly one byte-count item");
                }
                case "Damage" -> {
                    if (!value.matches("[0-9]{1,5}[sS]")) throw new IllegalArgumentException("Mapped UU metadata must be a nonnegative short");
                    int number = Integer.parseInt(value.substring(0, value.length() - 1));
                    if (number > Short.MAX_VALUE) throw new IllegalArgumentException("Mapped UU metadata exceeds short range");
                    damage = (short) number;
                }
                case "tag" -> {
                    if (!value.matches("\\{\\s*\\}")) throw new IllegalArgumentException("Mapped UU legacy item has unresolved NBT state");
                }
                default -> throw new IllegalArgumentException("Unknown mapped UU legacy field");
            }
        }
        if (!fields.containsAll(Set.of("id", "Count", "Damage")) || id == null || damage < 0)
            throw new IllegalArgumentException("Incomplete mapped UU legacy identity");
        try {
            var parsed = TagParser.parseTag(saved);
            if (!parsed.contains("id", Tag.TAG_STRING) || !parsed.contains("Count", Tag.TAG_BYTE)
                    || !parsed.contains("Damage", Tag.TAG_SHORT) || !parsed.getString("id").equals(id.toString())
                    || parsed.getByte("Count") != 1 || parsed.getShort("Damage") != damage)
                throw new IllegalArgumentException("Mapped UU legacy grammar does not describe the parsed item");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException malformed) {
            throw new IllegalArgumentException("Invalid mapped UU saved-item SNBT", malformed);
        }
        var identity = new CompoundTag();
        identity.putString("id", id.toString()); identity.putByte("Count", (byte) 1); identity.putShort("Damage", damage);
        return new Legacy(identity);
    }

    /** Schema 3 adds only the exact pristine-component/cable profiles observed in ordinary saved stacks. */
    private static Legacy statefulLegacy(String saved) {
        if (saved.length() > 8192) throw new IllegalArgumentException("Mapped UU legacy NBT exceeds limit");
        // TagParser overwrites duplicate compound keys. Detect them before giving it any input.
        Map<String, String> fields = new LegacySyntax(saved).read();
        if (!fields.keySet().equals(Set.of("id", "Count", "Damage", "tag"))) return legacy(saved);
        if (!fields.get("Count").matches("1[bB]") || !fields.get("Damage").matches("[0-9]{1,5}[sS]"))
            throw new IllegalArgumentException("Legacy count/metadata must retain their byte/short grammar");
        try {
            var identity = TagParser.parseTag(saved);
            if (!identity.contains("id", Tag.TAG_STRING) || !identity.contains("Count", Tag.TAG_BYTE)
                    || !identity.contains("Damage", Tag.TAG_SHORT) || !identity.contains("tag", Tag.TAG_COMPOUND)
                    || identity.getByte("Count") != 1 || identity.getShort("Damage") < 0)
                throw new IllegalArgumentException("Invalid typed legacy root");
            ResourceLocation id = explicitId(identity.getString("id"), "ic2");
            var state = identity.getCompound("tag");
            if (state.isEmpty()) return legacy(saved); // Preserve schema 1/2 empty-tag equivalence.
            boolean component = PRISTINE_COMPONENTS.contains(id.getPath()) && identity.getShort("Damage") == 0
                    && state.getAllKeys().equals(Set.of("advDmg"))
                    && state.contains("advDmg", Tag.TAG_INT) && state.getInt("advDmg") == 0;
            boolean cable = id.getPath().equals("cable") && state.getAllKeys().equals(Set.of("type", "insulation"))
                    && state.contains("type", Tag.TAG_BYTE) && state.contains("insulation", Tag.TAG_BYTE)
                    && identity.getShort("Damage") == state.getByte("type")
                    && CABLE_STATES.contains(state.getByte("type") + ":" + state.getByte("insulation"));
            if (!component && !cable) throw new IllegalArgumentException("Unreviewed legacy component profile");
            return new Legacy(identity);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException malformed) {
            throw new IllegalArgumentException("Invalid typed legacy saved-item SNBT", malformed);
        }
    }

    /** Small shape guard for the two scalar-only legacy profiles; TagParser remains the typed value authority. */
    private static final class LegacySyntax {
        private final String text;
        private int cursor;
        private LegacySyntax(String text) { this.text = text; }
        private Map<String, String> read() {
            var fields = compound(0); space();
            if (cursor != text.length()) throw new IllegalArgumentException("Trailing legacy NBT");
            return fields;
        }
        private Map<String, String> compound(int depth) {
            if (depth > 1) throw new IllegalArgumentException("Unsupported nested legacy state");
            expect('{'); space();
            Map<String, String> fields = new HashMap<>();
            if (at('}')) { cursor++; return fields; }
            while (true) {
                String key = key(); expect(':'); space(); int start = cursor;
                if (at('{')) compound(depth + 1);
                else if (at('"') || at('\'')) quoted();
                else {
                    while (cursor < text.length() && !Character.isWhitespace(text.charAt(cursor))
                            && !at(',') && !at('}')) {
                        char next = text.charAt(cursor++);
                        if (next == '[' || next == ']' || next == '{' || next == '\\' || next == '"' || next == '\'')
                            throw new IllegalArgumentException("Unsupported legacy scalar");
                    }
                    if (cursor == start) throw new IllegalArgumentException("Missing legacy value");
                }
                if (fields.putIfAbsent(key, text.substring(start, cursor)) != null)
                    throw new IllegalArgumentException("Duplicate legacy compound key");
                space();
                if (at('}')) { cursor++; return fields; }
                expect(','); space();
                if (at('}')) throw new IllegalArgumentException("Trailing legacy comma");
            }
        }
        private String key() {
            space();
            if (at('"') || at('\'')) return quoted();
            int start = cursor;
            while (cursor < text.length()) {
                char c = text.charAt(cursor);
                if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_')) break;
                cursor++;
            }
            if (cursor == start) throw new IllegalArgumentException("Invalid legacy key");
            return text.substring(start, cursor);
        }
        private String quoted() {
            char quote = text.charAt(cursor++); int start = cursor;
            while (cursor < text.length() && text.charAt(cursor) != quote) {
                if (text.charAt(cursor++) == '\\') throw new IllegalArgumentException("Escaped legacy tokens are outside reviewed profiles");
            }
            if (cursor == text.length()) throw new IllegalArgumentException("Unterminated legacy string");
            String value = text.substring(start, cursor); cursor++; return value;
        }
        private boolean at(char c) { return cursor < text.length() && text.charAt(cursor) == c; }
        private void space() { while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) cursor++; }
        private void expect(char c) {
            space(); if (!at(c)) throw new IllegalArgumentException("Malformed legacy compound"); cursor++;
        }
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && ((value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"')
                || (value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\'')))
            return value.substring(1, value.length() - 1);
        return value;
    }

    private static double price(String raw) {
        if (!raw.matches("(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?"))
            throw new IllegalArgumentException("Invalid mapped UU reference price");
        double buckets = Double.parseDouble(raw) / 100000.0;
        if (!StoredPattern.validCosts(buckets, 0)) throw new IllegalArgumentException("Mapped UU price is not finite and positive");
        return buckets;
    }
}
