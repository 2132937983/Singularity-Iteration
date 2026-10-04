// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.api.item.BatteryTransfer;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.api.item.IElectricArmorItem;
import com.miophas.singularity_iteration.core.prefab.item.EmergencyChargePolicy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Item charging for storage blocks that keep their own buffer outside the FE ledger (multiblock
 * cores such as the GESU, whose output budget belongs to its ports). The caller moves exactly the
 * returned amount into / out of its buffer; every method works on the stack in place.
 */
public final class DirectItemCharging {
    private DirectItemCharging() { }

    /** Puts up to {@code maxEu} into {@code stack}; returns the EU it accepted. */
    public static long charge(ItemStack stack, long maxEu) {
        if (stack.isEmpty() || stack.getCount() != 1 || maxEu <= 0) return 0;
        if (stack.getItem() instanceof IBatteryItem battery) {
            long offer = maxEu;
            if (!(battery instanceof IElectricArmorItem)) {
                long rate = battery.getChargeRate(stack);
                if (rate > 0) offer = Math.min(offer, rate);
            }
            return BatteryTransfer.transfer(stack, battery, offer, true, false);
        }
        IEnergyStorage cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap == null || !cap.canReceive()) return 0;
        int fe = (int) Math.min(Integer.MAX_VALUE / BatteryFeCapability.FE_PER_EU, maxEu) * BatteryFeCapability.FE_PER_EU;
        int simulated = cap.receiveEnergy(fe, true);
        long eu = simulated / BatteryFeCapability.FE_PER_EU;          // whole EU only
        if (eu <= 0) return 0;
        int accepted = cap.receiveEnergy((int) eu * BatteryFeCapability.FE_PER_EU, false);
        return Math.max(0, accepted) / BatteryFeCapability.FE_PER_EU;
    }

    /**
     * Takes up to {@code maxEu} out of {@code stack} (batteries, FE items, redstone as emergency
     * fuel); returns the EU removed. A redstone stack shrinks by one when its full value fits.
     */
    public static long discharge(ItemStack stack, long maxEu) {
        if (stack.isEmpty() || maxEu <= 0) return 0;
        if (stack.is(Items.REDSTONE)) {
            long value = EmergencyChargePolicy.energyPerItem();
            if (value <= 0 || value > maxEu) return 0;
            stack.shrink(1);
            return value;
        }
        if (stack.getCount() != 1 || !BatteryTransfer.canProvideExternally(stack)) return 0;
        if (stack.getItem() instanceof IBatteryItem battery) return BatteryTransfer.transfer(stack, battery, maxEu, false, false);
        IEnergyStorage cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap == null || !cap.canExtract()) return 0;
        int fe = (int) Math.min(Integer.MAX_VALUE / BatteryFeCapability.FE_PER_EU, maxEu) * BatteryFeCapability.FE_PER_EU;
        long eu = cap.extractEnergy(fe, true) / BatteryFeCapability.FE_PER_EU;
        if (eu <= 0) return 0;
        return Math.max(0, cap.extractEnergy((int) eu * BatteryFeCapability.FE_PER_EU, false)) / BatteryFeCapability.FE_PER_EU;
    }

    public static boolean canCharge(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof IBatteryItem) return true;
        IEnergyStorage cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.canReceive();
    }

    public static boolean canDischarge(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.REDSTONE)) return true;
        if (stack.getItem() instanceof IBatteryItem) return BatteryTransfer.canProvideExternally(stack);
        IEnergyStorage cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.canExtract();
    }
}
