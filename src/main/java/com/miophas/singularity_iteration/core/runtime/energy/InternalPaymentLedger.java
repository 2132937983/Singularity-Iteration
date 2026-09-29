// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

/** Whole-EU escrow metadata. The containing storage remains the only balance owner. */
public final class InternalPaymentLedger {
    private volatile long reserved;
    private Thread thread;

    public long reserved() { return reserved; }
    public long availableCapacity(long capacity) { return capacity <= reserved ? 0 : capacity - reserved; }
    public void checkPendingThread() { if (reserved != 0) checkThread(); }

    /** Reserve only an amount already present in the owner's spendable balance. */
    public Ticket reserve(long available, long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Payment must be positive");
        if (reserved != 0) checkThread();
        if (available < amount) return null;
        long next = Math.addExact(reserved, amount);
        thread = Thread.currentThread();
        reserved = next;
        return new Ticket(this, amount, thread);
    }

    private void checkThread() {
        if (Thread.currentThread() != thread) throw new IllegalStateException("Payment changed off its owner thread");
    }

    public static final class Ticket {
        private final InternalPaymentLedger owner;
        private final long amount;
        private final Thread thread;
        private State state = State.PENDING;
        private enum State { PENDING, COMMITTED, CANCELLED }

        private Ticket(InternalPaymentLedger owner, long amount, Thread thread) {
            this.owner = owner; this.amount = amount; this.thread = thread;
        }
        public long amount() { return amount; }
        public boolean isPending() { return state == State.PENDING; }
        public boolean commit() { return finish(State.COMMITTED); }
        public boolean cancel() { return finish(State.CANCELLED); }
        private boolean finish(State next) {
            if (Thread.currentThread() != thread) throw new IllegalStateException("Payment settled off its owner thread");
            if (state != State.PENDING) return false;
            owner.checkThread();
            owner.reserved = Math.subtractExact(owner.reserved, amount);
            state = next;
            return true;
        }
    }
}
