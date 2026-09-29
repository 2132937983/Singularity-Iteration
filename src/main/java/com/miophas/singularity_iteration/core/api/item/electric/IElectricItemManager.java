// SPDX-License-Identifier: Apache-2.0
// Independent SI public declaration. Positional flags do not assume an external API contract.
package com.miophas.singularity_iteration.core.api.item.electric;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface IElectricItemManager {
    long charge(ItemStack stack, long amount, int tier, boolean b0, boolean b1);
    long discharge(ItemStack stack, long amount, int tier, boolean b0, boolean b1, boolean b2);
    long getCharge(ItemStack stack);
    boolean canUse(ItemStack stack, long amount);
    boolean use(ItemStack stack, long amount, LivingEntity entity);
    void chargeFromArmor(ItemStack stack, LivingEntity entity);
    String getToolTip(ItemStack stack);
}
