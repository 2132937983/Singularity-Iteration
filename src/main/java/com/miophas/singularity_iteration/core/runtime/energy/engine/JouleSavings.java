// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

/**
 * 0.1.11.1: the Joules below one EU that a machine keeps between transfers with a Joule-based energy view.
 *
 * <p>The SI storage holds whole EU (and its own fraction, which a Joule view cannot reach). A Joule transfer moves
 * {@code unit} J per EU. Until 0.1.11.0 the view moved only whole EU: an insert of less than one EU was refused, and the
 * part of each insert below one EU was handed back, so a small steady flow never arrived. Now:
 * <ul>
 *   <li>Insert: the Joules join the savings; every whole EU of the savings goes into the storage, the rest
 *       ({@code < unit}) stays saved. When the storage takes fewer EU than offered, only the Joules of the EU it took
 *       count as accepted.</li>
 *   <li>Extract: the savings pay first; the rest is taken from the storage in whole EU (rounded up), and the Joules of
 *       that EU beyond the request stay saved.</li>
 * </ul>
 * The savings stay below one EU ({@code 0 <= saved < unit}). A new ratio drops them (they are below one EU of the old
 * ratio; {@link #dropped()} counts the Joules). Server thread only; not saved with the world (at most one EU less than
 * {@code unit} J goes when the machine unloads).
 */
public final class JouleSavings {
    /** Moves whole EU into or out of the storage; returns the EU moved ({@code 0..amount}). */
    @FunctionalInterface
    public interface EuMover { long move(long amount, boolean simulate); }

    private long unit;
    private long saved;
    private long dropped;

    /** The Joules kept below one EU ({@code 0 <= saved < unit}). */
    public long saved() { return saved; }

    /** The ratio of the savings (J per EU), or 0 before the first transfer. */
    public long unit() { return unit; }

    /** Joules dropped because the ratio changed. */
    public long dropped() { return dropped; }

    private void ratio(long next) {
        if (next == unit) return;
        dropped = Math.addExact(dropped, saved);
        saved = 0; unit = next;
    }

    /**
     * Inserts up to {@code joules} J at {@code unit} J per EU.
     *
     * @return the Joules not accepted ({@code 0..joules})
     * @throws IllegalStateException when {@code mover} reports more EU than asked or a negative amount
     */
    public long insert(long joules, long unit, boolean simulate, EuMover mover) {
        if (joules <= 0) return 0;
        if (unit <= 0) return joules;
        if (!simulate) ratio(unit);
        long held = unit == this.unit ? saved : 0;
        long offered = Math.min(joules, Long.MAX_VALUE - held);
        long total = held + offered;
        long eu = total / unit, rest = total % unit;
        long moved = eu == 0 ? 0 : mover.move(eu, simulate);
        if (moved < 0 || moved > eu) throw new IllegalStateException("Invalid EU transfer receipt");
        if (moved == eu) {
            if (!simulate) saved = rest;
            return joules - offered;
        }
        // The storage is full: the Joules of the EU it took (the savings went in first), no new savings.
        long taken = moved * unit;
        long accepted = Math.max(0, taken - held);
        if (!simulate) saved = Math.max(0, held - taken);
        return joules - accepted;
    }

    /**
     * Extracts up to {@code joules} J at {@code unit} J per EU.
     *
     * @return the Joules given ({@code 0..joules})
     * @throws IllegalStateException when {@code mover} reports more EU than asked or a negative amount
     */
    public long extract(long joules, long unit, boolean simulate, EuMover mover) {
        if (joules <= 0 || unit <= 0) return 0;
        if (!simulate) ratio(unit);
        long held = unit == this.unit ? saved : 0;
        long fromSaved = Math.min(held, joules);
        long need = joules - fromSaved;
        long eu = need / unit + (need % unit == 0 ? 0 : 1);
        long moved = eu == 0 ? 0 : mover.move(eu, simulate);
        if (moved < 0 || moved > eu) throw new IllegalStateException("Invalid EU transfer receipt");
        long movedJ = moved > Long.MAX_VALUE / unit ? Long.MAX_VALUE : moved * unit;
        long fromStorage = Math.min(need, movedJ);
        if (!simulate) saved = held - fromSaved + (movedJ - fromStorage);
        return fromSaved + fromStorage;
    }
}
