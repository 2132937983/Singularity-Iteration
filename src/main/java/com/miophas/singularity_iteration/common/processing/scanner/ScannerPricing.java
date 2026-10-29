// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing.scanner;

import com.miophas.singularity_iteration.common.processing.UuCraftingRecipes;
import com.miophas.singularity_iteration.common.processing.UuProcessingRecipes;
import com.miophas.singularity_iteration.core.runtime.uu.IndependentUuValueIndex;
import com.miophas.singularity_iteration.core.runtime.uu.UuProcessingPolicy;
import com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook;
import com.miophas.singularity_iteration.core.runtime.uu.UuRecipeSolver;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

/**
 * 0.1.10.36: the Scanner's own UU pricing for items the published generation neither prices nor denies (above all other
 * mods' items). The Replicator and the published pricing ({@code UuPricingLifecycle}) are not changed: a price found here is
 * registered in {@link UuQuoteBook} as a Scanner price, which the Replicator reads as it reads any price.
 *
 * <p>Order: (1) recipes - the published prices are fixed points, inputs that no recipe makes (a mod's ores, plants, mob drops)
 * get an estimate ({@link ScannerEstimates}), and the existing recipe solver (cheapest route, cycle-safe) prices everything
 * craftable from them, so an item whose recipes hold no vanilla item at all still gets a price; (2) an estimate for an item
 * no recipe reaches. Items of the excluded namespaces (magic mods) and creative-only items are never priced. The recipe graph
 * is built once per recipe set and published generation (start, /reload), on the first scan that needs it.
 */
public final class ScannerPricing {
    private ScannerPricing() {}

    public enum Source { RECIPE, ESTIMATE }

    public record Price(double buckets, Source source) { }

    private static final ResourceLocation PROCESSING = ResourceLocation.fromNamespaceAndPath("mio_icif", "uu/processing_policy.json");

    private record Graph(RecipeManager recipes, long generation, ScannerPolicy policy, Map<IndependentUuValueIndex.Key, Double> solved,
                         Set<IndependentUuValueIndex.Key> estimatedLeaves) { }

    private static final Map<MinecraftServer, Graph> GRAPHS = new HashMap<>();

    /**
     * The price of {@code item} (its default form), or null: excluded, creative-only, denied, or nothing to estimate from.
     * Server thread only.
     */
    public static @Nullable Price price(MinecraftServer server, ItemStack item) {
        if (server == null || !server.isSameThread() || item == null || item.isEmpty()) return null;
        Graph graph = graph(server);
        if (graph.policy().excluded(item.getItem())) return null;
        var key = IndependentUuValueIndex.keyOf(item.getItem().getDefaultInstance());
        if (UuQuoteBook.publishedDenied(server).contains(key)) return null;
        Double solved = graph.solved().get(key);
        if (solved != null && solved > 0)
            return new Price(solved, graph.estimatedLeaves().contains(key) ? Source.ESTIMATE : Source.RECIPE);
        Double estimate = ScannerEstimates.estimate(item.getItem(), graph.policy(), UuQuoteBook.publishedPrices(server));
        return estimate == null ? null : new Price(estimate, Source.ESTIMATE);
    }

    /** True when the Scanner never prices {@code item} (an excluded namespace or a creative-only item). */
    public static boolean excluded(MinecraftServer server, ItemStack item) {
        return server != null && server.isSameThread() && item != null && !item.isEmpty() && graph(server).policy().excluded(item.getItem());
    }

    /** Drops the cached graph of a stopping server. */
    public static void forget(MinecraftServer server) {
        GRAPHS.remove(server);
    }

    private static Graph graph(MinecraftServer server) {
        long generation = UuQuoteBook.generation(server);
        Graph graph = GRAPHS.get(server);
        if (graph != null && graph.recipes() == server.getRecipeManager() && graph.generation() == generation) return graph;
        graph = build(server, generation);
        GRAPHS.put(server, graph);
        return graph;
    }

    private static Graph build(MinecraftServer server, long generation) {
        long started = System.nanoTime();
        ScannerPolicy policy = ScannerPolicy.load(server.getResourceManager());
        Map<IndependentUuValueIndex.Key, Double> published = UuQuoteBook.publishedPrices(server);
        Set<IndependentUuValueIndex.Key> denied = UuQuoteBook.publishedDenied(server);
        List<UuRecipeSolver.Rule<IndependentUuValueIndex.Key>> rules = new ArrayList<>();
        try {
            rules.addAll(UuCraftingRecipes.read(server.getRecipeManager(), server.registryAccess()).rules());
        } catch (RuntimeException failure) {
            LogUtils.getLogger().warn("[UU Scanner] crafting recipes skipped: {}", failure.toString());
        }
        try {
            var resource = server.getResourceManager().getResource(PROCESSING);
            if (resource.isPresent()) try (var reader = resource.get().openAsReader()) {
                rules.addAll(UuProcessingRecipes.read(server.getRecipeManager(), server.registryAccess(), UuProcessingPolicy.read(reader)).rules());
            }
        } catch (Exception failure) {
            LogUtils.getLogger().warn("[UU Scanner] machine recipes skipped: {}", failure.toString());
        }
        // An excluded or creative-only item is neither made nor used here: a recipe touching one cannot price anything.
        rules.removeIf(rule -> excludedKey(rule.output(), policy)
            || rule.slots().stream().allMatch(slot -> slot.stream().allMatch(choice -> excludedKey(choice.consumed(), policy))));
        Set<IndependentUuValueIndex.Key> outputs = new HashSet<>();
        for (var rule : rules) outputs.add(rule.output());
        // Fixed points: the published prices, and an estimate for each input that no recipe makes.
        Map<IndependentUuValueIndex.Key, Double> fixed = new HashMap<>();
        for (var row : published.entrySet()) if (!denied.contains(row.getKey())) fixed.put(row.getKey(), row.getValue());
        Set<IndependentUuValueIndex.Key> leaves = new HashSet<>();
        for (var rule : rules) for (var slot : rule.slots()) for (var choice : slot) {
            var key = choice.consumed();
            if (fixed.containsKey(key) || outputs.contains(key) || denied.contains(key) || excludedKey(key, policy)) continue;
            Item item = item(key);
            Double estimate = item == null ? null : ScannerEstimates.estimate(item, policy, published);
            if (estimate != null && estimate > 0) { fixed.put(key, estimate); leaves.add(key); }
        }
        Map<IndependentUuValueIndex.Key, Double> solved = solve(fixed, denied, rules);
        // Items made only from each other (amethyst shard <-> block) have no outside source: after a pass, every input still
        // unpriced gets an estimate and the recipes are solved again (at most three passes).
        for (int pass = 0; pass < 3; pass++) {
            boolean seeded = false;
            for (var rule : rules) for (var slot : rule.slots()) for (var choice : slot) {
                var key = choice.consumed();
                if (solved.containsKey(key) || denied.contains(key) || excludedKey(key, policy)) continue;
                Item item = item(key);
                Double estimate = item == null ? null : ScannerEstimates.conservative(item, policy, published);
                if (estimate != null && estimate > 0) { fixed.put(key, estimate); solved.put(key, estimate); leaves.add(key); seeded = true; }
            }
            if (!seeded) break;
            solved = solve(fixed, denied, rules);
        }
        // A seed is never below its own recipes: a seeded item that recipes make takes their cost when that is higher, and
        // then counts as priced from recipes (a seed set too low would let the Replicator copy it too cheaply).
        Map<IndependentUuValueIndex.Key, List<UuRecipeSolver.Rule<IndependentUuValueIndex.Key>>> producers = new HashMap<>();
        for (var rule : rules) producers.computeIfAbsent(rule.output(), ignored -> new ArrayList<>()).add(rule);
        for (int round = 0; round < 3; round++) {
            boolean raised = false;
            for (var key : List.copyOf(leaves)) {
                Double best = null;
                for (var rule : producers.getOrDefault(key, List.of())) {
                    Double cost = ruleCost(rule, solved, key);
                    if (cost != null && (best == null || cost < best)) best = cost;
                }
                if (best != null && best > fixed.get(key)) { fixed.put(key, best); leaves.remove(key); raised = true; }
            }
            if (!raised) break;
            solved = solve(fixed, denied, rules);
        }
        LogUtils.getLogger().info("[UU Scanner] {} recipes, {} published, {} estimated inputs, {} priced in {} ms",
            rules.size(), published.size(), leaves.size(), solved.size(), (System.nanoTime() - started) / 1_000_000);
        return new Graph(server.getRecipeManager(), generation, policy, Map.copyOf(solved), Set.copyOf(leaves));
    }

    /** The fixed points and what the existing recipe solver derives from them (a cut-off solve keeps what it found). */
    private static Map<IndependentUuValueIndex.Key, Double> solve(Map<IndependentUuValueIndex.Key, Double> fixed,
            Set<IndependentUuValueIndex.Key> denied, List<UuRecipeSolver.Rule<IndependentUuValueIndex.Key>> rules) {
        Map<IndependentUuValueIndex.Key, Double> solved = new HashMap<>(fixed);
        if (fixed.size() > 16384 || rules.size() > 32768) {
            LogUtils.getLogger().warn("[UU Scanner] {} fixed points / {} recipes exceed the solver bounds; scanned items are estimated",
                fixed.size(), rules.size());
            return solved;
        }
        try {
            var solution = new UuRecipeSolver<IndependentUuValueIndex.Key>(16384, 2_000_000).solve(fixed, denied, rules);
            solved.putAll(solution.values());   // each value is the cost of a real route
            if (!solution.complete()) LogUtils.getLogger().warn("[UU Scanner] recipe solve stopped at its work limit; the rest is estimated");
        } catch (RuntimeException failure) {
            LogUtils.getLogger().warn("[UU Scanner] recipe solve failed, scanned items are estimated: {}", failure.toString());
        }
        return solved;
    }

    /** One recipe's cost per output from {@code values} (the cheapest priced choice of each slot, never {@code self}), or null. */
    private static @Nullable Double ruleCost(UuRecipeSolver.Rule<IndependentUuValueIndex.Key> rule,
            Map<IndependentUuValueIndex.Key, Double> values, IndependentUuValueIndex.Key self) {
        double sum = rule.overhead();
        for (var slot : rule.slots()) {
            Double cheapest = null;
            for (var choice : slot) {
                if (choice.consumed().equals(self)) continue;
                Double value = values.get(choice.consumed());
                if (value != null && (cheapest == null || value < cheapest)) cheapest = value;
            }
            if (cheapest == null) return null;
            sum += cheapest;
        }
        return sum / rule.outputCount();
    }

    private static @Nullable Item item(IndependentUuValueIndex.Key key) {
        var id = ResourceLocation.tryParse(key.itemId());
        return id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

    private static boolean excludedKey(IndependentUuValueIndex.Key key, ScannerPolicy policy) {
        Item item = item(key);
        return item == null || policy.excluded(item);
    }
}
