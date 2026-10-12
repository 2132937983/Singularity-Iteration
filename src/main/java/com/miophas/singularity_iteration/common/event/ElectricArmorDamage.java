// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.event;

import com.miophas.singularity_iteration.common.suit.SuitEnergyPool;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.item.EnergySaving;
import com.miophas.singularity_iteration.core.api.item.IElectricArmorItem;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Energy absorption of electric armor (0.1.8.0).
 *
 * <h2>Why this class exists</h2>
 * <p>Before 0.1.8.0 the event handler applied each piece ratio to the damage
 * that the previous piece left. The chestplate took 40 % of the hit, the
 * helmet 15 % of the rest, and so on. A full quantum set (40 + 15 + 30 + 15 =
 * 100 %) thus stopped only about 70 % of a hit. High hits, for example the
 * axe of a vindicator, went through. The chestplate also paid first and
 * emptied first. The quantum set also let all damage types with the tag
 * {@code bypasses_armor} through (magic, wither, freezing, sonic boom).
 *
 * <h2>Rules</h2>
 * <ol>
 *   <li>A piece takes part when it is electric armor with a ratio above 0 and
 *       an energy rate above 0 for its slot.</li>
 *   <li>The total ratio is the sum of the piece ratios (not a product), at
 *       most 1. A full nano set has a total ratio of 0.9.</li>
 *   <li>The absorbed damage is {@code damage * totalRatio}. Each piece is
 *       responsible for the part {@code ratio / sum} of it, at its own energy
 *       rate (Energy Saving applies to the rate).</li>
 *   <li>With a full set ({@link SuitEnergyPool#fullSet}), the pool pays the
 *       total cost in proportion to the energy of each piece. Without a full
 *       set, each piece pays its own part.</li>
 *   <li>When the energy is not sufficient, the absorbed damage is reduced in
 *       proportion to the energy that was paid.</li>
 * </ol>
 *
 * <h2>Damage types</h2>
 * <p>Electric armor absorbs every damage type except: the types with the tag
 * {@code bypasses_invulnerability} (void, {@code /kill}), starvation, and the
 * world border. This includes physical hits, projectiles, explosions, fire,
 * magic, wither, freezing, the sonic boom, thorns and armor-piercing attacks.
 */
public final class ElectricArmorDamage {
    private ElectricArmorDamage() { }

    /** Total ratio of a full nano set. */
    public static final float FULL_NANO_RATIO = 0.9F;
    private static final int NANO_TIER = 5;

    /** The result of one absorption. {@code absorbed} is in damage points. */
    public record Result(float absorbed, long energy) {
        public static final Result NONE = new Result(0, 0);
    }

    /** True when electric armor absorbs this damage type. */
    public static boolean covers(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
            && !source.is(DamageTypes.STARVE)
            && !source.is(DamageTypes.OUTSIDE_BORDER);
    }

    /**
     * Absorbs damage with the energy of the worn electric armor.
     *
     * @param entity the wearer
     * @param source the damage source
     * @param damage the damage after vanilla armor, before electric armor
     * @return the absorbed damage and the energy that was paid
     */
    public static Result absorb(LivingEntity entity, DamageSource source, float damage) {
        if (!(damage > 0) || !covers(source)) return Result.NONE;
        var api = MioIcifAPI.instance().getItemAPI();
        ItemStack[] set = SuitEnergyPool.fullSet(entity);
        ItemStack[] stacks = new ItemStack[4];
        float[] ratios = new float[4];
        long[] rates = new long[4];
        int count = 0;
        float ratioSum = 0;
        boolean allNano = true;
        for (EquipmentSlot slot : SuitEnergyPool.ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !api.isElectricArmor(stack) || !(stack.getItem() instanceof IElectricArmorItem armor)) continue;
            long rate = EnergySaving.rate(stack, armor.getEnergyPerDamage());
            float ratio = armor.getDamageAbsorptionRatio(slot);
            if (rate <= 0 || !(ratio > 0)) continue;
            // A piece of a full set can rely on the pool; another piece needs its own energy.
            if (set == null && api.getElectricArmorStored(stack) <= 0) continue;
            allNano &= armor.getArmorTier() == NANO_TIER;
            stacks[count] = stack;
            ratios[count] = ratio;
            rates[count] = rate;
            ratioSum += ratio;
            count++;
        }
        if (count == 0) return Result.NONE;
        float total = set != null && allNano && count == 4 ? FULL_NANO_RATIO : Math.min(1.0F, ratioSum);
        double wanted = (double) damage * total;
        if (set != null) {
            double cost = 0;
            for (int i = 0; i < count; i++) cost += wanted * ratios[i] / ratioSum * rates[i];
            long need = (long) Math.ceil(cost);
            long have = SuitEnergyPool.stored(set);
            long paid = SuitEnergyPool.drain(set, Math.min(need, have));
            double fraction = need <= 0 ? 1.0 : Math.min(1.0, (double) paid / need);
            return new Result((float) (wanted * fraction), paid);
        }
        double absorbed = 0;
        long paidTotal = 0;
        for (int i = 0; i < count; i++) {
            double share = wanted * ratios[i] / ratioSum;
            long need = (long) Math.ceil(share * rates[i]);
            long have = Math.max(0, api.getElectricArmorStored(stacks[i]));
            long paid = api.dischargeElectricArmor(stacks[i], Math.min(need, have), false);
            paidTotal += paid;
            absorbed += need <= 0 ? share : share * Math.min(1.0, (double) paid / need);
        }
        return new Result((float) Math.min(damage, absorbed), paidTotal);
    }
}
