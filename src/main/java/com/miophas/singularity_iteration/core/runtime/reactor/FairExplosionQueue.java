// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.reactor;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Single-threaded round-robin queue; a step is one bounded blast operation. */
public final class FairExplosionQueue {
    private static final class Work {
        final Object owner, category, identity;
        final BooleanSupplier step;
        final Runnable complete;
        final Consumer<RuntimeException> failed;
        long steps;
        Work(Object owner, Object category, Object identity, BooleanSupplier step,
             Runnable complete, Consumer<RuntimeException> failed) {
            this.owner = owner; this.category = category; this.identity = identity;
            this.step = step; this.complete = complete; this.failed = failed;
        }
    }
    private final ArrayDeque<Work> ready = new ArrayDeque<>();
    private final IdentityHashMap<Object, Work> active = new IdentityHashMap<>();
    private final LongSupplier clock;
    public static final int DEFAULT_CAPACITY = 256;
    private final int capacity;
    private long lastTick;
    private boolean hasTick, running;

    public FairExplosionQueue() { this(System::nanoTime); }
    public FairExplosionQueue(LongSupplier clock) { this(clock, DEFAULT_CAPACITY); }
    public FairExplosionQueue(LongSupplier clock, int capacity) {
        this.clock = Objects.requireNonNull(clock);
        if (capacity <= 0) throw new IllegalArgumentException("Positive explosion queue capacity required");
        this.capacity = capacity;
    }

    /** Legacy fire-and-forget entry point retained for binary/source compatibility. */
    public void submit(Object owner, Object category, Object identity, BooleanSupplier step,
                       Runnable complete, Consumer<RuntimeException> failed) {
        admit(owner, category, identity, step, complete, failed, true);
    }

    /**
     * Admit a new task without evicting work already in progress.
     *
     * @return {@code true} only when this identity was newly queued. A duplicate
     *         or a full queue returns {@code false}, allowing callers to perform
     *         irreversible side effects only after admission succeeds.
     */
    public boolean trySubmit(Object owner, Object category, Object identity, BooleanSupplier step,
                             Runnable complete, Consumer<RuntimeException> failed) {
        return admit(owner, category, identity, step, complete, failed, false);
    }

    private boolean admit(Object owner, Object category, Object identity, BooleanSupplier step,
                          Runnable complete, Consumer<RuntimeException> failed,
                          boolean reportCapacityRejection) {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(identity);
        if (active.containsKey(identity)) return false;
        Objects.requireNonNull(failed);
        if (active.size() >= capacity) {
            // Preserve every previously accepted/partially executed task. The
            // legacy fire-and-forget path reports rejection; admission-aware
            // callers observe it through the boolean result instead. Never evict
            // a task or execute new work synchronously outside the Tick budget.
            if (reportCapacityRejection) {
                failed.accept(new IllegalStateException("Explosion backlog capacity reached: " + capacity));
            }
            return false;
        }
        var work = new Work(owner, category, identity, Objects.requireNonNull(step),
                Objects.requireNonNull(complete), Objects.requireNonNull(failed));
        active.put(identity, work);
        ready.addLast(work);
        return true;
    }

    /** At most maxSteps across all owners; the time limit is cooperative between individual steps. */
    public int tick(long tick, int maxSteps, long maxNanos) {
        if (running || hasTick && lastTick == tick || maxSteps <= 0 || maxNanos <= 0) return 0;
        hasTick = true;
        lastTick = tick;
        running = true;
        int steps = 0;
        long start = clock.getAsLong();
        try {
            while (!ready.isEmpty() && steps < maxSteps
                    && (steps == 0 || clock.getAsLong() - start < maxNanos)) {
                var work = ready.removeFirst();
                if (active.get(work.identity) != work) continue;
                steps++;
                work.steps++;
                try {
                    boolean complete = work.step.getAsBoolean();
                    if (active.get(work.identity) != work) continue;
                    if (complete) {
                        active.remove(work.identity);
                        work.complete.run();
                    } else {
                        ready.addLast(work);
                    }
                } catch (RuntimeException failure) {
                    active.remove(work.identity, work);
                    // The owner receives an explicit failure; a partial mutation is never replayed.
                    work.failed.accept(failure);
                }
            }
        } finally {
            running = false;
        }
        return steps;
    }

    public int size() { return active.size(); }
    /** O(1) admission probe used before expensive blast snapshots. */
    public boolean hasCapacity() { return active.size() < capacity; }

    /** Cancel one reserved/queued identity without touching other blast work. */
    public void cancel(Object identity) {
        active.remove(identity);
        ready.removeIf(work -> active.get(work.identity) != work);
    }

    /** Returns -1 after a task leaves the queue; no completed task references are retained. */
    public long steps(Object identity) {
        var work = active.get(identity);
        return work == null ? -1 : work.steps;
    }
    public int sizeForOwner(Object owner) {
        int count = 0;
        for (var work : active.values()) if (work.owner == owner) count++;
        return count;
    }
    public int size(Object category) {
        int count = 0;
        for (var work : active.values()) if (work.category == category) count++;
        return count;
    }
    public void removeOwner(Object owner) { remove(owner, null, false); }
    public void removeCategory(Object category) { remove(null, category, true); }
    public void remove(Object owner, Object category) { remove(owner, category, true); }
    private void remove(Object owner, Object category, boolean matchCategory) {
        active.values().removeIf(work -> (owner == null || work.owner == owner)
                && (!matchCategory || work.category == category));
        ready.removeIf(work -> active.get(work.identity) != work);
    }
    public void clear() { active.clear(); ready.clear(); }
}
