// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.processing;

import com.miophas.singularity_iteration.core.runtime.processing.IndependentReplicationTransaction;
import com.miophas.singularity_iteration.core.runtime.uu.IndependentUuValueIndex;

import com.miophas.singularity_iteration.core.prefab.inventory.MachineItemHandler;
import com.miophas.singularity_iteration.common.item.resource.mio_icif_memory;
import com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Adapts an owned memory crystal to the independent replication transaction. */
public final class MemoryReplicationService {
    private MemoryReplicationService() { }

    public static boolean replicate(mio_icif_memory memory, ItemStack crystal, int amount,
                                    CustomEUEnergyStorage energy, FluidTank uuTank,
                                    MachineItemHandler inventory, int outputSlot) {
        Objects.requireNonNull(memory, "memory");
        if (crystal == null || !memory.hasData(crystal) || amount <= 0 || energy == null || uuTank == null
                || inventory == null || outputSlot < 0 || outputSlot >= inventory.getSlots()) return false;
        ItemStack source = memory.getStoredItemStack(crystal);
        long unitEu = memory.getEnergyCost(crystal);
        double unitUu = memory.getUuMatterCost(crystal);
        if (source.isEmpty() || unitEu <= 0 || !Double.isFinite(unitUu) || unitUu <= 0
                || amount > Long.MAX_VALUE / unitEu) return false;
        double totalUu = unitUu * amount;
        if (!Double.isFinite(totalUu) || totalUu <= 0) return false;
        var fluidType = uuTank.getFluid().copy();
        var tx = new IndependentReplicationTransaction(() -> true);
        var eu = new IndependentReplicationTransaction.Account() {
            public long revision() { return energy.getAmount(); }
            public long balance() { return energy.getAmount(); }
            public long debit(long value) { return energy.consumeEnergyInternal(value, false); }
            public long refund(long value) { return energy.generateEnergyInternal(value, false); }
        };
        var uu = new IndependentReplicationTransaction.MatterAccount() {
            public long revision() { return uuTank.getFluidAmount(); }
            public double balance() { return uuTank.getFluidAmount() / 1000.0; }
            public boolean debit(double value) {
                long milli = Math.round(value * 1000.0);
                return milli > 0 && uuTank.drain((int)Math.min(Integer.MAX_VALUE, milli), IFluidHandler.FluidAction.EXECUTE).getAmount() == milli;
            }
            public boolean refund(double value) {
                long milli = Math.round(value * 1000.0);
                return milli > 0 && uuTank.fill(new net.neoforged.neoforge.fluids.FluidStack(fluidType.getFluid(), (int)Math.min(Integer.MAX_VALUE, milli)), IFluidHandler.FluidAction.EXECUTE) == milli;
            }
        };
        var out = new IndependentReplicationTransaction.StackOutputSlot() {
            public String itemKey() { return inventory.getStackInSlot(outputSlot).isEmpty() ? "" : IndependentUuValueIndex.keyOf(inventory.getStackInSlot(outputSlot)).itemId(); }
            public int count() { return inventory.getStackInSlot(outputSlot).getCount(); }
            public int limit() { return Math.min(inventory.getSlotLimit(outputSlot), source.getMaxStackSize()); }
            public boolean insert(String key, int value) { return false; }
            public boolean insert(ItemStack stack) { return inventory.insertItem(outputSlot, stack, false).isEmpty(); }
        };
        return tx.commit(source, amount, unitEu * amount, totalUu, eu, uu, out);
    }
}
