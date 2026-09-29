// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage;
import com.miophas.singularity_iteration.core.runtime.energy.engine.EnergyAmount;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** One EU balance; output allowance is shared by native packets, items and FE. */
public final class FeLedger {
    public static final int FE_PER_EU = 4;
    private final CustomEUEnergyStorage storage;
    private final LongSupplier clock;
    private final BooleanSupplier active;
    private long tick = Long.MIN_VALUE;
    private long received;
    private EnergyAmount extracted = EnergyAmount.ZERO;
    private long outputRevision;
    private boolean busy;
    private int uncertain;

    public FeLedger(CustomEUEnergyStorage storage, LongSupplier clock, BooleanSupplier active) {
        this.storage = storage; this.clock = clock; this.active = active;
    }
    public static EnergyAmount eu(int fe) {
        if (fe < 0) throw new IllegalArgumentException("Negative FE");
        return new EnergyAmount(fe / FE_PER_EU, (fe % FE_PER_EU) * (EnergyAmount.UNITS / FE_PER_EU));
    }
    public static int fe(EnergyAmount eu) {
        if (eu.whole() >= (Integer.MAX_VALUE / FE_PER_EU) + 1L) return Integer.MAX_VALUE;
        return (int)Math.min(Integer.MAX_VALUE, eu.whole() * FE_PER_EU + eu.fraction() / (EnergyAmount.UNITS / FE_PER_EU));
    }
    private long limit(long eu) { return eu > Long.MAX_VALUE / FE_PER_EU ? Long.MAX_VALUE : Math.max(0, eu) * FE_PER_EU; }
    private long received() { return tick == clock.getAsLong() ? received : 0; }
    private EnergyAmount extracted() { return tick == clock.getAsLong() ? extracted : EnergyAmount.ZERO; }
    private EnergyAmount outputRoom() { return extracted().roomBelow(Math.max(0, storage.getMaxExtract())); }
    private void count(boolean input, long amount) {
        long now = clock.getAsLong();
        if (tick != now) { tick = now; received = 0; extracted = EnergyAmount.ZERO; }
        if (input) received += amount;
        else { extracted = extracted.add(eu(Math.toIntExact(amount))); outputRevision++; }
    }
    public int receive(int requested, boolean simulate) {
        if (requested <= 0 || busy || !active.getAsBoolean()) return 0;
        int amount = (int)Math.min(requested, Math.max(0, limit(storage.getMaxReceive()) - received()));
        amount = Math.min(amount, fe(storage.scexExactAmount().roomBelow(storage.getCapacity())));
        if (!simulate && amount > 0) { storage.scexTransferFe(amount, true); count(true, amount); }
        return amount;
    }
    /** Whole-EU item input shares the standard FE input budget and preserves fractional room. */
    public long receiveWholeEu(long requested, boolean simulate) {
        if (requested <= 0 || busy || !active.getAsBoolean()) return 0;
        long amount = Math.min(requested, Math.max(0, limit(storage.getMaxReceive()) - received()) / FE_PER_EU);
        amount = Math.min(amount, storage.scexExactAmount().roomBelow(storage.getCapacity()).whole());
        if (!simulate && amount > 0) {
            long actual = storage.generateEnergyInternal(amount, false);
            if (actual < 0 || actual > amount) throw new IllegalStateException("Invalid owned EU reception");
            count(true, actual * FE_PER_EU);
            return actual;
        }
        return amount;
    }
    public int extract(int requested, boolean simulate) {
        if (requested <= 0 || busy || uncertain != 0 || !active.getAsBoolean() || !storage.isOutputEnabled()) return 0;
        int amount = Math.min(requested, fe(outputRoom()));
        amount = Math.min(amount, fe(storage.scexExactAmount()));
        if (!simulate && amount > 0) { storage.scexTransferFe(amount, false); count(false, amount); }
        return amount;
    }
    /** Storage-slot EU items share the same output allowance as FE faces and active sending. */
    public long extractWholeEu(long requested, boolean simulate) {
        if (requested <= 0 || busy || uncertain != 0 || !active.getAsBoolean() || !storage.isOutputEnabled()) return 0;
        long amount = Math.min(requested, outputRoom().whole());
        amount = Math.min(amount, storage.scexExactAmount().whole());
        if (!simulate && amount > 0) {
            long actual = storage.consumeEnergyInternal(amount, false);
            if (actual < 0 || actual > amount) throw new IllegalStateException("Invalid owned EU extraction");
            countOutput(EnergyAmount.of(actual));
            return actual;
        }
        return amount;
    }
    private void countOutput(EnergyAmount amount) {
        long now = clock.getAsLong();
        if (tick != now) { tick = now; received = 0; extracted = EnergyAmount.ZERO; }
        extracted = extracted.add(amount); outputRevision++;
    }
    /** Immutable allowance quote. No balance or allowance is consumed by planning. */
    public static final class OutputQuote {
        private final FeLedger owner;
        private final long time, revision, limit;
        private final EnergyAmount spent, allowance;
        private OutputQuote(FeLedger owner) {
            this.owner = owner; time = owner.clock.getAsLong(); revision = owner.outputRevision;
            limit = owner.storage.getMaxExtract(); spent = owner.extracted();
            allowance = owner.outputAllowed() ? spent.roomBelow(Math.max(0, limit)) : EnergyAmount.ZERO;
        }
        public EnergyAmount allowance() { return allowance; }
        public EnergyAmount limitOffer(EnergyAmount balance) { return balance.min(allowance); }
        private boolean sameCounters() {
            return time == owner.clock.getAsLong() && revision == owner.outputRevision
                && limit == owner.storage.getMaxExtract() && spent.equals(owner.extracted())
                && !owner.busy && owner.uncertain == 0 && owner.storage.isOutputEnabled();
        }
    }
    /** Gross source debit, including wire loss; a simultaneous credit must not cancel it. */
    public static final class OutputDebit {
        private final OutputQuote quote;
        private final EnergyAmount nextSpent;
        public OutputDebit(OutputQuote quote, EnergyAmount amount) {
            this.quote = java.util.Objects.requireNonNull(quote);
            if (amount.isZero() || amount.compareTo(quote.allowance) > 0)
                throw new IllegalArgumentException("Native output exceeds the quoted allowance");
            nextSpent = quote.spent.add(amount);
        }
    }
    private boolean outputAllowed() {
        return !busy && uncertain == 0 && active.getAsBoolean() && storage.isOutputEnabled();
    }
    public OutputQuote quoteNativeOutput() { return new OutputQuote(this); }
    /**
     * Couple allowance writes to the owned network's atomic balance transaction.
     * The transaction must call the supplied guard in its final validation phase,
     * before any balance writes, and run no foreign callbacks after that guard.
     * The successful publication below only assigns already prepared state.
     */
    public static boolean commitNativeOutput(java.util.List<OutputDebit> requested,
            java.util.function.Function<BooleanSupplier, Boolean> transaction) {
        var debits = java.util.List.copyOf(requested);
        var owners = new java.util.IdentityHashMap<FeLedger, Boolean>();
        for (var debit : debits) if (owners.put(debit.quote.owner, Boolean.TRUE) != null)
            throw new IllegalArgumentException("Duplicate native output owner");
        boolean[] validated = {false};
        boolean committed = transaction.apply(() -> {
            validated[0] = false;
            for (var debit : debits) if (!debit.quote.owner.outputAllowed()) return false;
            // All potentially querying guards run before the final counter checks.
            for (var debit : debits) if (!debit.quote.sameCounters()) return false;
            validated[0] = true;
            return true;
        });
        if (!committed) return false;
        if (!validated[0]) throw new IllegalStateException("Network transaction omitted output validation");
        for (int i = 0; i < debits.size(); i++) {
            var debit = debits.get(i); var owner = debit.quote.owner;
            if (owner.tick != debit.quote.time) { owner.tick = debit.quote.time; owner.received = 0; }
            owner.extracted = debit.nextSpent; owner.outputRevision++;
        }
        return true;
    }
    /** Reserve before entering foreign code; release only the confirmed, rejected remainder. */
    public int push(IEnergyStorage target) {
        return push(target, () -> true);
    }
    public int push(IEnergyStorage target, BooleanSupplier allowed) {
        if (target == null || busy || uncertain != 0 || !active.getAsBoolean() || !allowed.getAsBoolean()) return 0;
        int offered = extract(Integer.MAX_VALUE, true);
        if (offered == 0) return 0;
        busy = true;
        try {
            if (!target.canReceive()) return 0;
            int quote = target.receiveEnergy(offered, true);
            if (quote <= 0 || quote > offered) return 0;
            // Quote callbacks may have changed the world or source through another interface.
            if (!active.getAsBoolean() || !allowed.getAsBoolean() || !storage.isOutputEnabled()) return 0;
            quote = Math.min(quote, fe(storage.scexExactAmount()));
            if (quote == 0) return 0;
            storage.scexTransferFe(quote, false); count(false, quote); uncertain = quote;
            int accepted = target.receiveEnergy(quote, false);
            if (accepted < 0 || accepted > quote) return 0;
            int refund = quote - accepted;
            storage.scexTransferFe(refund, true);
            extracted = extracted.subtract(eu(refund)); outputRevision++; uncertain = 0;
            return accepted;
        } finally { busy = false; }
    }
    /** Import only confirmed foreign extraction. The input reservation blocks recursive FE use. */
    public int pull(IEnergyStorage source, int lossFe, BooleanSupplier allowed) {
        if (source == null || lossFe < 0 || busy || !active.getAsBoolean() || !allowed.getAsBoolean()) return 0;
        int room = receive(Integer.MAX_VALUE, true);
        if (room == 0) return 0;
        busy = true;
        try {
            if (!source.canExtract()) return 0;
            int maximum = (int)Math.min(Integer.MAX_VALUE, (long)room + lossFe);
            int quoted = source.extractEnergy(maximum, true);
            if (quoted <= lossFe || quoted > maximum || !active.getAsBoolean() || !allowed.getAsBoolean()) return 0;
            // Foreign simulation can change policy. Recompute the actual input room before extraction.
            int currentRoom = (int)Math.min(Integer.MAX_VALUE, Math.max(0, limit(storage.getMaxReceive()) - received()));
            currentRoom = Math.min(currentRoom, fe(storage.scexExactAmount().roomBelow(storage.getCapacity())));
            if (currentRoom == 0) return 0;
            quoted = (int)Math.min(quoted, (long)currentRoom + lossFe);
            int extracted = source.extractEnergy(quoted, false);
            if (extracted < 0 || extracted > quoted) throw new IllegalStateException("Invalid foreign extraction receipt");
            int accepted = Math.max(0, extracted - lossFe);
            // Once extraction is confirmed, preserve the acquired balance even if the
            // callback shrank capacity. There is no callback between credit and accounting.
            if (accepted > 0) { storage.scexTransferFe(accepted, true); count(true, accepted); }
            return extracted;
        } finally { busy = false; }
    }
    public int uncertainOutput() { return uncertain; }
    public void loadUncertainOutput(int amount) { uncertain = Math.max(0, amount); outputRevision++; }
    public IEnergyStorage port(BooleanSupplier input, BooleanSupplier output) {
        return new IEnergyStorage() {
            @Override public int receiveEnergy(int amount, boolean simulate) { return input.getAsBoolean() ? receive(amount, simulate) : 0; }
            @Override public int extractEnergy(int amount, boolean simulate) { return output.getAsBoolean() ? extract(amount, simulate) : 0; }
            @Override public int getEnergyStored() { return fe(storage.scexExactAmount()); }
            @Override public int getMaxEnergyStored() { return fe(EnergyAmount.of(storage.getCapacity())); }
            @Override public boolean canReceive() { return input.getAsBoolean() && active.getAsBoolean() && storage.getMaxReceive() > 0; }
            @Override public boolean canExtract() { return output.getAsBoolean() && active.getAsBoolean() && uncertain == 0 && storage.isOutputEnabled() && storage.getMaxExtract() > 0; }
        };
    }
}
