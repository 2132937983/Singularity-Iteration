// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.suit;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.item.EnergySaving;
import com.miophas.singularity_iteration.core.api.item.IElectricArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Shared energy pool of a full electric armor set (0.1.8.0).
 *
 * <h2>Why this class exists</h2>
 * <p>Before 0.1.8.0, each cost was paid by one piece: damage absorption
 * started at the chestplate, and the chestplate carried the Special Maneuver
 * Mode, the deflector and most units. Thus the chestplate was empty while the
 * other pieces were almost full.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>A <b>full set</b> is four worn electric armor pieces (head, chest,
 *       legs, feet) with the same armor tier above 0. Nano (tier 5) and quantum
 *       (tier 6) sets qualify. Addon armor with a common tier also qualifies.</li>
 *   <li>With a full set, a cost is paid from all four pieces in proportion to
 *       the energy that each piece holds. The pieces thus empty together.</li>
 *   <li>Without a full set, the piece that causes the cost pays it alone (the
 *       0.1.7.x rule).</li>
 *   <li>The Energy Saving enchantment of the piece that causes the cost
 *       reduces the cost before the payment.</li>
 * </ul>
 *
 * <p>All methods are server and client safe: they only read and write the
 * energy data component of the worn stacks. A call costs at most four slot
 * reads. No method allocates on the common path except the returned array.
 */
public final class SuitEnergyPool {
    private SuitEnergyPool() { }

    /** The four armor slots, in the order head, chest, legs, feet. Do not modify. */
    public static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    /**
     * Returns the four worn pieces when the entity wears a full set, or
     * {@code null}.
     */
    @Nullable
    public static ItemStack[] fullSet(LivingEntity entity) {
        var api = MioIcifAPI.instance().getItemAPI();
        ItemStack[] pieces = new ItemStack[4];
        int tier = -1;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = entity.getItemBySlot(ARMOR_SLOTS[i]);
            if (stack.isEmpty() || !api.isElectricArmor(stack) || !(stack.getItem() instanceof IElectricArmorItem armor)) return null;
            int pieceTier = armor.getArmorTier();
            if (pieceTier <= 0 || tier >= 0 && pieceTier != tier) return null;
            tier = pieceTier;
            pieces[i] = stack;
        }
        return pieces;
    }

    /** Returns the sum of the stored energy of the pieces. */
    public static long stored(ItemStack[] pieces) {
        var api = MioIcifAPI.instance().getItemAPI();
        long total = 0;
        for (ItemStack piece : pieces) total = saturatingAdd(total, Math.max(0, api.getElectricArmorStored(piece)));
        return total;
    }

    /**
     * Drains {@code amount} EU from the pieces in proportion to their stored
     * energy. The sum is exact: a rounding remainder is taken from the pieces
     * that still hold energy, in slot order.
     *
     * @return the EU that the pieces gave, at most {@code amount}
     */
    public static long drain(ItemStack[] pieces, long amount) {
        if (amount <= 0) return 0;
        var api = MioIcifAPI.instance().getItemAPI();
        long[] held = new long[pieces.length];
        long total = 0;
        for (int i = 0; i < pieces.length; i++) {
            held[i] = Math.max(0, api.getElectricArmorStored(pieces[i]));
            total = saturatingAdd(total, held[i]);
        }
        if (total <= 0) return 0;
        long wanted = Math.min(amount, total), paid = 0;
        for (int i = 0; i < pieces.length; i++) {
            if (held[i] == 0) continue;
            long share = (long) Math.floor((double) wanted * held[i] / total);
            share = Math.min(share, Math.min(held[i], wanted - paid));
            if (share > 0) {
                long taken = api.dischargeElectricArmor(pieces[i], share, false);
                held[i] -= taken;
                paid += taken;
            }
        }
        for (int i = 0; i < pieces.length && paid < wanted; i++) {
            if (held[i] == 0) continue;
            long taken = api.dischargeElectricArmor(pieces[i], Math.min(held[i], wanted - paid), false);
            held[i] -= taken;
            paid += taken;
        }
        return paid;
    }

    /**
     * Returns the energy that is available for a cost of {@code consumer}:
     * the pool total with a full set, else the energy of the consumer.
     */
    public static long available(LivingEntity entity, ItemStack consumer) {
        if (consumer.isEmpty()) return 0;
        ItemStack[] set = fullSet(entity);
        if (set != null && contains(set, consumer)) return stored(set);
        return Math.max(0, MioIcifAPI.instance().getItemAPI().getElectricArmorStored(consumer));
    }

    /**
     * Pays a cost of {@code consumer}. Energy Saving of the consumer applies
     * first. With a full set, the pool pays. Else the consumer pays.
     *
     * @return the EU that was paid after Energy Saving
     */
    public static long pay(LivingEntity entity, ItemStack consumer, long amount) {
        if (consumer.isEmpty() || amount <= 0) return 0;
        long cost = EnergySaving.apply(consumer, amount);
        ItemStack[] set = fullSet(entity);
        if (set != null && contains(set, consumer)) return drain(set, cost);
        return MioIcifAPI.instance().getItemAPI().dischargeElectricArmor(consumer, cost, false);
    }

    /**
     * 0.1.10.5: the most a cost of {@code consumer} can take after its Energy Saving. Ask
     * {@link #canPay} before a cost; never compare the EU that {@link #pay} returns with the
     * nominal cost: with Energy Saving it is lower although the cost was met (up to 0.1.10.4 the
     * guided-shot check failed on an enchanted helmet, and a Wingman in an enchanted suit flew home
     * for low power).
     */
    public static long cost(ItemStack consumer, long amount) {
        return EnergySaving.maxCost(consumer, amount);
    }

    /** 0.1.10.5: true when a cost of {@code consumer} can be paid now (Energy Saving applied). */
    public static boolean canPay(LivingEntity entity, ItemStack consumer, long amount) {
        if (amount <= 0) return true;
        return !consumer.isEmpty() && available(entity, consumer) >= cost(consumer, amount);
    }

    /**
     * 0.1.10.5: pays a cost of {@code consumer} in full or not at all. Energy Saving applies first;
     * a cost that it reduces to 0 is met at no charge.
     *
     * @return true when the cost was met
     */
    public static boolean payAll(LivingEntity entity, ItemStack consumer, long amount) {
        if (amount <= 0) return true;
        if (consumer.isEmpty()) return false;
        long cost = EnergySaving.apply(consumer, amount);
        if (cost <= 0) return true;
        var api = MioIcifAPI.instance().getItemAPI();
        ItemStack[] set = fullSet(entity);
        boolean pooled = set != null && contains(set, consumer);
        long have = pooled ? stored(set) : Math.max(0, api.getElectricArmorStored(consumer));
        if (have < cost) return false;
        long paid = pooled ? drain(set, cost) : api.dischargeElectricArmor(consumer, cost, false);
        return paid >= cost;
    }

    /**
     * 0.1.10.5: pays as much of a cost of {@code consumer} as there is (Energy Saving applied) and
     * returns the part of the nominal {@code amount} that is covered: all of it when the reduced
     * cost was paid, else the paid share. For costs that several payers share (Wingman upkeep).
     */
    public static long payPart(LivingEntity entity, ItemStack consumer, long amount) {
        if (consumer.isEmpty() || amount <= 0) return 0;
        long cost = EnergySaving.apply(consumer, amount);
        if (cost <= 0) return amount;
        ItemStack[] set = fullSet(entity);
        long paid = set != null && contains(set, consumer) ? drain(set, cost)
            : MioIcifAPI.instance().getItemAPI().dischargeElectricArmor(consumer, cost, false);
        return paid >= cost ? amount : (long) Math.floor((double) amount * paid / cost);
    }

    private static boolean contains(ItemStack[] pieces, ItemStack stack) {
        for (ItemStack piece : pieces) if (piece == stack) return true;
        return false;
    }

    private static long saturatingAdd(long left, long right) {
        long sum = left + right;
        return sum < 0 ? Long.MAX_VALUE : sum;
    }
}
