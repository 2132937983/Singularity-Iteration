// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic fair-share distributor for the smart grid mode.
 *
 * <p>This class replaces the packet priority walk of {@link FractionalDistributor}
 * when {@link IndependentEnergyMode#fairDistribution()} is on. It does not use
 * a random number. It does not depend on the order of receivers. It gives the
 * same result for the same input.
 *
 * <h2>Why this class exists</h2>
 * <p>The packet priority walk gives one packet to the receivers in one order.
 * The first receiver takes all the energy that it can hold. The next receivers
 * get only the remainder. Each delivery also pays the full path loss in whole
 * EU. Thus receivers far from the source, or late in the order, got less
 * energy. Players saw this as 100 %, 75 %, 50 % power on machines in one line.
 * This class divides the energy in proportion to the demand. All receivers in
 * one stage get the same fraction of their demand.
 *
 * <h2>Model</h2>
 * <ul>
 *   <li><b>Source:</b> a budget (EU that the source can give in this round), a
 *       packet size (the voltage of the source), and a class. Class 0 is a
 *       generator or a relay (a transformer). Class 1 is a storage block.</li>
 *   <li><b>Receiver:</b> a room (free capacity), a rated input for this round
 *       ({@code null} for no limit), and a class. Class 0 is a load or a relay.
 *       Class 1 is a storage block.</li>
 *   <li><b>Domain:</b> one group of routes, as in {@link DomainDistributor.Domain}.
 *       This class reads the reach and the loss of each route. It ignores the
 *       receiver priorities of the domain.</li>
 *   <li><b>Shared storage:</b> a port that is a source and a receiver at the
 *       same time. Its debit increases its own room. It never sends energy to
 *       itself.</li>
 * </ul>
 *
 * <h2>Stages</h2>
 * <p>The round has four stages, in this order:
 * <ol>
 *   <li>Class 0 receivers, rated demand: {@code min(room, rated input)}.</li>
 *   <li>Class 0 receivers, surplus demand: the remaining room.</li>
 *   <li>Class 1 receivers, rated demand.</li>
 *   <li>Class 1 receivers, surplus demand.</li>
 * </ol>
 * <p>In each stage, the sources give energy in this order: class 0 sources,
 * then class 1 sources. In one class, the order starts at index
 * {@code rotation mod count} and wraps. Each source divides its remaining
 * budget among the receivers that it can reach, in proportion to their
 * remaining demand of the stage. When the budget is sufficient, each receiver
 * gets its full demand.
 *
 * <p><b>Result:</b> loads and relays get energy before storage. A storage
 * block gives energy only for demand that the generators cannot supply. The
 * surplus of the generators charges the storage blocks. When one source
 * supplies all receivers of one stage, all receivers get the same fraction of
 * their demand. This property also holds for more sources that reach the same
 * receivers: the fractions add.
 *
 * <h2>Line loss</h2>
 * <ul>
 *   <li>{@link LineLoss#LOSSLESS}: a delivery has no loss. This is the default.</li>
 *   <li>{@link LineLoss#PROPORTIONAL}: the loss is
 *       {@code credit * lossMilli / (1000 * packet)}. A higher voltage thus
 *       has a lower loss fraction. A route whose loss fraction is 1 or more
 *       cannot deliver.</li>
 * </ul>
 *
 * <h2>Arithmetic</h2>
 * <p>The planner works in fixed-point {@code long} values with
 * {@value #COARSE_BITS} fraction bits (2<sup>-24</sup> EU). It reads each
 * budget and room rounded down to this grid, so it never gives more than the
 * real budget or room. The proportional factor is a {@code double}. Each
 * credit is capped by the remaining budget of its source, so a rounding error
 * cannot overspend. Conservation is exact: each debit is the sum of its credit
 * and its loss in the same {@code long} grid. The method converts the result
 * to {@link EnergyAmount} without loss of precision (a left shift).
 * Budgets and rooms above 2<sup>38</sup> EU are clamped to 2<sup>38</sup> EU.
 * One round can never move more than that.
 *
 * <h2>Cost</h2>
 * <p>The method visits each linked (source, receiver) pair of the route table a
 * fixed number of times: once to select the best route, and twice per stage.
 * Thus the cost is {@code O(S * R)}, where S is the number of sources and R is
 * the number of receivers in the window. This is linear in the size of the
 * route table. There is no sort and no wide integer arithmetic in the loops.
 * The engine limits a window to 32 sources and 128 receivers.
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li>Conservation: the sum of debits equals the sum of credits plus the
 *       loss. The method throws {@link IllegalStateException} when this is
 *       false.</li>
 *   <li>A source never gives more than its budget.</li>
 *   <li>A receiver never gets more than its room at the start of the round,
 *       plus the energy that it gives as a shared storage source.</li>
 *   <li>The inputs are not changed. No world state is read or written.</li>
 * </ul>
 */
public final class FairShareDistributor {
    private FairShareDistributor() { }

    /** Fraction bits of the planning grid. */
    public static final int COARSE_BITS = 24;
    private static final int SHIFT = EnergyAmount.FRACTION_BITS - COARSE_BITS;
    private static final long COARSE_MASK = (1L << COARSE_BITS) - 1;
    /** Largest whole EU that the planning grid accepts for one budget or room. */
    private static final long MAX_WHOLE = 1L << 38;

    /** The line loss model. */
    public enum LineLoss { LOSSLESS, PROPORTIONAL }

    /**
     * One source of the round.
     *
     * @param budget the EU that the source can give in this round
     * @param packet the packet size (voltage) of the source, in EU. Must be above 0.
     * @param sourceClass 0 for a generator or relay, 1 for storage
     */
    public record Source(EnergyAmount budget, long packet, int sourceClass) {
        public Source {
            Objects.requireNonNull(budget, "budget");
            if (packet <= 0 || sourceClass < 0 || sourceClass > 1)
                throw new IllegalArgumentException("Invalid fair-share source");
        }
    }

    /**
     * One receiver of the round.
     *
     * @param room the free capacity at the start of the round
     * @param rated the rated input for this round, or {@code null} for no limit
     * @param receiverClass 0 for a load or relay, 1 for storage
     */
    public record Receiver(EnergyAmount room, EnergyAmount rated, int receiverClass) {
        public Receiver {
            Objects.requireNonNull(room, "room");
            if (receiverClass < 0 || receiverClass > 1)
                throw new IllegalArgumentException("Invalid fair-share receiver");
        }
    }

    /**
     * One positive delivery. {@code domain} and {@code entry} identify the
     * route (the same values as {@link FractionalDistributor.Delivery}).
     * {@code voltage} is the packet that the source pushes to this receiver,
     * for overvoltage effects: {@code min(packet, max(debit, min(room, budget)))},
     * where room and budget are the values at the start of the round.
     */
    public record Delivery(int domain, int entry, int source, int receiver,
                           EnergyAmount credit, EnergyAmount pathLoss, double voltage) {
        public EnergyAmount sourceDebit() { return credit.add(pathLoss); }
    }

    /** The result of one round. Indexes match the input lists. */
    public static final class Round {
        private final EnergyAmount[] debits, credits;
        private final EnergyAmount dissipated;
        private final List<Delivery> deliveries;
        private Round(EnergyAmount[] debits, EnergyAmount[] credits, EnergyAmount dissipated, List<Delivery> deliveries) {
            this.debits = debits; this.credits = credits; this.dissipated = dissipated;
            this.deliveries = List.copyOf(deliveries);
        }
        public EnergyAmount debit(int source) { return debits[source]; }
        public EnergyAmount credit(int receiver) { return credits[receiver]; }
        public EnergyAmount dissipated() { return dissipated; }
        public List<Delivery> deliveries() { return deliveries; }
        public int sourceCount() { return debits.length; }
        public int receiverCount() { return credits.length; }
    }

    /** Converts an amount to the planning grid, rounded down and clamped. */
    static long toCoarse(EnergyAmount amount) {
        if (amount.whole() >= MAX_WHOLE) return MAX_WHOLE << COARSE_BITS;
        return (amount.whole() << COARSE_BITS) | (amount.fraction() >>> SHIFT);
    }

    /** Converts a planning grid value to an amount. This conversion is exact. */
    static EnergyAmount fromCoarse(long value) {
        if (value < 0) throw new IllegalArgumentException("Negative planned energy");
        return new EnergyAmount(value >>> COARSE_BITS, (value & COARSE_MASK) << SHIFT);
    }

    /**
     * Allocates one round.
     *
     * @param sources the sources, indexed as in the domains
     * @param domains the route groups. The priorities are ignored.
     * @param contacts the contact index of each receiver in the route costs
     * @param receivers the receivers
     * @param shared the ports that are a source and a receiver
     * @param loss the line loss model
     * @param rotation the start offset of the source order (for example the world time)
     * @return the round
     */
    public static Round allocate(List<Source> sources, List<DomainDistributor.Domain> domains, int[] contacts,
            List<Receiver> receivers, List<DomainDistributor.SharedStorage> shared, LineLoss loss, long rotation) {
        Objects.requireNonNull(loss, "loss");
        int sourceCount = sources.size(), receiverCount = receivers.size();
        if (contacts.length != receiverCount) throw new IllegalArgumentException("Receiver vector lengths");

        // Shared storage: source -> own receiver.
        int[] ownReceiver = new int[sourceCount];
        Arrays.fill(ownReceiver, -1);
        boolean[] bound = new boolean[receiverCount];
        for (var binding : shared) {
            if (binding.source() >= sourceCount || binding.receiver() >= receiverCount
                    || ownReceiver[binding.source()] >= 0 || bound[binding.receiver()])
                throw new IllegalArgumentException("Invalid shared storage binding");
            ownReceiver[binding.source()] = binding.receiver();
            bound[binding.receiver()] = true;
        }

        long[] packet = new long[sourceCount];
        int[] sourceClass = new int[sourceCount];
        long[] budget = new long[sourceCount], initialBudget = new long[sourceCount];
        for (int i = 0; i < sourceCount; i++) {
            var source = sources.get(i);
            packet[i] = source.packet();
            sourceClass[i] = source.sourceClass();
            budget[i] = toCoarse(source.budget());
            initialBudget[i] = budget[i];
        }
        int[] receiverClass = new int[receiverCount];
        long[] room = new long[receiverCount], initialRoom = new long[receiverCount], rateLeft = new long[receiverCount];
        for (int r = 0; r < receiverCount; r++) {
            var receiver = receivers.get(r);
            receiverClass[r] = receiver.receiverClass();
            room[r] = toCoarse(receiver.room());
            initialRoom[r] = room[r];
            rateLeft[r] = receiver.rated() == null ? -1 : toCoarse(receiver.rated());
        }

        // Select the best route for each (source, receiver) pair: the lowest loss.
        // A tie keeps the first route in domain and entry order. This is deterministic.
        // linkDomain < 0 means "no route".
        int[][] linkDomain = new int[sourceCount][receiverCount], linkEntry = new int[sourceCount][receiverCount];
        long[][] linkLoss = new long[sourceCount][receiverCount];
        for (int i = 0; i < sourceCount; i++) Arrays.fill(linkDomain[i], -1);
        for (int d = 0; d < domains.size(); d++) {
            var domain = domains.get(d);
            for (int entry = 0; entry < domain.sources.length; entry++) {
                int source = domain.sources[entry];
                if (source < 0 || source >= sourceCount) throw new IllegalArgumentException("Invalid domain source");
                var route = domain.routes.get(entry);
                long den = Math.multiplyExact(1000L, packet[source]);
                for (int r = 0; r < receiverCount; r++) {
                    if (r == ownReceiver[source] || !route.reaches(contacts[r])) continue;
                    long milli = route.lossMilliTo(contacts[r]);
                    if (milli < 0) throw new IllegalArgumentException("Negative route loss");
                    if (loss == LineLoss.PROPORTIONAL && milli >= den) continue; // The loss takes the full packet.
                    long effective = loss == LineLoss.LOSSLESS ? 0 : milli;
                    if (linkDomain[source][r] < 0 || effective < linkLoss[source][r]) {
                        linkDomain[source][r] = d; linkEntry[source][r] = entry; linkLoss[source][r] = effective;
                    }
                }
            }
        }
        // Compact the linked receivers of each source, and the loss fraction of each link.
        int[][] linked = new int[sourceCount][];
        double[][] fraction = new double[sourceCount][];
        for (int i = 0; i < sourceCount; i++) {
            int count = 0;
            for (int r = 0; r < receiverCount; r++) if (linkDomain[i][r] >= 0) count++;
            linked[i] = new int[count];
            fraction[i] = new double[count];
            double den = 1000.0 * packet[i];
            int at = 0;
            for (int r = 0; r < receiverCount; r++) if (linkDomain[i][r] >= 0) {
                linked[i][at] = r;
                fraction[i][at++] = linkLoss[i][r] / den;
            }
        }

        int[] order = sourceOrder(sourceClass, rotation);
        int[][] pools = pools(order, sourceClass, linked, fraction);
        long[] debits = new long[sourceCount], credits = new long[receiverCount];
        long lossTotal = 0;
        long[][] pairCredit = new long[sourceCount][], pairLoss = new long[sourceCount][];
        long[] demand = new long[receiverCount];

        for (int stage = 0; stage < 4; stage++) {
            int stageClass = stage / 2;
            boolean rated = stage % 2 == 0;
            for (int[] pool : pools) {
                // A pool is one source, or more sources of one class with the same
                // links and no loss. It acts as one source with the summed budget.
                long poolBudget = 0;
                for (int member : pool) poolBudget = saturatingAdd(poolBudget, budget[member]);
                if (poolBudget <= 0) continue;
                int lead = pool[0];
                int[] targets = linked[lead];
                double[] f = fraction[lead];
                double weight = 0;
                long exactDemand = 0;
                boolean any = false;
                for (int c = 0; c < targets.length; c++) {
                    int r = targets[c];
                    long want = 0;
                    if (receiverClass[r] == stageClass) {
                        want = room[r];
                        if (rated && rateLeft[r] >= 0) want = Math.min(want, rateLeft[r]);
                    }
                    demand[r] = Math.max(0, want);
                    if (demand[r] == 0) continue;
                    any = true;
                    weight += demand[r] * (1.0 + f[c]);
                    exactDemand = saturatingAdd(exactDemand, demand[r]);
                }
                if (!any) continue;
                // Lossless with an exact long sum: full when the budget covers the demand.
                double factor = loss == LineLoss.LOSSLESS
                    ? (exactDemand <= poolBudget ? 1.0 : (double) poolBudget / exactDemand)
                    : Math.min(1.0, poolBudget / weight);
                int member = 0;
                for (int c = 0; c < targets.length && poolBudget > 0; c++) {
                    int r = targets[c];
                    if (demand[r] == 0) continue;
                    long credit = factor >= 1.0 ? demand[r] : Math.min(demand[r], (long) Math.floor(demand[r] * factor));
                    long pathLoss = f[c] == 0 ? 0 : (long) Math.floor(credit * f[c]);
                    // Cap by the remaining budget: a rounding error cannot overspend.
                    if (credit + pathLoss > poolBudget) {
                        credit = f[c] == 0 ? poolBudget : (long) Math.floor(poolBudget / (1.0 + f[c]));
                        pathLoss = f[c] == 0 ? 0 : Math.min(poolBudget - credit, (long) Math.floor(credit * f[c]));
                    }
                    if (credit <= 0) continue;
                    poolBudget -= credit + pathLoss;
                    credits[r] += credit;
                    room[r] -= credit;
                    if (rateLeft[r] >= 0) rateLeft[r] = Math.max(0, rateLeft[r] - credit);
                    lossTotal += pathLoss;
                    // Northwest corner: pay this credit from the pool members in
                    // visit order. A pool with loss has exactly one member.
                    long creditLeft = credit, lossLeft = pathLoss;
                    while (creditLeft > 0 || lossLeft > 0) {
                        while (budget[pool[member]] == 0) member++;
                        int payer = pool[member];
                        long take = Math.min(creditLeft + lossLeft, budget[payer]);
                        long lossPart = Math.min(lossLeft, take), creditPart = take - lossPart;
                        budget[payer] -= take;
                        debits[payer] += take;
                        creditLeft -= creditPart;
                        lossLeft -= lossPart;
                        int own = ownReceiver[payer];
                        if (own >= 0) room[own] += take;
                        if (pairCredit[payer] == null) {
                            pairCredit[payer] = new long[receiverCount];
                            pairLoss[payer] = new long[receiverCount];
                        }
                        pairCredit[payer][r] += creditPart;
                        pairLoss[payer][r] += lossPart;
                    }
                }
                for (int payer : pool) if (budget[payer] < 0) throw new IllegalStateException("Fair-share source overspent");
            }
        }

        // One delivery per (source, receiver) pair, in source then receiver order.
        var deliveries = new ArrayList<Delivery>();
        for (int source = 0; source < sourceCount; source++) {
            if (pairCredit[source] == null) continue;
            for (int r = 0; r < receiverCount; r++) {
                long credit = pairCredit[source][r];
                if (credit <= 0) continue;
                long pathLoss = pairLoss[source][r];
                double pushed = fromCoarse(Math.min(initialRoom[r], initialBudget[source])).toDouble();
                double debit = fromCoarse(credit + pathLoss).toDouble();
                double voltage = Math.min((double) packet[source], Math.max(debit, pushed));
                deliveries.add(new Delivery(linkDomain[source][r], linkEntry[source][r], source, r,
                    fromCoarse(credit), fromCoarse(pathLoss), voltage));
            }
        }

        long debitTotal = 0, creditTotal = 0;
        var debitAmounts = new EnergyAmount[sourceCount];
        var creditAmounts = new EnergyAmount[receiverCount];
        for (int i = 0; i < sourceCount; i++) { debitTotal = Math.addExact(debitTotal, debits[i]); debitAmounts[i] = fromCoarse(debits[i]); }
        for (int r = 0; r < receiverCount; r++) { creditTotal = Math.addExact(creditTotal, credits[r]); creditAmounts[r] = fromCoarse(credits[r]); }
        if (debitTotal != Math.addExact(creditTotal, lossTotal))
            throw new IllegalStateException("Fair-share allocation lost energy");
        return new Round(debitAmounts, creditAmounts, fromCoarse(lossTotal), deliveries);
    }

    /**
     * Groups the sources into pools, in visit order.
     *
     * <p>Sources of the same class with the same linked receivers and no loss
     * on any link join one pool. A pool gives the same credits as its members
     * in sequence (the fractions add), but it makes at most one delivery per
     * receiver and member change. This keeps the number of deliveries near
     * {@code S + R} on a shared bus, instead of {@code S * R}. Each other source
     * is a pool of one.
     */
    static int[][] pools(int[] order, int[] sourceClass, int[][] linked, double[][] fraction) {
        var groups = new java.util.LinkedHashMap<Object, java.util.ArrayList<Integer>>();
        for (int source : order) {
            boolean lossless = true;
            for (double value : fraction[source]) if (value != 0) { lossless = false; break; }
            Object key = lossless ? new PoolKey(sourceClass[source], linked[source]) : Integer.valueOf(-1 - source);
            groups.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(source);
        }
        int[][] result = new int[groups.size()][];
        int at = 0;
        for (var members : groups.values()) result[at++] = members.stream().mapToInt(Integer::intValue).toArray();
        return result;
    }

    /** Pool identity: source class and linked receivers. */
    private record PoolKey(int sourceClass, int[] linked) {
        @Override public boolean equals(Object other) {
            return other instanceof PoolKey key && key.sourceClass == sourceClass && Arrays.equals(key.linked, linked);
        }
        @Override public int hashCode() { return 31 * sourceClass + Arrays.hashCode(linked); }
    }

    private static long saturatingAdd(long left, long right) {
        long sum = left + right;
        return sum < 0 ? Long.MAX_VALUE : sum;
    }

    /**
     * Returns the source visit order: all class 0 sources, then all class 1
     * sources. Each class starts at {@code rotation mod size} and wraps.
     */
    static int[] sourceOrder(int[] sourceClass, long rotation) {
        int[] order = new int[sourceClass.length];
        int at = 0;
        for (int wanted = 0; wanted <= 1; wanted++) {
            int size = 0;
            for (int value : sourceClass) if (value == wanted) size++;
            if (size == 0) continue;
            int[] members = new int[size];
            int m = 0;
            for (int i = 0; i < sourceClass.length; i++) if (sourceClass[i] == wanted) members[m++] = i;
            int start = (int) Math.floorMod(rotation, (long) size);
            for (int k = 0; k < size; k++) order[at++] = members[(start + k) % size];
        }
        return order;
    }
}
