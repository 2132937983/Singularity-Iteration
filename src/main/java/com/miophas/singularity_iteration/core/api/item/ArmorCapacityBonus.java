// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.item;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.item.ItemStack;

/**
 * <p><b>EN:</b> Extra EU capacity of one electric armor piece from its own data (0.1.8.5), for
 * example installed capacitor units. {@code AbstractElectricArmor#getMaxEnergy(ItemStack)} adds
 * the sum of all providers to the base capacity. Stored energy above a lowered capacity is cut.
 * <p><b>ZH:</b> 单件电力护甲由自身数据带来的额外 EU 容量（0.1.8.5），例如已安装的电容单元。
 * {@code AbstractElectricArmor#getMaxEnergy(ItemStack)} 把所有提供者之和加到基础容量上。容量降低时超出的储能被截去。
 * <p><b>JA:</b> 電力アーマー 1 部位が自身のデータから得る追加の EU 容量（0.1.8.5）。例：取り付けたキャパシタユニット。
 * {@code AbstractElectricArmor#getMaxEnergy(ItemStack)} は全プロバイダーの合計を基本容量に足す。容量が下がると超えた分の蓄電は切り捨てる。
 */
public final class ArmorCapacityBonus {
    private ArmorCapacityBonus() {}

    /** EN: One source of extra capacity. ZH: 一个额外容量来源。JA: 追加容量の 1 つの供給元。 */
    @FunctionalInterface
    public interface Provider {
        /** @return extra EU capacity of {@code stack}, 0 or more */
        long bonus(ItemStack stack);
    }

    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

    /** EN: Registers a provider once during mod construction. ZH: 在模组构造期间注册一次。JA: MOD 構築中に 1 回登録する。 */
    public static void register(Provider provider) {
        PROVIDERS.add(java.util.Objects.requireNonNull(provider));
    }

    /** EN: Sum of all providers for {@code stack} (0 for an empty stack). ZH: {@code stack} 的所有提供者之和。JA: {@code stack} に対する全プロバイダーの合計。 */
    public static long of(ItemStack stack) {
        if (stack.isEmpty() || PROVIDERS.isEmpty()) return 0;
        long sum = 0;
        for (Provider p : PROVIDERS) {
            long b = p.bonus(stack);
            if (b > 0) sum = sum > Long.MAX_VALUE - b ? Long.MAX_VALUE : sum + b;
        }
        return sum;
    }
}
