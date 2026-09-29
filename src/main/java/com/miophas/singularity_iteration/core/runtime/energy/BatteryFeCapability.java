// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy;

import com.miophas.singularity_iteration.core.api.item.BatteryTransfer;

import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Standard NeoForge FE view of an SI whole-EU battery item. */
public final class BatteryFeCapability implements IEnergyStorage {
    public static final int FE_PER_EU = 4;
    private final ItemStack stack;
    private final IBatteryItem battery;

    private BatteryFeCapability(ItemStack stack, IBatteryItem battery) { this.stack = stack; this.battery = battery; }

    public static BatteryFeCapability create(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getCount() != 1
                || !(stack.getItem() instanceof IBatteryItem battery)) return null;
        return new BatteryFeCapability(stack, battery);
    }

    private static int toFe(long eu) {
        if (eu <= 0) return 0;
        if (eu >= Integer.MAX_VALUE / (long) FE_PER_EU)
            return Integer.MAX_VALUE - (Integer.MAX_VALUE % FE_PER_EU);
        return (int) eu * FE_PER_EU;
    }

    private long rate() { return Math.max(0L, battery.getChargeRate(stack)); }

    @Override public int receiveEnergy(int maxReceive, boolean simulate) {
        if (maxReceive < FE_PER_EU || !canReceive()) return 0;
        long amount = Math.min(maxReceive / FE_PER_EU, rate());
        amount = Math.min(amount, Math.max(0L, battery.getMaxEnergy(stack) - battery.getEnergy(stack)));
        if (amount <= 0) return 0;
        if (!simulate) {
            long accepted = battery.addEnergy(stack, amount);
            if (accepted < 0 || accepted > amount) throw new IllegalStateException("SI battery returned invalid FE acceptance");
            amount = accepted;
        }
        return toFe(amount);
    }

    @Override public int extractEnergy(int maxExtract, boolean simulate) {
        if (maxExtract < FE_PER_EU || !canExtract()) return 0;
        long amount = Math.min(maxExtract / FE_PER_EU, rate());
        amount = Math.min(amount, Math.max(0L, battery.getEnergy(stack)));
        if (amount <= 0) return 0;
        if (!simulate) {
            long extracted = battery.extractEnergy(stack, amount);
            if (extracted < 0 || extracted > amount) throw new IllegalStateException("SI battery returned invalid FE extraction");
            amount = extracted;
        }
        return toFe(amount);
    }

    @Override public int getEnergyStored() { return toFe(battery.getEnergy(stack)); }
    @Override public int getMaxEnergyStored() { return toFe(battery.getMaxEnergy(stack)); }
    @Override public boolean canExtract() { return BatteryTransfer.canProvideExternally(stack) && rate() > 0 && battery.getEnergy(stack) > 0; }
    @Override public boolean canReceive() { return stack.getCount() == 1 && rate() > 0 && battery.getEnergy(stack) < battery.getMaxEnergy(stack); }
}
