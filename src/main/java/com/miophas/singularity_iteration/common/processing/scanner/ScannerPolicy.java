// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing.scanner;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DebugStickItem;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

/**
 * 0.1.10.36: the datapack settings of the Scanner's pricing (reloaded with the recipes).
 *
 * <pre>{@code
 * data/<ns>/uu/scanner/excluded_namespaces.json   merged over all packs: { "namespaces": ["botania", "ars_*", ...] }
 *                                                  ("ars_*" = every namespace starting with "ars_"; "replace": true drops lower packs)
 * data/mio_icif/uu/scanner/estimate_policy.json  the top pack wins: { "item_base": 0.1, "rarity": [1, 3, 10, 30],
 *     "stack": {"64": 1, "16": 2, "1": 5}, "block_tiers": [none, stone, iron, diamond, netherite], "ore_factor": 3,
 *     "hardness_min": 0.5, "hardness_max": 3 }   (buckets of UU-matter)
 * }</pre>
 * Creative-only items (spawn eggs, command and structure blocks, the debug stick, barrier, light, items tagged
 * {@code c:hidden_from_recipe_viewers}) and unbreakable blocks are never priced.
 */
public record ScannerPolicy(Set<String> namespaces, List<String> prefixes, double itemBase, double[] rarity, double stack64,
                            double stack16, double stack1, double[] blockTiers, double oreFactor, double hardnessMin, double hardnessMax) {
    private static final TagKey<Item> HIDDEN = TagKey.create(Registries.ITEM, ResourceLocation.parse("c:hidden_from_recipe_viewers"));
    private static final ResourceLocation EXCLUDED = ResourceLocation.fromNamespaceAndPath("mio_icif", "uu/scanner/excluded_namespaces.json");
    private static final ResourceLocation ESTIMATES = ResourceLocation.fromNamespaceAndPath("mio_icif", "uu/scanner/estimate_policy.json");

    /** The defaults when a pack gives no estimate policy (the shipped file holds the same values). */
    static ScannerPolicy defaults(Set<String> namespaces, List<String> prefixes) {
        return new ScannerPolicy(namespaces, prefixes, 0.1, new double[]{1, 3, 10, 30}, 1, 2, 5,
            new double[]{0.0002, 0.0011, 0.006, 0.03, 0.3}, 3, 0.5, 3);
    }

    /** True when the Scanner never prices {@code item}. */
    public boolean excluded(Item item) {
        var id = BuiltInRegistries.ITEM.getKey(item);
        if (namespaces.contains(id.getNamespace())) return true;
        for (String prefix : prefixes) if (id.getNamespace().startsWith(prefix)) return true;
        if (item instanceof SpawnEggItem || item instanceof GameMasterBlockItem || item instanceof DebugStickItem
                || item == Items.BARRIER || item == Items.LIGHT || item == Items.KNOWLEDGE_BOOK || item == Items.AIR) return true;
        if (item.getDefaultInstance().is(HIDDEN)) return true;
        return item instanceof BlockItem block && block.getBlock().defaultDestroyTime() < 0;
    }

    static ScannerPolicy load(ResourceManager resources) {
        Set<String> namespaces = new HashSet<>();
        List<String> prefixes = new java.util.ArrayList<>();
        for (var resource : resources.getResourceStack(EXCLUDED)) {
            try (var reader = resource.openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("replace") && json.get("replace").getAsBoolean()) { namespaces.clear(); prefixes.clear(); }
                for (JsonElement entry : json.getAsJsonArray("namespaces")) {
                    String name = entry.getAsString().trim();
                    if (name.endsWith("*")) prefixes.add(name.substring(0, name.length() - 1));
                    else if (!name.isEmpty()) namespaces.add(name);
                }
            } catch (Exception failure) {
                LogUtils.getLogger().warn("[UU Scanner] {} in {}: {}", EXCLUDED, resource.sourcePackId(), failure.toString());
            }
        }
        ScannerPolicy base = defaults(Set.copyOf(namespaces), List.copyOf(prefixes));
        var top = resources.getResource(ESTIMATES);
        if (top.isEmpty()) return base;
        try (var reader = top.get().openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            double[] rarity = numbers(json, "rarity", base.rarity());
            double[] tiers = numbers(json, "block_tiers", base.blockTiers());
            JsonObject stack = json.has("stack") ? json.getAsJsonObject("stack") : new JsonObject();
            return new ScannerPolicy(base.namespaces(), base.prefixes(), number(json, "item_base", base.itemBase()), rarity,
                number(stack, "64", base.stack64()), number(stack, "16", base.stack16()), number(stack, "1", base.stack1()), tiers,
                number(json, "ore_factor", base.oreFactor()), number(json, "hardness_min", base.hardnessMin()),
                number(json, "hardness_max", base.hardnessMax()));
        } catch (Exception failure) {
            LogUtils.getLogger().warn("[UU Scanner] {}: {}; using the defaults", ESTIMATES, failure.toString());
            return base;
        }
    }

    private static double number(JsonObject json, String key, double fallback) {
        if (!json.has(key)) return fallback;
        double value = json.get(key).getAsDouble();
        return Double.isFinite(value) && value > 0 ? value : fallback;
    }

    private static double[] numbers(JsonObject json, String key, double[] fallback) {
        if (!json.has(key)) return fallback;
        var array = json.getAsJsonArray(key);
        if (array.size() != fallback.length) return fallback;
        double[] out = new double[fallback.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsDouble();
            if (!Double.isFinite(out[i]) || out[i] <= 0) return fallback;
        }
        return out;
    }
}
