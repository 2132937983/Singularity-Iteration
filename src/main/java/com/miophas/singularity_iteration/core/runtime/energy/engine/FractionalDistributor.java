// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Exact fractional extension of this project's domain accounting. The frozen
 * R29 ordinary-save experiments establish residual generator offers, whole-EU
 * copper loss, and fractional receiver demand. Broader domain/batch policy is
 * inherited from our integer model, not a claim of fractional reference parity
 * in every topology. No callbacks or world mutation occur during allocation.
 */
public final class FractionalDistributor {
    private FractionalDistributor() { }
    private static final BigInteger ZERO = BigInteger.ZERO;
    public record Delivery(int domain, int entry, int source, int receiver,
                           EnergyAmount credit, EnergyAmount pathLoss) {
        public EnergyAmount sourceDebit() { return credit.add(pathLoss); }
    }
    public static final class Round {
        private final EnergyAmount[] debits, credits;
        private final EnergyAmount dissipated;
        private final List<Delivery> deliveries;
        private Round(BigInteger[] debits, BigInteger[] credits, BigInteger dissipated, List<Delivery> deliveries) {
            this.debits = Arrays.stream(debits).map(EnergyAmount::fromUnits).toArray(EnergyAmount[]::new);
            this.credits = Arrays.stream(credits).map(EnergyAmount::fromUnits).toArray(EnergyAmount[]::new);
            this.dissipated = EnergyAmount.fromUnits(dissipated); this.deliveries = List.copyOf(deliveries);
        }
        private Round(DomainDistributor.Round integers) {
            debits = new EnergyAmount[integers.sourceCount()]; credits = new EnergyAmount[integers.receiverCount()];
            for (int i = 0; i < debits.length; i++) debits[i] = EnergyAmount.of(integers.debit(i));
            for (int i = 0; i < credits.length; i++) credits[i] = EnergyAmount.of(integers.credit(i));
            dissipated = EnergyAmount.of(integers.dissipated());
            deliveries = integers.deliveries().stream().map(d -> new Delivery(d.domain(), d.entry(), d.source(),
                d.receiver(), EnergyAmount.of(d.credit()), EnergyAmount.of(d.pathLoss()))).toList();
        }
        public EnergyAmount debit(int source) { return debits[source]; }
        public EnergyAmount credit(int receiver) { return credits[receiver]; }
        public EnergyAmount dissipated() { return dissipated; }
        public List<Delivery> deliveries() { return deliveries; }
    }
    private static BigInteger units(long amount) { return BigInteger.valueOf(amount).shiftLeft(EnergyAmount.FRACTION_BITS); }
    private static BigInteger[] zeros(int count) {
        var result = new BigInteger[count]; Arrays.fill(result, ZERO); return result;
    }

    /** Keep the established integer planner when all actual balances and demands are integral. */
    public static Round allocateTraced(List<DomainDistributor.Source> sourceQuotes,
            List<EnergyAmount> sourceAmounts, List<DomainDistributor.Domain> conductorDomains,
            int[] receiverContacts, List<EnergyAmount> receiverRoom,
            List<DomainDistributor.SharedStorage> sharedStorage, RandomGenerator random) {
        return allocateTraced(sourceQuotes, sourceAmounts, conductorDomains, receiverContacts, receiverRoom, sharedStorage, null, random);
    }

    /** Balances and already eligible unspent offers are separate across windows. */
    public static Round allocateQuotedTraced(List<DomainDistributor.Source> sources,
            List<EnergyAmount> amounts, List<DomainDistributor.Domain> domains, int[] contacts,
            List<EnergyAmount> room, List<DomainDistributor.SharedStorage> shared,
            List<EnergyAmount> budgets, RandomGenerator random) {
        return allocateTraced(sources, amounts, domains, contacts, room, shared, List.copyOf(budgets), random);
    }

    private static Round allocateTraced(List<DomainDistributor.Source> sourceQuotes,
            List<EnergyAmount> sourceAmounts, List<DomainDistributor.Domain> conductorDomains,
            int[] receiverContacts, List<EnergyAmount> receiverRoom,
            List<DomainDistributor.SharedStorage> sharedStorage, List<EnergyAmount> quotedBudgets, RandomGenerator random) {
        if (sourceQuotes.size() != sourceAmounts.size() || receiverContacts.length != receiverRoom.size())
            throw new IllegalArgumentException("Fractional vector dimensions");
        for (int i = 0; i < sourceQuotes.size(); i++)
            if (sourceQuotes.get(i).reserve() != sourceAmounts.get(i).whole())
                throw new IllegalArgumentException("Inconsistent integral reserve mirror");
        if (IndependentEnergyMode.roundEnetLoss() && (quotedBudgets == null || quotedBudgets.stream().allMatch(v -> v.fraction() == 0))
                && sourceAmounts.stream().allMatch(v -> v.fraction() == 0)
                && receiverRoom.stream().allMatch(v -> v.fraction() == 0)) {
            if (quotedBudgets != null) return new Round(DomainDistributor.allocateQuotedTraced(sourceQuotes, conductorDomains,
                receiverContacts, receiverRoom.stream().mapToLong(EnergyAmount::whole).toArray(), sharedStorage,
                quotedBudgets.stream().mapToLong(EnergyAmount::whole).toArray(), random));
            return new Round(DomainDistributor.allocateTraced(sourceQuotes, conductorDomains, receiverContacts,
                receiverRoom.stream().mapToLong(EnergyAmount::whole).toArray(), sharedStorage, random));
        }
        return allocateExact(sourceQuotes, sourceAmounts, conductorDomains, receiverContacts, receiverRoom, sharedStorage, quotedBudgets, random);
    }

    // Package-private entry also permits independent integer differential tests
    // to exercise the fractional algorithm instead of silently taking the fast path.
    static Round allocateExact(List<DomainDistributor.Source> sourceQuotes,
            List<EnergyAmount> sourceAmounts, List<DomainDistributor.Domain> conductorDomains,
            int[] receiverContacts, List<EnergyAmount> receiverRoom,
            List<DomainDistributor.SharedStorage> sharedStorage, RandomGenerator random) {
        return allocateExact(sourceQuotes, sourceAmounts, conductorDomains, receiverContacts, receiverRoom, sharedStorage, null, random);
    }

    private static Round allocateExact(List<DomainDistributor.Source> sourceQuotes,
            List<EnergyAmount> sourceAmounts, List<DomainDistributor.Domain> conductorDomains,
            int[] receiverContacts, List<EnergyAmount> receiverRoom,
            List<DomainDistributor.SharedStorage> sharedStorage, List<EnergyAmount> quotedBudgets, RandomGenerator random) {
        var sources = List.copyOf(sourceQuotes); var amounts = List.copyOf(sourceAmounts);
        var domains = List.copyOf(conductorDomains); var bindings = List.copyOf(sharedStorage);
        int[] contacts = receiverContacts.clone(); var remaining = receiverRoom.stream().map(EnergyAmount::units).toArray(BigInteger[]::new);
        Objects.requireNonNull(random, "random");
        if (sources.size() != amounts.size() || contacts.length != remaining.length)
            throw new IllegalArgumentException("Fractional vector dimensions");
        for (int i = 0; i < sources.size(); i++)
            if (sources.get(i).reserve() != amounts.get(i).whole()) throw new IllegalArgumentException("Inconsistent source quote");
        int[] sourceReceiver = new int[sources.size()]; Arrays.fill(sourceReceiver, -1);
        boolean[] bound = new boolean[remaining.length];
        for (var binding : bindings) {
            if (binding.source() >= sources.size() || binding.receiver() >= remaining.length
                    || sourceReceiver[binding.source()] >= 0 || bound[binding.receiver()])
                throw new IllegalArgumentException("Invalid shared storage binding");
            BigInteger room = units(binding.capacity()).subtract(amounts.get(binding.source()).units());
            if (!remaining[binding.receiver()].equals(room.max(ZERO))) throw new IllegalArgumentException("Inconsistent shared demand");
            sourceReceiver[binding.source()] = binding.receiver(); bound[binding.receiver()] = true;
            remaining[binding.receiver()] = room;
        }
        for (var domain : domains) {
            boolean[] seen = new boolean[sources.size()];
            for (int entry = 0; entry < domain.sources.length; entry++) {
                int source = domain.sources[entry];
                if (source < 0 || source >= seen.length || seen[source] && !domain.sharedContacts)
                    throw new IllegalArgumentException("Invalid domain source");
                seen[source] = true;
                if (domain.priorities[entry].length != remaining.length) throw new IllegalArgumentException("Priority dimensions");
                boolean[] prioritySeen = new boolean[remaining.length];
                for (int receiver : domain.priorities[entry]) {
                    if (receiver < 0 || receiver >= remaining.length || prioritySeen[receiver])
                        throw new IllegalArgumentException("Priority must be a complete permutation");
                    prioritySeen[receiver] = true;
                    var route = domain.routes.get(entry);
                    if (route.reaches(contacts[receiver]) && (route.lossMilliTo(contacts[receiver]) < 0 || route.wholeLossTo(contacts[receiver]) < 0))
                        throw new IllegalArgumentException("Negative route loss");
                }
            }
        }
        int[] order = new int[domains.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        for (int end = order.length - 1; end > 0; end--) {
            int other = random.nextInt(end + 1), saved = order[end]; order[end] = order[other]; order[other] = saved;
        }
        var budgets = zeros(sources.size());
        if (quotedBudgets != null && quotedBudgets.size() != sources.size()) throw new IllegalArgumentException("Budget dimensions");
        for (int i = 0; i < sources.size(); i++) {
            var source = sources.get(i);
            budgets[i] = quotedBudgets != null ? quotedBudgets.get(i).units()
                : source.partialPackets() ? amounts.get(i).units().min(units(source.policy().maximumTransfer()))
                : units(source.policy().quote(source.reserve()));
            if (budgets[i].compareTo(amounts.get(i).units()) > 0 || budgets[i].compareTo(units(source.policy().maximumTransfer())) > 0)
                throw new IllegalArgumentException("Quoted budget exceeds source balance/policy");
        }
        var debits = zeros(sources.size()); var credits = zeros(remaining.length); BigInteger lossTotal = ZERO;
        var deliveries = new ArrayList<Delivery>();
        for (int domainId : order) {
            var domain = domains.get(domainId); int[] offering = new int[domain.sources.length]; int count = 0;
            for (int entry = 0; entry < domain.sources.length; entry++)
                if (budgets[domain.sources[entry]].compareTo(debits[domain.sources[entry]]) > 0) offering[count++] = entry;
            for (int selected : SourceOrder.create(count, random)) {
                int entry = offering[selected], source = domain.sources[entry], ownReceiver = sourceReceiver[source];
                BigInteger available = budgets[source].subtract(debits[source]);
                var route = domain.routes.get(entry); var sourceQuote = sources.get(source);
                BigInteger packet = units(sourceQuote.packet());
                int packets = available.add(packet).subtract(BigInteger.ONE).divide(packet).intValueExact();
                for (int n = 0; n < packets && available.signum() > 0; n++) {
                    BigInteger packetLeft = available.min(packet), before = packetLeft;
                    for (int receiver : domain.priorities[entry]) {
                        if (remaining[receiver].signum() <= 0 || receiver == ownReceiver || !route.reaches(contacts[receiver])) continue;
                        BigInteger pathLoss = EnergyLoss.fromMilli(route.lossMilliTo(contacts[receiver])).units();
                        if (packetLeft.compareTo(pathLoss) <= 0) continue;
                        BigInteger credit = remaining[receiver].min(packetLeft.subtract(pathLoss));
                        BigInteger debit = credit.add(pathLoss);
                        packetLeft = packetLeft.subtract(debit);
                        available = available.subtract(debit); debits[source] = debits[source].add(debit);
                        credits[receiver] = credits[receiver].add(credit);
                        remaining[receiver] = remaining[receiver].subtract(credit);
                        if (ownReceiver >= 0) remaining[ownReceiver] = remaining[ownReceiver].add(debit);
                        lossTotal = lossTotal.add(pathLoss);
                        deliveries.add(new Delivery(domainId, entry, source, receiver, EnergyAmount.fromUnits(credit), EnergyAmount.fromUnits(pathLoss)));
                    }
                    if (packetLeft.equals(before)) break;
                }
            }
        }
        BigInteger debitTotal = Arrays.stream(debits).reduce(ZERO, BigInteger::add);
        BigInteger creditTotal = Arrays.stream(credits).reduce(ZERO, BigInteger::add);
        if (!debitTotal.equals(creditTotal.add(lossTotal))) throw new IllegalStateException("Fractional allocation lost energy");
        return new Round(debits, credits, lossTotal, deliveries);
    }
}
