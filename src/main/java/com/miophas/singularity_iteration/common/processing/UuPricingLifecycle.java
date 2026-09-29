// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import com.miophas.singularity_iteration.core.runtime.uu.IndependentUuValueIndex;
import com.miophas.singularity_iteration.core.runtime.uu.UuProcessingFamily;
import com.miophas.singularity_iteration.core.runtime.uu.UuProcessingPolicy;
import com.miophas.singularity_iteration.core.runtime.uu.UuQuoteBook;
import com.miophas.singularity_iteration.core.runtime.uu.UuRecipeSolver;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** Publishes one immutable generation after startup/reload; no work on player joins or machine ticks. */
public final class UuPricingLifecycle {
    private static final ResourceLocation CATALOG = ResourceLocation.fromNamespaceAndPath("mio_icif", "uu/observed_1122.json");
    private static final ResourceLocation MAPPED = ResourceLocation.fromNamespaceAndPath("mio_icif", "uu/mapped_ic2.json");
    private static final ResourceLocation PROCESSING = ResourceLocation.fromNamespaceAndPath("mio_icif", "uu/processing_policy.json");
    private static final ResourceLocation SCAN_ELIGIBILITY = ResourceLocation.fromNamespaceAndPath("mio_icif", "uu/scan_eligibility.json");
    private static final Map<MinecraftServer, Report> REPORTS = new WeakHashMap<>();
    private static final Map<MinecraftServer, UuReferenceCatalog.MigrationCache> MAPPINGS = new WeakHashMap<>();
    private static boolean installed;
    public record Report(long generation, int reference, int denied, int derived, int unmapped,
                         int unsupportedRecipes, int work, long migratedItems, long catalogMillis, long solveMillis, String status) { }
    private UuPricingLifecycle() { }

    public static synchronized void install() {
        if (installed) return;
        NeoForge.EVENT_BUS.addListener(UuPricingLifecycle::started);
        NeoForge.EVENT_BUS.addListener(UuPricingLifecycle::reloaded);
        NeoForge.EVENT_BUS.addListener(UuPricingLifecycle::stopping);
        installed = true;
    }
    private static void started(ServerStartedEvent event) { rebuild(event.getServer()); }
    private static void reloaded(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) rebuild(event.getPlayerList().getServer());
    }
    private static void stopping(ServerStoppingEvent event) {
        UuQuoteBook.remove(event.getServer());
        synchronized (REPORTS) { REPORTS.remove(event.getServer()); }
        synchronized (MAPPINGS) { MAPPINGS.remove(event.getServer()); }
    }
    public static Report report(MinecraftServer server) {
        if (server == null || !server.isSameThread()) return null;
        synchronized (REPORTS) { return REPORTS.get(server); }
    }

    public static void rebuild(MinecraftServer server) {
        if (server == null || !server.isSameThread()) throw new IllegalStateException("UU reload must run on server thread");
        try {
            long started = System.nanoTime();
            var resource = server.getResourceManager().getResource(CATALOG)
                    .orElseThrow(() -> new IOException("Missing observed UU catalog " + CATALOG));
            UuReferenceCatalog.MigrationCache mappings;
            synchronized (MAPPINGS) {
                mappings = MAPPINGS.get(server);
                if (mappings == null || !mappings.matches(server.registryAccess())) {
                    mappings = new UuReferenceCatalog.MigrationCache(server.registryAccess());
                    MAPPINGS.put(server, mappings);
                }
            }
            long priorMigrations = mappings.migrations();
            UuReferenceCatalog.Catalog reference;
            UuMappedCatalog.ReviewedCatalog mapped;
            try (var reader = resource.openAsReader()) { reference = UuReferenceCatalog.read(reader, mappings); }
            try (var reader = server.getResourceManager().getResource(MAPPED)
                    .orElseThrow(() -> new IOException("Missing mapped UU catalog")).openAsReader()) {
                mapped = UuMappedCatalog.readReviewed(reader, server.registryAccess());
                reference = merge(reference, mapped.catalog());
            }
            UuProcessingFamily.PricingPolicy processingPolicy;
            try (var reader = server.getResourceManager().getResource(PROCESSING)
                    .orElseThrow(() -> new IOException("Missing processing UU policy")).openAsReader()) {
                processingPolicy = UuProcessingPolicy.read(reader);
            }
            java.util.Set<IndependentUuValueIndex.Key> scanEligible;
            try (var reader = server.getResourceManager().getResource(SCAN_ELIGIBILITY)
                    .orElseThrow(() -> new IOException("Missing explicit scanner eligibility")).openAsReader()) {
                scanEligible = UuScanEligibility.read(reader, server.registryAccess(), reference.denied());
            }
            long catalogNanos = System.nanoTime() - started;
            long solveStarted = System.nanoTime();
            var recipes = UuCraftingRecipes.read(server.getRecipeManager(), server.registryAccess());
            var processing = UuProcessingRecipes.read(server.getRecipeManager(), server.registryAccess(), processingPolicy);
            var rules = new ArrayList<>(recipes.rules());
            rules.addAll(processing.rules());
            // A vanilla recipe's result may be a charged default. An observed empty
            // prototype does not authorize its charge or other unseen components.
            int componentScopedRecipes = rules.size();
            rules.removeIf(rule -> !mapped.allowsDerivedOutput(rule.output()));
            componentScopedRecipes -= rules.size();
            var solved = new UuRecipeSolver<IndependentUuValueIndex.Key>(16384, 2000000)
                    .solve(reference.prices(), reference.denied(), rules);
            if (!solved.complete()) throw new IllegalStateException("UU resolver exhausted bounded work; partial quotes not published");
            var index = new IndependentUuValueIndex(16384);
            for (var row : solved.values().entrySet())
                if (!index.register(row.getKey(), row.getValue())) throw new IllegalStateException("Invalid resolved quote");
            long generation = UuQuoteBook.install(server, index, reference.denied(), scanEligible);
            var result = new Report(generation, reference.prices().size(), reference.denied().size(), solved.derived(),
                    reference.unmapped(), recipes.unsupported() + processing.unsupported() + processing.policyDisabled() + componentScopedRecipes,
                    solved.work(), mappings.migrations() - priorMigrations,
                    catalogNanos / 1000000, (System.nanoTime() - solveStarted) / 1000000, "LOADED_SCOPED_REFERENCE_AND_RECIPES");
            synchronized (REPORTS) { REPORTS.put(server, result); }
            LogUtils.getLogger().info("[UU] Published {} reference and {} derived quotes; {} denied, {} unmapped, {} unsupported recipes",
                    result.reference(), result.derived(), result.denied(), result.unmapped(), result.unsupportedRecipes());
        } catch (Exception failure) {
            // A failed new generation must not keep old prices valid against changed recipes.
            UuQuoteBook.remove(server);
            synchronized (REPORTS) { REPORTS.put(server, new Report(-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, "FAILED_NO_QUOTES")); }
            LogUtils.getLogger().error("[UU] Price generation failed; replication paused without debiting", failure);
        }
    }

    private static UuReferenceCatalog.Catalog merge(UuReferenceCatalog.Catalog first, UuReferenceCatalog.Catalog second) {
        var prices = new HashMap<>(first.prices());
        var denied = new HashSet<>(first.denied());
        for (var row : second.prices().entrySet()) {
            if (denied.contains(row.getKey())) throw new IllegalArgumentException("Mapped quote conflicts with denial");
            var previous = prices.putIfAbsent(row.getKey(), row.getValue());
            if (previous != null && Math.abs(previous - row.getValue()) > Math.max(previous, row.getValue()) * 1e-12)
                throw new IllegalArgumentException("Conflicting mapped reference quotes");
        }
        for (var key : second.denied()) {
            if (prices.containsKey(key)) throw new IllegalArgumentException("Mapped denial conflicts with finite quote");
            denied.add(key);
        }
        return new UuReferenceCatalog.Catalog(Map.copyOf(prices), java.util.Set.copyOf(denied),
                first.unmapped() + second.unmapped(), first.entries() + second.entries());
    }
}