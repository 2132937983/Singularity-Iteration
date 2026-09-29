// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.minecraft.world.item.ItemStack;

/**
 * Two-phase replication payment.  The caller supplies the actual UU account
 * and output slot, allowing the same transaction to serve SI and other energy
 * providers without importing their implementation details.
 */
public final class IndependentReplicationTransaction {
    public interface Account {
        long revision();
        long balance();
        long debit(long amount);
        default long refund(long amount) { return 0; }
    }
    public interface MatterAccount {
        long revision();
        double balance();
        boolean debit(double amount);
        default boolean refund(double amount) { return false; }
    }
    public interface OutputSlot {
        String itemKey();
        int count();
        int limit();
        boolean insert(String itemKey, int amount);
    }
    public interface StackOutputSlot extends OutputSlot {
        boolean insert(ItemStack stack);
    }
    private final BooleanSupplier serverThread;
    private boolean used;

    public IndependentReplicationTransaction(BooleanSupplier serverThread) {
        this.serverThread = Objects.requireNonNull(serverThread, "server thread");
    }

    public boolean commit(String itemKey, int amount, long euCost, double uuCost,
                          Account energy, MatterAccount matter, OutputSlot output) {
        if (used || !serverThread.getAsBoolean() || itemKey == null || itemKey.isBlank() || amount <= 0
                || euCost <= 0 || !Double.isFinite(uuCost) || uuCost <= 0
                || energy == null || matter == null || output == null) return false;
        if (output.count() < 0 || output.count() > output.limit() - amount
                || (!output.itemKey().isEmpty() && !output.itemKey().equals(itemKey))
                || energy.balance() < euCost || matter.balance() < uuCost) return false;
        long matterRevision = matter.revision();
        if (energy.debit(euCost) != euCost || matter.revision() != matterRevision) return false;
        if (!matter.debit(uuCost)) {
            energy.refund(euCost);
            return false;
        }
        if (!output.insert(itemKey, amount)) {
            matter.refund(uuCost);
            energy.refund(euCost);
            return false;
        }
        used = true;
        return true;
    }

    /** Item preserving overload used by machine inventories; component data is never flattened. */
    public boolean commit(ItemStack item, int amount, long euCost, double uuCost,
                          Account energy, MatterAccount matter, StackOutputSlot output) {
        if (item == null || item.isEmpty() || item.getCount() != 1 || output == null) return false;
        String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
        if (used || !serverThread.getAsBoolean() || amount <= 0 || euCost <= 0
                || !Double.isFinite(uuCost) || uuCost <= 0 || energy == null || matter == null) return false;
        if (output.count() < 0 || output.count() > output.limit() - amount
                || (!output.itemKey().isEmpty() && !output.itemKey().equals(key))
                || energy.balance() < euCost || matter.balance() < uuCost) return false;
        long matterRevision = matter.revision();
        if (energy.debit(euCost) != euCost || matter.revision() != matterRevision) return false;
        if (!matter.debit(uuCost)) { energy.refund(euCost); return false; }
        if (!output.insert(item.copyWithCount(amount))) {
            matter.refund(uuCost); energy.refund(euCost); return false;
        }
        used = true;
        return true;
    }

    public boolean used() { return used; }
}
