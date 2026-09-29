// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.math.BigInteger;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Independently owned numeric state for a server-thread transaction. This is
 * storage, not a wrapper around arbitrary legacy setters. No callbacks run in
 * the write phase. Game adapters must use this state as their authoritative
 * balance before they can participate in the same transaction.
 */
public final class NetworkCell {
    private final Thread owner = Thread.currentThread();
    private long amount;
    private long fraction;
    private Object revision = new Object();
    private boolean retired;
    private Quote currentQuote;

    /** Opaque identity-bound snapshot; callers cannot manufacture one. */
    public static final class Quote {
        private final NetworkCell cell;
        private final Object revision;
        private final long amount;
        private final long fraction;
        private Quote(NetworkCell cell) {
            this.cell = cell; this.revision = cell.revision; this.amount = cell.amount; this.fraction = cell.fraction;
        }
        public long amount() { return amount; }
        public long fraction() { return fraction; }
        public EnergyAmount exactAmount() { return new EnergyAmount(amount, fraction); }
    }
    /** Include zero-delta participants to protect their demand snapshots too. */
    public record Write(Quote expected, long nextAmount, long nextFraction) {
        /** An integer transfer preserves the quoted fractional remainder. */
        public Write(Quote expected, long nextAmount) { this(expected, nextAmount, expected.fraction()); }
        public Write(Quote expected, EnergyAmount next) { this(expected, next.whole(), next.fraction()); }
        public EnergyAmount exactNextAmount() { return new EnergyAmount(nextAmount, nextFraction); }
        public Write {
            Objects.requireNonNull(expected, "expected");
            new EnergyAmount(nextAmount, nextFraction);
        }
    }

    public NetworkCell(long initialAmount) {
        this(EnergyAmount.of(initialAmount));
    }
    public NetworkCell(EnergyAmount initialAmount) {
        amount = initialAmount.whole(); fraction = initialAmount.fraction();
    }
    private void onOwner() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Wrong storage thread");
    }
    public Quote quote() {
        onOwner();
        if (retired) throw new IllegalStateException("Retired storage");
        if (currentQuote == null) currentQuote = new Quote(this);
        return currentQuote;
    }
    /** Loading, local consumption and policy changes invalidate prior quotes. */
    public void replace(long nextAmount) {
        replace(EnergyAmount.of(nextAmount));
    }
    public void replace(EnergyAmount nextAmount) {
        onOwner();
        if (retired) throw new IllegalStateException("Retired storage");
        Objects.requireNonNull(nextAmount, "nextAmount");
        Object nextRevision = new Object();
        amount = nextAmount.whole(); fraction = nextAmount.fraction(); revision = nextRevision; currentQuote = null;
    }
    /** Unload/removal permanently revokes this identity; reload creates a new cell. */
    public void retire() { onOwner(); retired = true; }

    /**
     * Validate everything before assigning any balance. Capacity/packet policy
     * belongs to the planner; observed over-capacity storage is representable.
     * The world guard runs once, before the final snapshot checks, so a guard
     * which changes a participant cannot authorize a stale transaction.
     */
    public static boolean commit(List<Write> requested, long dissipated, BooleanSupplier worldGuard) {
        return commit(requested, EnergyAmount.of(dissipated), worldGuard);
    }
    public static boolean commit(List<Write> requested, EnergyAmount dissipated, BooleanSupplier worldGuard) {
        Objects.requireNonNull(worldGuard, "worldGuard");
        var writes = List.copyOf(requested);
        var identities = new IdentityHashMap<NetworkCell, Boolean>();
        BigInteger balance = dissipated.units();
        for (var write : writes) {
            var cell = write.expected.cell;
            cell.onOwner();
            if (identities.put(cell, Boolean.TRUE) != null) throw new IllegalArgumentException("Duplicate cell");
            balance = balance.add(write.exactNextAmount().units())
                .subtract(write.expected.exactAmount().units());
        }
        if (balance.signum() != 0) throw new IllegalArgumentException("Unbalanced transaction");
        // Allocate all revisions before the guard or writes; no allocation is
        // needed after validation has succeeded.
        Object[] revisions = new Object[writes.size()];
        for (int i = 0; i < revisions.length; i++) revisions[i] = new Object();
        if (!worldGuard.getAsBoolean()) return false;
        for (var write : writes) {
            var q = write.expected;
            if (q.cell.retired || q.cell.revision != q.revision) return false;
        }
        for (int i = 0; i < writes.size(); i++) {
            var write = writes.get(i);
            write.expected.cell.amount = write.nextAmount;
            write.expected.cell.fraction = write.nextFraction;
            write.expected.cell.revision = revisions[i];
            write.expected.cell.currentQuote = null;
        }
        return true;
    }
}
