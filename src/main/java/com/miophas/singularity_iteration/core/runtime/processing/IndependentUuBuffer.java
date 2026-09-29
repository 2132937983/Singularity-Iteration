// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Owned fractional UU account. One bucket is 1000 integer tank units.
 * R94 normal IC2 replication observations establish that excess withdrawn
 * fluid remains available after an item completes and across a world restart.
 * The machine must persist this account alongside its tank and work progress.
 * This class does not select item costs or decide when an item may be emitted.
 */
public final class IndependentUuBuffer {
    public interface FluidPort {
        int availableMilliBuckets();
        /** Return the amount actually withdrawn, including partial replies. */
        int drainMilliBuckets(int requested);
    }
    private static final double MAX_BUCKETS = Integer.MAX_VALUE / 1000.0;
    private static final double MAX_CREDIT = MAX_BUCKETS + .001;
    private static final Set<String> FIELDS = Set.of("version", "credit_buckets", "pending_millibuckets", "uncertain");
    private final BooleanSupplier serverThread;
    private final Runnable changed;
    private double credit;
    private int pending;
    private boolean busy, uncertain;
    private CompoundTag heldRaw;

    public IndependentUuBuffer(BooleanSupplier serverThread, Runnable changed) {
        this.serverThread = Objects.requireNonNull(serverThread, "server thread");
        this.changed = Objects.requireNonNull(changed, "change notification");
    }
    public double creditBuckets() { return credit; }
    public boolean blocked() { return uncertain || pending != 0 || heldRaw != null; }
    public int pendingMilliBuckets() { return pending; }

    /**
     * Pay the requested fractional amount, retaining any unspent withdrawal.
     * Incomplete withdrawals retain their actual credit for a later attempt.
     * An ambiguous callback is held for inspection rather than automatically
     * replayed. At most one external drain occurs per call, with no recipe scan.
     */
    public boolean consume(double buckets, FluidPort fluid) {
        if (!serverThread.getAsBoolean() || busy || blocked() || !Double.isFinite(buckets)
                || buckets <= 0 || buckets > MAX_BUCKETS || fluid == null) return false;
        busy = true;
        try {
            if (credit < buckets) {
                double request = Math.ceil((buckets - credit) * 1000.0);
                if (request < 1 || request > Integer.MAX_VALUE) return false;
                int amount = (int) request;
                int available;
                try { available = fluid.availableMilliBuckets(); }
                catch (RuntimeException failure) { return false; }
                if (available < amount) return false;
                pending = amount;
                changed.run();
                int received;
                try { received = fluid.drainMilliBuckets(amount); }
                catch (RuntimeException failure) { uncertain = true; changed.run(); return false; }
                if (received < 0 || received > amount) {
                    uncertain = true; changed.run(); return false;
                }
                pending = 0;
                credit += received / 1000.0;
                changed.run();
            }
            if (credit < buckets) return false;
            double remainder = credit - buckets;
            // A caller cannot turn a positive but unrepresentably small price
            // into repeated free output at a much larger stored balance.
            if (remainder == credit) return false;
            credit = remainder;
            changed.run();
            return true;
        } finally { busy = false; }
    }

    public CompoundTag save() {
        if (heldRaw != null) return heldRaw.copy();
        var tag = new CompoundTag();
        tag.putInt("version", 1); tag.putDouble("credit_buckets", credit);
        tag.putInt("pending_millibuckets", pending); tag.putBoolean("uncertain", uncertain);
        return tag;
    }

    /** Invalid or future data is retained verbatim and cannot authorize output. */
    public boolean load(CompoundTag tag) {
        Objects.requireNonNull(tag, "account data");
        if (!serverThread.getAsBoolean() || busy) return false;
        if (!FIELDS.equals(tag.getAllKeys()) || !tag.contains("version", Tag.TAG_INT) || tag.getInt("version") != 1
                || !tag.contains("credit_buckets", Tag.TAG_DOUBLE) || !tag.contains("pending_millibuckets", Tag.TAG_INT)
                || !tag.contains("uncertain", Tag.TAG_BYTE) || tag.getByte("uncertain") < 0 || tag.getByte("uncertain") > 1
                || !Double.isFinite(tag.getDouble("credit_buckets")) || tag.getDouble("credit_buckets") < 0
                || tag.getDouble("credit_buckets") > MAX_CREDIT || tag.getInt("pending_millibuckets") < 0) {
            heldRaw = tag.copy(); return false;
        }
        credit = tag.getDouble("credit_buckets"); pending = tag.getInt("pending_millibuckets");
        uncertain = tag.getBoolean("uncertain") || pending != 0;
        heldRaw = null;
        return !blocked();
    }
}
