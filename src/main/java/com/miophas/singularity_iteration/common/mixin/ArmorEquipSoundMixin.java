// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.mixin;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code ArmorItem#getEquipSound()} is assumed non-null by footstep / sound mods (Presence
 * Footsteps resolves {@code bootItem.getEquipSound().value()} every step). An armour material
 * registered without an equip sound - by any mod - crashed them with a NullPointerException as
 * soon as such boots were worn. Every armour item now falls back to the generic equip sound.
 */
@Mixin(ArmorItem.class)
public abstract class ArmorEquipSoundMixin {
    @Inject(method = "getEquipSound", at = @At("RETURN"), cancellable = true)
    private void mio_icif$neverNull(CallbackInfoReturnable<Holder<SoundEvent>> cir) {
        if (cir.getReturnValue() == null) cir.setReturnValue(SoundEvents.ARMOR_EQUIP_GENERIC);
    }
}
