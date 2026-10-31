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
 * <p>0.1.10.38: levels above V (the enchanting table and the anvil stop at V; items from commands
 * or other mods can have more) keep going: each one takes another 5 % off the remaining cost,
 * factor 0.75 x 0.95^(level - 5). The cost gets close to zero but never reaches it.
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
    /** The highest level of the enchanting table and the anvil (energy_saving.json). */
    public static final int MAX_LEVEL = 5;
    /** 0.1.10.38: the highest level that counts (the enchantment level limit of the game). */
    public static final int MAX_EFFECTIVE_LEVEL = 255;
    /** Percent removed per level. */
    public static final int PERCENT_PER_LEVEL = 5;
    /** 0.1.10.38: cost factors in parts per million (the levels above V are not whole percents). */
    private static final long PPM = 1_000_000L;
    private static final long[] FACTOR_PPM = new long[MAX_EFFECTIVE_LEVEL + 1];
    static {
        for (int level = 0; level <= MAX_EFFECTIVE_LEVEL; level++) FACTOR_PPM[level] = Math.round(PPM * factor(level));
    }

    /** Enchantment level on the stack (0 when absent). No registry lookup, no allocation. */
    public static int level(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        ItemEnchantments enchantments = stack.getTagEnchantments();
        if (enchantments.isEmpty()) return 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (entry.getKey().is(KEY)) return Math.min(MAX_EFFECTIVE_LEVEL, entry.getIntValue());
        }
        return 0;
    }

    /** Cost multiplier of a level: 1.0, 0.95, 0.90, 0.85, 0.80, 0.75, then x 0.95 for each level above V. */
    public static double factor(int level) {
        int l = Math.max(0, Math.min(MAX_EFFECTIVE_LEVEL, level));
        if (l <= MAX_LEVEL) return 1.0 - PERCENT_PER_LEVEL * l / 100.0;
        return (1.0 - PERCENT_PER_LEVEL * MAX_LEVEL / 100.0) * Math.pow(1.0 - PERCENT_PER_LEVEL / 100.0, l - MAX_LEVEL);
    }

    /** The reduced cost of one debit from {@code stack}. */
    public static long apply(ItemStack stack, long amount) {
        return apply(level(stack), amount);
    }

    public static long apply(int level, long amount) {
        if (amount <= 0 || level <= 0) return amount;
        long ppm = FACTOR_PPM[Math.min(MAX_EFFECTIVE_LEVEL, level)];
        long whole = amount / PPM * ppm;
        long rest = amount % PPM * ppm;              // 0 .. 10^12, in millionths of an EU
        whole += rest / PPM;
        long fraction = rest % PPM;
        if (fraction > 0 && ThreadLocalRandom.current().nextLong(PPM) < fraction) whole++;
        return whole;
    }

    /**
     * 0.1.10.5: the most that one debit of {@code amount} from {@code stack} can cost (the reduced
     * cost rounded up; {@link #apply} never returns more). An energy check before a debit compares
     * the stored energy with this value. The EU that a debit took must not be compared with the
     * nominal {@code amount}: with Energy Saving it is lower although the cost was met.
     */
    public static long maxCost(ItemStack stack, long amount) {
        return maxCost(level(stack), amount);
    }

    public static long maxCost(int level, long amount) {
        if (amount <= 0 || level <= 0) return amount;
        long ppm = FACTOR_PPM[Math.min(MAX_EFFECTIVE_LEVEL, level)];
        long whole = amount / PPM * ppm;
        long rest = amount % PPM * ppm;
        return whole + (rest + PPM - 1) / PPM;
    }

    /** A per-unit rate (EU per damage point, EU per tick): reduced, never below 1 when it was above 0. */
    public static long rate(ItemStack stack, long rate) {
        int level = level(stack);
        if (rate <= 0 || level <= 0) return rate;
        return Math.max(1, Math.round(rate * factor(level)));
    }
}
