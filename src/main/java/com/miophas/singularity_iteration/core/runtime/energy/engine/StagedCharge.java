// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import java.util.Optional;

/** Prepare on a detached item; the owner must recheck and commit source/slot on its server thread. */
public final class StagedCharge {
    private StagedCharge() { }
    public interface Battery<T> {
        T copy(T item);
        long stored(T item);
        long capacity(T item);
        long rate(T item);
        long charge(T item, long offer);
    }
    public record Prepared<T>(T item, long debit) { }

    public static <T> Optional<Prepared<T>> prepare(T original, long available, long sourceLimit, Battery<T> api) {
        if (available <= 0 || sourceLimit <= 0) return Optional.empty();
        T copy = api.copy(original);
        if (copy == null || copy == original) throw new IllegalArgumentException("A detached item copy is required");
        long before = api.stored(copy), capacity = api.capacity(copy), rate = api.rate(copy);
        if (before < 0 || capacity <= before || rate <= 0) return Optional.empty();
        long offer = Math.min(Math.min(available, sourceLimit), Math.min(rate, capacity - before));
        long accepted = api.charge(copy, offer);
        long after = api.stored(copy);
        // Reject lying or inconsistent adapters without debiting/publishing anything.
        if (accepted <= 0 || accepted > offer || after < before || after > capacity
                || after - before != accepted || api.capacity(copy) != capacity) return Optional.empty();
        return Optional.of(new Prepared<>(copy, accepted));
    }
}
