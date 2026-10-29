// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.uu;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/** Server-owned, immutable price generations; stored crystal prices cannot grant authority. */
public final class UuQuoteBook {
    /*
     * A quote is read from machine ticks far more often than a generation is
     * published.  Keep the published snapshots in a concurrent map so reads
     * do not contend on a global monitor.  ServerStoppingEvent calls remove()
     * and is the ownership boundary for the strong server key; retaining a
     * WeakHashMap here would make every read perform a potentially mutating
     * weak-key lookup and would still require synchronization around reloads.
     */
    private static final Map<MinecraftServer, Snapshot> SERVERS = new ConcurrentHashMap<>();
    /**
     * 0.1.10.36: prices the Scanner worked out for items without a published price (other mods' items), by item id. They
     * answer {@link #quote} and {@link #classify} only for items the published generation neither prices nor denies, and
     * carry the current generation, so the Replicator and the Pattern Storage use them as before. Saved by the Scanner.
     */
    private static final Map<MinecraftServer, Map<IndependentUuValueIndex.Key, Double>> SCANNED = new ConcurrentHashMap<>();
    private static long nextGeneration;
    public record Quote(double buckets, long generation) { }
    /** KNOWN_DENIED means quote exclusion only; eligibility is separately observed. */
    public enum Disposition { FINITE, KNOWN_DENIED, UNSUPPORTED, UNAVAILABLE }
    public record Assessment(Disposition disposition, Quote finite, long generation, boolean deniedScanEligible) {
        public Assessment {
            if (disposition == null || (disposition == Disposition.UNAVAILABLE ? generation != -1 : generation <= 0)
                    || (disposition == Disposition.FINITE) != (finite != null)
                    || finite != null && (finite.generation() != generation || !UuScanResult.validCosts(finite.buckets(), 0))
                    || deniedScanEligible && disposition != Disposition.KNOWN_DENIED)
                throw new IllegalArgumentException("Inconsistent UU assessment");
        }
    }
    private record Snapshot(long generation, Map<IndependentUuValueIndex.Key, Quote> prices,
                            Set<IndependentUuValueIndex.Key> denied, Set<IndependentUuValueIndex.Key> scanEligible,
                            Map<IndependentUuValueIndex.Key, Long> energy) { }
    private UuQuoteBook() { }

    /** A reviewed recipe/config resolver publishes one bounded generation after reload. */
    public static synchronized long install(MinecraftServer server, IndependentUuValueIndex prices) {
        return install(server, prices, Set.of(), Set.of());
    }
    public static synchronized long install(MinecraftServer server, IndependentUuValueIndex prices,
            Set<IndependentUuValueIndex.Key> denied, Set<IndependentUuValueIndex.Key> scanEligible) {
        return install(server, prices, denied, scanEligible, Map.of());
    }

    /**
     * 0.1.10.5: the same with the EU per copy of the items that a data-pack replication list
     * gives an energy cost ({@code uu_replication}); the other items use the replicator's
     * standard rate. Every key of {@code energy} must have a finite price.
     */
    public static synchronized long install(MinecraftServer server, IndependentUuValueIndex prices,
            Set<IndependentUuValueIndex.Key> denied, Set<IndependentUuValueIndex.Key> scanEligible,
            Map<IndependentUuValueIndex.Key, Long> energy) {
        if (server == null || !server.isSameThread() || prices == null || denied == null || scanEligible == null || energy == null)
            throw new IllegalArgumentException("Server-owned bounded prices required");
        var entries=prices.snapshot();
        if(entries.size()>16384)throw new IllegalArgumentException("Price snapshot too large");
        var exclusions = Set.copyOf(denied); var eligible = Set.copyOf(scanEligible);
        if (entries.size() + exclusions.size() > 16384 || !exclusions.containsAll(eligible)
                || exclusions.stream().anyMatch(entries::containsKey))
            throw new IllegalArgumentException("Inconsistent bounded UU generation");
        long generation = Math.incrementExact(nextGeneration);
        var quoted = new HashMap<IndependentUuValueIndex.Key, Quote>(entries.size());
        for (var entry : entries.entrySet()) {
            if (!UuScanResult.validCosts(entry.getValue(), 0))
                throw new IllegalArgumentException("Invalid finite UU price");
            quoted.put(entry.getKey(), new Quote(entry.getValue(), generation));
        }
        for (var row : energy.entrySet())
            if (!quoted.containsKey(row.getKey()) || row.getValue() == null || row.getValue() <= 0)
                throw new IllegalArgumentException("Energy cost without a finite UU price");
        var snapshot = new Snapshot(generation, Map.copyOf(quoted), exclusions, eligible, Map.copyOf(energy));
        nextGeneration = snapshot.generation(); SERVERS.put(server, snapshot); return nextGeneration;
    }

    /** 0.1.10.5: EU per copy of {@code item} from a replication list, or -1 for the standard rate. */
    public static long energy(MinecraftServer server, ItemStack item) {
        if (server == null || !server.isSameThread() || item == null || item.isEmpty()) return -1;
        var snapshot = SERVERS.get(server); if (snapshot == null) return -1;
        Long eu = snapshot.energy().get(IndependentUuValueIndex.keyOf(item));
        return eu == null ? -1 : eu;
    }
    public static long generation(MinecraftServer server) {
        if (server == null || !server.isSameThread()) return -1;
        var snapshot = SERVERS.get(server); return snapshot == null ? -1 : snapshot.generation();
    }
    public static Quote quote(MinecraftServer server, ItemStack item) {
        if (server == null || !server.isSameThread() || item == null || item.isEmpty()) return null;
        var snapshot = SERVERS.get(server); if (snapshot == null) return null;
        var key = IndependentUuValueIndex.keyOf(item);
        var quote = snapshot.prices().get(key);
        return quote != null ? quote : scannedQuote(server, snapshot, key);
    }

    /** 0.1.10.36: a Scanner price for {@code key} when the generation neither prices nor denies it. */
    private static Quote scannedQuote(MinecraftServer server, Snapshot snapshot, IndependentUuValueIndex.Key key) {
        if (snapshot.denied().contains(key)) return null;
        var scanned = SCANNED.get(server);
        Double buckets = scanned == null ? null : scanned.get(key);
        return buckets == null ? null : new Quote(buckets, snapshot.generation());
    }

    /** 0.1.10.36: only the published generation's price (no Scanner price): the market's fundamental value. */
    public static Quote publishedQuote(MinecraftServer server, ItemStack item) {
        if (server == null || !server.isSameThread() || item == null || item.isEmpty()) return null;
        var snapshot = SERVERS.get(server); if (snapshot == null) return null;
        return snapshot.prices().get(IndependentUuValueIndex.keyOf(item));
    }

    /** 0.1.10.36: the published prices of this generation (the Scanner's fixed points), empty when none is published. */
    public static Map<IndependentUuValueIndex.Key, Double> publishedPrices(MinecraftServer server) {
        if (server == null || !server.isSameThread()) return Map.of();
        var snapshot = SERVERS.get(server); if (snapshot == null) return Map.of();
        var out = new HashMap<IndependentUuValueIndex.Key, Double>(snapshot.prices().size());
        snapshot.prices().forEach((key, quote) -> out.put(key, quote.buckets()));
        return out;
    }

    /** 0.1.10.36: the items this generation denies (never priced by the Scanner either). */
    public static Set<IndependentUuValueIndex.Key> publishedDenied(MinecraftServer server) {
        if (server == null || !server.isSameThread()) return Set.of();
        var snapshot = SERVERS.get(server); return snapshot == null ? Set.of() : snapshot.denied();
    }

    /** 0.1.10.36: records a Scanner price for {@code item} (its default form); false when it is invalid or the item is published. */
    public static boolean registerScanned(MinecraftServer server, ItemStack item, double buckets) {
        if (server == null || !server.isSameThread() || item == null || item.isEmpty() || !UuScanResult.validCosts(buckets, 0)) return false;
        var key = IndependentUuValueIndex.keyOf(item);
        var snapshot = SERVERS.get(server);
        if (snapshot != null && (snapshot.prices().containsKey(key) || snapshot.denied().contains(key))) return false;
        SCANNED.computeIfAbsent(server, ignored -> new ConcurrentHashMap<>()).put(key, buckets);
        return true;
    }

    /** 0.1.10.36: forgets a Scanner price (admin command); true when there was one. */
    public static boolean forgetScanned(MinecraftServer server, ItemStack item) {
        if (server == null || !server.isSameThread() || item == null || item.isEmpty()) return false;
        var scanned = SCANNED.get(server);
        return scanned != null && scanned.remove(IndependentUuValueIndex.keyOf(item)) != null;
    }

    /** 0.1.10.36: true when {@code item} is priced by the Scanner only (not by the published generation). */
    public static boolean isScanned(MinecraftServer server, ItemStack item) {
        if (server == null || !server.isSameThread() || item == null || item.isEmpty()) return false;
        var snapshot = SERVERS.get(server); if (snapshot == null) return false;
        return scannedQuote(server, snapshot, IndependentUuValueIndex.keyOf(item)) != null
            && !snapshot.prices().containsKey(IndependentUuValueIndex.keyOf(item));
    }
    public static Assessment classify(MinecraftServer server, ItemStack item) {
        if (server == null || !server.isSameThread()) return new Assessment(Disposition.UNAVAILABLE, null, -1, false);
        var snapshot = SERVERS.get(server);
        if (snapshot == null) return new Assessment(Disposition.UNAVAILABLE, null, -1, false);
        long generation = snapshot.generation();
        if (item == null || item.isEmpty()) return new Assessment(Disposition.UNSUPPORTED, null, generation, false);
        var key = IndependentUuValueIndex.keyOf(item); var quote = snapshot.prices().get(key);
        if (quote == null) quote = scannedQuote(server, snapshot, key);
        if (quote != null) return new Assessment(Disposition.FINITE, quote, generation, false);
        if (snapshot.denied().contains(key))
            return new Assessment(Disposition.KNOWN_DENIED, null, generation, snapshot.scanEligible().contains(key));
        return new Assessment(Disposition.UNSUPPORTED, null, generation, false);
    }
    public static void remove(MinecraftServer server) {
        if (server != null && server.isSameThread()) SERVERS.remove(server);
    }

    /** 0.1.10.36: drops the Scanner prices of a stopping server (they live in its save, not here). */
    public static void removeScanned(MinecraftServer server) {
        if (server != null) SCANNED.remove(server);
    }
}
