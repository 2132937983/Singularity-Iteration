// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.mixin;

import com.miophas.singularity_iteration.common.item.EnergyStackDiff;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Charging or draining worn electric armor changes its components every tick; vanilla would treat
 * that as a re-equip and play the equip sound (and emit a game event) each time. A stack that only
 * differs in stored charge is the same piece still being worn.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEquipEnergyMixin {
    @Inject(method = "onEquipItem", at = @At("HEAD"), cancellable = true)
    private void mio_icif$ignoreChargeOnly(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem, CallbackInfo ci) {
        if (EnergyStackDiff.energyOnly(oldItem, newItem)) ci.cancel();
    }
}
