package com.miophas.singularity_iteration.core.runtime.uu;

import net.minecraft.world.item.ItemStack;

/**
 * 扫描结果的公开值模型：一次扫描产出的单件物品与其 UU/能源报价。
 *
 * <p>这是核心侧的规范类型；内置扫描机的 {@code ScanResult} 只是它的兼容外壳，
 * 其余代码（模式存储、复制、迁移）只依赖本类。
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
