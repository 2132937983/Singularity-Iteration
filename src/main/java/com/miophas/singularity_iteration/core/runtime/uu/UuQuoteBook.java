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
                            Set<IndependentUuValueIndex.Key> denied, Set<IndependentUuValueIndex.Key> scanEligible) { }
    private UuQuoteBook() { }

    /** A reviewed recipe/config resolver publishes one bounded generation after reload. */
    public static synchronized long install(MinecraftServer server, IndependentUuValueIndex prices) {
        return install(server, prices, Set.of(), Set.of());
    }
    public static synchronized long install(MinecraftServer server, IndependentUuValueIndex prices,
            Set<IndependentUuValueIndex.Key> denied, Set<IndependentUuValueIndex.Key> scanEligible) {
        if (server == null || !server.isSameThread() || prices == null || denied == null || scanEligible == null)
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
        var snapshot = new Snapshot(generation, Map.copyOf(quoted), exclusions, eligible);
        nextGeneration = snapshot.generation(); SERVERS.put(server, snapshot); return nextGeneration;
    }
    public static long generation(MinecraftServer server) {
        if (server == null || !server.isSameThread()) return -1;
        var snapshot = SERVERS.get(server); return snapshot == null ? -1 : snapshot.generation();
    }
    public static Quote quote(MinecraftServer server, ItemStack item) {
        if (server == null || !server.isSameThread() || item == null || item.isEmpty()) return null;
        var snapshot = SERVERS.get(server); if (snapshot == null) return null;
        return snapshot.prices().get(IndependentUuValueIndex.keyOf(item));
    }
    public static Assessment classify(MinecraftServer server, ItemStack item) {
        if (server == null || !server.isSameThread()) return new Assessment(Disposition.UNAVAILABLE, null, -1, false);
        var snapshot = SERVERS.get(server);
        if (snapshot == null) return new Assessment(Disposition.UNAVAILABLE, null, -1, false);
        long generation = snapshot.generation();
        if (item == null || item.isEmpty()) return new Assessment(Disposition.UNSUPPORTED, null, generation, false);
        var key = IndependentUuValueIndex.keyOf(item); var quote = snapshot.prices().get(key);
        if (quote != null) return new Assessment(Disposition.FINITE, quote, generation, false);
        if (snapshot.denied().contains(key))
            return new Assessment(Disposition.KNOWN_DENIED, null, generation, snapshot.scanEligible().contains(key));
        return new Assessment(Disposition.UNSUPPORTED, null, generation, false);
    }
    public static void remove(MinecraftServer server) {
        if (server != null && server.isSameThread()) SERVERS.remove(server);
    }
}
