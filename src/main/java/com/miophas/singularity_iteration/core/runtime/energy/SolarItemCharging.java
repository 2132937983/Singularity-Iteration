// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import com.miophas.singularity_iteration.core.runtime.energy.engine.StagedCharge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Shared four-slot charging for the opt-in extended-solar candidate. */
public final class SolarItemCharging {
    private SolarItemCharging() { }
    private record BatteryAdapter(IItemAPI api) implements StagedCharge.Battery<ItemStack> {
        public ItemStack copy(ItemStack item) { return item.copy(); }
        public long stored(ItemStack item) { return api.getBatteryStored(item); }
        public long capacity(ItemStack item) { return api.getBatteryCapacity(item); }
        public long rate(ItemStack item) { return api.getChargeRate(item); }
        public long charge(ItemStack item, long offer) { return api.chargeBattery(item, offer, false); }
    }

    public static boolean charge(ItemStackHandler slots, int count, CustomEUEnergyStorage storage, IItemAPI api) {
        return chargeRange(slots, 0, count, storage, api);
    }

    public static boolean chargeRange(ItemStackHandler slots, int firstSlot, int count, CustomEUEnergyStorage storage, IItemAPI api) {
        if (firstSlot < 0 || firstSlot > slots.getSlots() || count < 0) throw new IllegalArgumentException("Invalid charging slots");
        var initial = storage.scexNetworkQuote();
        if (!storage.scexNetworkControlled() || !(initial.ownerLevel() instanceof ServerLevel level)
                || !level.getServer().isSameThread()) throw new IllegalStateException("Charging requires the owning server thread");
        boolean changed = false;
        var adapter = new BatteryAdapter(api);
        int end = firstSlot + Math.min(count, slots.getSlots() - firstSlot);
        for (int slot = firstSlot; slot < end; slot++) {
            var live = slots.getStackInSlot(slot);
            // An edited multi-item battery stack has ambiguous per-stack energy semantics.
            if (live.isEmpty() || live.getCount() != 1 || !api.isBattery(live)) continue;
            var before = live.copy();
            var quote = storage.scexNetworkQuote();
            long limit = storage.extract(Long.MAX_VALUE, true);
            var proposal = StagedCharge.prepare(before, quote.amount(), limit, adapter);
            if (proposal.isEmpty()) continue;
            var prepared = proposal.orElseThrow();
            if (prepared.item().getItem() != live.getItem() || prepared.item().getCount() != live.getCount()) continue;
            // Item adapters operate only on the copy. Reentrant changes revoke this proposal.
            if (slots.getStackInSlot(slot) != live || !ItemStack.matches(live, before)
                    || !storage.scexNetworkQuote().equals(quote)) continue;
            long debit = storage.extract(prepared.debit(), false);
            if (debit != prepared.debit()) throw new IllegalStateException("Stable source quote failed to debit");
            slots.setStackInSlot(slot, prepared.item());
            changed = true;
        }
        return changed;
    }
}
