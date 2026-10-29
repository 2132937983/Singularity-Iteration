// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.exchange;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.10.27 (X2): the contents of the container in a settlement slot (futures machine panel, Trade Terminal): a shulker
 * box or backpack (item handler capability), an AE2 storage cell (items, and UU-matter of a fluid cell), or a fluid
 * container of UU-matter. Opened by {@link SettlementStorages#open}; one storage serves one action on the server thread.
 *
 * <p>Items are counted per prototype (an item with its components, count 1). Extraction and insertion act at once on the
 * container; {@link #commit()} writes what needs writing (the AE2 cell, the emptied fluid container) and must follow every
 * action that changed the storage.
 */
public interface SettlementStorage {
    /** What the slot holds: 1 items (item handler), 2 AE2 cell, 3 fluid container. */
    int kind();

    /** The items per prototype, in a stable order. */
    List<Entry> items();

    /** Takes up to {@code amount} items equal to {@code proto} (item and components); returns the amount taken. */
    long extract(ItemStack proto, long amount, boolean simulate);

    /** Puts up to {@code amount} items equal to {@code proto} into the container; returns the amount that went in. */
    long insert(ItemStack proto, long amount, boolean simulate);

    /** UU-matter in the container (mB). */
    long uu();

    /** Takes up to {@code mb} of UU-matter; returns the mB taken. */
    long extractUu(long mb, boolean simulate);

    /** Writes the changes (AE2: persists the cell; fluid: puts the emptied container back into the slot). */
    void commit();

    record Entry(ItemStack proto, long count) { }

    /** Plain items of {@code item} (the item without changed components). */
    default long plain(Item item) {
        long n = 0;
        for (Entry e : items()) if (e.proto().getItem() == item && e.proto().isComponentsPatchEmpty()) n += e.count();
        return n;
    }

    /** The plain items of this container as the source of a delivery. */
    default ItemSource source() {
        SettlementStorage self = this;
        return new ItemSource() {
            @Override
            public long count(Item item) { return self.plain(item); }

            @Override
            public boolean take(Item item, long amount) {
                if (amount <= 0) return amount == 0;
                ItemStack proto = new ItemStack(item);
                if (self.extract(proto, amount, true) < amount) return false;
                long got = self.extract(proto, amount, false);
                if (got < amount) {
                    // cannot happen after the simulation; put back what came out so nothing is lost
                    self.insert(proto, got, false);
                    return false;
                }
                return true;
            }

            @Override
            public long count(ItemMatch match) {
                long n = 0;
                for (Entry e : self.items()) if (e.proto().isComponentsPatchEmpty() && match.test(e.proto())) n += e.count();
                return n;
            }

            @Override
            public boolean take(ItemMatch match, long amount) {
                if (amount <= 0) return amount == 0;
                if (count(match) < amount) return false;
                // the requested item first, then the other items of its tags; on a shortfall everything taken goes back
                List<Entry> entries = List.copyOf(self.items()), taken = new java.util.ArrayList<>();
                long remaining = amount;
                for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                    for (Entry e : entries) {
                        if (remaining <= 0) break;
                        if (!e.proto().isComponentsPatchEmpty() || !match.test(e.proto()) || match.exact(e.proto()) != (pass == 0)) continue;
                        long got = self.extract(e.proto(), Math.min(remaining, e.count()), false);
                        if (got > 0) { taken.add(new Entry(e.proto(), got)); remaining -= got; }
                    }
                }
                if (remaining <= 0) return true;
                for (Entry e : taken) self.insert(e.proto(), e.count(), false);
                return false;
            }
        };
    }
}
