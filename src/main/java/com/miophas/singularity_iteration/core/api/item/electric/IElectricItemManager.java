// SPDX-License-Identifier: Apache-2.0
// Independent SI public declaration. Positional flags do not assume an external API contract.
package com.miophas.singularity_iteration.core.api.item.electric;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 0.1.11.194 (T1, I-05): the positional flags, as every implementation in SI reads them:
 * {@code charge(stack, amount, tier, ignoreTransferLimit, simulate)} and
 * {@code discharge(stack, amount, tier, ignoreTransferLimit, externally, simulate)}. {@code externally} is a machine or
 * another item draining the stack; an electric tool never provides energy that way. Tier is informative only (tiers do
 * not gate charging in SI).
 */
public interface IElectricItemManager {
    long charge(ItemStack stack, long amount, int tier, boolean b0, boolean b1);
    long discharge(ItemStack stack, long amount, int tier, boolean b0, boolean b1, boolean b2);
    long getCharge(ItemStack stack);
    boolean canUse(ItemStack stack, long amount);
    boolean use(ItemStack stack, long amount, LivingEntity entity);
    void chargeFromArmor(ItemStack stack, LivingEntity entity);
    String getToolTip(ItemStack stack);
}
