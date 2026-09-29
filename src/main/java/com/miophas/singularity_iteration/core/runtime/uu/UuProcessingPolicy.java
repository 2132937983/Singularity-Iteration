// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.uu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

/** Strict bounded external processing-price policy; metadata never participates in arithmetic. */
public final class UuProcessingPolicy {
    private static final int MAX_BYTES = 65536;
    private static final Set<String> FIELDS = Set.of("schema", "raw_overheads", "observed_sand", "basis");
    private UuProcessingPolicy() { }

    public static UuProcessingFamily.PricingPolicy read(Reader reader) throws IOException {
        var text = new StringBuilder();
        char[] buffer = new char[4096];
        for (int count; (count = reader.read(buffer)) >= 0;) {
            text.append(buffer, 0, count);
            if (text.length() > MAX_BYTES) throw new IllegalArgumentException("Processing policy too large");
        }
        if (text.toString().getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            throw new IllegalArgumentException("Processing policy too large");
        rejectDuplicateFields(text.toString());
        var parsed = JsonParser.parseString(text.toString());
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("Processing policy object required");
        JsonObject root = parsed.getAsJsonObject();
        if (!FIELDS.containsAll(root.keySet()) || !root.has("schema") || !root.get("schema").isJsonPrimitive()
                || !root.getAsJsonPrimitive("schema").isNumber()
                || root.get("schema").getAsBigDecimal().compareTo(java.math.BigDecimal.ONE) != 0)
            throw new IllegalArgumentException("Unknown processing policy schema");
        if (!root.has("raw_overheads") || !root.get("raw_overheads").isJsonObject())
            throw new IllegalArgumentException("Processing overhead map required");
        if (root.has("basis") && (!root.get("basis").isJsonPrimitive()
                || !root.getAsJsonPrimitive("basis").isString()))
            throw new IllegalArgumentException("Processing policy basis must be text");
        boolean observed = false;
        if (root.has("observed_sand")) {
            if (!root.get("observed_sand").isJsonPrimitive() || !root.getAsJsonPrimitive("observed_sand").isBoolean())
                throw new IllegalArgumentException("Observed sand flag must be boolean");
            observed = root.get("observed_sand").getAsBoolean();
        }
        var raw = root.getAsJsonObject("raw_overheads");
        if (raw.size() > UuProcessingFamily.Family.values().length)
            throw new IllegalArgumentException("Too many processing families");
        var buckets = new EnumMap<UuProcessingFamily.Family, Double>(UuProcessingFamily.Family.class);
        for (var row : raw.entrySet()) {
            var family = UuProcessingFamily.Family.valueOf(row.getKey());
            if (!row.getValue().isJsonPrimitive() || !row.getValue().getAsJsonPrimitive().isNumber())
                throw new IllegalArgumentException("Numeric raw overhead required");
            double amount = row.getValue().getAsDouble();
            if (!Double.isFinite(amount) || amount <= 0 || amount > 100000000.0)
                throw new IllegalArgumentException("Raw overhead outside supported bounds");
            buckets.put(family, amount / 100000.0);
        }
        return new UuProcessingFamily.PricingPolicy(buckets, observed);
    }

    @SuppressWarnings("deprecation") // Available across the resolved Gson 2.10/2.11 compatibility range.
    private static void rejectDuplicateFields(String text) throws IOException {
        try (var reader = new JsonReader(new StringReader(text))) {
            reader.setLenient(false);
            if (reader.peek() != JsonToken.BEGIN_OBJECT) throw new IllegalArgumentException("Policy object required");
            reader.beginObject();
            var fields = new HashSet<String>();
            while (reader.hasNext()) {
                String field = reader.nextName();
                if (!fields.add(field)) throw new IllegalArgumentException("Duplicate policy field");
                if (field.equals("raw_overheads") && reader.peek() == JsonToken.BEGIN_OBJECT) {
                    reader.beginObject();
                    var families = new HashSet<String>();
                    while (reader.hasNext()) {
                        if (!families.add(reader.nextName())) throw new IllegalArgumentException("Duplicate processing family");
                        reader.skipValue();
                    }
                    reader.endObject();
                } else reader.skipValue();
            }
            reader.endObject();
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Trailing policy document");
        }
    }
}