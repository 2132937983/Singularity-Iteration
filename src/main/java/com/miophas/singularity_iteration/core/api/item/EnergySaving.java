// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.item;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Energy Saving enchantment (0.1.7.35): each level removes 5 % of the energy that the item uses,
 * up to level V (75 % of the cost).
 *
 * <p>Every energy debit of an electric tool, weapon or armor piece goes through {@link #apply}:
 * tool use and paid actions ({@code BatteryTransfer.consume}), damage absorption, fall
 * protection, flight, jetpacks, suit units and Special Maneuver Mode. Charging and battery
 * transfers do not use it.
 *
 * <p>Small costs keep the exact average: the fraction of an EU is paid as a random extra EU with
 * the same probability (1 EU at level I costs 1 EU in 95 of 100 calls).
 */
public final class EnergySaving {
    private EnergySaving() {}

    public static final ResourceKey<Enchantment> KEY = ResourceKey.create(Registries.ENCHANTMENT,
        ResourceLocation.fromNamespaceAndPath("mio_icif", "energy_saving"));
    public static final int MAX_LEVEL = 5;
    /** Percent removed per level. */
    public static final int PERCENT_PER_LEVEL = 5;

    /** Enchantment level on the stack (0 when absent). No registry lookup, no allocation. */
    public static int level(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        ItemEnchantments enchantments = stack.getTagEnchantments();
        if (enchantments.isEmpty()) return 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (entry.getKey().is(KEY)) return Math.min(MAX_LEVEL, entry.getIntValue());
        }
        return 0;
    }

    /** Cost multiplier of a level: 1.0, 0.95, 0.90, 0.85, 0.80, 0.75. */
    public static double factor(int level) {
        return 1.0 - PERCENT_PER_LEVEL * Math.max(0, Math.min(MAX_LEVEL, level)) / 100.0;
    }

    /** The reduced cost of one debit from {@code stack}. */
    public static long apply(ItemStack stack, long amount) {
        return apply(level(stack), amount);
    }

    public static long apply(int level, long amount) {
        if (amount <= 0 || level <= 0) return amount;
        long percent = 100 - (long) PERCENT_PER_LEVEL * Math.min(MAX_LEVEL, level);
        long whole = amount / 100 * percent;
        long rest = amount % 100 * percent;          // 0 .. 9900, in 1/100 EU
        whole += rest / 100;
        long fraction = rest % 100;
        if (fraction > 0 && ThreadLocalRandom.current().nextInt(100) < fraction) whole++;
        return whole;
    }

    /** A per-unit rate (EU per damage point, EU per tick): reduced, never below 1 when it was above 0. */
    public static long rate(ItemStack stack, long rate) {
        int level = level(stack);
        if (rate <= 0 || level <= 0) return rate;
        return Math.max(1, Math.round(rate * factor(level)));
    }
}
