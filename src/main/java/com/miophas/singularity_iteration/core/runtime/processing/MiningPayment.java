// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage;
import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Owned payment held between admission and successful world removal. Failed removal keeps paid credit. */
public final class MiningPayment {
    private long machineCredit, toolCredit;
    private boolean busy;
    private final Runnable changed;
    public MiningPayment(Runnable changed) { this.changed = changed; }
    public boolean isBusy() { return busy; }
    public long machineCredit() { return machineCredit; }
    public long toolCredit() { return toolCredit; }

    public boolean attempt(CustomEUEnergyStorage energy, MachineItemHandler inventory, int toolSlot, IItemAPI api,
                           long machineCost, long toolCost, Function<Permit, Boolean> action) {
        if (busy || machineCost <= 0 || toolCost < 0) return false;
        busy = true;
        var permit = new Permit(energy, inventory, toolSlot, api, machineCost, toolCost);
        try {
            boolean result = action.apply(permit);
            if (result && !permit.completed) throw new IllegalStateException("Mining completed without settled payment");
            return result;
        } finally { permit.active = false; busy = false; }
    }

    public final class Permit {
        private final CustomEUEnergyStorage energy;
        private final MachineItemHandler inventory;
        private final int slot;
        private final IItemAPI api;
        private final long machineCost, toolCost;
        private boolean active = true, funded, completed, funding;
        private Permit(CustomEUEnergyStorage energy, MachineItemHandler inventory, int slot, IItemAPI api,
                       long machineCost, long toolCost) {
            this.energy = energy; this.inventory = inventory; this.slot = slot; this.api = api;
            this.machineCost = machineCost; this.toolCost = toolCost;
        }
        /** Called only after world/claim/loot preflight. Tool adapters receive a detached single-item stack. */
        public boolean fund() {
            if (!active || completed || funding) return false;
            if (funded) return true;
            funding = true;
            try { return fundOnce(); }
            finally { funding = false; }
        }
        private boolean fundOnce() {
            long machineNeeded = Math.max(0, machineCost - machineCredit);
            long toolNeeded = Math.max(0, toolCost - toolCredit);
            var quote = energy.scexNetworkQuote();
            if (energy.consumeEnergyInternal(machineNeeded, true) != machineNeeded) return false;
            ItemStack before = ItemStack.EMPTY, after = ItemStack.EMPTY;
            if (toolNeeded > 0) {
                if (slot < 0 || slot >= inventory.getSlots()) return false;
                before = inventory.getStackInSlot(slot).copy();
                if (before.isEmpty() || before.getCount() != 1 || !api.isElectricTool(before)) return false;
                after = before.copy();
                long stored = api.getElectricToolStored(after);
                if (stored < toolNeeded || api.dischargeElectricTool(after, toolNeeded, false) != toolNeeded
                        || api.getElectricToolStored(after) != stored - toolNeeded
                        || after.getItem() != before.getItem() || after.getCount() != 1) return false;
            }
            if (!energy.scexNetworkQuote().equals(quote)
                    || toolNeeded > 0 && !ItemStack.matches(before, inventory.getStackInSlot(slot))) return false;
            long extracted = energy.consumeEnergyInternal(machineNeeded, false);
            if (extracted < 0 || extracted > machineNeeded) throw new IllegalStateException("Invalid owned energy debit");
            machineCredit += extracted;
            changed.run();
            if (extracted != machineNeeded) return false;
            // Publish paid credit before inventory callbacks, which can save or attempt reentry.
            toolCredit += toolNeeded;
            if (toolNeeded > 0 && !inventory.scexCommitSlots(new int[]{slot}, new ItemStack[]{before}, new ItemStack[]{after})) {
                toolCredit -= toolNeeded; changed.run(); return false;
            }
            funded = true;
            changed.run();
            return true;
        }
        public void complete() {
            complete(() -> {});
        }
        public void complete(Runnable accountRemoval) {
            if (!active || !funded || completed) throw new IllegalStateException("Invalid mining settlement");
            machineCredit -= machineCost; toolCredit -= toolCost;
            completed = true; accountRemoval.run(); changed.run();
        }
    }
    public CompoundTag save() {
        var tag = new CompoundTag(); tag.putLong("machine_eu", machineCredit); tag.putLong("tool_eu", toolCredit); return tag;
    }
    public void load(CompoundTag tag) {
        if (busy) throw new IllegalStateException("Cannot replace an active mining payment");
        machineCredit = Math.max(0, tag.getLong("machine_eu")); toolCredit = Math.max(0, tag.getLong("tool_eu"));
    }
}
