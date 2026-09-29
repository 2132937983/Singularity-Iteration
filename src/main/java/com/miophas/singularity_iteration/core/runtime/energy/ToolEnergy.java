// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage;
import net.minecraft.world.item.ItemStack;

/** Owned single-tool charging. Internal work debit is independent of the outward cable packet limit. */
public final class ToolEnergy {
    private ToolEnergy() { }
    public static long charge(MachineItemHandler inventory, int slot, CustomEUEnergyStorage energy, IItemAPI api) {
        if (slot < 0 || slot >= inventory.getSlots()) return 0;
        var quote = energy.scexNetworkQuote();
        var before = inventory.getStackInSlot(slot).copy();
        if (before.isEmpty() || before.getCount() != 1 || !api.isElectricTool(before)) return 0;
        var after = before.copy();
        long stored = api.getElectricToolStored(after), capacity = api.getElectricToolMaxEnergy(after);
        if (stored < 0 || capacity <= stored) return 0;
        long offered = energy.consumeEnergyInternal(Math.min(capacity - stored, Math.max(0, quote.amount())), true);
        if (offered <= 0) return 0;
        long accepted = api.chargeElectricTool(after, offered, false);
        if (accepted <= 0 || accepted > offered || api.getElectricToolStored(after) != stored + accepted
                || after.getItem() != before.getItem() || after.getCount() != 1
                || !ItemStack.matches(before, inventory.getStackInSlot(slot))
                || !energy.scexNetworkQuote().equals(quote)) return 0;
        long debit = energy.consumeEnergyInternal(accepted, false);
        if (debit != accepted) throw new IllegalStateException("Stable owned charging source failed to debit");
        if (!inventory.scexCommitSlots(new int[]{slot}, new ItemStack[]{before}, new ItemStack[]{after}))
            throw new IllegalStateException("Stable owned charging slot changed during internal debit");
        return accepted;
    }
}
