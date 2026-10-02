// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.api.item.BatteryTransfer;

import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.item.IElectricArmorItem;
import net.minecraft.world.item.ItemStack;

/** Prepare an owned SI charging slot before paying its actual accepted energy. */
public final class StorageItemCharging {
    private StorageItemCharging() { }

    /** True means this is an SI item, including a full or currently rejected stack. */
    public static boolean charge(FeLedger ledger, CustomEUEnergyStorage storage, MachineItemHandler inventory, int slot) {
        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty() || !(current.getItem() instanceof IBatteryItem battery)) return false;
        if (current.getCount() != 1) return true;
        long offered = ledger.extractWholeEu(Long.MAX_VALUE, true);
        if (offered <= 0) return true;
        // Preserve armor's existing box-limited rate; other batteries also publish their own rate.
        if (!(battery instanceof IElectricArmorItem)) offered = Math.min(offered, Math.max(0, battery.getChargeRate(current)));
        offered = BatteryTransfer.transfer(current, battery, offered, true, true);
        if (offered <= 0) return true;
        ItemStack before = current.copy(), after = before.copy();
        var quote = storage.scexNetworkQuote(); var exact = storage.scexExactAmount();
        long accepted = BatteryTransfer.transfer(after, battery, offered, true, false);
        if (accepted <= 0 || after.getCount() != 1 || after.getItem() != before.getItem()
                || !ItemStack.matches(before, inventory.getStackInSlot(slot))
                || !quote.equals(storage.scexNetworkQuote()) || !exact.equals(storage.scexExactAmount())
                || ledger.extractWholeEu(accepted, true) != accepted) return true;
        if (ledger.extractWholeEu(accepted, false) != accepted)
            throw new IllegalStateException("Owned SI charging allowance changed during debit");
        if (!inventory.scexCommitSlots(new int[]{slot}, new ItemStack[]{before}, new ItemStack[]{after}))
            throw new IllegalStateException("Owned SI charging slot changed during debit");
        return true;
    }
}
