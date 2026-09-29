// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.random.RandomGenerator;
import com.miophas.singularity_iteration.core.api.energy.EnergyPacketPolicy;

/** Pure accounting across separate conductor domains sharing endpoint balances. */
public final class DomainDistributor {
    private DomainDistributor() { }

    /** Full tier packets for storage, bounded residual packets for generators. */
    public record Source(long reserve, long packet, boolean partialPackets, int packetCount) {
        public Source(long reserve, long packet, boolean partialPackets) {
            this(reserve, packet, partialPackets, 1);
        }
        public Source {
            if (reserve < 0) throw new IllegalArgumentException("Invalid source reserve");
            new EnergyPacketPolicy(packet, packetCount, partialPackets);
        }
        public EnergyPacketPolicy policy() { return new EnergyPacketPolicy(packet, packetCount, partialPackets); }
    }

    /** A source and receiver ID backed by the same storage snapshot. */
    public record SharedStorage(int source, int receiver, long capacity) {
        public SharedStorage {
            if (source < 0 || receiver < 0 || capacity < 0)
                throw new IllegalArgumentException("Invalid shared storage binding");
        }
    }

    /** Sources use global IDs in registration order; route/receiver IDs are shared. */
    public static final class Domain {
        // Package access for the independently owned fractional planner. The
        // public constructor still copies every caller-owned array.
        final int[] sources;
        final List<RouteCosts> routes;
        final int[][] priorities;
        final boolean sharedContacts;

        public Domain(int[] sourceIds, List<? extends RouteCosts> sourceRoutes, int[][] receiverPriorities) {
            this(sourceIds, sourceRoutes, receiverPriorities, false);
        }

        /** Explicit contact entries may share a physical source's packet budget. */
        public static Domain withSharedSourceContacts(int[] sourceIds, List<? extends RouteCosts> contactRoutes,
                                                       int[][] receiverPriorities) {
            return new Domain(sourceIds, contactRoutes, receiverPriorities, true);
        }

        private Domain(int[] sourceIds, List<? extends RouteCosts> sourceRoutes, int[][] receiverPriorities,
                       boolean sharedContacts) {
            this.sharedContacts = sharedContacts;
            sources = Objects.requireNonNull(sourceIds, "sourceIds").clone();
            routes = List.copyOf(sourceRoutes);
            priorities = Objects.requireNonNull(receiverPriorities, "receiverPriorities").clone();
            if (sources.length != routes.size() || sources.length != priorities.length)
                throw new IllegalArgumentException("Domain source vector lengths");
            for (int i = 0; i < priorities.length; i++) priorities[i] = priorities[i].clone();
        }
    }

    /** Immutable aggregate vectors; checked arithmetic rejects unrepresentable totals. */
    public static final class Round {
        private final long[] debits, credits;
        private final long dissipated;
        private final List<Delivery> deliveries;
        private Round(long[] debits, long[] credits, long dissipated, List<Delivery> deliveries) {
            this.debits = debits; this.credits = credits; this.dissipated = dissipated;
            this.deliveries = List.copyOf(deliveries);
        }
        public int sourceCount() { return debits.length; }
        public int receiverCount() { return credits.length; }
        public long debit(int source) { return debits[source]; }
        public long credit(int receiver) { return credits[receiver]; }
        public long dissipated() { return dissipated; }
        /** Positive deliveries in domain/source/receiver visitation order; empty unless requested. */
        public List<Delivery> deliveries() { return deliveries; }
    }

    /** IDs refer to the supplied vectors, including repeated source contact entries. */
    public record Delivery(int domain, int entry, int source, int receiver, long credit, long pathLoss) {
        public Delivery {
            if (domain < 0 || entry < 0 || source < 0 || receiver < 0 || credit <= 0 || pathLoss < 0)
                throw new IllegalArgumentException("Invalid positive delivery");
            Math.addExact(credit, pathLoss);
        }
        public long sourceDebit() { return Math.addExact(credit, pathLoss); }
    }

    /**
     * Independently shuffle separate conductor domains. Each delivery consumes
     * actual remaining capacity; a sink can receive multiple sources and packets.
     * Direct machine contacts are separate domains. Unlike the reference's frozen
     * demand overshoot, the owned-storage policy never credits beyond live room.
     * One quoted budget per source is shared across all domains. Ordinary sources
     * quote one packet; explicit transformer batches quote up to four whole packets
     * from the initial reserve. Later domains retain usable partial remainders.
     * The complete
     * reserve threshold is checked once, before any domain; a partially spent
     * packet may continue into later domains. Incoming credit cannot replenish
     * that budget in this round. A fresh snapshot is required before any commit.
     * Mixed shared-source/multiple-domain behavior remains an integration model,
     * not an assertion that all target scheduling details have been established.
     */
    public static Round allocate(List<Source> sourceQuotes, List<Domain> conductorDomains,
                                 int[] receiverContacts, long[] receiverRoom, RandomGenerator random) {
        return allocate(sourceQuotes, conductorDomains, receiverContacts, receiverRoom, random, false);
    }

    /** Capture numeric delivery details without invoking callbacks or mutating a world. */
    public static Round allocateTraced(List<Source> sourceQuotes, List<Domain> conductorDomains,
                                       int[] receiverContacts, long[] receiverRoom, RandomGenerator random) {
        return allocate(sourceQuotes, conductorDomains, receiverContacts, receiverRoom, random, true);
    }

    /**
     * Release actual source debits as receiver capacity for later domains.
     * Bindings must describe the same initial snapshot as source reserve and
     * receiver room. Initial overcapacity is retained as signed headroom.
     * A domain keeps its own initial demand quote; input credit never increases
     * the previously quoted source budget. Bindings do not mutate storage.
     */
    public static Round allocateTraced(List<Source> sourceQuotes, List<Domain> conductorDomains,
                                       int[] receiverContacts, long[] receiverRoom,
                                       List<SharedStorage> sharedStorage, RandomGenerator random) {
        return allocate(sourceQuotes, conductorDomains, receiverContacts, receiverRoom, random, true, sharedStorage);
    }

    public static Round allocate(List<Source> sourceQuotes, List<Domain> conductorDomains,
                                 int[] receiverContacts, long[] receiverRoom,
                                 List<SharedStorage> sharedStorage, RandomGenerator random) {
        return allocate(sourceQuotes, conductorDomains, receiverContacts, receiverRoom, random, false, sharedStorage);
    }

    private static Round allocate(List<Source> sourceQuotes, List<Domain> conductorDomains,
                                  int[] receiverContacts, long[] receiverRoom, RandomGenerator random, boolean trace) {
        return allocate(sourceQuotes, conductorDomains, receiverContacts, receiverRoom, random, trace, List.of());
    }

    private static Round allocate(List<Source> sourceQuotes, List<Domain> conductorDomains,
                                  int[] receiverContacts, long[] receiverRoom, RandomGenerator random, boolean trace,
                                  List<SharedStorage> sharedStorage) {
        return allocate(sourceQuotes, conductorDomains, receiverContacts, receiverRoom, random, trace, sharedStorage, null);
    }

    /**
     * Continue an already eligible settlement offer across receiver windows.
     * Balances describe the live snapshot; budgets describe its unspent initial
     * offer. Do not reapply a full-packet threshold to an already split packet.
     */
    public static Round allocateQuotedTraced(List<Source> sources, List<Domain> domains,
            int[] contacts, long[] room, List<SharedStorage> shared, long[] budgets, RandomGenerator random) {
        return allocate(sources, domains, contacts, room, random, true, shared, budgets.clone());
    }

    private static Round allocate(List<Source> sourceQuotes, List<Domain> conductorDomains,
                                  int[] receiverContacts, long[] receiverRoom, RandomGenerator random, boolean trace,
                                  List<SharedStorage> sharedStorage, long[] quotedBudgets) {
        var sources = List.copyOf(sourceQuotes);
        var domains = List.copyOf(conductorDomains);
        int[] contacts = Objects.requireNonNull(receiverContacts, "receiverContacts").clone();
        long[] remaining = Objects.requireNonNull(receiverRoom, "receiverRoom").clone();
        Objects.requireNonNull(random, "random");
        if (contacts.length != remaining.length) throw new IllegalArgumentException("Receiver vector lengths");
        for (long room : remaining) if (room < 0) throw new IllegalArgumentException("Negative room");
        var bindings = List.copyOf(sharedStorage);
        int[] sourceReceiver = null;
        if (!bindings.isEmpty()) {
            sourceReceiver = new int[sources.size()];
            java.util.Arrays.fill(sourceReceiver, -1);
            boolean[] boundReceivers = new boolean[remaining.length];
            for (var binding : bindings) {
                if (binding.source() >= sources.size() || binding.receiver() >= remaining.length
                        || sourceReceiver[binding.source()] != -1 || boundReceivers[binding.receiver()])
                    throw new IllegalArgumentException("Invalid or repeated shared storage identity");
                long headroom = binding.capacity() - sources.get(binding.source()).reserve();
                if (remaining[binding.receiver()] != Math.max(0, headroom))
                    throw new IllegalArgumentException("Inconsistent shared storage snapshot");
                remaining[binding.receiver()] = headroom;
                sourceReceiver[binding.source()] = binding.receiver();
                boundReceivers[binding.receiver()] = true;
            }
        }
        for (var domain : domains) {
            boolean[] seen = new boolean[sources.size()];
            for (int i = 0; i < domain.sources.length; i++) {
                int source = domain.sources[i];
                if (source < 0 || source >= seen.length || seen[source] && !domain.sharedContacts)
                    throw new IllegalArgumentException("Invalid or duplicate domain source");
                seen[source] = true;
                if (domain.priorities[i].length != remaining.length)
                    throw new IllegalArgumentException("Receiver priority dimensions");
            }
        }
        int[] domainOrder = new int[domains.size()];
        for (int i = 0; i < domainOrder.length; i++) domainOrder[i] = i;
        for (int end = domainOrder.length - 1; end > 0; end--) {
            int other = random.nextInt(end + 1), saved = domainOrder[end];
            domainOrder[end] = domainOrder[other]; domainOrder[other] = saved;
        }
        long[] budgets = new long[sources.size()];
        if (quotedBudgets != null && quotedBudgets.length != sources.size()) throw new IllegalArgumentException("Budget dimensions");
        for (int source = 0; source < sources.size(); source++) {
            var quote = sources.get(source);
            budgets[source] = quotedBudgets == null ? quote.policy().quote(quote.reserve()) : quotedBudgets[source];
            if (budgets[source] < 0 || budgets[source] > quote.reserve() || budgets[source] > quote.policy().maximumTransfer())
                throw new IllegalArgumentException("Quoted budget exceeds source balance/policy");
        }
        long[] debits = new long[sources.size()], credits = new long[remaining.length];
        List<Delivery> deliveries = trace ? new ArrayList<>() : List.of();
        long loss = 0;
        for (int domainId : domainOrder) {
            var domain = domains.get(domainId);
            int[] offeringEntries = new int[domain.sources.length];
            int offeringCount = 0;
            for (int i = 0; i < domain.sources.length; i++) {
                int source = domain.sources[i];
                if (budgets[source] > debits[source]) offeringEntries[offeringCount++] = i;
            }
            for (int selected : SourceOrder.create(offeringCount, random)) {
                int entry = offeringEntries[selected], source = domain.sources[entry];
                long packet = budgets[source] - debits[source];
                if (packet <= 0) continue;
                // Positive delivery does not retire a sink. It can accept more
                // packets/sources until its actual remaining capacity is used.
                long[] activeQuotes = new long[remaining.length];
                for (int receiver = 0; receiver < remaining.length; receiver++)
                    activeQuotes[receiver] = Math.max(0, remaining[receiver]);
                int ownReceiver = sourceReceiver == null ? -1 : sourceReceiver[source];
                if (ownReceiver >= 0) activeQuotes[ownReceiver] = 0;
                if (sources.get(source).packetCount() > 1) {
                    var route = domain.routes.get(entry);
                    long[] pathLoss = new long[remaining.length];
                    for (int receiver = 0; receiver < remaining.length; receiver++) {
                        if (!route.reaches(contacts[receiver])) {
                            activeQuotes[receiver] = 0;
                        } else {
                            if (route.lossMilliTo(contacts[receiver]) < 0)
                                throw new IllegalArgumentException("Negative route loss");
                            pathLoss[receiver] = route.wholeLossTo(contacts[receiver]);
                        }
                    }
                    var batch = TransformerBatch.allocateQuoted(sources.get(source).packet(), packet,
                        activeQuotes, domain.priorities[entry], pathLoss, trace);
                    if (trace) for (var delivery : batch.deliveries()) {
                        deliveries.add(new Delivery(domainId, entry, source, delivery.receiver(),
                            delivery.credit(), delivery.pathLoss()));
                    }
                    debits[source] = Math.addExact(debits[source], batch.debit());
                    if (ownReceiver >= 0)
                        remaining[ownReceiver] = Math.addExact(remaining[ownReceiver], batch.debit());
                    loss = Math.addExact(loss, batch.dissipated());
                    for (int receiver = 0; receiver < remaining.length; receiver++) {
                        long amount = batch.credit(receiver);
                        credits[receiver] = Math.addExact(credits[receiver], amount);
                        remaining[receiver] = Math.subtractExact(remaining[receiver], amount);
                    }
                    continue;
                }
                var receipt = trace
                    ? PacketDistributor.allocateTraced(packet, packet, domain.routes.get(entry), contacts, activeQuotes, domain.priorities[entry])
                    : PacketDistributor.allocate(packet, packet, domain.routes.get(entry), contacts, activeQuotes, domain.priorities[entry]);
                if (trace) for (int receiver : domain.priorities[entry]) {
                    if (receipt.credit(receiver) > 0) deliveries.add(new Delivery(domainId, entry, source, receiver,
                        receipt.credit(receiver), receipt.deliveryLoss(receiver)));
                }
                debits[source] = Math.addExact(debits[source], receipt.sourceDebit());
                if (ownReceiver >= 0)
                    remaining[ownReceiver] = Math.addExact(remaining[ownReceiver], receipt.sourceDebit());
                loss = Math.addExact(loss, receipt.dissipated());
                for (int receiver = 0; receiver < remaining.length; receiver++) {
                    long amount = receipt.credit(receiver);
                    credits[receiver] = Math.addExact(credits[receiver], amount);
                    remaining[receiver] = Math.subtractExact(remaining[receiver], amount);
                }
            }
        }
        return new Round(debits, credits, loss, deliveries);
    }
}
