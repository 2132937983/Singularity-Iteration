package com.miophas.singularity_iteration.core.runtime.uu;

import net.minecraft.world.item.ItemStack;

/**
 * Public value model of a scan result: the single item from one scan and its UU and energy cost.
 *
 * <p>This is the canonical type on the core side. The {@code ScanResult} of the built-in scanner is only a compatibility wrapper for it.
 * All other code (pattern storage, replication, migration) depends only on this class.
 */
public class UuScanResult {

    public final ItemStack item;
    public final double uuMatterCostBuckets;
    public final long energyCost;

    public UuScanResult(ItemStack item, double buckets, long energy) {
        if (!valid(item, buckets, energy)) throw new IllegalArgumentException("Invalid scan result");
        this.item = item.copy();
        this.uuMatterCostBuckets = buckets;
        this.energyCost = energy;
    }

    public static boolean valid(ItemStack item, double buckets, long energy) {
        return item != null && !item.isEmpty() && item.getCount() == 1 && validCosts(buckets, energy);
    }

    public static boolean validCosts(double buckets, long energy) {
        return Double.isFinite(buckets) && buckets > 0 && buckets < Long.MAX_VALUE / 1000.0 && energy >= 0;
    }

    public long getUuMatterCostMB() {
        return (long) Math.ceil(uuMatterCostBuckets * 1000);
    }
}
